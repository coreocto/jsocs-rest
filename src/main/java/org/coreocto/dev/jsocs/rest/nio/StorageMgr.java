//package org.coreocto.dev.jsocs.rest.nio;
//
//import org.apache.commons.io.FilenameUtils;
//import org.apache.commons.io.IOUtils;
//import org.coreocto.dev.jsocs.rest.Constant;
//import org.coreocto.dev.jsocs.rest.db.AccountService;
//import org.coreocto.dev.jsocs.rest.db.BlockService;
//import org.coreocto.dev.jsocs.rest.db.FileService;
//import org.coreocto.dev.jsocs.rest.db.FileTableService;
//import org.coreocto.dev.jsocs.rest.exception.InsufficientSpaceAvailableException;
//import org.coreocto.dev.jsocs.rest.exception.InvalidChecksumException;
//import org.coreocto.dev.jsocs.rest.pojo.Account;
//import org.coreocto.dev.jsocs.rest.pojo.Block;
//import org.coreocto.dev.jsocs.rest.pojo.FileEntry;
//import org.coreocto.dev.jsocs.rest.pojo.FileTable;
//import org.coreocto.dev.jsocs.rest.repo.FileRepo;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import javax.crypto.Cipher;
//import javax.crypto.CipherInputStream;
//import javax.crypto.CipherOutputStream;
//import javax.crypto.NoSuchPaddingException;
//import javax.crypto.spec.IvParameterSpec;
//import javax.crypto.spec.SecretKeySpec;
//import java.io.*;
//import java.nio.file.FileAlreadyExistsException;
//import java.security.InvalidAlgorithmParameterException;
//import java.security.InvalidKeyException;
//import java.security.NoSuchAlgorithmException;
//import java.util.*;
//
//@Service
//public class StorageMgr {
//
//    public Cipher getCipher(int mode) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
//        byte[] iv_key = "097c1090379b4f28".getBytes();
//        IvParameterSpec iv = new IvParameterSpec(iv_key);
//        SecretKeySpec m_keySpec = new SecretKeySpec(iv_key, "AES");
//        Cipher m_cipher = javax.crypto.Cipher.getInstance("AES/CTR/NoPadding");
//        m_cipher.init(mode, m_keySpec, iv);
//        return m_cipher;
//    }
//
//    public static final int BLOCKSIZE = 64 * 1024 * 1024;
//
//    private final Logger logger = LoggerFactory.getLogger(StorageMgr.class);
//
//    private final File TMP_DIR = new File("c:\\temp");
//
//    @Autowired
//    AccountService accountService;
//
//    @Autowired
//    BlockService blockService;
//
//    @Autowired
//    FileService fileService;
//
//    @Autowired
//    FileTableService fileTableService;
//
//    @Autowired
//    RemoteStorageFactory remoteStorageFactory;
//
//    @Autowired
//    FileRepo fileRepo;
//
//    public Map.Entry<Integer, IRemoteStorage> getAvailableStorage() {
//        Map.Entry<Integer, IRemoteStorage> entry = null;
//
////        List<CloudStorage> serviceList = new ArrayList<>();
//        for (Account account : accountService.getAllAccounts()) {
//            IRemoteStorage remoteStorage = remoteStorageFactory.make(account.getCtype(), account.getCid(), account.getCauthToken());
//            long availableSpace = 0;
//            try {
//                availableSpace = remoteStorage.getAvailable();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
////            CloudStorage cloudStorage = remoteStorageFactory.get(account.getCtype(), DEFAULT_CALLBACK_PORT, "1z5SVQMlzHk", "1dWvvNqzfkpSeVJqYLenD5SvTVIV", account.getCid() + "");
////
////            boolean crTokenOk = true;
////
////            if (account.getCcrtoken() != null) {
////                try {
////                    cloudStorage.loadAsString(account.getCcrtoken());
////                } catch (ParseException e) {
////                    logger.error("error when parsing savedState: " + account.getCcrtoken(), e);
////                    crTokenOk = false;
////                }
////            }
////
////            if (!crTokenOk || account.getCcrtoken() == null) {
////                cloudStorage.login();
////                String savedState = cloudStorage.saveAsString();
////                accountService.updateToken(account.getCid(), savedState);
////            }
////
////            SpaceAllocation allocation = cloudStorage.getAllocation();
//
//            if (availableSpace >= BLOCKSIZE) {
//                entry = new AbstractMap.SimpleEntry<>(account.getCid(), remoteStorage);
////                entry = new AbstractMap.SimpleEntry(account.getCid(), cloudStorage);
//                break;
//            }
//
////            serviceList.add(cloudStorage);
//        }
//        return entry;
//    }
//
//    public void save(String virtualPath, java.io.File file) throws IOException {
//
//        String parentPath = FilenameUtils.getFullPathNoEndSeparator(virtualPath);
//        String fileName = FilenameUtils.getName(virtualPath);
//
//        if (parentPath.isEmpty()) {
//            parentPath = Constant.PATH_SEP;
//        }
//
//        if (isFileExists(parentPath, fileName)) {
//            throw new FileAlreadyExistsException(virtualPath);
//        }
//
//        long fileSz = file.length();
//
//        int requiredBlks = (int) (fileSz % BLOCKSIZE == 0 ? (fileSz / BLOCKSIZE) : (fileSz / BLOCKSIZE) + 1);
//
//        FileEntry parentEntry = fileService.getByPath(parentPath);
//
//        FileEntry fileEntry = new FileEntry();
//        fileEntry.setCparent(parentEntry.getCid());
//        fileEntry.setCisdir(0);
//        fileEntry.setCname(fileName);
//        fileEntry.setCsize(fileSz);
//        fileRepo.save(fileEntry);
//
//        fileEntry = fileService.getByParentAndName(parentEntry.getCid(), fileName);
//
////        fileService.create(parentPath, fileName, fileSz);
//
////        FileEntry fileEntry = fileService.getByName(parentPath, fileName);
//
//        java.io.File blockFile = java.io.File.createTempFile("jsocs-", ".tmp", TMP_DIR);    //this temp file will be reused
//
//        InputStream is = new FileInputStream(file);
//
//        if (fileSz < (requiredBlks * BLOCKSIZE)) {
//            is = new SequenceInputStream(is, new DummyInputStream());
//        }
//
//        IOException ioException = null;
//        RuntimeException runtimeException = null;
//
//        try (BufferedInputStream in = new BufferedInputStream(is)) {
//
//            for (int i = 0; i < requiredBlks; i++) {
//                String id = UUID.randomUUID().toString();
//
//                Map.Entry<Integer, IRemoteStorage> entry = this.getAvailableStorage();
//
//                if (entry == null) {
//                    throw new InsufficientSpaceAvailableException();
//                } else {
//
//                    boolean abort = false;
//
//                    try (BufferedOutputStream out = new BufferedOutputStream(new CipherOutputStream(new FileOutputStream(blockFile), getCipher(Cipher.ENCRYPT_MODE)))) {
//                        IOUtils.copyLarge(in, out, 0, BLOCKSIZE);
//                        out.flush();
//
//                    } catch (IOException ex) {
//                        logger.error("error when copying file content to temporary data block", ex);
//                        throw ex;
//                    } catch (NoSuchPaddingException | InvalidKeyException | NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
//                        logger.error("encryption of output stream failed, please check the parameters", e);
//                    }
//
//                    do {
//                        IRemoteStorage remoteStorage = entry.getValue();
//
//                        Map<String, Object> newInfo = null;
//
//                        try {
//                            newInfo = remoteStorage.upload(blockFile, id);
//                            String remoteId = (String) newInfo.get("fileId");
//                            String downloadLink = (String) newInfo.get("downloadLink");
//
//                            blockService.create(id, BLOCKSIZE, entry.getKey(), remoteId, downloadLink);
//                            Block blockEntry = blockService.getByName(id);
//
//                            fileTableService.create(fileEntry.getCid(), blockEntry.getCid());
//
//                            break;
//
//                        } catch (InvalidChecksumException ex) {
//                            logger.debug("mismatch between local & remote checksum, retrying...");
//                        }
//
//                    } while (true);
//                }
//            }
//        } catch (IOException ex) {
//            ioException = ex;
//        } catch (RuntimeException ex) {
//            runtimeException = ex;
//        } finally {
//            blockFile.delete();
//        }
//
//        if (ioException != null || runtimeException != null) {
//            logger.debug("error occurred, deleting associated records from database");
//            fileService.deleteById(fileEntry.getCid());
//            if (ioException != null) throw ioException;
//            if (runtimeException != null) throw runtimeException;
//        }
//    }
//
//    private boolean isFileExists(String parentPath, String fileName) {
//        boolean fileExists = true;
//
//        //check if the given path already exists
//        try {
//            FileEntry parent = fileService.getByPath(parentPath);
//            fileService.getByParentAndName(parent.getCid(), fileName);
//        } catch (Exception ex) {
//            fileExists = false;
//        }
//        return fileExists;
//    }
//
//    public void extract(String virtualPath, java.io.File out) throws IOException {
//
//        String parentPath = FilenameUtils.getFullPathNoEndSeparator(virtualPath);
//        String fileName = FilenameUtils.getName(virtualPath);
//
//        if (!isFileExists(parentPath, fileName)) {
//            logger.debug("file not exists: " + virtualPath);
//            throw new FileNotFoundException();
//        }
//
//        long fileSize = -1;
//
//        FileEntry fileEntry = null;
//
//        try {
//            FileEntry parent = fileService.getByPath(parentPath);
//            fileEntry = fileService.getByParentAndName(parent.getCid(), fileName);
//        } catch (Exception ex) {
//        }
//
//        if (fileEntry != null) {
//
//            fileSize = fileEntry.getCsize();
//
//            List<Block> fileBlocks = blockService.getByFileId(fileEntry.getCid());
//
//            //provision input stream from occupied blocks and concatenate them together according to their order (defined by cid)
////            InputStream in = null;
////            InputStream prevInputStream = null;
//
//            if (out.exists()) {
//                out.delete();
//            }
//
//            try (BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(out, true))) {
//
//                File tmpFile = File.createTempFile("jsocs-", ".tmp", TMP_DIR);
//
//                for (int i = 0; i < fileBlocks.size(); i++) {
//
//                    logger.debug("reading block-" + i);
//
//                    Block curBlock = fileBlocks.get(i);
//
//                    Account account = accountService.getById(curBlock.getCaccid());
//
//                    IRemoteStorage remoteStorage = remoteStorageFactory.make(account.getCtype(), account.getCid(), account.getCtoken());
//
//                    remoteStorage.download(curBlock.getCdirectlink(), tmpFile);
//
//                    try (BufferedInputStream in = new BufferedInputStream(new CipherInputStream(new FileInputStream(tmpFile), getCipher(Cipher.DECRYPT_MODE)))) {
//                        long byteCnt = (i == fileBlocks.size() - 1) ? fileSize % BLOCKSIZE : BLOCKSIZE;
//                        IOUtils.copyLarge(in, outStream, 0, byteCnt);
//                    } catch (NoSuchPaddingException | InvalidKeyException | NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
//                        logger.error("decryption of input stream failed, please check the parameters", e);
//                    } finally {
//
//                    }
//                }
//
//            } catch (IOException e) {
//                logger.error(e.getMessage(), e);
//            }
//        }
//    }
//
//    public void delete(String virtualPath) throws IOException {
//
//        String parentPath = FilenameUtils.getFullPathNoEndSeparator(virtualPath);
//        String fileName = FilenameUtils.getName(virtualPath);
//
//        if (!isFileExists(parentPath, fileName)) {
//            throw new FileNotFoundException();
//        }
//
//        FileEntry parent = fileService.getByPath(parentPath);
//        FileEntry fileEntry = fileService.getByParentAndName(parent.getCid(), fileName);
//
//        int fileId = fileEntry.getCid();
//        fileRepo.deleteById(fileId);
////        fileService.deleteById(fileId);
//
//        if (new Integer(0).equals(fileEntry.getCisdir())) {
//            List<FileTable> fileTablesList = fileTableService.getByFileId(fileId);
//
//            for (FileTable entry : fileTablesList) {
//                blockService.update(entry.getCblkid(), false);
//            }
//
//            fileTableService.deleteByFileId(fileId);
//        }
//    }
//
//    public void makeDir(String path, String dirName) {
//
//        logger.debug("path = " + path);
//        logger.debug("dirName = " + dirName);
//
//        FileEntry fileEntry = null;
//
//        try {
//            fileEntry = fileService.getByPath(path);    //can throw file entry not found
//
//            FileEntry newEntry = new FileEntry();
//            newEntry.setCname(dirName);
//            newEntry.setCisdir(1);
//            newEntry.setCparent(fileEntry.getCid());
//            fileRepo.save(newEntry);
//
//        } catch (Exception ex) {
//
//        }
//    }
//}
