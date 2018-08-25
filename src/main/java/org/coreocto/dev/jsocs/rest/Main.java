package org.coreocto.dev.jsocs.rest;

import com.cloudrail.si.CloudRail;
import org.coreocto.dev.jsocs.rest.config.AppConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.content.commons.repository.Store;
import org.springframework.content.rest.StoreRestResource;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@SpringBootApplication
@EnableAsync
public class Main extends SpringBootServletInitializer {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(Main.class, args);
        CloudRail.setAppKey(context.getBean(AppConfig.class).APP_CLOUDRAIL_KEY);
        // removed due to unable to persists entity beans, init() should not be invoked here (2018/07/30)
        //StorageManager storageManager = context.getBean(StorageManager.class);
        //storageManager.init();
        // end
    }

//    @StoreRestResource(path="videos")
//    public interface VideoStore extends Store<String> {}

    @Bean
    public Executor asyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(2);
        executor.setQueueCapacity(500);
//        executor.setThreadNamePrefix("GithubLookup-");
        executor.initialize();
        return executor;
    }

}
