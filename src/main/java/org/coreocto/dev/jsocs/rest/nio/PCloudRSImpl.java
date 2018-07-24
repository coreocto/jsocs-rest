package org.coreocto.dev.jsocs.rest.nio;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.pcloud.sdk.*;
import okhttp3.*;
import okio.BufferedSink;
import okio.Okio;
import org.apache.commons.io.IOUtils;
import org.openqa.selenium.json.Json;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PCloudRSImpl implements IRemoteStorage {

    private final int userId;
    private final String oauthToken;
    private final ApiClient apiClient;
    private final OkHttpClient okHttpClient = new OkHttpClient();

    public PCloudRSImpl(int userId, String oauthToken) {
        this.userId = userId;
        this.oauthToken = oauthToken;
        apiClient = PCloudSdk.newClientBuilder()
                .authenticator(Authenticators.newOAuthAuthenticator(oauthToken))
                .create();
    }

    private static final HttpUrl API_BASE_URL = HttpUrl.parse("https://api.pcloud.com");

    private Request.Builder newRequest() {
        return new Request.Builder().url(API_BASE_URL);
    }

    @Override
    public List<Map<String, Object>> provision(File srcFile, List<String> blockNames) throws IOException {

        // when we attempt to provision a large storage, if it is done by upload one by one, it would consume to much traffic and time.
        // hence this method provides a workaround, we first upload a single block to remote storage
        // and then we create other blocks by creating clone of that block

//                ApiClient apiClient = PCloudSdk.newClientBuilder()
//                .authenticator(Authenticators.newOAuthAuthenticator(oauthToken))
//                .create();

        List<Map<String, Object>> result = new ArrayList<>();

        int length = blockNames.size();

        long templateId = -1;

        for (int i = 0; i < length; i++) {
            if (i == 0) {
                result.add(this.upload(srcFile, blockNames.get(i)));
                try {
                    templateId = Long.parseLong((String) result.get(0).get("fileId"));
                } catch (NumberFormatException e) {

                }
            } else {

                FormBody.Builder builder = new FormBody.Builder()
                        .add("fileid", String.valueOf(templateId))
                        .add("tofolderid", String.valueOf(RemoteFolder.ROOT_FOLDER_ID))
                        .add("toname", blockNames.get(i))
                        .add("noover", String.valueOf(1));

                Request request = newRequest()
                        .url(API_BASE_URL.newBuilder()
                                .addPathSegment("copyfile")
                                .build())
                        .post(builder.build())
                        .addHeader("Authorization", "Bearer " + oauthToken)
                        .build();

                try {

                    Response response = okHttpClient.newCall(request).execute();

                    String responseStr = response.body().string();

                    Gson gson = new Gson();
                    JsonObject outObj = gson.fromJson(responseStr, JsonObject.class);

                    long fileId = -1;

                    if (outObj.has("metadata")) {
                        JsonObject metadata = outObj.get("metadata").getAsJsonObject();
                        String s = metadata.get("fileid").getAsString();

                        try {
                            fileId = Long.parseLong(s);
                        } catch (NumberFormatException ex) {
                        }
                    } else {
                        throw new RuntimeException();
                    }

                    FileLink fileLink = apiClient.createFileLink(fileId, DownloadOptions.DEFAULT).execute();
                    Map<String, Object> tmp = new HashMap<>();
                    tmp.put("fileId", "" + fileId);
                    tmp.put("downloadLink", fileLink.bestUrl().toString());
                    result.add(tmp);

//                    newFile = apiClient.copyFile(templateId, RemoteFolder.ROOT_FOLDER_ID, false).execute();
//                    newFile.rename(blockNames.get(i));
                } catch (Exception e) {
//                    apiError.printStackTrace();
                    e.printStackTrace();
                }
            }
        }

//        apiClient.shutdown();

        return result;
    }

    @Override
    public void update() {

    }

    @Override
    public void delete(long fileId) throws IOException {

//        ApiClient apiClient = PCloudSdk.newClientBuilder()
//                .authenticator(Authenticators.newOAuthAuthenticator(oauthToken))
//                .create();

        try {
            apiClient.deleteFile(fileId).execute();
        } catch (ApiError apiError) {
            apiError.printStackTrace();
        }

//        apiClient.shutdown();
    }

    @Override
    public void download(String fileName, File targetFile) throws IOException {
        Request request = new Request.Builder().url(fileName).addHeader("Authorization", "Bearer " + oauthToken).build();
        Response response = okHttpClient.newCall(request).execute();
        try (
                BufferedInputStream input = new BufferedInputStream(response.body().byteStream());
                BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(targetFile));
        ) {
            IOUtils.copyLarge(input, output);
        } finally {

        }
    }

    @Override
    public Map<String, Object> upload(File srcFile, String fileName) throws IOException {

        Map<String, Object> result = new HashMap<>();

//        {
//
//            final DataSource fileDS = DataSource.create(srcFile);
//
//            RequestBody dataBody = new RequestBody() {
//                @Override
//                public MediaType contentType() {
//                    return MediaType.parse("multipart/form-data");
//                }
//
//                @Override
//                public void writeTo(BufferedSink sink) throws IOException {
////                    if (listener != null) {
////                        ProgressListener realListener = listener;
////                        if (callbackExecutor != null) {
////                            realListener = new ExecutorProgressListener(listener, callbackExecutor);
////                        }
////
////                        sink = Okio.buffer(new ProgressCountingSink(
////                                sink,
////                                data.contentLength(),
////                                realListener,
////                                progressCallbackThresholdBytes));
////                    }
//                    fileDS.writeTo(sink);
//                    sink.flush();
//                }
//
//                @Override
//                public long contentLength() throws IOException {
//                    return fileDS.contentLength();
//                }
//            };
//
//            RequestBody compositeBody = new MultipartBody.Builder("--")
//                    .setType(MultipartBody.FORM)
//                    .addFormDataPart("file", fileName, dataBody)
//                    .build();
//
//            HttpUrl.Builder urlBuilder = API_BASE_URL.newBuilder().
//                    addPathSegment("uploadfile")
//                    .addQueryParameter("folderid", String.valueOf(RemoteFolder.ROOT_FOLDER_ID))
//                    .addQueryParameter("renameifexists", String.valueOf(1))
//                    .addQueryParameter("nopartial", String.valueOf(1));
//
////            if (modifiedDate != null) {
////                urlBuilder.addQueryParameter("mtime", String.valueOf(TimeUnit.MILLISECONDS.toSeconds(modifiedDate.getTime())));
////            }
//
//            Request uploadRequest = new Request.Builder()
//                    .url(urlBuilder.build())
//                    .method("POST", compositeBody)
//                    .addHeader("Authorization", "Bearer " + oauthToken)
//                    .build();
//
//            Response response = okHttpClient.newCall(uploadRequest).execute();
//
//            JsonObject jsonObject = new Gson().fromJson(response.body().string(),JsonObject.class);
//
//            JsonArray fileIds = jsonObject.get("fileids").getAsJsonArray();
//
//            JsonObject metaData = jsonObject.get("metadata").getAsJsonObject();
//
//            result.put("fileId", fileIds.get(0).getAsString());
//            result.put("downloadLink", null);


//            return newCall(uploadRequest, new ResponseAdapter<RemoteFile>() {
//                @Override
//                public RemoteFile adapt(Response response) throws IOException, ApiError {
//                    UploadFilesResponse body = getAsApiResponse(response, UploadFilesResponse.class);
//                    if (!body.getUploadedFiles().isEmpty()) {
//                        return body.getUploadedFiles().get(0);
//                    } else {
//                        throw new IOException("API uploaded file but did not return remote file data.");
//                    }
//                }
//            });
//        }

//        ApiClient apiClient = PCloudSdk.newClientBuilder()
//                .authenticator(Authenticators.newOAuthAuthenticator(oauthToken))
//                .create();


        RemoteFile remoteFile = null;

        try {
            remoteFile = apiClient.createFile(RemoteFolder.ROOT_FOLDER_ID, fileName, DataSource.create(srcFile)).execute();

            result.put("fileId", "" + remoteFile.fileId());

            FileLink downloadLink = apiClient.createFileLink(remoteFile, DownloadOptions.DEFAULT).execute();
            result.put("downloadLink", downloadLink.bestUrl().toString());

        } catch (ApiError apiError) {
            apiError.printStackTrace();
        }
//
//        apiClient.shutdown();

        return result;
    }

    @Override
    public int getUserId() {
        return userId;
    }

    @Override
    public long getAvailable() throws IOException {
        long result = 0;
        try {
            UserInfo userInfo = apiClient.getUserInfo().execute();
            result = userInfo.totalQuota() - userInfo.usedQuota();
        } catch (ApiError apiError) {
            apiError.printStackTrace();
        }
        return result;
    }


}
