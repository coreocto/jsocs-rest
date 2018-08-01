//package org.coreocto.dev.jsocs.rest.nio;
//
//import com.google.gson.Gson;
//import com.google.gson.JsonObject;
//import com.pcloud.sdk.*;
//import okhttp3.*;
//import org.apache.commons.codec.digest.DigestUtils;
//import org.apache.commons.codec.digest.MessageDigestAlgorithms;
//import org.apache.commons.io.IOUtils;
//import org.coreocto.dev.jsocs.rest.exception.InvalidChecksumException;
//import org.coreocto.dev.jsocs.rest.exception.InvalidResponseException;
//import org.coreocto.dev.jsocs.rest.exception.UploadFailedException;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.*;
//import java.util.HashMap;
//import java.util.Map;
//
//public class PCloudRSImpl implements IRemoteStorage {
//
//    private final Logger logger = LoggerFactory.getLogger(PCloudRSImpl.class);
//
//    private final int userId;
//    private final String oauthToken;
//    private final ApiClient apiClient;
//    private final OkHttpClient okHttpClient = new OkHttpClient();
//
//    public PCloudRSImpl(int userId, String oauthToken) {
//        this.userId = userId;
//        this.oauthToken = oauthToken;
//        apiClient = PCloudSdk.newClientBuilder()
//                .authenticator(Authenticators.newOAuthAuthenticator(oauthToken))
//                .create();
//    }
//
//    private static final HttpUrl API_BASE_URL = HttpUrl.parse("https://api.pcloud.com");
//
//    private Request.Builder newRequest() {
//        return new Request.Builder().url(API_BASE_URL);
//    }
//
//    @Override
//    public void update() {
//        throw new UnsupportedOperationException();
//    }
//
//    @Override
//    public void delete(long fileId) throws IOException {
//
//        try {
//            apiClient.deleteFile(fileId).execute();
//        } catch (ApiError apiError) {
//            logger.error("error when deleting remote file, fileId: " + fileId, apiError);
//        }
//
//    }
//
//    @Override
//    public void download(String fileName, File targetFile) throws IOException {
//        Request request = new Request.Builder().url(fileName).addHeader("Authorization", "Bearer " + oauthToken).build();
//        Response response = okHttpClient.newCall(request).execute();
//        try (
//                BufferedInputStream input = new BufferedInputStream(response.body().byteStream());
//                BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(targetFile));
//        ) {
//            IOUtils.copyLarge(input, output);
//        } finally {
//
//        }
//    }
//
//    private String getChecksum(long fileId) {
//
//        String checksum = null;
//
//        FormBody.Builder builder = new FormBody.Builder()
//                .add("fileid", String.valueOf(fileId));
//
//        Request request = newRequest()
//                .url(API_BASE_URL.newBuilder()
//                        .addPathSegment("checksumfile")
//                        .build())
//                .post(builder.build())
//                .addHeader("Authorization", "Bearer " + oauthToken)
//                .build();
//
//        Gson gson = new Gson();
//
//        try {
//            Response response = okHttpClient.newCall(request).execute();
//            String responseStr = response.body().string();
//            JsonObject outObj = gson.fromJson(responseStr, JsonObject.class);
//
//            if (outObj.has("md5")) {
//                checksum = outObj.get("md5").getAsString();
//            } else {
//                throw new InvalidResponseException();
//            }
//        } catch (IOException ex) {
//            logger.error("error when getting checksum for remote file", ex);
//        }
//
//        return checksum;
//    }
//
//    @Override
//    public Map<String, Object> upload(File srcFile, String fileName) throws IOException {
//
//        Map<String, Object> result = new HashMap<>();
//
//        String hex = new DigestUtils(MessageDigestAlgorithms.MD5).digestAsHex(srcFile);
//
//        RemoteFile remoteFile = null;
//
//        boolean updateSuccess = false;
//
//        try {
//            remoteFile = apiClient.createFile(RemoteFolder.ROOT_FOLDER_ID, fileName, DataSource.create(srcFile)).execute();
//
//            result.put("fileId", "" + remoteFile.fileId());
//
//            FileLink downloadLink = apiClient.createFileLink(remoteFile, DownloadOptions.DEFAULT).execute();
//            result.put("downloadLink", downloadLink.bestUrl().toString());
//
//            updateSuccess = true;
//
//        } catch (ApiError apiError) {
//            logger.error("error when uploading file: " + srcFile + " to cloud storage", apiError);
//            throw new UploadFailedException();
//        }
//
//        if (updateSuccess) {
//            String remoteChecksum = this.getChecksum(remoteFile.fileId());
//            logger.debug("local checksum: " + hex + ", remote checksum: " + remoteChecksum);
//
//            if (!hex.equals(remoteChecksum)) {
//                delete(remoteFile.fileId());
//                throw new InvalidChecksumException();
//            }
//        }
//
//        return result;
//    }
//
//    @Override
//    public long getAvailable() throws IOException {
//        long result = 0;
//        try {
//            UserInfo userInfo = apiClient.getUserInfo().execute();
//            result = userInfo.totalQuota() - userInfo.usedQuota();
//        } catch (ApiError apiError) {
//            logger.error("error when inquiring available space", apiError);
//        }
//        return result;
//    }
//}
