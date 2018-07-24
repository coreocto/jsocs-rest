package org.coreocto.dev.jsocs.rest.ctrl;

import com.google.gson.Gson;
import org.coreocto.dev.jsocs.rest.Constant;
import org.coreocto.dev.jsocs.rest.db.AccountService;
import org.coreocto.dev.jsocs.rest.db.FileService;
import org.coreocto.dev.jsocs.rest.exception.MissingAccessTokenException;
import org.coreocto.dev.jsocs.rest.exception.MissingTokenException;
import org.coreocto.dev.jsocs.rest.nio.IRemoteStorage;
import org.coreocto.dev.jsocs.rest.nio.RemoteStorageFactory;
import org.coreocto.dev.jsocs.rest.nio.StorageMgr;
import org.coreocto.dev.jsocs.rest.pojo.Account;
import org.coreocto.dev.jsocs.rest.pojo.FileEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
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
    RemoteStorageFactory remoteStorageFactory;

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
    @RequestMapping("/api/files/c")
    public String save(@RequestParam("path") String path, @RequestParam("file") MultipartFile file) {

        boolean success = true;

        File targetFile = null;

        try {
            targetFile = new File("r:\\temp\\" + file.getOriginalFilename());
            file.transferTo(targetFile);
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

        if (success) {
            return "success";
        } else {
            return "failed";
        }
    }

    @RequestMapping("/api/files/r")
    public String listFiles(@RequestParam("path") Optional<String> path) {

        String newPath = null;

        if (path.isPresent()) {
            newPath = path.get();
            if (!newPath.endsWith(Constant.PATH_SEP)) {
                newPath += Constant.PATH_SEP;
            }
        } else {
            newPath = Constant.PATH_SEP;
        }

        List<FileEntry> fileList = new ArrayList<>();
        FileEntry parent = new FileEntry();
        parent.setCid(-2);
        parent.setCname("..");
        fileList.add(parent);
        fileList.addAll(fileService.getFiles(newPath, true));

        return "{ \"data\":" + new Gson().toJson(fileList) + " }";
    }

    @RequestMapping("/api/files/d")
    public String deleteFile(@RequestParam("fileName") String fileName) {

        boolean success = true;

        try {
            storageMgr.delete(fileName);
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
