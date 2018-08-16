package org.coreocto.dev.jsocs.rest.nio;

import com.cloudrail.si.exceptions.ParseException;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.PCloud;
import com.cloudrail.si.types.SpaceAllocation;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.coreocto.dev.jsocs.rest.Constant;
import org.coreocto.dev.jsocs.rest.cloudrail.CustomLocalReceiver;
import org.coreocto.dev.jsocs.rest.cloudrail.OneDriveForBusiness;
import org.coreocto.dev.jsocs.rest.config.AppConfig;
import org.coreocto.dev.jsocs.rest.exception.*;
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
import java.util.concurrent.TimeUnit;

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

    private Map<Integer, CloudStorage> storageMap = new HashMap<>();
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

            CloudStorage cloudStorage = null;

            if (account.getCactive()==null || account.getCactive().equals(0)){
                continue;
            }

            if (account.getCtype()!=null && account.getCtype().equalsIgnoreCase("pcloud")) {

                cloudStorage = new PCloud(
                        new CustomLocalReceiver(DEFAULT_CALLBACK_PORT, "<h1>Please close this window!</h1>", appConfig.APP_WEBDRIVER_FIREFOX),
                        appConfig.APP_PCLOUD_CLIENT_ID,
                        appConfig.APP_PCLOUD_CLIENT_SECRET,
                        "http://localhost:" + DEFAULT_CALLBACK_PORT + "/auth",
                        ""
                );
            }else if (account.getCtype()!=null && account.getCtype().equalsIgnoreCase("onedrive for business")){
                cloudStorage = new OneDriveForBusiness(
                        new CustomLocalReceiver(DEFAULT_CALLBACK_PORT, "<h1>Please close this window!</h1>", appConfig.APP_WEBDRIVER_FIREFOX),
                        appConfig.APP_ONEDRIVE_FOR_BUSINESS_CLIENT_ID,
                        appConfig.APP_ONEDRIVE_FOR_BUSINESS_CLIENT_SECRET,
                        "http://localhost:" + DEFAULT_CALLBACK_PORT + "/auth",
                        "");
            }else if (account.getCtype()!=null && account.getCtype().equalsIgnoreCase("onedrive")){
//                cloudStorage = new OneDrive(
//                        new CustomLocalReceiver(DEFAULT_CALLBACK_PORT, "<h1>Please close this window!</h1>", appConfig.APP_WEBDRIVER_FIREFOX),
//                        appConfig.APP_ONEDRIVE_FOR_BUSINESS_CLIENT_ID,
//                        "fgpZARO538?:npgtCJP02^?",
//                        "http://localhost:" + DEFAULT_CALLBACK_PORT + "/auth",
//                        "");
            }

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

            if (!(cloudStorage instanceof OneDriveForBusiness)){
                if (saveCred) {
                    String crToken = cloudStorage.saveAsString();
                    account.setCcrtoken(crToken);
                    accountRepo.save(account);
                }
            }

            synchronized (storageMap) {
                storageMap.put(account.getCid(), cloudStorage);
            }
        }
    }

    public Map.Entry<Integer, CloudStorage> getNextAvailableStorage() {
        Map.Entry<Integer, CloudStorage> result = null;

        synchronized (storageMap) {
            for (Map.Entry<Integer, CloudStorage> entry : storageMap.entrySet()) {
                if (entry.getValue() instanceof OneDriveForBusiness){   //workaround for ODfB
                    result = entry;
                    break;
                }else {
                    SpaceAllocation spaceAllocation = entry.getValue().getAllocation();
                    long availableSpace = spaceAllocation.getTotal() - spaceAllocation.getUsed();
                    if (availableSpace >= BLOCKSIZE) {
                        result = entry;
                        break;
                    }
                }
            }
        }

        return result;
    }

    /**
     * Return a list of files of given folder
     *
     * @param path
     * @return children of given folder
     * @throws FolderNotFoundException
     */
    public List<ExtendedFileEntry> list(String path) throws FolderNotFoundException {
        String newPath = null;
        if (path == null) {
            newPath = Constant.PATH_SEP;
        } else {
            newPath = path;
        }

        boolean exists = extendedFileRepo.existsByPath(newPath);

        if (!exists) {
            throw new FolderNotFoundException(path);
        }

        return extendedFileRepo.findFileEntriesByPath(newPath);
    }

    /**
     * Save the file object to the given path
     *
     * @param virtualPath the path to be saved to
     * @param file        the file to be saved
     * @throws IOException
     */
    public void save(String virtualPath, java.io.File file) throws IOException, InvalidCryptoParamException {

        init();

        String parentPath = FilenameUtils.getFullPathNoEndSeparator(virtualPath);
        String fileName = FilenameUtils.getName(virtualPath);

        boolean parentExists = extendedFileRepo.existsByPath(parentPath);

        if (!parentExists) {
            throw new FolderNotFoundException(parentPath);
        }

        boolean targetExists = extendedFileRepo.existsByPath(virtualPath);

        if (targetExists) {
            throw new FileAlreadyExistsException(virtualPath);
        }

        if (!file.exists()) {
            throw new SourceFileNotFoundException(file.getAbsolutePath());
        }

        if (parentPath.isEmpty()) {
            parentPath = Constant.PATH_SEP;
        }

        long fileSz = file.length();

        int requiredBlockCnt = (int) (fileSz % BLOCKSIZE == 0 ? (fileSz / BLOCKSIZE) : (fileSz / BLOCKSIZE) + 1);

        ExtendedFileEntry parentEntry = extendedFileRepo.findFileEntryByPath(parentPath);

        FileEntry fileEntry = new FileEntry();
        fileEntry.setCparent(parentEntry.getCid());
        fileEntry.setCisdir(0);
        fileEntry.setCname(fileName);
        fileEntry.setCsize(fileSz);
        fileEntry.setCcrtdt(new Date());
        fileEntry.setClastlock(new Date());
        fileEntry = fileRepo.save(fileEntry);

        File tmpDir = new File(appConfig.APP_TEMP_DIR);

        java.io.File blockFile = null;

        try {
            blockFile = java.io.File.createTempFile("jsocs-", ".tmp", tmpDir);  //this temp file will be reused
        } finally {

        }

        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {

            for (int i = 0; i < requiredBlockCnt; i++) {
                String id = UUID.randomUUID().toString();

                Map.Entry<Integer, CloudStorage> entry = this.getNextAvailableStorage();

                if (entry == null) {
                    throw new InsufficientSpaceException();
                } else {

                    long bytesToCopy = BLOCKSIZE;

                    if (i == requiredBlockCnt - 1 && fileSz % BLOCKSIZE > 0) {
                        bytesToCopy = fileSz % BLOCKSIZE;
                    }

                    try (BufferedOutputStream out = new BufferedOutputStream(new CipherOutputStream(new FileOutputStream(blockFile), getCipher(Cipher.ENCRYPT_MODE)))) {
                        IOUtils.copyLarge(in, out, 0, bytesToCopy);

                        if (i == requiredBlockCnt - 1 && fileSz % BLOCKSIZE > 0) {
                            for (int j = 0; j < BLOCKSIZE - bytesToCopy; j++) {
                                out.write(0);
                            }
                        }

                        out.flush();

                    } catch (IOException ex) {
                        throw new CannotWriteTempFileException(blockFile.getAbsolutePath());
                    } catch (NoSuchPaddingException | InvalidKeyException | NoSuchAlgorithmException | InvalidAlgorithmParameterException ex) {
                        throw new InvalidCryptoParamException();
                    }

                    String xFileName = Constant.PATH_SEP + id;

                    fileEntry.setClastlock(new Date()); //maintain the lock on the file
                    fileRepo.save(fileEntry);

                    do {
                        CloudStorage remoteStorage = entry.getValue();

                        try (InputStream is = new BufferedInputStream(new FileInputStream(blockFile))) {
                            remoteStorage.upload(xFileName, is, BLOCKSIZE, false);

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

                            break;

                        } catch (IOException | RuntimeException ex) {
                            throw new FileUploadException();
                        }

                    } while (true);
                }
            }

            fileEntry.setClastlock(null);
            fileRepo.save(fileEntry);

        } catch (IOException ex) {
            if (fileEntry != null) {
                fileRepo.deleteById(fileEntry.getCid());
            }
            throw ex;
        } finally {
            blockFile.delete();
            file.delete();
        }
    }

    /**
     * Extract & save file content to target file
     *
     * @param virtualPath
     * @param out
     * @throws IOException
     * @throws InvalidCryptoParamException
     */
    public void extract(String virtualPath, java.io.File out) throws IOException, InvalidCryptoParamException {

        init();

        boolean targetExists = extendedFileRepo.existsByPath(virtualPath);

        if (!targetExists) {
            throw new FileNotFoundException(virtualPath);
        }

        long fileSize = -1;

        ExtendedFileEntry fileEntry = extendedFileRepo.findFileEntryByPath(virtualPath);

        if (fileEntry != null) {

            if (fileEntry.getClastlock() != null) {
                Date curTime = new Date();
                long diff = curTime.getTime() - fileEntry.getClastlock().getTime();
                long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
                if (minutes <= 5) {
                    throw new FileLockedExeption();
                }
            }

            fileSize = fileEntry.getCsize();

            List<Block> fileBlocks = blockRepo.findBlocksByFileId(fileEntry.getCid());

            if (out.exists()) {
                out.delete();
            }

            try (BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(out, true))) {

                int blockCnt = fileBlocks.size();

                for (int i = 0; i < blockCnt; i++) {

                    Block curBlock = fileBlocks.get(i);

                    CloudStorage cloudStorage = null;

                    synchronized (storageMap) {
                        cloudStorage = storageMap.get(curBlock.getCaccid());
                    }

                    try (BufferedInputStream in = new BufferedInputStream(new CipherInputStream(cloudStorage.download(curBlock.getCdirectlink()), getCipher(Cipher.DECRYPT_MODE)))) {
                        long byteCnt = (i == fileBlocks.size() - 1) ? fileSize % BLOCKSIZE : BLOCKSIZE;
                        IOUtils.copyLarge(in, outStream, 0, byteCnt);
                    } catch (NoSuchPaddingException | InvalidKeyException | NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
                        throw new InvalidCryptoParamException();
                    } finally {

                    }
                }

            } finally {

            }
        }
    }

    /**
     * Delete file or folder of given path
     *
     * @param virtualPath
     * @throws IOException
     */
    public void delete(String virtualPath) throws IOException {

        init();

        boolean targetExists = extendedFileRepo.existsByPath(virtualPath);

        if (!targetExists) {
            throw new FileNotFoundException(virtualPath);
        }

        ExtendedFileEntry fileEntry = extendedFileRepo.findFileEntryByPath(virtualPath);

        if (fileEntry != null) {

            if (fileEntry.getClastlock() != null) {
                Date curTime = new Date();
                long diff = curTime.getTime() - fileEntry.getClastlock().getTime();
                long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
                if (minutes <= 5) {
                    throw new FileLockedExeption();
                }
            }

            int fileId = fileEntry.getCid();
            fileRepo.deleteById(fileId);

            if (new Integer(0).equals(fileEntry.getCisdir())) {
                List<FileTable> fileTables = fileTableRepo.findByCfileid(fileId);

                for (FileTable entry : fileTables) {

                    Optional<Block> oBlock = blockRepo.findById(entry.getId().getCblkid());

                    if (oBlock.isPresent()) {
                        Block block = oBlock.get();

                        synchronized (storageMap) {
                            CloudStorage cloudStorage = storageMap.get(block.getCaccid());
                            cloudStorage.delete(block.getCdirectlink());  //delete from cloud storage
                        }

                        blockRepo.deleteById(block.getCid());

                    }
                }

                fileTableRepo.deleteByCfileid(fileId);
            }
        }
    }

    /**
     * Create a directory on given path
     *
     * @param path
     * @param dirName
     * @throws FolderNotFoundException
     * @throws FileAlreadyExistsException
     */
    public void makeDir(String path, String dirName) throws FolderNotFoundException, FileAlreadyExistsException {
        boolean parentExists = extendedFileRepo.existsByPath(path);

        if (!parentExists) {
            throw new FolderNotFoundException(path);
        }

        boolean targetExists = extendedFileRepo.existsByPath(path + dirName);

        if (targetExists) {
            throw new FolderAlreadyExistsException(path + dirName);
        }

        ExtendedFileEntry fileEntry = extendedFileRepo.findFileEntryByPath(path);

        if (fileEntry != null) {
            FileEntry newEntry = new FileEntry();
            newEntry.setCname(dirName);
            newEntry.setCisdir(1);
            newEntry.setCparent(fileEntry.getCid());
            fileRepo.save(newEntry);
        }
    }
}
