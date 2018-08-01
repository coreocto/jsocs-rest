package org.coreocto.dev.jsocs.rest.ctrl;

import org.coreocto.dev.jsocs.rest.Constant;
import org.coreocto.dev.jsocs.rest.nio.StorageManager;
import org.coreocto.dev.jsocs.rest.pojo.Block;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class PageController {

    private Logger logger = LoggerFactory.getLogger(PageController.class);

    @Autowired
    StorageManager storageMgr;

    @RequestMapping("/")
    public String viewLogin() {
        storageMgr.init();
        return "login";
    }

    @RequestMapping("/accounts")
    public String viewAccounts(Model model) {
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
}
