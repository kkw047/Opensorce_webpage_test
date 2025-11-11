package com.cbnu11team.team11.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String root = System.getProperty("user.dir").replace("\\", "/");
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + root + "/" + uploadDir + "/");
    }
}
