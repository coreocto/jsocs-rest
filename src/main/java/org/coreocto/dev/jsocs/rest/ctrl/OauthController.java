package org.coreocto.dev.jsocs.rest.ctrl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.coreocto.dev.jsocs.rest.db.AccountService;
import org.coreocto.dev.jsocs.rest.pojo.Account;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.yaml.snakeyaml.util.UriEncoder;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Controller
public class OauthController {

    public static final String CLIENT_ID = "1z5SVQMlzHk";
    public static final String CLIENT_SECRET = "1dWvvNqzfkpSeVJqYLenD5SvTVIV";
    public static final String REDIRECT_URI = "http://localhost:8080/oauth2?userId=";

    @Autowired
    AccountService accountService;

    @RequestMapping("/oauth")
    public String oauthRedirect(@RequestParam("userId") String userId) {

        int userIdInt = -1;

        try {
            userIdInt = Integer.parseInt(userId);
        } catch (NumberFormatException ex) {

        }

        accountService.updateToken(userIdInt, "pcloud-token");

        return "redirect:https://my.pcloud.com/oauth2/authorize?client_id=" + CLIENT_ID + "&response_type=code" + "&state=" + userId + "&redirect_uri=" + UriEncoder.encode("http://localhost:8080/oauth2");
    }

    @RequestMapping("/oauth2")
    public String oauthReturn(Model model, HttpServletRequest request, @RequestParam("state") Optional<String> userId, @RequestParam("code") String code) {

        String accessToken = null;

        {
            Request oauthRequest = new Request.Builder()
                    .url("https://api.pcloud.com/oauth2_token?client_id=" + OauthController.CLIENT_ID + "&client_secret=" + OauthController.CLIENT_SECRET + "&code=" + code)
                    .build();

            try {
                Response oauthResponse = new OkHttpClient().newCall(oauthRequest).execute();
                JsonObject response = new Gson().fromJson(oauthResponse.body().string(), JsonObject.class);
                accessToken = response.get("access_token").getAsString();

            } catch (IOException e) {

            }
        }

        if (userId.isPresent()) {
            int userIdInt = -1;

            try {
                userIdInt = Integer.parseInt(userId.get());
            } catch (NumberFormatException ex) {

            }

            if (userIdInt > -1) {
                accountService.updateToken(userIdInt, code);
                accountService.updateAuthToken(userIdInt, accessToken);
            }
        } else {
            accountService.replaceTokens("pcloud-token", code, accessToken);
        }

        List<Account> accountList = accountService.getAllAccounts();
        model.addAttribute("accountList", accountList);
        return "accountsView";
    }
}
