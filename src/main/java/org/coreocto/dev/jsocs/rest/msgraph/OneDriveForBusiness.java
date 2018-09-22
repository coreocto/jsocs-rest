package org.coreocto.dev.jsocs.rest.msgraph;

import com.cloudrail.si.exceptions.ParseException;
import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.servicecode.commands.awaitCodeRedirect.RedirectReceiver;
import com.cloudrail.si.types.CloudMetaData;
import com.cloudrail.si.types.SpaceAllocation;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.*;
import org.coreocto.dev.jsocs.rest.okhttp3.RequestBodyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * this is an temporary(?) solution to fix 'The app ID is blocked for access of the O365 Discovery Service' problem of accessing OneDrive for Business using CloudRail library
 * it relies on the official graph api but follows the unified interface offered by CloudRail
 * this class is not fully functional currently but good enough to serve my needs
 */
public class OneDriveForBusiness implements CloudStorageExtended {

    private static final String bearer = "Bearer ";
    private static final String ME_DRIVE = "https://graph.microsoft.com/v1.0/me/drive";
    private final Logger logger = LoggerFactory.getLogger(OneDriveForBusiness.class);
    ScheduledExecutorService renewTokenExecutor = Executors.newScheduledThreadPool(1);
    private OkHttpClient okHttpClient = new OkHttpClient();
    private String mAccessToken;
    private String mRefreshToken;
    private RedirectReceiver mRedirectReceiver;
    private String mClientId;
    private String mClientSecret;
    private String mRedirectUri;
    private String mState;
    private String mAuthorizationCode;
    private long lastRefreshTime;
    private final ReadWriteLock rwlock = new ReentrantReadWriteLock();

    public OneDriveForBusiness(RedirectReceiver redirectReceiver, String clientId, String clientSecret, String redirectUri, String state) {
        this.mClientId = clientId;
        this.mClientSecret = clientSecret;
        this.mRedirectReceiver = redirectReceiver;
        this.mRedirectUri = redirectUri;
        this.mState = state;
    }

    private OkHttpClient getHttpClient() {
        return okHttpClient;
//        return new OkHttpClient();
    }

    private Request.Builder getRequestBuilder() {
        Request.Builder tmp = new Request.Builder();

        rwlock.readLock().lock();
        try {
            if (mAccessToken != null) {
                tmp.addHeader("Authorization", bearer + mAccessToken);
            }
        } finally {
            rwlock.readLock().unlock();
        }
        return tmp;
    }

    @Override
    public InputStream download(String s) {

//        this.validateToken();

        String fileUrl = ME_DRIVE + "/root:" + s + ":/content";

        Request request = getRequestBuilder()
                .url(fileUrl)
                .get()
                .build();

        InputStream out = null;

        // as this method needs to return usable input stream, the code does not close the steam here
        // the invoker is required to free the resource
        try {

            Response response = getHttpClient().newCall(request).execute();

            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            out = response.body().byteStream();

        } catch (IOException e) {
            logger.error("error when processing download request", e);
        }

        return out;
    }

    @Override
    public String getHash(String s) {
        String newFileUrl = ME_DRIVE + "/root:" + s;
        String returnHash = null;
        Request request = this.getRequestBuilder()
                        .url(newFileUrl).build();

                try (Response response = getHttpClient().newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        throw new IOException("Unexpected code " + response);
                    }

                    String responseStr =response.body().string();
                    logger.debug("response = "+responseStr);

                    JsonObject jsonObject = new Gson().fromJson(responseStr, JsonObject.class);

                    returnHash = jsonObject.get("file").getAsJsonObject().get("hashes").getAsJsonObject().get("quickXorHash").getAsString();
                } catch (IOException e) {
                    logger.error("error when getting quickXorHash", e);
                }
        return returnHash;
    }

    @Override
    public void upload(String s, InputStream inputStream, long l, boolean b) {

//        this.validateToken();

        String newFileUrl = ME_DRIVE + "/root:" + s + ":/createUploadSession";

        Request request = getRequestBuilder()
                .url(newFileUrl)
                .post(RequestBody.create(null, ""))
                .build();

        String uploadUrl = null;

        try (Response response = getHttpClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseStr = response.body().string();

            JsonObject jsonObject = new Gson().fromJson(responseStr, JsonObject.class);
            uploadUrl = jsonObject.get("uploadUrl").getAsString();

        } catch (IOException e) {
            logger.error("unable to retrieve uploadUrl", e);
        }

        if (uploadUrl != null) {
            RequestBody body = RequestBodyUtil.create(MediaType.parse("application/octet-stream"), inputStream, l);

            request = this.getRequestBuilder()
                    .url(uploadUrl)
                    .addHeader("Content-Length", l + "")
                    .addHeader("Content-Range", "bytes 0-" + (l - 1) + "/" + l)
                    .put(body) //PUT
                    .build();

            String hash = null;
            try (Response response = getHttpClient().newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

                String responseStr =response.body().string();
                logger.debug("response = "+responseStr);

            } catch (IOException e) {
                logger.error("error when getting response when uploading document", e);
            }

//            {
//                request = this.getRequestBuilder()
//                        .url(ME_DRIVE + "/items/"+fileId).build();
//
//                try (Response response = getHttpClient().newCall(request).execute()) {
//                    if (!response.isSuccessful()) {
//                        throw new IOException("Unexpected code " + response);
//                    }
//
//                    String responseStr =response.body().string();
//                    logger.debug("response = "+responseStr);
//                } catch (IOException e) {
//                    logger.error("error when getting meta data", e);
//                }
//
//            }
        }
    }

    @Override
    public void uploadWithContentModifiedDate(String s, InputStream inputStream, long l, boolean b, long l1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void move(String s, String s1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(String s) {
        //get the file id with given path
        String newFileUrl = ME_DRIVE + "/root:" + s;

        Request request = getRequestBuilder()
                .url(newFileUrl)
                .get()
                .build();

        String id = null;

        try (Response response = getHttpClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            String responseStr = response.body().string();

            JsonObject jsonObject = new Gson().fromJson(responseStr, JsonObject.class);
            id = jsonObject.get("id").getAsString();

        } catch (IOException e) {
            logger.error("unable to retrieve uploadUrl", e);
        }

        //delete target with id
        //note that this action moves the file to recycle bin by default
        if (id != null && !id.isEmpty()) {
            String delFileUrl = ME_DRIVE + "/items/" + id;
            Request delRequest = getRequestBuilder()
                    .url(delFileUrl)
                    .delete()
                    .build();

            try (Response response = getHttpClient().newCall(delRequest).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

            } catch (IOException e) {
                logger.error("unable to retrieve uploadUrl", e);
            }
        }
    }

    @Override
    public void copy(String s, String s1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void createFolder(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public CloudMetaData getMetadata(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<CloudMetaData> getChildren(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<CloudMetaData> getChildrenPage(String s, long l, long l1) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getUserLogin() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getUserName() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String createShareLink(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SpaceAllocation getAllocation() {
        return null;
//        throw new UnsupportedOperationException();
    }

    @Override
    public boolean exists(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getThumbnail(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<CloudMetaData> search(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void login() {
        String authUri = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize?client_id=" + mClientId + "&scope=" + "offline_access%20https%3A%2F%2Fgraph.microsoft.com%2Ffiles.readwrite" +
                "  &response_type=code&redirect_uri=" + mRedirectUri;
        String result = mRedirectReceiver.openAndAwait(authUri, mState);
        if (result == null) {
            throw new NullPointerException("result == null");
        }

        rwlock.writeLock().lock();
        try {
            mAuthorizationCode = result.substring(result.indexOf("code=") + "code=".length(), result.indexOf("&session_state="));

            if (mAuthorizationCode != null) {
                RequestBody formBody = new FormBody.Builder()
                        .add("client_id", mClientId)
                        .add("client_secret", mClientSecret)
                        .add("code", mAuthorizationCode)
                        .add("redirect_uri", mRedirectUri)
                        .add("grant_type", "authorization_code")
                        .build();

                Request request = getRequestBuilder()
                        .url("https://login.microsoftonline.com/common/oauth2/v2.0/token")
                        .post(formBody)
                        .build();

                String responseStr = null;

                boolean safeToProceed = false;

                try (Response response = this.getHttpClient().newCall(request).execute()) {
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                    responseStr = response.body().string();

                    JsonObject jsonObject = new Gson().fromJson(responseStr, JsonObject.class);
                    mAccessToken = jsonObject.get("access_token").getAsString();
                    mRefreshToken = jsonObject.get("refresh_token").getAsString();  //this requires offline_access in scopes
                    lastRefreshTime = getCurrentTime();

                    safeToProceed = true;

                } catch (IOException e) {
                    logger.debug("failed to sign in to OneDrive for Business");
                    throw new RuntimeException(e.getMessage());
                }

                if (safeToProceed) {
                    renewTokenExecutor.scheduleAtFixedRate(new RenewThread(), 0, 3595, TimeUnit.SECONDS);
                }
            }

        } finally {
            rwlock.writeLock().unlock();
        }
    }

    private long getCurrentTime() {
        return Calendar.getInstance().getTimeInMillis();
    }

    private synchronized void validateToken() {
        long curTime = getCurrentTime();

        if (curTime - lastRefreshTime <= ((3600 - 3000) * 1000)) {   //refresh the token if the remaining time is less than 50 minutes
            this.refreshToken();
        }
    }

    private void refreshToken() {
        rwlock.writeLock().lock();
        try {
            if (mRefreshToken != null) {
                RequestBody formBody = new FormBody.Builder()
                        .add("client_id", mClientId)
                        .add("client_secret", mClientSecret)
                        .add("code", mAuthorizationCode)
                        .add("redirect_uri", mRedirectUri)
                        .add("refresh_token", mRefreshToken)
                        .add("grant_type", "refresh_token")
                        .build();

                Request request = getRequestBuilder()
                        .url("https://login.microsoftonline.com/common/oauth2/v2.0/token")
                        .post(formBody)
                        .build();

                try (Response response = this.getHttpClient().newCall(request).execute()) {
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                    String responseStr = response.body().string();

                    JsonObject jsonObject = new Gson().fromJson(responseStr, JsonObject.class);
                    mAccessToken = jsonObject.get("access_token").getAsString();
                    mRefreshToken = jsonObject.get("refresh_token").getAsString();
                    lastRefreshTime = getCurrentTime();

                } catch (IOException e) {
                    throw new RuntimeException(e.getMessage());
                }
            } else {
                throw new NullPointerException("mRefreshToken == null");
            }
        } finally {
            rwlock.writeLock().unlock();
        }
    }

    @Override
    public void logout() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void resumeLogin(String s) throws ParseException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String saveAsString() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void loadAsString(String s) throws ParseException {
        throw new UnsupportedOperationException();
    }

    class RenewThread implements Runnable {

        @Override
        public void run() {

            rwlock.writeLock().lock();
            try {

                if (mRefreshToken != null) {
                    RequestBody formBody = new FormBody.Builder()
                            .add("client_id", mClientId)
                            .add("client_secret", mClientSecret)
                            .add("code", mAuthorizationCode)
                            .add("redirect_uri", mRedirectUri)
                            .add("refresh_token", mRefreshToken)
                            .add("grant_type", "refresh_token")
                            .build();

                    Request request = getRequestBuilder()
                            .url("https://login.microsoftonline.com/common/oauth2/v2.0/token")
                            .post(formBody)
                            .build();

                    try (Response response = OneDriveForBusiness.this.getHttpClient().newCall(request).execute()) {
                        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                        String responseStr = response.body().string();

                        JsonObject jsonObject = new Gson().fromJson(responseStr, JsonObject.class);
                        mAccessToken = jsonObject.get("access_token").getAsString();
                        mRefreshToken = jsonObject.get("refresh_token").getAsString();
                        lastRefreshTime = getCurrentTime();

                        logger.debug("token renewed");

                    } catch (IOException e) {
                        throw new RuntimeException(e.getMessage());
                    }
                } else {
                    throw new NullPointerException("mRefreshToken == null");
                }

            } finally {
                rwlock.writeLock().unlock();
            }
        }
    }
}
