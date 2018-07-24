package org.coreocto.dev.jsocs.rest;

import com.cloudrail.si.CloudRail;
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
        CloudRail.setAppKey("5b552bba3ef31211abe85e8c");
        ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);
    }
}
