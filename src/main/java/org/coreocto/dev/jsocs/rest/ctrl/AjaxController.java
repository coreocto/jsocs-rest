package org.coreocto.dev.jsocs.rest.ctrl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.HttpStatus;
import org.checkerframework.checker.units.qual.A;
import org.coreocto.dev.jsocs.rest.Constant;
import org.coreocto.dev.jsocs.rest.config.AppConfig;
import org.coreocto.dev.jsocs.rest.exception.InvalidCryptoParamException;
import org.coreocto.dev.jsocs.rest.nio.MultipartFileSender;
import org.coreocto.dev.jsocs.rest.nio.StorageManager;
import org.coreocto.dev.jsocs.rest.pojo.Account;
import org.coreocto.dev.jsocs.rest.pojo.ExtendedFileEntry;
import org.coreocto.dev.jsocs.rest.pojo.RequestEntry;
import org.coreocto.dev.jsocs.rest.pojo.VideoCache;
import org.coreocto.dev.jsocs.rest.repo.*;
import org.coreocto.dev.jsocs.rest.resp.JsonResponseFactory;
import org.coreocto.dev.jsocs.rest.util.CipherUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.WebAsyncTask;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.ws.Response;
import java.io.*;
import java.nio.charset.Charset;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

@RestController
public class AjaxController {

    private final Logger logger = LoggerFactory.getLogger(AjaxController.class);
    @Autowired
    private StorageManager storageMgr;
    @Autowired
    private FileRepo fileRepo;
    @Autowired
    private ExtendedFileRepo extendedFileRepo;
    @Autowired
    private AccountRepo accountRepo;
    @Autowired
    private AppConfig appConfig;
    @Autowired
    private JsonResponseFactory jsonResponseFactory;
    @Autowired
    private VideoCacheRepo videoCacheRepo;
    @Autowired
    private RequestRepo requestRepo;
    @Autowired
    private CipherUtil cipherUtil;

    @PostConstruct
    public void init() {
        storageMgr.init();
    }

    private RequestEntry logEntry(HttpServletRequest request){
        RequestEntry req = new RequestEntry();
        req.setCrequesturi(request.getRequestURI());
        req.setCcrtdt(new Date());
        requestRepo.save(req);
        return req;
    }

    private void logExit(RequestEntry requestEntry, String output){
        requestEntry.setCupddt(new Date());
        requestEntry.setCresponse(output);
        requestRepo.save(requestEntry);
    }

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
    public String createAccount(
            @RequestParam("username") String username, @RequestParam("type") String type,
            HttpServletRequest request
    ) {

        RequestEntry entry = this.logEntry(request);

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

        String respStr = resp.toString();

        this.logExit(entry, respStr);

        return resp.toString();
    }

    @GetMapping(value = "/api/accounts", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String listAccounts(HttpServletRequest request) {

        RequestEntry entry = this.logEntry(request);

        List<Account> accountList = accountRepo.findAll();
        byte[] bytes = new Gson().toJson(accountList).getBytes();
        String s = "\"\"";
        try {
            s = new String(bytes, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        String respStr = "{\"data\":" + s + "}";

        this.logExit(entry, respStr);

        return respStr;
    }

    @DeleteMapping(value = "/api/accounts/{userId}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String deleteAccount(@PathVariable("userId") int userId, HttpServletRequest request) {

        RequestEntry entry = this.logEntry(request);

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

        String respStr = resp.toString();

        this.logExit(entry, respStr);

        return respStr;
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

    @PostMapping(value = POST_API_FILES, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String save(@RequestParam("path") String path, @RequestParam("folder") Optional<String> folder, @RequestParam("file") Optional<MultipartFile> file, HttpServletRequest request) {

        RequestEntry entry = this.logEntry(request);

        JsonObject resp = null;

        // currently, this method starts handling file upload once the upload is completed
        // if the file is big in size, user would need to wait a prolonged period of time
        // i tried Apache Commons FileUpload with DiskFileItemFactory, but it gives similar behavior as what spring boot offers.
        // at the moment, there appears no other way to handle this problem.

        try {

            if (file.isPresent()) {
                File tmpDir = new File(appConfig.APP_TEMP_DIR);

                String tmpStr = file.get().getOriginalFilename();

                File targetFile = new File(tmpDir, tmpStr);
                file.get().transferTo(targetFile);

                storageMgr.save(path, targetFile);

//                MultipartFile mpf = file.get();
//                mpf.
//
//                try (InputStream is = file.get().getInputStream()){
//                    storageMgr.save(path, is, mpf.getSize());
//                }finally {
//
//                }

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

            String respStr = resp.toString();

            this.logExit(entry, respStr);

            return respStr;
        }
    }

    //merged code into listFiles(HttpServletRequest, HttpServletResponse)
//    @GetMapping(value = API_FILES, produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
//    public String listFiles() {
//        List<ExtendedFileEntry> entries = null;
//        String s = "\"\"";
//        try {
//            entries = storageMgr.list(Constant.PATH_SEP);
//            s = new Gson().toJson(entries);
//        } catch (FolderNotFoundException e) {
//            logger.debug("given path does not exists", e);
//        }
//
//        return "{ \"data\":" + s + " }";
//    }

    @GetMapping(value = API_FILES + "**")
    public StreamingResponseBody listFiles(HttpServletRequest request, HttpServletResponse response) {

        RequestEntry entry = this.logEntry(request);

        String path = extractFilePath(request);

        String newPath = pathMassage(path);

        String fileName = FilenameUtils.getName(newPath);

        ExtendedFileEntry fileEntry = extendedFileRepo.findFileEntryByPath(newPath);

        try {

            if (fileEntry == null) {
                response.setStatus(HttpStatus.SC_NOT_FOUND);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                this.logExit(entry, null);

                String body = "{\"status\":\"error\", \"message\":\"file not found\"}";
                return out -> out.write(body.getBytes());
            }

            if (new Integer(0).equals(fileEntry.getCisdir())) {

                this.logExit(entry, null);

                response.setStatus(HttpStatus.SC_OK);
                response.setContentType(MediaType.APPLICATION_OCTET_STREAM_VALUE);
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"");
                response.setContentLengthLong(fileEntry.getCsize());

                StreamingResponseBody body = outputStream -> {
                    try {
                        storageMgr.extract(newPath, outputStream, -1);
                    } catch (InvalidCryptoParamException e) {
                        logger.error("invalid encryption/decryption parameter", e);
                    }
                };

                return body;

//                return ResponseEntity
//                        .ok()
//                        .contentType(MediaType.valueOf(MediaType.APPLICATION_OCTET_STREAM_VALUE))
//                        .contentLength(fileEntry.getCsize())
//                        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
//                        .body(body);

                // i get a HttpMediaTypeNotAcceptableException when tried to use StreamingResponseBody with ResponseEntity
                // it seems it is needed to define HttpMessageConverter to parse StreamingResponseBody in correct way
//                return out -> {
//                    try {
//                        storageMgr.extract(newPath, out, -1);
//                    } catch (InvalidCryptoParamException e) {
//                        logger.debug(e.getMessage(), e);
//                    }
//                };





//                return new ResponseEntity(body, org.springframework.http.HttpStatus.OK);

//                return outputStream -> {
//                    try {
//                        storageMgr.extract(newPath, outputStream, -1);
//                    } catch (InvalidCryptoParamException e) {
//                        logger.debug(e.getMessage(), e);
//                    }
//                };

//                InputStream inputStream = storageMgr.extractAsInputStream(newPath, -1);
//
////                return new ResponseEntity(responseBody, HttpStatus.SC_OK);
//
//                InputStreamResource res = new InputStreamResource(inputStream);
//
//                return ResponseEntity
//                        .ok()
//                        .header("Content-Disposition", "attachment; filename=\"" + fileName + "\"")
////                        .header("Content-Type","application/octet-stream")
//                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
//                        .contentLength(fileEntry.getCsize())
//                        .body(responseBody);
            } else {
//                response.setContentType("application/json");
                List<ExtendedFileEntry> entries = null;

                if (Constant.PATH_SEP.equalsIgnoreCase(newPath)) {
                    entries = storageMgr.list(Constant.PATH_SEP);
                } else {
                    entries = extendedFileRepo.findFileEntriesByPathWithParent(newPath);
                }

                this.logExit(entry, null);

                String s = "{ \"data\":" + new Gson().toJson(entries) + " }";

                return out -> out.write(s.getBytes());

//                response.setStatus(HttpStatus.SC_OK);
//                response.setContentType(MediaType.APPLICATION_JSON_VALUE);



//                StreamingResponseBody body = outputStream -> {
//                    outputStream.write(s.getBytes());
//                };
//
//                return new ResponseEntity(body, org.springframework.http.HttpStatus.OK);

//                return outputStream -> outputStream.write(s.getBytes());

//                return ResponseEntity
//                        .ok()
//                        .contentType(MediaType.APPLICATION_JSON_UTF8)
//                        .body(s);

//                return out -> out.write(s.getBytes());
            }

        } catch (Exception ex) {
            logger.error("error when handling listFiles() request", ex);
        }

        this.logExit(entry, null);

        response.setStatus(HttpStatus.SC_BAD_REQUEST);
        return null;
    }

    private String extractFilePath(HttpServletRequest request) {
        String path = (String)
                request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        if (path == null) {
            return null;
        } else {
            return path.substring(API_FILES.length() - 1);
        }
    }

    private String extractVideoPath(HttpServletRequest request, String apiUrlPrefix) {
        String path = (String)
                request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        if (path == null) {
            return null;
        } else {
            return path.substring(apiUrlPrefix.length() - 1);
        }
    }

    public static final String API_VIDEO_CACHE = "/api/video/cache/";
    public static final String API_VIDEO_STREAM = "/api/video/stream/";
    public static final String API_FILES = "/api/files/";
    public static final String POST_API_FILES = "/api/files";

    @DeleteMapping(value = API_FILES + "**", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String deleteFile(HttpServletRequest request) {

        RequestEntry entry = this.logEntry(request);

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

        String respStr = resp.toString();

        this.logExit(entry, respStr);

        return respStr;
    }
    // end of files

//    private List<File> buildBlocks(File file, long blockSize_2K) {
//
////        long blockSize_2K = 2*1024*1024;
//
//        List<File> blockList = new ArrayList<>();
//
//        long remainSize = file.length();
//
//        File tmp = new File("R:\\SampleVideo_1280x720_30mb.mp4" + ".enc.part5");
//        if (tmp.exists()) {
//            blockList.add(new File("R:\\SampleVideo_1280x720_30mb.mp4.enc.part0"));
//            blockList.add(new File("R:\\SampleVideo_1280x720_30mb.mp4.enc.part1"));
//            blockList.add(new File("R:\\SampleVideo_1280x720_30mb.mp4.enc.part2"));
//            blockList.add(new File("R:\\SampleVideo_1280x720_30mb.mp4.enc.part3"));
//            blockList.add(new File("R:\\SampleVideo_1280x720_30mb.mp4.enc.part4"));
//            blockList.add(new File("R:\\SampleVideo_1280x720_30mb.mp4.enc.part5"));
//        } else {
//
//
//            try (InputStream input = new BufferedInputStream(new FileInputStream(file))) {
//
//                while (remainSize > 0) {
//
//                    long writeSize = remainSize >= blockSize_2K ? blockSize_2K : remainSize;
//
//                    File partFile = new File("R:\\SampleVideo_1280x720_10mb.mp4" + ".enc.part" + blockList.size());
//
//                    try (OutputStream output = new BufferedOutputStream(new CipherOutputStream(new FileOutputStream(partFile), cipherUtil.getCipher(Cipher.ENCRYPT_MODE)))) {
//
//                        IOUtils.copyLarge(input, output, 0, writeSize);
//
//                        output.flush();
//
//                    } catch (Exception ex) {
//                        logger.debug("error when splitting file", ex);
//                    }
//
//                    blockList.add(partFile);
//
//                    remainSize -= writeSize;
//                }
//
//            } catch (FileNotFoundException e) {
//                e.printStackTrace();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
//
//        return blockList;
//    }

    // begin of video
//    @GetMapping(value = API_VIDEO_CACHE + "**", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
//    public WebAsyncTask<String> cacheVideo(HttpServletRequest request) {
//        String path = extractVideoPath(request, API_VIDEO_CACHE);
//        String newPath = pathMassage(path);
//
//        final File cacheDir = new File(appConfig.APP_VIDEO_CACHE_DIR);
//
//        // 模拟开启一个异步任务，超时时间为10s
//        WebAsyncTask<String> asyncTask = new WebAsyncTask<String>(3600 * 1000, () -> {
//            JsonObject resp = null;
//
//            ExtendedFileEntry fileEntry = extendedFileRepo.findFileEntryByPath(newPath);
//            if (fileEntry == null) {
//                resp = jsonResponseFactory.getError(new FileNotFoundException(newPath));
//            } else {
//                resp = jsonResponseFactory.getSuccess();
//                VideoCache videoCache = videoCacheRepo.findByFileId(fileEntry.getCid());
//
//                boolean fsDeleted = false;
//
//                if (videoCache != null) {
//                    File cacheFile = new File(cacheDir, videoCache.getCfilename());
//                    if (!cacheFile.exists()) {
//                        fsDeleted = true;
//                    }
//                }
//
//                if (videoCache == null || fsDeleted) {
//                    String cacheFileName = UUID.randomUUID().toString() + ".cache";
//                    File cacheFile = new File(cacheDir, cacheFileName);
//                    storageMgr.extract(newPath, cacheFile, -1);
//
//                    if (!fsDeleted) {
//                        videoCache = new VideoCache();
//                        videoCache.setCfileid(fileEntry.getCid());
//                        videoCache.setCcrtdt(new Date());
//                    }
//
//                    videoCache.setCfilename(cacheFileName);
//                    videoCacheRepo.save(videoCache);
//                }
//
//                if (videoCache != null) {
//                    resp.addProperty("url", API_VIDEO_STREAM + fileEntry.getCname());
//                }
//            }
//            return resp.toString();
//        });
//
//        asyncTask.onCompletion(() -> logger.debug("caching process done."));
//        asyncTask.onError(() -> {
//            logger.debug("error occurred during caching process");
//            JsonObject resp = jsonResponseFactory.getError(new Exception());
//            return resp.toString();
//        });
//
//        return asyncTask;
//    }

    @GetMapping(value = API_VIDEO_STREAM + "**")
    public StreamingResponseBody getVideo(HttpServletRequest request, HttpServletResponse response) {

        RequestEntry entry = this.logEntry(request);

        String path = extractVideoPath(request, API_VIDEO_STREAM);
        String newPath = pathMassage(path);

        ExtendedFileEntry fileEntry = extendedFileRepo.findFileEntryByPath(newPath);

        if (fileEntry == null) {
            this.logExit(entry, null);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        } else {

            VideoCache videoCache = videoCacheRepo.findByFileId(fileEntry.getCid());

            if (videoCache == null) {
                this.logExit(entry, null);
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return null;
            } else {
                File cacheDir = new File(appConfig.APP_VIDEO_CACHE_DIR);
                File cacheFile = new File(cacheDir, videoCache.getCfilename());

                if (!cacheFile.exists()) {
                    this.logExit(entry, null);
                    response.setStatus(HttpServletResponse.SC_NOT_FOUND);
                    return null;
                } else {

                    this.logExit(entry, null);

                    return new StreamingResponseBody() {

                        @Override
                        public void writeTo(OutputStream outputStream) throws IOException {
                            InputStream inputStream = null;

                            try {
                                inputStream = new FileInputStream(cacheFile);

                                MultipartFileSender
                                        .fromInputStream(inputStream)
                                        .withFileName(fileEntry.getCname())
                                        .withMimetype("video/mp4")
                                        .withLength(fileEntry.getCsize())
                                        .with(request)
                                        .with(response)
                                        .withOutputStream(outputStream)
                                        .serveResource();

                            } catch (Exception e) {
                                logger.error("error when process video stream", e);
                            }
                        }
                    };

//                    InputStream inputStream = null;
//
//                    try {
//                        inputStream = new FileInputStream(cacheFile);
//
//                        MultipartFileSender
//                                .fromInputStream(inputStream)
//                                .withFileName(fileEntry.getCname())
//                                .withMimetype("video/mp4")
//                                .withLength(fileEntry.getCsize())
//                                .with(request)
//                                .with(response)
//                                .withOutputStream()
//                                .serveResource();
//
//                    } catch (FileNotFoundException e) {
//                        e.printStackTrace();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
                }
            }
        }
    }

//    @GetMapping(value = "/api/video/**")
//    public void getVideo(HttpServletRequest request, HttpServletResponse response) throws Exception {
//        long blockSize_2K = 2 * 1024 * 1024;
//
//        MultipartFileSender sender = new MultipartFileSender()
//                .with(response)
//                .with(request);
//
//        MultipartFileSender.Range full = sender.getFullRange();
//        List<MultipartFileSender.Range> ranges = sender.getRanges(full, true);
//
//        File baseFile = new File("r:\\SampleVideo_1280x720_30mb.mp4");
//
//        List<File> video = buildBlocks(baseFile, blockSize_2K);
//
////        int startPart = 0;
////        int endPart = video.size() - 1;
////
////        if (ranges != null && ranges.size() > 0) {
////            MultipartFileSender.Range range = ranges.get(0);
////            startPart = (int) (range.start / blockSize_2K);
////            endPart = (int) (range.end / blockSize_2K);
////        }
////
////        logger.debug("start-{}, end-{}", startPart, endPart);
////
////        List<File> video2 = video.subList(startPart, endPart);
//
//        InputStream is = null;
//
//        for (File f : video) {
//            if (is == null) {
//                is = new CipherInputStream(new FileInputStream(f), getCipher(Cipher.DECRYPT_MODE));
//            } else {
//                is = new SequenceInputStream(is, new CipherInputStream(new FileInputStream(f), getCipher(Cipher.DECRYPT_MODE)));
//            }
//        }
//
//        sender.setInputStream(is);
//        sender
//                .withFileName(baseFile.getName())
//                .withMimetype(Files.probeContentType(baseFile.toPath()))
//                .withLength(baseFile.length())
//                .withOffset(0)  //range.start % blockSize_2K
//                .serveResource();
//    }

    // end of video
}
