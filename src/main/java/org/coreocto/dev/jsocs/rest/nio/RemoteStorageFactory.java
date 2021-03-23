//package org.coreocto.dev.jsocs.rest.nio;
//
//import com.cloudrail.si.interfaces.CloudStorage;
//import com.cloudrail.si.servicecode.commands.awaitCodeRedirect.LocalReceiver;
//import com.cloudrail.si.services.PCloud;
//import org.springframework.stereotype.Service;
//
//@Service
//public class RemoteStorageFactory {
//    public IRemoteStorage make(String type, int userId, String token) {
//        if ("pcloud".equalsIgnoreCase(type)) {
//            return new PCloudRSImpl(userId, token);
//        }
//        return null;
//    }
//
//    public CloudStorage get(String storageType, int port, String identifier, String secret, String additional) {
//        CloudStorage result = null;
//
//        switch (storageType) {
//            case CLOUD_STOR_PCLOUD: {
//                result = new PCloud(new LocalReceiver(port), identifier, secret, "http://localhost:" + port + "/", additional);
//                break;
//            }
////            case CLOUD_STOR_GOOGLE_DRV: {
////                result = new GoogleDrive(new LocalReceiver(port), identifier, secret, "http://localhost:" + port + "/", additional);
////                break;
////            }
//        }
//
//        return result;
//    }
//
//    public static final String CLOUD_STOR_PCLOUD = "pcloud";
////    public static final String CLOUD_STOR_GOOGLE_DRV = 2;
//
//}
