package org.coreocto.dev.jsocs.rest.ctrl;

import com.google.gson.Gson;
import org.apache.commons.io.FilenameUtils;
import org.coreocto.dev.jsocs.rest.Constant;
import org.coreocto.dev.jsocs.rest.db.AccountService;
import org.coreocto.dev.jsocs.rest.db.BlockService;
import org.coreocto.dev.jsocs.rest.db.FileService;
import org.coreocto.dev.jsocs.rest.exception.MissingAccessTokenException;
import org.coreocto.dev.jsocs.rest.exception.MissingTokenException;
import org.coreocto.dev.jsocs.rest.nio.IRemoteStorage;
import org.coreocto.dev.jsocs.rest.nio.RemoteStorageFactory;
import org.coreocto.dev.jsocs.rest.nio.StorageMgr;
import org.coreocto.dev.jsocs.rest.pojo.Account;
import org.coreocto.dev.jsocs.rest.pojo.Block;
import org.coreocto.dev.jsocs.rest.pojo.FileEntry;
import org.coreocto.dev.jsocs.rest.repo.FileRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.websocket.server.PathParam;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
public class AjaxController {

    @Autowired
    StorageMgr storageMgr;

    @Autowired
    AccountService accountService;

    @Autowired
    FileService fileService;

    @Autowired
    BlockService blockService;

    @Autowired
    RemoteStorageFactory remoteStorageFactory;

    @Autowired
    FileRepo fileRepo;

//    @RequestMapping("/api/storage/init")
//    public String init(
//            @RequestParam("userId") Optional<Integer> userId
//    ) {
//
//        boolean success = true;
//
//        try {
//            if (userId.isPresent()) {
//                Account userAcc = accountService.getById(userId.get());
//                IRemoteStorage remoteStorage = remoteStorageFactory.make(userAcc.getCtype(), userAcc.getCid(), userAcc.getCauthToken());
//                storageMgr.init(remoteStorage);
//            } else {
//                List<Account> accList = accountService.getAllAccounts();
//                for (Account userAcc : accList) {
//                    IRemoteStorage remoteStorage = remoteStorageFactory.make(userAcc.getCtype(), userAcc.getCid(), userAcc.getCauthToken());
//                    storageMgr.init(remoteStorage);
//                }
//            }
//
//        } catch (MissingTokenException e) { //need further handling
//            success = false;
//        } catch (MissingAccessTokenException e){
//            success = false;
//        }
//
//        if (success) {
//            return "success";
//        } else {
//            return "failed";
//        }
//    }

    //accounts

    private boolean isAccountExists(String username, String type) {
        boolean found = false;
        try {
            accountService.getByUsername(username, type);
            found = true;
        } catch (Exception ex) {
            found = false;
        }
        return found;
    }

    private boolean isAccountExists(int userId) {
        boolean found = false;
        try {
            accountService.getById(userId);
            found = true;
        } catch (Exception ex) {
            found = false;
        }
        return found;
    }

    @RequestMapping("/api/accounts/c")
    public String createAccount(@RequestParam("username") String username, @RequestParam("type") String type) {

        boolean found = isAccountExists(username, type);

        if (!found) {
            accountService.create(username, type);
            return "success";
        } else {
            return "failed";
        }
    }

    @RequestMapping("/api/accounts/r")
    public String listAccounts() {

        List<Account> accountList = accountService.getAllAccounts();

        return "{ \"data\":" + new Gson().toJson(accountList) + " }";
    }

    @RequestMapping(value = "/api/accounts/d", method = RequestMethod.POST)
    public String deleteAccount(@RequestParam("userId") int userId) {

        boolean success = true;
        try {
            accountService.deleteById(userId);
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
    @PostMapping("/api/files")
    public String save(@RequestParam("path") String path, @RequestParam("folder") Optional<String> folder, @RequestParam("file") Optional<MultipartFile> file) {

        boolean success = true;

        if (file.isPresent()) {

            File targetFile = null;

            try {
                targetFile = new File("r:\\temp\\" + file.get().getOriginalFilename());
                file.get().transferTo(targetFile);
            } catch (IOException e) {
                success = false;
            }

            if (success) {

                try {
                    storageMgr.save(path, targetFile);
                } catch (IOException e) {
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
        return "{ \"data\":" + new Gson().toJson(fileService.getFiles(Constant.PATH_SEP, true)) + " }";
    }

    @GetMapping("/api/files/**")
    public void listFiles(HttpServletRequest request, HttpServletResponse response) {

        String path = (String)
                request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        path = path.substring("/api/files/".length()-1);

        String newPath = null;

        if (path!=null) {
            newPath = path;
            if (!newPath.startsWith(Constant.PATH_SEP)) {
                newPath = Constant.PATH_SEP + newPath;
            }
        } else {
            newPath = Constant.PATH_SEP;
        }

        FileEntry fileEntry = null;

        String parentPath = FilenameUtils.getFullPathNoEndSeparator(newPath);
        String fileName = FilenameUtils.getName(newPath);

        try {
            FileEntry parent = fileService.getByPath(parentPath);
            fileEntry = fileService.getByParentAndName(parent.getCid(), fileName);
        } catch (Exception ex) {

        }

        if (new Integer(0).equals(fileEntry.getCisdir())){
            if (fileEntry != null) {

                long fileSize = fileEntry.getCsize();

                java.io.File tmpFile = null;

                try {
                    tmpFile = java.io.File.createTempFile("jsocs-", ".tmp", new java.io.File("r:\\temp"));
                    storageMgr.extract(newPath, tmpFile);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }

                List<Block> fileBlocks = blockService.getByFileId(fileEntry.getCid());

                response.setContentType("application/octet-stream");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");

                try (
                        BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(tmpFile));
                        BufferedOutputStream outStream = new BufferedOutputStream(response.getOutputStream());
                ) {

                    org.apache.commons.io.IOUtils.copy(inputStream, outStream);
                    outStream.flush();

                } catch (IOException e) {
//                logger.error(e.getMessage(), e);
                }
            } else {

            }
        }else{
            response.setContentType("application/json");
            try (PrintWriter out = response.getWriter()){
out.write("{ \"data\":" + new Gson().toJson(fileService.getFiles(newPath, true)) + " }");
out.flush();
            }catch(IOException ex){

            }
//            return ResponseEntity.ok().contentType(MediaType.parse("appplication/json"));
//            return "{ \"data\":" + new Gson().toJson(fileService.getFiles(newPath, true)) + " }";
        }

//        String parentPath = FileSystemUtils
//
//        Resource resource =




    }

    @DeleteMapping("/api/files/**")
//    @RequestMapping("/api/files/d")
    public String deleteFile(HttpServletRequest request) {

        String path = (String)
                request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        path = path.substring("/api/files/".length()-1);

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
