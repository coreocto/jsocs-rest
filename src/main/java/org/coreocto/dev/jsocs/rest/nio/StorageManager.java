package org.coreocto.dev.jsocs.rest.nio;

import com.cloudrail.si.exceptions.ParseException;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.PCloud;
import com.cloudrail.si.types.SpaceAllocation;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.coreocto.dev.jsocs.rest.Constant;
import org.coreocto.dev.jsocs.rest.cloudrail.CustomLocalReceiver;
import org.coreocto.dev.jsocs.rest.config.AppConfig;
import org.coreocto.dev.jsocs.rest.exception.InsufficientSpaceAvailableException;
import org.coreocto.dev.jsocs.rest.exception.InvalidChecksumException;
import org.coreocto.dev.jsocs.rest.pojo.*;
import org.coreocto.dev.jsocs.rest.repo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.*;

@Service
public class StorageManager {

    public static final int BLOCKSIZE = 64 * 1024 * 1024;
    private static final int DEFAULT_CALLBACK_PORT = 8082;
    private final Logger logger = LoggerFactory.getLogger(StorageManager.class);

    @Autowired
    AppConfig appConfig;

    @Autowired
    FileRepo fileRepo;

    @Autowired
    ExtendedFileRepo extendedFileRepo;

    @Autowired
    BlockRepo blockRepo;

    @Autowired
    AccountRepo accountRepo;

    @Autowired
    FileTableRepo fileTableRepo;

    private Map<Integer, CloudStorage> storageMap = new HashMap<Integer, CloudStorage>();
    private Boolean init = false;

    public Cipher getCipher(int mode) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        byte[] iv_key = appConfig.APP_ENCRYPT_KEY.getBytes();
        IvParameterSpec iv = new IvParameterSpec(iv_key);
        SecretKeySpec m_keySpec = new SecretKeySpec(iv_key, "AES");
        Cipher m_cipher = javax.crypto.Cipher.getInstance("AES/CTR/NoPadding");
        m_cipher.init(mode, m_keySpec, iv);
        return m_cipher;
    }

    public void init() {

        synchronized (init) {
            if (init) {
                return;
            }
        }

        synchronized (init) {
            init = true;
        }

        for (Account account : accountRepo.findAll()) {

            CloudStorage cloudStorage = new PCloud(
                    new CustomLocalReceiver(DEFAULT_CALLBACK_PORT),
                    appConfig.APP_PCLOUD_CLIENT_ID,
                    appConfig.APP_PCLOUD_CLIENT_SECRET,
                    "http://localhost:" + DEFAULT_CALLBACK_PORT + "/auth",
                    ""
            );

            boolean saveCred = false;

            if (account.getCcrtoken() != null) {
                try {
                    cloudStorage.loadAsString(account.getCcrtoken());
                } catch (ParseException e) {
                    logger.debug("error when loading saved credential", e);
                    saveCred = true;
                }
            } else {
                saveCred = true;
            }

            cloudStorage.login();

            if (saveCred) {
                String crToken = cloudStorage.saveAsString();
                account.setCcrtoken(crToken);
                accountRepo.save(account);
            }

            synchronized (storageMap) {
                storageMap.put(account.getCid(), cloudStorage);
            }
        }
    }

    public Map.Entry<Integer, CloudStorage> getAvailableStorage() {
        Map.Entry<Integer, CloudStorage> result = null;

        synchronized (storageMap) {
            for (Map.Entry<Integer, CloudStorage> entry : storageMap.entrySet()) {
                SpaceAllocation spaceAllocation = entry.getValue().getAllocation();
                long availableSpace = spaceAllocation.getTotal() - spaceAllocation.getUsed();
                if (availableSpace >= BLOCKSIZE) {
                    result = entry;
                    break;
                }
            }
        }

        return result;
    }

    public void save(String virtualPath, java.io.File file) throws IOException {

        init();

        String parentPath = FilenameUtils.getFullPathNoEndSeparator(virtualPath);
        String fileName = FilenameUtils.getName(virtualPath);

        if (parentPath.isEmpty()) {
            parentPath = Constant.PATH_SEP;
        }

        if (isFileExists(parentPath, fileName)) {
            throw new FileAlreadyExistsException(virtualPath);
        }

        long fileSz = file.length();

        int requiredBlks = (int) (fileSz % BLOCKSIZE == 0 ? (fileSz / BLOCKSIZE) : (fileSz / BLOCKSIZE) + 1);

        ExtendedFileEntry parentEntry = extendedFileRepo.findFileEntryByPath(parentPath);

//        FileEntry parentEntry = fileService.getByPath(parentPath);

        FileEntry fileEntry = new FileEntry();
        fileEntry.setCparent(parentEntry.getCid());
        fileEntry.setCisdir(0);
        fileEntry.setCname(fileName);
        fileEntry.setCsize(fileSz);
        fileEntry.setCcrtdt(new Date());
        fileEntry = fileRepo.save(fileEntry);

//        fileEntry = fileService.getByParentAndName(parentEntry.getCid(), fileName);

        File tmpDir = new File(appConfig.APP_TEMP_DIR);

        java.io.File blockFile = java.io.File.createTempFile("jsocs-", ".tmp", tmpDir);    //this temp file will be reused

//        InputStream is = new FileInputStream(file);

//        if (fileSz < (requiredBlks * BLOCKSIZE)) {
//            is = new SequenceInputStream(is, new DummyInputStream());
//        }

        IOException ioException = null;
        RuntimeException runtimeException = null;

        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {

            for (int i = 0; i < requiredBlks; i++) {
                String id = UUID.randomUUID().toString();

                Map.Entry<Integer, CloudStorage> entry = this.getAvailableStorage();

                if (entry == null) {
                    throw new InsufficientSpaceAvailableException();
                } else {

                    boolean abort = false;

                    long bytesToCopy = BLOCKSIZE;

                    if (i == requiredBlks - 1 && fileSz % BLOCKSIZE > 0) {
                        bytesToCopy = fileSz % BLOCKSIZE;
                    }

                    try (BufferedOutputStream out = new BufferedOutputStream(new CipherOutputStream(new FileOutputStream(blockFile), getCipher(Cipher.ENCRYPT_MODE)))) {
                        IOUtils.copyLarge(in, out, 0, bytesToCopy);

                        if (i == requiredBlks - 1 && fileSz % BLOCKSIZE > 0) {
                            for (int j = 0; j < BLOCKSIZE - bytesToCopy; j++) {
                                out.write(0);
                            }
                        }

                        out.flush();

                    } catch (IOException ex) {
                        logger.error("error when copying file content to temporary data block", ex);
                        throw ex;
                    } catch (NoSuchPaddingException | InvalidKeyException | NoSuchAlgorithmException | InvalidAlgorithmParameterException ex) {
                        logger.error("encryption of output stream failed, please check the parameters", ex);
                        throw ex;
                    }

                    String xFileName = Constant.PATH_SEP + id;

                    do {
                        CloudStorage remoteStorage = entry.getValue();

                        try {
                            remoteStorage.upload(xFileName, new BufferedInputStream(new FileInputStream(blockFile)), BLOCKSIZE, false);

                            Block newEntry = new Block();
                            newEntry.setCname(id);
                            newEntry.setCsize(BLOCKSIZE);
                            newEntry.setCaccid(entry.getKey());
                            newEntry.setCdirectlink(xFileName);
                            newEntry.setCuse(1);

                            newEntry = blockRepo.save(newEntry);

                            FileTableId fileTableId = new FileTableId();
                            fileTableId.setCfileid(fileEntry.getCid());
                            fileTableId.setCblkid(newEntry.getCid());

                            FileTable fileTable = new FileTable();
                            fileTable.setId(fileTableId);

                            fileTableRepo.save(fileTable);

//                            fileTableService.create(fileEntry.getCid(), newEntry.getCid());

                            break;

                        } catch (InvalidChecksumException ex) {
                            remoteStorage.delete(xFileName);
                            logger.debug("mismatch between local & remote checksum, retrying...");
                        } catch (IOException ex) {
                            if (ex.getMessage().contains("incomplete output stream")) {  // i always receive this exception when uploading large files
                                logger.error("error when uploading files to remote storage", ex);
//                                overwrite = true;
                                continue;
                            } else {
                                throw ex;
                            }
                        } catch (RuntimeException ex) {
                            if (ex.getMessage().contains("ServiceCode Error")) {
                                logger.error("error when uploading files to remote storage", ex);
//                                overwrite = true;
                                continue;
                            } else {
                                throw ex;
                            }
                        }

                    } while (true);
                }
            }
        } catch (IOException ex) {
            ioException = ex;
        } catch (RuntimeException ex) {
            runtimeException = ex;
        } catch (NoSuchPaddingException | InvalidKeyException | NoSuchAlgorithmException | InvalidAlgorithmParameterException ex) {

        } finally {
            blockFile.delete();
        }

        file.delete();

        if (ioException != null || runtimeException != null) {
            logger.debug("error occurred, deleting associated records from database");
            fileRepo.deleteById(fileEntry.getCid());
//            fileService.deleteById(fileEntry.getCid());
            if (ioException != null) throw ioException;
            if (runtimeException != null) throw runtimeException;
        }
    }

    private boolean isFileExists(String parentPath, String fileName) {
        boolean fileExists = true;

        String newParentPath = (parentPath.endsWith(Constant.PATH_SEP) ? parentPath : parentPath + Constant.PATH_SEP);

        ExtendedFileEntry entry = extendedFileRepo.findFileEntryByPath(newParentPath + fileName);

        //check if the given path already exists
//        try {
//            FileEntry parent = fileService.getByPath(parentPath);
//            fileService.getByParentAndName(parent.getCid(), fileName);
//        } catch (Exception ex) {
//            fileExists = false;
//        }

        return entry != null;
    }

    public void extract(String virtualPath, java.io.File out) throws IOException {

        init();

        String parentPath = FilenameUtils.getFullPathNoEndSeparator(virtualPath);
        String fileName = FilenameUtils.getName(virtualPath);

        if (!isFileExists(parentPath, fileName)) {
            logger.debug("file not exists: " + virtualPath);
            throw new FileNotFoundException();
        }

        long fileSize = -1;

//        FileEntry fileEntry = null;
//
//        try {
//            FileEntry parent = fileService.getByPath(parentPath);
//            fileEntry = fileService.getByParentAndName(parent.getCid(), fileName);
//        } catch (Exception ex) {
//        }

        ExtendedFileEntry fileEntry = extendedFileRepo.findFileEntryByPath(virtualPath);

        if (fileEntry != null) {

            fileSize = fileEntry.getCsize();

            List<Block> fileBlocks = blockRepo.findBlocksByFileId(fileEntry.getCid());

//            List<Block> fileBlocks = blockService.getByFileId(fileEntry.getCid());

            //provision input stream from occupied blocks and concatenate them together according to their order (defined by cid)
//            InputStream in = null;
//            InputStream prevInputStream = null;

            if (out.exists()) {
                out.delete();
            }

            File tmpDir = new File(appConfig.APP_TEMP_DIR);

            try (BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(out, true))) {

//                File tmpFile = File.createTempFile("jsocs-", ".tmp", tmpDir);

                for (int i = 0; i < fileBlocks.size(); i++) {

                    logger.debug("reading block-" + i);

                    Block curBlock = fileBlocks.get(i);

                    CloudStorage cloudStorage = null;

                    synchronized (storageMap) {
                        cloudStorage = storageMap.get(curBlock.getCaccid());
                    }

                    try (BufferedInputStream in = new BufferedInputStream(new CipherInputStream(cloudStorage.download(curBlock.getCdirectlink()), getCipher(Cipher.DECRYPT_MODE)))) {
                        long byteCnt = (i == fileBlocks.size() - 1) ? fileSize % BLOCKSIZE : BLOCKSIZE;
                        IOUtils.copyLarge(in, outStream, 0, byteCnt);
                    } catch (NoSuchPaddingException | InvalidKeyException | NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
                        logger.error("decryption of input stream failed, please check the parameters", e);
                    } finally {

                    }
                }

            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public void delete(String virtualPath) throws IOException {

        init();

        String parentPath = FilenameUtils.getFullPathNoEndSeparator(virtualPath);
        String fileName = FilenameUtils.getName(virtualPath);

        if (!isFileExists(parentPath, fileName)) {
            throw new FileNotFoundException();
        }

        ExtendedFileEntry fileEntry = extendedFileRepo.findFileEntryByPath(virtualPath);

//        FileEntry parent = fileService.getByPath(parentPath);
//        FileEntry fileEntry = fileService.getByParentAndName(parent.getCid(), fileName);

        int fileId = fileEntry.getCid();
        fileRepo.deleteById(fileId);
//        fileService.deleteById(fileId);

        if (new Integer(0).equals(fileEntry.getCisdir())) {
            List<FileTable> fileTables = fileTableRepo.findByCfileid(fileId);
//            List<FileTable> fileTables = fileTableService.getByFileId(fileId);

            for (FileTable entry : fileTables) {

                Optional<Block> oBlock = blockRepo.findById(entry.getId().getCblkid());

                if (oBlock.isPresent()) {
                    Block block = oBlock.get();

                    synchronized (storageMap) {
                        CloudStorage cloudStorage = storageMap.get(block.getCid());
                        cloudStorage.delete(block.getCdirectlink());  //delete from cloud storage
                    }

                    blockRepo.deleteById(block.getCid());

                }

//                blockService.update(entry.getCblkid(), false);
            }

            fileTableRepo.deleteByCfileid(fileId);

//            fileTableService.deleteByFileId(fileId);
        }
    }

    public void makeDir(String path, String dirName) {

        logger.debug("path = " + path);
        logger.debug("dirName = " + dirName);

//        FileEntry fileEntry = null;

        try {

            ExtendedFileEntry fileEntry = extendedFileRepo.findFileEntryByPath(path);

//            fileEntry = fileService.getByPath(path);    //can throw file entry not found

            FileEntry newEntry = new FileEntry();
            newEntry.setCname(dirName);
            newEntry.setCisdir(1);
            newEntry.setCparent(fileEntry.getCid());
            fileRepo.save(newEntry);

        } catch (Exception ex) {

        }
    }
}
