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

import java.io.*;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * this is an temporary(?) solution to fix 'The app ID is blocked for access of the O365 Discovery Service' problem of accessing OneDrive for Business using CloudRail library
 * it relies on the official graph api but follows the unified interface offered by CloudRail
 * this class is not fully functional currently but good enough to serve my needs
 */
public class OneDriveForBusiness implements CloudStorage {

    private final Logger logger = LoggerFactory.getLogger(OneDriveForBusiness.class);

    private static final String bearer = "Bearer ";
    private static final String myDrive = "https://graph.microsoft.com/v1.0/me/drive";
    private OkHttpClient okHttpClient = new OkHttpClient();
    private String mAccessToken;
    private String mRefreshToken;
    private RedirectReceiver mRedirectReceiver;
    private String mClientId;
    private String mClientSecret;
    private String mRedirectUri;
    private String mState;
    private String mAuthorizationCode;
    private AtomicLong lastRefreshTime;

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
        if (mAccessToken != null) {
            tmp.addHeader("Authorization", bearer + mAccessToken);
        }
        return tmp;
    }

    @Override
    public InputStream download(String s) {

//        this.validateToken();

        String fileUrl = myDrive + "/root:" + s + ":/content";

        Request request = getRequestBuilder()
                .url(fileUrl)
                .get()
                .build();

        InputStream out = null;

        try {

            Response response = getHttpClient().newCall(request).execute();

            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            out = response.body().byteStream();

        } catch (IOException e) {
            e.printStackTrace();
        }

        return out;
    }

    @Override
    public void upload(String s, InputStream inputStream, long l, boolean b) {

//        this.validateToken();

        String newFileUrl = myDrive + "/root:" + s + ":/createUploadSession";

        Request request = getRequestBuilder()
                .url(newFileUrl)
                .post(RequestBody.create(null, ""))
                .build();

        String responseStr = null;
        String uploadUrl = null;

        try (Response response = getHttpClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Unexpected code " + response);
            }

            responseStr = response.body().string();

            JsonObject jsonObject = new Gson().fromJson(responseStr, JsonObject.class);
            uploadUrl = jsonObject.get("uploadUrl").getAsString();

        } catch (IOException e) {
            logger.debug("unable to retrieve uploadUrl", e);
        }

        if (uploadUrl != null) {
            RequestBody body = RequestBodyUtil.create(MediaType.parse("application/octet-stream"), inputStream);
                    //new InputStreamRequestBody(MediaType.parse("application/octet-stream"), inputStream, l);
            request = this.getRequestBuilder()
                    .url(uploadUrl)
                    .addHeader("Content-Length", l+"")
                    .addHeader("Content-Range", "bytes 0-"+(l-1)+"/"+l)
                    .put(body) //PUT
                    .build();

            try (Response response = getHttpClient().newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("Unexpected code " + response);
                }

                responseStr = response.body().string();

            } catch (IOException e) {
                e.printStackTrace();
            }
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
//        throw new UnsupportedOperationException();
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
        if (result==null){
            throw new NullPointerException("result == null");
        }
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
                synchronized (this) {
                    mAccessToken = jsonObject.get("access_token").getAsString();
                    mRefreshToken = jsonObject.get("refresh_token").getAsString();  //this requires offline_access in scopes
                    lastRefreshTime = new AtomicLong(Calendar.getInstance().getTimeInMillis());
                }

                safeToProceed = true;

            } catch (IOException e) {
                throw new RuntimeException(e.getMessage());
            }

            if (safeToProceed) {
                renewTokenExecutor.scheduleAtFixedRate(new RenewThread(), 0, 3595, TimeUnit.SECONDS);
            }
        }
    }

    ScheduledExecutorService renewTokenExecutor = Executors.newScheduledThreadPool(1);

    class RenewThread implements Runnable{

        @Override
        public void run() {

            logger.debug("run()");

            synchronized (OneDriveForBusiness.this){
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

                    String responseStr = null;

                    try (Response response = OneDriveForBusiness.this.getHttpClient().newCall(request).execute()) {
                        if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                        responseStr = response.body().string();

                        JsonObject jsonObject = new Gson().fromJson(responseStr, JsonObject.class);
                        mAccessToken = jsonObject.get("access_token").getAsString();
                        mRefreshToken = jsonObject.get("refresh_token").getAsString();
                        lastRefreshTime = new AtomicLong(Calendar.getInstance().getTimeInMillis());

                        logger.debug("token renewed");

                    } catch (IOException e) {
                        throw new RuntimeException(e.getMessage());
                    }
                } else {
                    throw new NullPointerException("mRefreshToken == null");
                }
            }

        }
    }

    private synchronized void validateToken(){
        long curTime = Calendar.getInstance().getTimeInMillis();

        if (curTime - lastRefreshTime.get() <= ((3600 - 3000) * 1000)){   //refresh the token if the remaining time is less than 50 minutes
            this.refreshToken();
        }
    }

    private void refreshToken() {
        synchronized (this) {
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

                String responseStr = null;

                try (Response response = this.getHttpClient().newCall(request).execute()) {
                    if (!response.isSuccessful()) throw new IOException("Unexpected code " + response);

                    responseStr = response.body().string();

                    JsonObject jsonObject = new Gson().fromJson(responseStr, JsonObject.class);
                    mAccessToken = jsonObject.get("access_token").getAsString();
                    mRefreshToken = jsonObject.get("refresh_token").getAsString();
                    lastRefreshTime = new AtomicLong(Calendar.getInstance().getTimeInMillis());

                } catch (IOException e) {
                    throw new RuntimeException(e.getMessage());
                }
            } else {
                throw new NullPointerException("mRefreshToken == null");
            }
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
}
