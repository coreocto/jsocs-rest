package org.coreocto.dev.jsocs.rest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Main extends SpringBootServletInitializer {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);
//        CloudRail.setAppKey(context.getBean(AppConfig.class).APP_CLOUDRAIL_KEY);
        // removed due to unable to persists entity beans, init() should not be invoked here (2018/07/30)
        //StorageManager storageManager = context.getBean(StorageManager.class);
        //storageManager.init();
        // end
    }
}
