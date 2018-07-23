package org.coreocto.dev.jsocs.rest.nio;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.coreocto.dev.jsocs.rest.Constant;
import org.coreocto.dev.jsocs.rest.db.AccountService;
import org.coreocto.dev.jsocs.rest.db.BlockService;
import org.coreocto.dev.jsocs.rest.db.FileService;
import org.coreocto.dev.jsocs.rest.db.FileTableService;
import org.coreocto.dev.jsocs.rest.exception.MissingAccessTokenException;
import org.coreocto.dev.jsocs.rest.exception.MissingTokenException;
import org.coreocto.dev.jsocs.rest.pojo.Account;
import org.coreocto.dev.jsocs.rest.pojo.Block;
import org.coreocto.dev.jsocs.rest.pojo.FileEntry;
import org.coreocto.dev.jsocs.rest.pojo.FileTable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;

@Service
public class StorageMgr {

    public static final int BLOCKSIZE = 64 * 1024 * 1024;

    private final Logger logger = LoggerFactory.getLogger(StorageMgr.class);

    private final File TMP_DIR = new File("r:\\temp");

    @Autowired
    AccountService accountService;

    @Autowired
    BlockService blockService;

    @Autowired
    FileService fileService;

    @Autowired
    FileTableService fileTableService;

    @Autowired
    RemoteStorageFactory remoteStorageFactory;

    public void init() {
        List<Account> accList = accountService.getAllAccounts();
        for (Account account : accList) {
            IRemoteStorage remoteStorage = remoteStorageFactory.make(account.getCtype(), account.getCid(), account.getCauthToken());
            this.init(remoteStorage);
        }
    }

    private File makeEmptyBlock() throws IOException {
        java.io.File tmpFile = java.io.File.createTempFile("jsocs-", ".tmp", TMP_DIR);

        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(tmpFile))) {
            for (int i = 0; i < BLOCKSIZE; i++) {
                out.write(0);
            }
            out.flush();
        } finally {

        }

        return tmpFile;
    }

    //function to initialize specific remote storage
    public void init(IRemoteStorage remoteStorage) {
        Account account = accountService.getById(remoteStorage.getUserId());
        if (account.getCinit() == 0) {

            if (account.getCtoken() == null || account.getCtoken().isEmpty()) {
                throw new MissingTokenException();
            } else if (account.getCauthToken() == null || account.getCauthToken().isEmpty()) {
                throw new MissingAccessTokenException();
            }

            File tmpFile = null;

            try {
                tmpFile = makeEmptyBlock();
            } catch (IOException e) {
                logger.error("failed to create empty block file", e);
            }

            List<String> blockNames = new ArrayList<>();

            // allocate 150 x 64MB block of memory
            // for some storage provider, they don't allow us to directly manipulate the content of uploaded files
            // hence when updating those files, we need to explicitly create another data block and remove the old one
            // it provides a buffer from exceed the storage limit
            for (int i = 0; i < 150; i++) {
                String id = UUID.randomUUID().toString();
                blockNames.add(id + ".bin");
            }

            List<Map<String, Object>> provisionResult = null;

            try {
                provisionResult = remoteStorage.provision(tmpFile, blockNames);
            } catch (IOException e) {
                logger.error("failed to provision space on remote storage", e);
            }

            int len = provisionResult.size();

            for (int i = 0; i < len; i++) {
                String id = blockNames.get(i);
                String remoteId = null;
                String downloadLink = null;
                Map<String, Object> tmpMap = provisionResult.get(i);
                remoteId = (String) tmpMap.get("fileId");
                downloadLink = (String) tmpMap.get("downloadLink"); //for pCloud only, it allows direct download of uploaded files

                blockService.create(id, BLOCKSIZE, account.getCid(), remoteId, downloadLink);    //uuid, blockSize, accountName, remoteFileId
                logger.debug("block - " + id + " (" + remoteId + ") created");
            }

            accountService.updateInitStatus(account.getCid(), true);

        } else {
            logger.debug("account: " + account.getCusername() + " already initialized");
        }
    }

    public void save(String virtualPath, java.io.File file) throws IOException {

        String parentPath = FilenameUtils.getFullPath(virtualPath);
        String fileName = FilenameUtils.getName(virtualPath);

        if (parentPath.isEmpty()) {
            parentPath = Constant.PATH_SEP;
        }

        if (isFileExists(parentPath, fileName)) {
            logger.debug("file exists: " + virtualPath);
            return;
        }

        long fileSz = file.length();

        int requiredBlks = (int) (fileSz % BLOCKSIZE == 0 ? (fileSz / BLOCKSIZE) : (fileSz / BLOCKSIZE) + 1);

        //find how much block is available for use
        List<Block> unusedBlocks = blockService.getBlocks(false, true);

        //continue logic when sufficient block is available
        if (unusedBlocks.size() >= requiredBlks) {

            fileService.create(parentPath, fileName, fileSz);

            FileEntry fileEntry = fileService.getByName(parentPath, fileName);

            List<Block> pendingBlocks = unusedBlocks.subList(0, requiredBlks);

            int blockIdx = 0;

            //for counting the remaining bytes to be written
            long remainBytes = fileSz;
            long bytesCnt = BLOCKSIZE;

            if (fileSz < BLOCKSIZE) {
                bytesCnt = fileSz;
            }

            try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {

                //write data to each block
                for (int i = 0; i < pendingBlocks.size(); i++) {

                    Block block = pendingBlocks.get(i);

                    Account account = accountService.getById(block.getCaccid());

                    IRemoteStorage remoteStorage = remoteStorageFactory.make(account.getCtype(), account.getCid(), account.getCauthToken());

                    blockService.update(block.getCid(), true);

                    java.io.File blockFile = java.io.File.createTempFile("jsocs-", ".tmp", TMP_DIR);

                    try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(blockFile))) {

                        if (remainBytes < BLOCKSIZE) {
                            bytesCnt = remainBytes;
                        }

                        IOUtils.copyLarge(in, out, BLOCKSIZE * blockIdx, bytesCnt);

                        remainBytes -= bytesCnt;

                        if (BLOCKSIZE % bytesCnt > 0) {
                            for (int ii = 0; ii < BLOCKSIZE - bytesCnt; ii++) {
                                out.write(0);
                            }
                        }

                        fileTableService.create(fileEntry.getCid(), block.getCid());

                    } finally {

                    }

                    //pCloud specific implementation
                    long fileId = -1;
                    try {
                        fileId = Long.parseLong(block.getCremoteid());
                    } catch (NumberFormatException ex) {
                    }

                    remoteStorage.delete(fileId);
                    //end

                    Map<String, Object> newInfo = remoteStorage.upload(blockFile, block.getCname());
                    String remoteId = (String) newInfo.get("fileId");
                    String downloadLink = (String) newInfo.get("downloadLink");

                    blockService.update(block.getCid(), remoteId, downloadLink);
                    blockService.update(block.getCid(), true);

                    logger.debug("before: " + block.getCremoteid() + ", after: " + remoteId);
                }

            } finally {

            }

        } else {
            //prompt error when insufficient block is available
            logger.debug("insufficient space for persisting file: " + file.getName());
        }
    }

    private boolean isFileExists(String parentPath, String fileName) {
        boolean fileExists = true;

        //check if the given path already exists
        try {
            fileService.getByName(parentPath, fileName);
        } catch (Exception ex) {
            fileExists = false;
        }
        return fileExists;
    }

    public void save(IRemoteStorage remoteStorage, String virtualPath, java.io.File file) throws IOException {

        String parentPath = FilenameUtils.getFullPath(virtualPath);
        String fileName = FilenameUtils.getName(virtualPath);

        if (isFileExists(parentPath, fileName)) {
            logger.debug("file exists: " + virtualPath);
            return;
        }

        long fileSz = file.length();

        int requiredBlks = (int) (fileSz % BLOCKSIZE == 0 ? (fileSz / BLOCKSIZE) : (fileSz / BLOCKSIZE) + 1);

        //find how much block is available for use
        List<Block> unusedBlocks = blockService.getBlocks(false, true);

        //continue logic when sufficient block is available
        if (unusedBlocks.size() >= requiredBlks) {

            fileService.create(parentPath, fileName, fileSz);

            FileEntry fileEntry = fileService.getByName(parentPath, fileName);

            List<Block> pendingBlocks = unusedBlocks.subList(0, requiredBlks);

            int blockIdx = 0;

            //for counting the remaining bytes to be written
            long remainBytes = fileSz;
            long bytesCnt = BLOCKSIZE;

            if (fileSz < BLOCKSIZE) {
                bytesCnt = fileSz;
            }

            try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {

                //write data to each block
                for (int i = 0; i < pendingBlocks.size(); i++) {

                    Block block = pendingBlocks.get(i);

                    blockService.update(block.getCid(), true);

                    java.io.File blockFile = java.io.File.createTempFile("jsocs-", ".tmp", TMP_DIR);

                    try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(blockFile))) {

                        if (remainBytes < BLOCKSIZE) {
                            bytesCnt = remainBytes;
                        }

                        IOUtils.copyLarge(in, out, BLOCKSIZE * blockIdx, bytesCnt);

                        remainBytes -= bytesCnt;

                        if (BLOCKSIZE % bytesCnt > 0) {
                            for (int ii = 0; ii < BLOCKSIZE - bytesCnt; ii++) {
                                out.write(0);
                            }
                        }

                        fileTableService.create(fileEntry.getCid(), block.getCid());

                    } finally {

                    }

                    long fileId = -1;
                    try {
                        fileId = Long.parseLong(block.getCremoteid());
                    } catch (NumberFormatException ex) {

                    }

                    remoteStorage.delete(fileId);

                    String remoteId = null;
                    String downloadLink = null;

                    Map<String, Object> newInfo = remoteStorage.upload(blockFile, block.getCname());
                    remoteId = (String) newInfo.get("fileId");
                    downloadLink = (String) newInfo.get("downloadLink");

                    blockService.update(block.getCid(), remoteId, downloadLink);
                    blockService.update(block.getCid(), true);

                    logger.debug("before: " + block.getCremoteid() + ", after: " + remoteId);
                }

            } finally {

            }

        } else {
            //prompt error when insufficient block is available
            logger.debug("insufficient space for persisting file: " + file.getName());
        }
    }

    public void extract(String virtualPath, java.io.File out) throws IOException {

        String parentPath = FilenameUtils.getFullPath(virtualPath);
        String fileName = FilenameUtils.getName(virtualPath);

        if (isFileExists(parentPath, fileName)) {
            logger.debug("file exists: " + virtualPath);
            return;
        }

        long fileSize = -1;

        FileEntry fileEntry = null;

        try {
            fileEntry = fileService.getByName(parentPath, fileName);
        } catch (Exception ex) {
        }

        if (fileEntry != null) {

            fileSize = fileEntry.getCsize();

            List<Block> fileBlocks = blockService.getByFileId(fileEntry.getCid());

            //provision input stream from occupied blocks and concatenate them together according to their order (defined by cid)
//            InputStream in = null;
//            InputStream prevInputStream = null;

            if (out.exists()) {
                out.delete();
            }

            try (BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(out, true))) {
                for (int i = 0; i < fileBlocks.size(); i++) {

                    logger.debug("reading block-" + i);

                    Block curBlock = fileBlocks.get(i);

                    Account account = accountService.getById(curBlock.getCaccid());

                    IRemoteStorage remoteStorage = remoteStorageFactory.make(account.getCtype(), account.getCid(), account.getCtoken());

                    File tmpFile = File.createTempFile("jsocs-", ".tmp", TMP_DIR);

                    remoteStorage.download(curBlock.getCdirectlink(), tmpFile);

                    try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(tmpFile))) {
                        long byteCnt = (i == fileBlocks.size() - 1) ? fileSize % BLOCKSIZE : BLOCKSIZE;
                        IOUtils.copyLarge(in, outStream, 0, byteCnt);
                    } finally {

                    }
                }

            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public void extract(IRemoteStorage remoteStorage, String virtualPath, java.io.File out) throws IOException {

        String parentPath = FilenameUtils.getFullPath(virtualPath);
        String fileName = FilenameUtils.getName(virtualPath);

        if (isFileExists(parentPath, fileName)) {
            logger.debug("file exists: " + virtualPath);
            return;
        }

        long fileSize = -1;

        FileEntry fileEntry = null;

        try {
            fileEntry = fileService.getByName(parentPath, fileName);
        } catch (Exception ex) {
        }

        if (fileEntry != null) {

            fileSize = fileEntry.getCsize();

            List<Block> fileBlocks = blockService.getByFileId(fileEntry.getCid());

            //provision input stream from occupied blocks and concatenate them together according to their order (defined by cid)
//            InputStream in = null;
//            InputStream prevInputStream = null;

            if (out.exists()) {
                out.delete();
            }

            try (BufferedOutputStream outStream = new BufferedOutputStream(new FileOutputStream(out, true))) {
                for (int i = 0; i < fileBlocks.size(); i++) {

                    logger.debug("reading block-" + i);

                    Block curBlock = fileBlocks.get(i);

                    File tmpFile = File.createTempFile("jsocs-", ".tmp", new File("r:\\temp"));

                    remoteStorage.download(curBlock.getCdirectlink(), tmpFile);

                    try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(tmpFile))) {
                        long byteCnt = (i == fileBlocks.size() - 1) ? fileSize % BLOCKSIZE : BLOCKSIZE;
                        IOUtils.copyLarge(in, outStream, 0, byteCnt);
                    } finally {

                    }
                }

            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    public void delete(String virtualPath) throws IOException {

        String parentPath = FilenameUtils.getFullPath(virtualPath);
        String fileName = FilenameUtils.getName(virtualPath);

        if (!isFileExists(parentPath, fileName)) {
            logger.debug("file not exists: " + virtualPath);
            return;
        }

        FileEntry fileEntry = fileService.getByName(parentPath, fileName);

        int fileId = fileEntry.getCid();
        fileService.deleteById(fileId);

        List<FileTable> fileTablesList = fileTableService.getByFileId(fileId);

        for (FileTable entry : fileTablesList) {
            blockService.update(entry.getCblkid(), false);
        }

        fileTableService.deleteByFileId(fileId);
    }
}
