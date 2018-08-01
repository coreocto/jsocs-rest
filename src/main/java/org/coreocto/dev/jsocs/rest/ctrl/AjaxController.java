package org.coreocto.dev.jsocs.rest.ctrl;

import com.google.gson.Gson;
import org.apache.commons.io.FilenameUtils;
import org.coreocto.dev.jsocs.rest.Constant;
import org.coreocto.dev.jsocs.rest.config.AppConfig;
import org.coreocto.dev.jsocs.rest.nio.StorageManager;
import org.coreocto.dev.jsocs.rest.pojo.Account;
import org.coreocto.dev.jsocs.rest.pojo.ExtendedFileEntry;
import org.coreocto.dev.jsocs.rest.repo.AccountRepo;
import org.coreocto.dev.jsocs.rest.repo.ExtendedFileRepo;
import org.coreocto.dev.jsocs.rest.repo.FileRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.List;
import java.util.Optional;

//import org.coreocto.dev.jsocs.rest.db.FileService;
//import org.coreocto.dev.jsocs.rest.nio.RemoteStorageFactory;

@RestController
public class AjaxController {

    @Autowired
    StorageManager storageMgr;

//    @Autowired
//    AccountService accountService;

//    @Autowired
//    FileService fileService;

//    @Autowired
//    BlockService blockService;

//    @Autowired
//    RemoteStorageFactory remoteStorageFactory;

    @Autowired
    FileRepo fileRepo;

    @Autowired
    ExtendedFileRepo extendedFileRepo;

    @Autowired
    AccountRepo accountRepo;

    @Autowired
    AppConfig appConfig;

    private final Logger logger = LoggerFactory.getLogger(AjaxController.class);

    //accounts
    private boolean isAccountExists(String username, String type) {
        Account account = null;
        try {
            account = accountRepo.findByNameAndType(username, type);
        } catch (Exception ex) {
        }
        return account != null;
    }

    @PostMapping("/api/accounts")
    public String createAccount(@RequestParam("username") String username, @RequestParam("type") String type) {

        boolean found = isAccountExists(username, type);

        if (!found) {
            Account newAcc = new Account();
            newAcc.setCusername(username);
            newAcc.setCtype(type);
            accountRepo.save(newAcc);
            return "success";
        } else {
            return "failed";
        }
    }

    @GetMapping("/api/accounts")
    public String listAccounts() {
        List<Account> accountList = accountRepo.findAll();
        return "{ \"data\":" + new Gson().toJson(accountList) + " }";
    }

    @DeleteMapping("/api/accounts/{userId}")
    public String deleteAccount(@PathVariable("userId") int userId) {

        boolean success = true;
        try {
            accountRepo.deleteById(userId);
        } catch (Exception ex) {
            success = false;
        }

        if (success) {
            return "success";
        } else {
            return "failed";
        }
    }
    //end of accounts

    // begin of files
    private String pathMassage(String path) {
        String newPath = null;

        if (path != null) {
            newPath = path;
            if (!newPath.startsWith(Constant.PATH_SEP)) {
                newPath = Constant.PATH_SEP + newPath;
            }
        } else {
            newPath = Constant.PATH_SEP;
        }
        return newPath;
    }

    @PostMapping("/api/files")
    public String save(@RequestParam("path") String path, @RequestParam("folder") Optional<String> folder, @RequestParam("file") Optional<MultipartFile> file) {

        boolean success = true;

        if (file.isPresent()) {

            File targetFile = null;

            File tmpDir = new File(appConfig.APP_TEMP_DIR);

            try {
                targetFile = new File(tmpDir, file.get().getOriginalFilename());
                file.get().transferTo(targetFile);
            } catch (IOException e) {
                logger.error("error when copying uploaded file to temp folder", e);
                success = false;
            }

            if (success) {
                try {
                    storageMgr.save(path, targetFile);
                } catch (IOException e) {
                    logger.error("error when uploading file to remote storage", e);
                    success = false;
                }
            }
        } else if (folder.isPresent()) {
            storageMgr.makeDir(path, folder.get());
        }

        if (success) {
            return "success";
        } else {
            return "failed";
        }
    }

    @GetMapping("/api/files")
    public String listFiles() {
        List<ExtendedFileEntry> entries = extendedFileRepo.findFileEntriesByPath(Constant.PATH_SEP);
        return "{ \"data\":" + new Gson().toJson(entries) + " }";
    }

    @GetMapping("/api/files/**")
    public void listFiles(HttpServletRequest request, HttpServletResponse response) {

        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        path = path.substring("/api/files/".length() - 1);

        String newPath = pathMassage(path);

//        FileEntry fileEntry = null;

//        String parentPath = FilenameUtils.getFullPathNoEndSeparator(newPath);
        String fileName = FilenameUtils.getName(newPath);

        ExtendedFileEntry fileEntry = extendedFileRepo.findFileEntryByPath(path);
//        try {
//            FileEntry parent = fileRepogetByPath(parentPath);
//            fileEntry = fileService.getByParentAndName(parent.getCid(), fileName);
//        } catch (Exception ex) {
//
//        }

        if (fileEntry == null) {
            response.setContentType("application/json");
            try (PrintWriter out = response.getWriter()) {
                out.write("{\"status\":\"error\", \"message\":\"file not found\"}");
                out.flush();
            } catch (IOException ex) {
            }
            return;
        }

        if (new Integer(0).equals(fileEntry.getCisdir())) {

            java.io.File tmpFile = null;

            boolean success = true;

            File tmpDir = new File(appConfig.APP_TEMP_DIR);

            try {
                tmpFile = java.io.File.createTempFile("jsocs-", ".tmp", tmpDir);
                storageMgr.extract(newPath, tmpFile);
            } catch (IOException ex) {
                logger.error("error when downloading file from remote storage", ex);
                success = false;
            }

            if (success) {
                response.setContentType("application/octet-stream");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

                try (
                        BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(tmpFile));
                        BufferedOutputStream outStream = new BufferedOutputStream(response.getOutputStream());
                ) {

                    org.apache.commons.io.IOUtils.copy(inputStream, outStream);
                    outStream.flush();

                } catch (IOException ex) {
                    logger.error("error when writing file to response", ex);
                    success = false;
                }
            }
        } else {
            response.setContentType("application/json");
            List<ExtendedFileEntry> entries = extendedFileRepo.findFileEntriesByPathWithParent(newPath);
            try (PrintWriter out = response.getWriter()) {
                out.write("{ \"data\":" + new Gson().toJson(entries) + " }");
                out.flush();
            } catch (IOException ex) {
            }
        }
    }

    @DeleteMapping("/api/files/**")
    public String deleteFile(HttpServletRequest request) {

        String path = (String)
                request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        path = path.substring("/api/files/".length() - 1);

        boolean success = true;

        String newPath = path;

        if (!newPath.startsWith(Constant.PATH_SEP)) {
            newPath = Constant.PATH_SEP + newPath;
        }

        try {
            storageMgr.delete(newPath);
        } catch (IOException ex) {
            success = false;
        }

        if (success) {
            return "success";
        } else {
            return "failed";
        }
    }
    // end of files
}
