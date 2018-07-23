package org.coreocto.dev.jsocs.rest.nio;

import com.google.gson.Gson;
import com.google.common.reflect.TypeToken;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.coreocto.dev.jsocs.rest.ctrl.OauthController;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

@Service
public class RemoteStorageFactory {
    public IRemoteStorage make(String type, int userId, String token) {
        if ("pcloud".equalsIgnoreCase(type)) {
            return new PCloudRSImpl(userId, token);
        }
        return null;
    }
}
