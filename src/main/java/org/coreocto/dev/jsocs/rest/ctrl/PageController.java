package org.coreocto.dev.jsocs.rest.ctrl;

import org.apache.commons.io.FilenameUtils;
import org.coreocto.dev.jsocs.rest.Constant;
import org.coreocto.dev.jsocs.rest.db.AccountService;
import org.coreocto.dev.jsocs.rest.db.BlockService;
import org.coreocto.dev.jsocs.rest.db.FileService;
import org.coreocto.dev.jsocs.rest.nio.StorageMgr;
import org.coreocto.dev.jsocs.rest.pojo.Account;
import org.coreocto.dev.jsocs.rest.pojo.Block;
import org.coreocto.dev.jsocs.rest.pojo.FileEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class PageController {

    Logger logger = LoggerFactory.getLogger(PageController.class);

    @Autowired
    AccountService accountService;

    @Autowired
    BlockService blockService;

    @Autowired
    FileService fileService;

    @RequestMapping("/")
    public String viewLogin(){
        return "login";
    }

    @RequestMapping("/accounts")
    public String viewAccounts(Model model) {
        List<Account> accountList = accountService.getAllAccounts();
        model.addAttribute("accountList", accountList);
        return "accountsView";
    }

    @RequestMapping("/accounts/add")
    public String addAccount(Model model) {
        return "addAccount";
    }

    @RequestMapping(value = "/storage", method = RequestMethod.GET)
    public String viewStorage(Model model) {

        List<Block> blockList = new ArrayList<>();//blockService.getAllBlocks();

        model.addAttribute("blockList", blockList);

        return "storageView";
    }

    @RequestMapping(value = "/files", method = RequestMethod.GET)
    public String viewFiles(Model model, @RequestParam("path") Optional<String> path) {

        String findPath = null;

        if (!path.isPresent()) {
            findPath = Constant.PATH_SEP;
        } else {
            findPath = path.get();
        }

        logger.debug("findPath = " + findPath);

//        List<FileEntry> fileList = fileService.getFiles(findPath);
        model.addAttribute("path", findPath);
//        model.addAttribute("fileList", fileList);

        return "filesView";
    }

    @Autowired
    StorageMgr storageMgr;

//    @RequestMapping(value = "/files/download/**", method = RequestMethod.GET)
//    public void downloadFile(HttpServletRequest request,
//                             HttpServletResponse response) {
//
//        String filePath = (String)
//                request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
//        filePath = filePath.substring("/files/download/".length()-1);
//
//        //check whether the target file exists
//        long fileSize = -1;
//
//        FileEntry fileEntry = null;
//
//        String parentPath = FilenameUtils.getFullPathNoEndSeparator(filePath);
//        String fileName = FilenameUtils.getName(filePath);
//
//        try {
//            FileEntry parent = fileService.getByPath(parentPath);
//            fileEntry = fileService.getByParentAndName(parent.getCid(), fileName);
//        } catch (Exception ex) {
//
//        }
//
//        if (fileEntry != null) {
//
//            fileSize = fileEntry.getCsize();
//
//            java.io.File tmpFile = null;
//
//            try {
//                tmpFile = java.io.File.createTempFile("jsocs-", ".tmp", new java.io.File("r:\\temp"));
//                storageMgr.extract(filePath, tmpFile);
//            } catch (IOException ex) {
//                ex.printStackTrace();
//            }
//
//            List<Block> fileBlocks = blockService.getByFileId(fileEntry.getCid());
//
//            //create input stream from occupied blocks and concatenate them together according to their order (defined by cid)
////            InputStream in = null;
////            InputStream prevInputStream = null;
//
//            response.setContentType("application/octet-stream");
//            response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
//
//            try (
//                    BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(tmpFile));
//                    BufferedOutputStream outStream = new BufferedOutputStream(response.getOutputStream());
//            ) {
//
//                org.apache.commons.io.IOUtils.copy(inputStream, outStream);
//                outStream.flush();
//
//            } catch (IOException e) {
////                logger.error(e.getMessage(), e);
//            }
//        } else {
//
//        }
//    }
}
