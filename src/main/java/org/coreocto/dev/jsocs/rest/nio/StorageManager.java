package org.coreocto.dev.jsocs.rest.nio;

import com.cloudrail.si.exceptions.ParseException;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.services.PCloud;
import com.cloudrail.si.types.SpaceAllocation;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.coreocto.dev.jsocs.rest.Constant;
import org.coreocto.dev.jsocs.rest.cloudrail.CustomLocalReceiver;
import org.coreocto.dev.jsocs.rest.config.AppConfig;
import org.coreocto.dev.jsocs.rest.exception.*;
import org.coreocto.dev.jsocs.rest.msgraph.OneDriveForBusiness;
import org.coreocto.dev.jsocs.rest.pojo.*;
import org.coreocto.dev.jsocs.rest.repo.*;
import org.coreocto.dev.jsocs.rest.util.CipherUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.nio.file.FileAlreadyExistsException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/*
    Date                        Description
========================================================================================
    XXXX-XX-XX                  Initial release
    2018-09-19                  Remove intermediate file when preparing encrypted file for upload
 */

@Service
public class StorageManager {

    private static final int DEFAULT_CALLBACK_PORT = 8082;
    private final Logger logger = LoggerFactory.getLogger(StorageManager.class);
    private final ReadWriteLock rwlock = new ReentrantReadWriteLock();

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

    @Autowired
    private CipherUtil cipherUtil;

    private Base64 base64 = new Base64();

    private Map<Integer, CloudStorage> storageMap = new HashMap<>();
    private Boolean init = false;

    public void init() {

        rwlock.writeLock().lock();
        try {
            if (init) {
                return;
            } else {
                init = true;
            }
        } finally {
            rwlock.writeLock().unlock();
        }

        for (Account account : accountRepo.findAll()) {

            CloudStorage cloudStorage = null;

            if (account.getCactive() == null || account.getCactive().equals(0)) {
                continue;
            }

            if (account.getCtype() != null && account.getCtype().equalsIgnoreCase("pcloud")) {

                cloudStorage = new PCloud(
                        new CustomLocalReceiver(DEFAULT_CALLBACK_PORT, "<h1>Please close this window!</h1>", appConfig.APP_WEBDRIVER_PATH),
                        appConfig.APP_PCLOUD_CLIENT_ID,
                        appConfig.APP_PCLOUD_CLIENT_SECRET,
                        "http://localhost:" + DEFAULT_CALLBACK_PORT + "/auth",
                        ""
                );
            } else if (account.getCtype() != null && account.getCtype().equalsIgnoreCase("onedrive for business")) {
                cloudStorage = new OneDriveForBusiness(
                        new CustomLocalReceiver(DEFAULT_CALLBACK_PORT, "<h1>Please close this window!</h1>", appConfig.APP_WEBDRIVER_PATH),
                        appConfig.APP_ONEDRIVE_FOR_BUSINESS_CLIENT_ID,
                        appConfig.APP_ONEDRIVE_FOR_BUSINESS_CLIENT_SECRET,
                        "http://localhost:" + DEFAULT_CALLBACK_PORT + "/auth",
                        "");
            } else {
                throw new UnsupportedOperationException("unsupported cloud service provider");
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

            if (!(cloudStorage instanceof OneDriveForBusiness)) {
                if (saveCred) {
                    String crToken = cloudStorage.saveAsString();
                    account.setCcrtoken(crToken);
                    accountRepo.save(account);
                }
            }

            rwlock.writeLock().lock();
            try {
                storageMap.put(account.getCid(), cloudStorage);
            } finally {
                rwlock.writeLock().unlock();
            }
        }
    }

    public Map.Entry<Integer, CloudStorage> getNextAvailableStorage() {
        Map.Entry<Integer, CloudStorage> result = null;

        rwlock.writeLock().lock();
        try {
            for (Map.Entry<Integer, CloudStorage> entry : storageMap.entrySet()) {
                if (entry.getValue() instanceof OneDriveForBusiness) {   //workaround for ODfB
                    result = entry;
                    break;
                } else {
                    SpaceAllocation spaceAllocation = entry.getValue().getAllocation();
                    long availableSpace = spaceAllocation.getTotal() - spaceAllocation.getUsed();
                    if (availableSpace >= Constant.FILE_BLOCKSIZE) {
                        result = entry;
                        break;
                    }
                }
            }
        } finally {
            rwlock.writeLock().unlock();
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

    public void save(String virtualPath, InputStream inputStream, long fileSz) throws IOException, InvalidCryptoParamException {
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

        if (parentPath.isEmpty()) {
            parentPath = Constant.PATH_SEP;
        }

        final long blockSize = fileName.endsWith(".mp4") ? Constant.VIDEO_BLOCKSIZE : Constant.FILE_BLOCKSIZE;

        int requiredBlockCnt = (int) (fileSz % blockSize == 0 ? (fileSz / blockSize) : (fileSz / blockSize) + 1);

        ExtendedFileEntry parentEntry = extendedFileRepo.findFileEntryByPath(parentPath);

        FileEntry fileEntry = new FileEntry();
        fileEntry.setCparent(parentEntry.getCid());
        fileEntry.setCisdir(0);
        fileEntry.setCname(fileName);
        fileEntry.setCsize(fileSz);
        Date curTime = Calendar.getInstance().getTime();
        fileEntry.setCcrtdt(curTime);
        fileEntry.setClastlock(curTime);
        fileEntry = fileRepo.save(fileEntry);

        Map.Entry<Integer, CloudStorage> entry = this.getNextAvailableStorage();

        try (BufferedInputStream in = new BufferedInputStream(inputStream)) {

            for (int i = 0; i < requiredBlockCnt; i++) {
                String id = UUID.randomUUID().toString();

                long bytesToCopy = blockSize;

                if (i == requiredBlockCnt - 1 && fileSz % blockSize > 0) {
                    bytesToCopy = fileSz % blockSize;
                }

                byte[] ivBytes = new byte[16];
                new SecureRandom().nextBytes(ivBytes);

//                try (BufferedOutputStream out = new BufferedOutputStream(new CipherOutputStream(new FileOutputStream(blockFile), cipherUtil.getCipher(Cipher.ENCRYPT_MODE, ivBytes)))) {
//                    IOUtils.copyLarge(in, out, 0, bytesToCopy);
//
//                    if (i == requiredBlockCnt - 1 && fileSz % blockSize > 0) {
//                        for (int j = 0; j < blockSize - bytesToCopy; j++) {
//                            out.write(0);
//                        }
//                    }
//
//                    out.flush();
//
//                } catch (IOException ex) {
//                    throw new CannotWriteTempFileException(blockFile.getAbsolutePath());
//                } catch (NoSuchPaddingException | InvalidKeyException | NoSuchAlgorithmException | InvalidAlgorithmParameterException ex) {
//                    throw new InvalidCryptoParamException();
//                }

                String xFileName = Constant.PATH_SEP + id;

                fileEntry.setClastlock(Calendar.getInstance().getTime()); //maintain the lock on the file
                fileRepo.save(fileEntry);

                Block newEntry = new Block();
                newEntry.setCname(id);
                newEntry.setCsize(blockSize);
                newEntry.setCaccid(entry.getKey());
                newEntry.setCdirectlink(xFileName);
                newEntry.setCuse(1);
                newEntry.setCcrtdt(Calendar.getInstance().getTime());
                newEntry.setCiv(base64.encodeToString(ivBytes));

                CloudStorage remoteStorage = entry.getValue();
                try{
//                        (
                                CipherInputStream cis = new CipherInputStream(in, cipherUtil.getCipher(Cipher.ENCRYPT_MODE, ivBytes));
//                ){
                    remoteStorage.upload(xFileName, cis, bytesToCopy, false);
                } catch (Exception ex) {
                    logger.error("error when uploading stream data to remote storage", ex);
                }

//                do {
//                    CloudStorage remoteStorage = entry.getValue();
//
//                    try (InputStream is = new BufferedInputStream(new FileInputStream(blockFile))) {
//                        remoteStorage.upload(xFileName, is, blockSize, false);
//                        break;
//                    } catch (IOException | RuntimeException ex) {
//                        logger.debug("error when uploading file to remote storage", ex);
////                                    throw new FileUploadException();
//                    }
//
//                } while (true);
//
//                blockFile.delete();

                blockRepo.save(newEntry);

                FileTableId fileTableId = new FileTableId();
                fileTableId.setCfileid(fileEntry.getCid());
                fileTableId.setCblkid(newEntry.getCid());

                FileTable fileTable = new FileTable();
                fileTable.setId(fileTableId);
                fileTable.setCcrtdt(Calendar.getInstance().getTime());

                fileTableRepo.save(fileTable);
            }

            fileEntry.setClastlock(null);
            fileRepo.save(fileEntry);

        } catch (IOException ex) {
            if (fileEntry != null) {
                fileRepo.deleteById(fileEntry.getCid());
            }
            throw ex;
        }
    }

    /**
     * Save the file object to the given path
     *
     * @param virtualPath the path to be saved to
     * @param file        the file to be saved
     * @throws IOException
     */
    public void save(String virtualPath, java.io.File file) throws IOException, InvalidCryptoParamException {

//        init();

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

        final long blockSize = fileName.endsWith(".mp4") ? Constant.VIDEO_BLOCKSIZE : Constant.FILE_BLOCKSIZE;

        long fileSz = file.length();

        int requiredBlockCnt = (int) (fileSz % blockSize == 0 ? (fileSz / blockSize) : (fileSz / blockSize) + 1);

        ExtendedFileEntry parentEntry = extendedFileRepo.findFileEntryByPath(parentPath);

        FileEntry fileEntry = new FileEntry();
        fileEntry.setCparent(parentEntry.getCid());
        fileEntry.setCisdir(0);
        fileEntry.setCname(fileName);
        fileEntry.setCsize(fileSz);
        Date curTime = Calendar.getInstance().getTime();
        fileEntry.setCcrtdt(curTime);
        fileEntry.setClastlock(curTime);
        fileEntry = fileRepo.save(fileEntry);

        File tmpDir = new File(appConfig.APP_TEMP_DIR);

//        java.io.File blockFile = null;

//        try {
//            blockFile = java.io.File.createTempFile("jsocs-", ".tmp", tmpDir);  //this temp file will be reused
//        } finally {
//
//        }

        // pad the input stream with dummy input stream, so that the output is at fixed size
        SequenceInputStream sis = new SequenceInputStream(new FileInputStream(file), new DummyInputStream());

        Map.Entry<Integer, CloudStorage> entry = this.getNextAvailableStorage();

//        ExecutorService executor = Executors.newFixedThreadPool(2); //reduce the thread count from 10 to 2 as it does not show much improvement on the speed
//        List<Future<Block>> futureList = new ArrayList<>();

        try (BufferedInputStream in = new BufferedInputStream(sis)) {

            if (in.markSupported()){
                logger.debug("input stream supports mark method");
            }else{
                logger.debug("input stream does not support mark method");
            }

            for (int i = 0; i < requiredBlockCnt; i++) {
                String id = UUID.randomUUID().toString();

                //temporary disable space check

//                Map.Entry<Integer, CloudStorage> entry = this.getNextAvailableStorage();
//
//                if (entry == null) {
//                    throw new InsufficientSpaceException();
//                } else {

                long bytesToCopy = blockSize;

                if (i == requiredBlockCnt - 1 && fileSz % blockSize > 0) {
                    bytesToCopy = fileSz % blockSize;
                }

                //ExecutorService executor = Executors.newFixedThreadPool(10);

//                    try {
                final java.io.File blockFile = java.io.File.createTempFile("jsocs-", ".tmp", tmpDir);
//                    } finally {
//
//                    }

                byte[] ivBytes = new byte[16];
                new SecureRandom().nextBytes(ivBytes);

//                try (BufferedOutputStream out = new BufferedOutputStream(new CipherOutputStream(new FileOutputStream(blockFile), cipherUtil.getCipher(Cipher.ENCRYPT_MODE, ivBytes)))) {
//                    IOUtils.copyLarge(in, out, 0, bytesToCopy);
//
//                    if (i == requiredBlockCnt - 1 && fileSz % blockSize > 0) {
//                        for (int j = 0; j < blockSize - bytesToCopy; j++) {
//                            out.write(0);
//                        }
//                    }
//
//                    out.flush();
//
//                } catch (IOException ex) {
//                    throw new CannotWriteTempFileException(blockFile.getAbsolutePath());
//                } catch (NoSuchPaddingException | InvalidKeyException | NoSuchAlgorithmException | InvalidAlgorithmParameterException ex) {
//                    throw new InvalidCryptoParamException();
//                }

                String xFileName = Constant.PATH_SEP + id;

                fileEntry.setClastlock(Calendar.getInstance().getTime()); //maintain the lock on the file
                fileRepo.save(fileEntry);

                Block newEntry = new Block();
                newEntry.setCname(id);
                newEntry.setCsize(blockSize);
                newEntry.setCaccid(entry.getKey());
                newEntry.setCdirectlink(xFileName);
                newEntry.setCuse(1);
                newEntry.setCcrtdt(Calendar.getInstance().getTime());
                newEntry.setCiv(base64.encodeToString(ivBytes));

                do {
                    CloudStorage remoteStorage = entry.getValue();
                    // 2018-09-19   Remove intermediate file when preparing encrypted file for upload - Begin
                    //try () {
                    try{
                        InputStream is = new BufferedInputStream(new CipherInputStream(in, cipherUtil.getCipher(Cipher.ENCRYPT_MODE, ivBytes)));

//                    try (InputStream is = new BufferedInputStream(new FileInputStream(blockFile))) {
                        remoteStorage.upload(xFileName, is, blockSize, false);
                        break;
                    } catch (Exception ex) {
                        in.reset();
                        logger.debug("error when uploading file to remote storage", ex);
//                                    throw new FileUploadException();
                    }

                } while (true);

//                blockFile.delete();

                blockRepo.save(newEntry);

                FileTableId fileTableId = new FileTableId();
                fileTableId.setCfileid(fileEntry.getCid());
                fileTableId.setCblkid(newEntry.getCid());

                FileTable fileTable = new FileTable();
                fileTable.setId(fileTableId);
                fileTable.setCcrtdt(Calendar.getInstance().getTime());

                fileTableRepo.save(fileTable);


//                futureList.add(executor.submit(
//                        new Callable<Block>() {
//                            @Override
//                            public Block call() throws Exception {
//
//                                Block newEntry = new Block();
//                                newEntry.setCname(id);
//                                newEntry.setCsize(Constant.FILE_BLOCKSIZE);
//                                newEntry.setCaccid(entry.getKey());
//                                newEntry.setCdirectlink(xFileName);
//                                newEntry.setCuse(1);
//                                newEntry.setCcrtdt(Calendar.getInstance().getTime());
//
//                                do {
//                                    CloudStorage remoteStorage = entry.getValue();
//
//                                    try (InputStream is = new BufferedInputStream(new FileInputStream(blockFile))) {
//                                        remoteStorage.upload(xFileName, is, Constant.FILE_BLOCKSIZE, false);
//                                        break;
//                                    } catch (IOException | RuntimeException ex) {
//                                        logger.debug("error when uploading file to remote storage", ex);
////                                    throw new FileUploadException();
//                                    }
//
//                                } while (true);
//
//                                blockFile.delete();
//
//                                return newEntry;
//                            }
//                        }
//                ));
            }

//            for (Future<Block> f : futureList) {
//                Block blockEntry = f.get();
//
//                blockRepo.save(blockEntry);
//
//                FileTableId fileTableId = new FileTableId();
//                fileTableId.setCfileid(fileEntry.getCid());
//                fileTableId.setCblkid(blockEntry.getCid());
//
//                FileTable fileTable = new FileTable();
//                fileTable.setId(fileTableId);
//                fileTable.setCcrtdt(Calendar.getInstance().getTime());
//
//                fileTableRepo.save(fileTable);
//            }

            fileEntry.setClastlock(null);
            fileRepo.save(fileEntry);

        } catch (IOException ex) {
            if (fileEntry != null) {
                fileRepo.deleteById(fileEntry.getCid());
            }
            throw ex;
        }
//        catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//        catch (FileUploadException e) {
//            e.printStackTrace();
//        }
//        catch (ExecutionException e) {
//            e.printStackTrace();
//        }
        finally {
            file.delete();
        }
    }

    private void checkFileLock(ExtendedFileEntry fileEntry) throws FileLockedExeption {
        if (fileEntry.getClastlock() != null) {
            Date curTime = new Date();
            long diff = curTime.getTime() - fileEntry.getClastlock().getTime();
            long minutes = TimeUnit.MILLISECONDS.toMinutes(diff);
            if (minutes <= 5) {
                throw new FileLockedExeption();
            }
        }
    }

    public void extract(String virtualPath, java.io.File out, int part) throws IOException, InvalidCryptoParamException {
        if (out.exists()) {
            out.delete();
        }

        this.extract(virtualPath, new FileOutputStream(out, true), part);
    }

    public InputStream extractAsInputStream(String virtualPath, int part) throws IOException, InvalidCryptoParamException {

        InputStream result = null;

        boolean targetExists = extendedFileRepo.existsByPath(virtualPath);

        if (!targetExists) {
            throw new FileNotFoundException(virtualPath);
        }

        long fileSize = -1;

        ExtendedFileEntry fileEntry = extendedFileRepo.findFileEntryByPath(virtualPath);

        if (fileEntry != null) {
            checkFileLock(fileEntry);
            fileSize = fileEntry.getCsize();

            long blockSize = fileEntry.getCname().endsWith(".mp4") ? Constant.VIDEO_BLOCKSIZE : Constant.FILE_BLOCKSIZE;

            List<Block> fileBlocks = blockRepo.findBlocksByFileId(fileEntry.getCid());

            int blockCnt = fileBlocks.size();


            for (int i = 0; i < blockCnt; i++) {

                if (part > -1) {
                    i = part;
                }

                Block curBlock = fileBlocks.get(i);

                CloudStorage cloudStorage = null;

                rwlock.readLock().lock();
                try {
                    cloudStorage = storageMap.get(curBlock.getCaccid());
                } finally {
                    rwlock.readLock().unlock();
                }

                byte[] ivBytes = base64.decode(curBlock.getCiv());

                try {
                    CipherInputStream tmpInput = new CipherInputStream(cloudStorage.download(curBlock.getCdirectlink()), cipherUtil.getCipher(Cipher.DECRYPT_MODE, ivBytes));
                    if (result == null) {
                        result = tmpInput;
                    } else {
                        result = new SequenceInputStream(result, tmpInput);
                    }
                } catch (NoSuchPaddingException | InvalidKeyException | NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
                    throw new InvalidCryptoParamException();
                }

                if (part > -1) {
                    break;
                }
            }
        } else {
            throw new FileNotFoundException();
        }

        return result;
    }

    /**
     * Extract & save file content to target file
     *
     * @param virtualPath
     * @param out
     * @throws IOException
     * @throws InvalidCryptoParamException
     */
    public void extract(String virtualPath, OutputStream out, int part) throws IOException, InvalidCryptoParamException {

//        init();

        boolean targetExists = extendedFileRepo.existsByPath(virtualPath);

        if (!targetExists) {
            throw new FileNotFoundException(virtualPath);
        }

        long fileSize = -1;

        ExtendedFileEntry fileEntry = extendedFileRepo.findFileEntryByPath(virtualPath);

        if (fileEntry != null) {
            checkFileLock(fileEntry);
            fileSize = fileEntry.getCsize();

            long blockSize = fileEntry.getCname().endsWith(".mp4") ? Constant.VIDEO_BLOCKSIZE : Constant.FILE_BLOCKSIZE;

            List<Block> fileBlocks = blockRepo.findBlocksByFileId(fileEntry.getCid());

            try (BufferedOutputStream outStream = new BufferedOutputStream(out)) {

                int blockCnt = fileBlocks.size();


                for (int i = 0; i < blockCnt; i++) {

                    if (part > -1) {
                        i = part;
                    }

                    Block curBlock = fileBlocks.get(i);

                    CloudStorage cloudStorage = null;

                    rwlock.readLock().lock();
                    try {
                        cloudStorage = storageMap.get(curBlock.getCaccid());
                    } finally {
                        rwlock.readLock().unlock();
                    }

                    byte[] ivBytes = base64.decode(curBlock.getCiv());

                    try (BufferedInputStream in = new BufferedInputStream(new CipherInputStream(cloudStorage.download(curBlock.getCdirectlink()), cipherUtil.getCipher(Cipher.DECRYPT_MODE, ivBytes)))) {
                        long byteCnt = (i == fileBlocks.size() - 1) ? fileSize % blockSize : blockSize;
                        IOUtils.copyLarge(in, outStream, 0, byteCnt);
                    } catch (NoSuchPaddingException | InvalidKeyException | NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
                        throw new InvalidCryptoParamException();
                    }

                    if (part > -1) {
                        break;
                    }
                }

            } finally {

            }
        } else {
            throw new FileNotFoundException();
        }
    }

    /**
     * Delete file or folder of given path
     *
     * @param virtualPath
     * @throws IOException
     */
    public void delete(String virtualPath) throws IOException {

//        init();

        boolean targetExists = extendedFileRepo.existsByPath(virtualPath);

        if (!targetExists) {
            throw new FileNotFoundException(virtualPath);
        }

        ExtendedFileEntry fileEntry = extendedFileRepo.findFileEntryByPath(virtualPath);

        if (fileEntry != null) {

            checkFileLock(fileEntry);

            int fileId = fileEntry.getCid();
            fileRepo.deleteById(fileId);

            if (new Integer(0).equals(fileEntry.getCisdir())) {
                List<FileTable> fileTables = fileTableRepo.findByCfileid(fileId);

                for (FileTable entry : fileTables) {

                    Optional<Block> oBlock = blockRepo.findById(entry.getId().getCblkid());

                    if (oBlock.isPresent()) {
                        Block block = oBlock.get();

                        rwlock.readLock().lock();
                        try {
                            CloudStorage cloudStorage = storageMap.get(block.getCaccid());
                            cloudStorage.delete(block.getCdirectlink());  //delete from cloud storage
                        } finally {
                            rwlock.readLock().unlock();
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
            newEntry.setCcrtdt(Calendar.getInstance().getTime());
            fileRepo.save(newEntry);
        }
    }
}
