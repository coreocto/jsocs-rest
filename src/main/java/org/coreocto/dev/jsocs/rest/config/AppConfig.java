package org.coreocto.dev.jsocs.rest.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.servlet.config.annotation.AsyncSupportConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableAsync
public class AppConfig implements WebMvcConfigurer, AsyncConfigurer {

    private final Logger logger = LoggerFactory.getLogger(AppConfig.class);

    @Value("${app.onedrive_for_business.client.secret}")
    public String APP_ONEDRIVE_FOR_BUSINESS_CLIENT_SECRET;

    @Value("${app.onedrive_for_business.client.id}")
    public String APP_ONEDRIVE_FOR_BUSINESS_CLIENT_ID;

    @Value("${app.encrypt-key}")
    public String APP_ENCRYPT_KEY;

    @Value("${app.upload.tmp-dir}")
    public String APP_TEMP_DIR;

    @Value("${app.cloudrail.key}")
    public String APP_CLOUDRAIL_KEY;

    @Value("${app.pcloud.client.id}")
    public String APP_PCLOUD_CLIENT_ID;

    @Value("${app.pcloud.client.secret}")
    public String APP_PCLOUD_CLIENT_SECRET;

    @Value("${app.webdriver.path}")
    public String APP_WEBDRIVER_PATH;

    @Value("${app.video.cache.dir}")
    public String APP_VIDEO_CACHE_DIR;

    @Autowired
    private AsyncTaskExecutor asyncTaskExecutor;

    @Override
    public void configureAsyncSupport(AsyncSupportConfigurer configurer) {
        configurer.setDefaultTimeout(3600000L);
        configurer.setTaskExecutor(asyncTaskExecutor);
    }

    @Bean
    public AsyncTaskExecutor getAsyncTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setThreadNamePrefix("mvc-async-");
        executor.setCorePoolSize(400);
        executor.setMaxPoolSize(400);
        executor.setQueueCapacity(400);
        executor.initialize();
        return executor;
    }
}
