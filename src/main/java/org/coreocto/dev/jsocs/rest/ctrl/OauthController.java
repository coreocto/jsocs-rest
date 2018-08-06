//package org.coreocto.dev.jsocs.rest.ctrl;
//
//import com.google.gson.Gson;
//import com.google.gson.JsonObject;
//import okhttp3.OkHttpClient;
//import okhttp3.Request;
//import okhttp3.Response;
//import org.coreocto.dev.jsocs.rest.Constant;
//import org.coreocto.dev.jsocs.rest.config.AppConfig;
//import org.coreocto.dev.jsocs.rest.pojo.Account;
//import org.coreocto.dev.jsocs.rest.repo.AccountRepo;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//import org.yaml.snakeyaml.util.UriEncoder;
//
//import javax.servlet.http.HttpServletRequest;
//import java.io.IOException;
//import java.util.Optional;
//
//@RestController
//public class OauthController {
//
//    public static final String REDIRECT_URI = "http://localhost:8080/oauth2?userId=";
//
//    @Autowired
//    AccountRepo accountRepo;
//
//    @Autowired
//    AppConfig appConfig;
//
//    @RequestMapping("/oauth")
//    public String oauthRedirect(@RequestParam("userId") int userId) {
//
//        Optional<Account> oAccount = accountRepo.findById(userId);
//
//        if (oAccount.isPresent()) {
//            Account account = oAccount.get();
//            account.setCtoken("pcloud-token");
//            accountRepo.save(account);
//        }
//
//        JsonObject response = new JsonObject();
//        response.addProperty("status", "success");
//        response.addProperty("url", "https://my.pcloud.com/oauth2/authorize?client_id=" + appConfig.APP_PCLOUD_CLIENT_ID + "&response_type=code" + "&state=" + userId + "&redirect_uri=" + UriEncoder.encode("http://localhost:8080/oauth2"));
//
//        return new Gson().toJson(response);
//    }
//
//    @RequestMapping("/oauth2")
//    public String oauthReturn(Model model, HttpServletRequest request, @RequestParam("state") Optional<Integer> userId, @RequestParam("code") String code) {
//
//        String accessToken = null;
//
//        {
//            Request oauthRequest = new Request.Builder()
//                    .url("https://api.pcloud.com/oauth2_token?client_id=" + appConfig.APP_PCLOUD_CLIENT_ID + "&client_secret=" + appConfig.APP_PCLOUD_CLIENT_SECRET + "&code=" + code)
//                    .build();
//
//            try {
//                Response oauthResponse = new OkHttpClient().newCall(oauthRequest).execute();
//                JsonObject response = new Gson().fromJson(oauthResponse.body().string(), JsonObject.class);
//                accessToken = response.get("access_token").getAsString();
//
//            } catch (IOException e) {
//
//            }
//        }
//
//        if (userId.isPresent()) {
//
//            Optional<Account> oAccount = accountRepo.findById(userId.get());
//
//            if (oAccount.isPresent()) {
//                Account account = oAccount.get();
//                account.setCtoken(code);
//                account.setCauthToken(accessToken);
//                accountRepo.save(account);
//            }
//        } else {
//            accountRepo.updateAccessTokenAndCodeByToken(code, accessToken, "pcloud-token");
//        }
//
//        return "accountsView";
//    }
//}
