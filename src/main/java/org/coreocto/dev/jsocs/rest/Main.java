package org.coreocto.dev.jsocs.rest;

import com.cloudrail.si.CloudRail;
import org.coreocto.dev.jsocs.rest.config.AppConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class Main extends SpringBootServletInitializer {

    public static void main(String[] args) {
        CloudRail.setAppKey("5b552bba3ef31211abe85e8c");

        ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);
        CloudRail.setAppKey(context.getBean(AppConfig.class).APP_CLOUDRAIL_KEY);
        // removed due to unable to persists entity beans, init() should not be invoked here (2018/07/30)
        //StorageManager storageManager = context.getBean(StorageManager.class);
        //storageManager.init();
        // end
    }
}
