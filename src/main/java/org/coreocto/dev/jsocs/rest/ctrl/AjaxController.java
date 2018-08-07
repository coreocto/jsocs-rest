package org.coreocto.dev.jsocs.rest.ctrl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.io.FilenameUtils;
import org.coreocto.dev.jsocs.rest.Constant;
import org.coreocto.dev.jsocs.rest.config.AppConfig;
import org.coreocto.dev.jsocs.rest.exception.FolderNotFoundException;
import org.coreocto.dev.jsocs.rest.nio.StorageManager;
import org.coreocto.dev.jsocs.rest.pojo.Account;
import org.coreocto.dev.jsocs.rest.pojo.ExtendedFileEntry;
import org.coreocto.dev.jsocs.rest.repo.AccountRepo;
import org.coreocto.dev.jsocs.rest.repo.ExtendedFileRepo;
import org.coreocto.dev.jsocs.rest.repo.FileRepo;
import org.coreocto.dev.jsocs.rest.resp.JsonResponseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
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
    @Autowired
    JsonResponseFactory jsonResponseFactory;

    //accounts
    private boolean isAccountExists(String username, String type) {
        Account account = null;
        try {
            account = accountRepo.findByNameAndType(username, type);
        } catch (Exception ex) {
        }
        return account != null;
    }

    @PostMapping(value = "/api/accounts", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String createAccount(@RequestParam("username") String username, @RequestParam("type") String type) {

        boolean found = isAccountExists(username, type);

        JsonObject resp = null;

        if (!found) {
            Account newAcc = new Account();
            newAcc.setCusername(username);
            newAcc.setCtype(type);
            try {
                accountRepo.save(newAcc);
            } catch (Exception ex) {
                logger.error("error when creating account: " + new Gson().toJson(newAcc), ex);
                resp = jsonResponseFactory.getError(ex);
            }
        }

        if (resp == null) {
            resp = jsonResponseFactory.getSuccess();
        }

        return resp.toString();
    }

    @GetMapping(value = "/api/accounts", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String listAccounts() {
        List<Account> accountList = accountRepo.findAll();
        byte[] bytes = new Gson().toJson(accountList).getBytes();
        String s = "\"\"";
        try {
            s = new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return "{\"data\":" + s + "}";
    }

    @DeleteMapping(value = "/api/accounts/{userId}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String deleteAccount(@PathVariable("userId") int userId) {

        JsonObject resp = null;

        try {
            accountRepo.deleteById(userId);
        } catch (Exception ex) {
            logger.error("error when deleting account with id: " + userId, ex);
            resp = jsonResponseFactory.getError(ex);
        }

        if (resp == null) {
            resp = jsonResponseFactory.getSuccess();
        }

        return resp.toString();
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

    @PostMapping(value = "/api/files", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String save(@RequestParam("path") String path, @RequestParam("folder") Optional<String> folder, @RequestParam("file") Optional<MultipartFile> file) {

        JsonObject resp = null;

        try {
            if (file.isPresent()) {
                File tmpDir = new File(appConfig.APP_TEMP_DIR);

                String tmpStr = file.get().getOriginalFilename();

                File targetFile = new File(tmpDir, tmpStr);
                file.get().transferTo(targetFile);

                storageMgr.save(path, targetFile);

            } else if (folder.isPresent()) {
                storageMgr.makeDir(path, folder.get());
            }
        } catch (Exception e) {
            logger.error("error when save()", e);
            resp = jsonResponseFactory.getError(e);
        } finally {
            if (resp == null) {
                resp = jsonResponseFactory.getSuccess();
            }
            return resp.toString();
        }
    }

    @GetMapping(value = "/api/files", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String listFiles() {
        List<ExtendedFileEntry> entries = null;
        String s = "\"\"";
        try {
            entries = storageMgr.list(Constant.PATH_SEP);
            s = new Gson().toJson(entries);
        } catch (FolderNotFoundException e) {
            e.printStackTrace();
        }

        return "{ \"data\":" + s + " }";
    }

    @GetMapping("/api/files/**")
    public void listFiles(HttpServletRequest request, HttpServletResponse response) {

        String path = extractFilePath(request);

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

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private String extractFilePath(HttpServletRequest request) {
        String path = (String)
                request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        if (path == null) {
            return null;
        } else {
            return path.substring("/api/files/".length() - 1);
        }
    }

    @DeleteMapping(value = "/api/files/**", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String deleteFile(HttpServletRequest request) {

        String path = extractFilePath(request);
        String newPath = pathMassage(path);

        JsonObject resp = null;

        try {
            storageMgr.delete(newPath);
        } catch (IOException ex) {
            logger.error("error when deleting file at path: " + newPath, ex);
            resp = jsonResponseFactory.getError(ex);
        }

        if (resp == null) {
            resp = jsonResponseFactory.getSuccess();
        }

        return resp.toString();
    }
    // end of files
}
