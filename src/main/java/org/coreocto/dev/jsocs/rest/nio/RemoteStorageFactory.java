package org.coreocto.dev.jsocs.rest.nio;

import com.cloudrail.si.interfaces.CloudStorage;
import com.cloudrail.si.servicecode.commands.awaitCodeRedirect.LocalReceiver;
import com.cloudrail.si.services.GoogleDrive;
import com.cloudrail.si.services.PCloud;
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

    public CloudStorage get(String storageType, int port, String identifier, String secret, String additional) {
        CloudStorage result = null;

        switch (storageType) {
            case CLOUD_STOR_PCLOUD: {
                result = new PCloud(new LocalReceiver(port), identifier, secret, "http://localhost:" + port + "/", additional);
                break;
            }
//            case CLOUD_STOR_GOOGLE_DRV: {
//                result = new GoogleDrive(new LocalReceiver(port), identifier, secret, "http://localhost:" + port + "/", additional);
//                break;
//            }
        }

        return result;
    }

    public static final String CLOUD_STOR_PCLOUD = "pcloud";
//    public static final String CLOUD_STOR_GOOGLE_DRV = 2;

}
