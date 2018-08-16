package org.coreocto.dev.jsocs.rest.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CharacterEncodingFilter;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

@Configuration
public class AppConfig {

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

    @Value("${app.webdriver.firefox}")
    public String APP_WEBDRIVER_FIREFOX;
}
