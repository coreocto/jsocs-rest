package org.coreocto.dev.jsocs.rest.ctrl;

import com.google.gson.Gson;
import org.apache.commons.io.FilenameUtils;
import org.coreocto.dev.jsocs.rest.Constant;
import org.coreocto.dev.jsocs.rest.config.AppConfig;
import org.coreocto.dev.jsocs.rest.exception.FolderNotFoundException;
import org.coreocto.dev.jsocs.rest.exception.InvalidCryptoParamException;
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
import java.nio.file.FileAlreadyExistsException;
import java.util.List;
import java.util.Optional;

@RestController
public class AjaxController {

    private final Logger logger = LoggerFactory.getLogger(AjaxController.class);
    @Autowired
    StorageManager storageMgr;
    @Autowired
    FileRepo fileRepo;
    @Autowired
    ExtendedFileRepo extendedFileRepo;
    @Autowired
    AccountRepo accountRepo;
    @Autowired
    AppConfig appConfig;

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

        try {
            if (file.isPresent()) {
                File tmpDir = new File(appConfig.APP_TEMP_DIR);

                File targetFile = new File(tmpDir, file.get().getOriginalFilename());
                file.get().transferTo(targetFile);

                if (success) {
                    storageMgr.save(path, targetFile);
                }
            } else if (folder.isPresent()) {
                storageMgr.makeDir(path, folder.get());
            }
        } catch (FolderNotFoundException e) {
            e.printStackTrace();
        } catch (FileAlreadyExistsException e) {
            e.printStackTrace();
        } catch (IOException e) {
            logger.error("error when uploading file to remote storage", e);
            success = false;
        } catch (InvalidCryptoParamException e) {
            e.printStackTrace();
        } finally {

        }

        if (success) {
            return "success";
        } else {
            return "failed";
        }
    }

    @GetMapping("/api/files")
    public String listFiles() {
        List<ExtendedFileEntry> entries = null;
        try {
            entries = storageMgr.list(Constant.PATH_SEP);
        } catch (FolderNotFoundException e) {
            e.printStackTrace();
        }
        return "{ \"data\":" + new Gson().toJson(entries) + " }";
    }

    @GetMapping("/api/files/**")
    public void listFiles(HttpServletRequest request, HttpServletResponse response) {

        String path = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        path = path.substring("/api/files/".length() - 1);

        String newPath = pathMassage(path);

        String fileName = FilenameUtils.getName(newPath);

        ExtendedFileEntry fileEntry = extendedFileRepo.findFileEntryByPath(path);

        try {

            if (fileEntry == null) {
                response.setContentType("application/json");
                try (PrintWriter out = response.getWriter()) {
                    out.write("{\"status\":\"error\", \"message\":\"file not found\"}");
                    out.flush();
                } finally {

                }
                return;
            }

            if (new Integer(0).equals(fileEntry.getCisdir())) {

                boolean success = true;

                File tmpDir = new File(appConfig.APP_TEMP_DIR);

                java.io.File tmpFile = java.io.File.createTempFile("jsocs-", ".tmp", tmpDir);

                storageMgr.extract(newPath, tmpFile);

                if (success) {
                    response.setContentType("application/octet-stream");
                    response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

                    try (
                            BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(tmpFile));
                            BufferedOutputStream outStream = new BufferedOutputStream(response.getOutputStream());
                    ) {

                        org.apache.commons.io.IOUtils.copy(inputStream, outStream);
                        outStream.flush();

                    } finally {

                    }
                }
            } else {
                response.setContentType("application/json");
                List<ExtendedFileEntry> entries = extendedFileRepo.findFileEntriesByPathWithParent(newPath);
                try (PrintWriter out = response.getWriter()) {
                    out.write("{ \"data\":" + new Gson().toJson(entries) + " }");
                    out.flush();
                } finally {

                }
            }

        } catch (IOException ex) {

        } catch (InvalidCryptoParamException e) {
            e.printStackTrace();
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
