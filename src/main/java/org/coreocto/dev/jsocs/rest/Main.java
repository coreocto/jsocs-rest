package org.coreocto.dev.jsocs.rest;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.org.apache.regexp.internal.RE;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.coreocto.dev.jsocs.rest.ctrl.OauthController;
import org.coreocto.dev.jsocs.rest.db.AccountService;
import org.coreocto.dev.jsocs.rest.pojo.Account;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

@SpringBootApplication
public class Main extends SpringBootServletInitializer {

    static Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);

//        OkHttpClient okHttpClient = new OkHttpClient();
//        Gson gson = new Gson();
//
//        AccountService accountService = context.getBean(AccountService.class);
//        List<Account> accountList = accountService.getAllAccounts();
//        for (Account account : accountList) {
//
//            String auth = null;
//            String accessToken = null;
//
//            {
//                Request request = new Request.Builder()
//                        .url("https://api.pcloud.com/userinfo?getauth=1&username=" + account.getCusername() + "&password=" + account.getCpassword())
//                        .build();
//                try {
//                    Response response = okHttpClient.newCall(request).execute();
//                    JsonObject json = gson.fromJson(response.body().string(), JsonObject.class);
//                    auth = json.get("auth").getAsString();  //do not simply use toString() here, it gives you double quotes at your values
//                } catch (IOException e) {
//                    logger.error("error when getting authentication token from pcloud", e);
//                }
//            }
//
//            {
//                Request request = new Request.Builder()
//                        .url("https://api.pcloud.com/oauth2_token?client_id=" + OauthController.CLIENT_ID + "&client_secret=" + OauthController.CLIENT_SECRET + "&code=" + auth).build();
//
//                try {
//                    Response oauthResponse = okHttpClient.newCall(request).execute();
//                    JsonObject json = gson.fromJson(oauthResponse.body().string(), JsonObject.class);
//                    accessToken = json.get("access_token").getAsString();
//                } catch (IOException e) {
//                    logger.error("error when getting access token from pcloud", e);
//                }
//            }
//
//            if (auth != null && accessToken != null) {
//                accountService.updateToken(account.getCid(), auth);
//                accountService.updateAuthToken(account.getCid(), accessToken);
//            }
//        }
    }
}
