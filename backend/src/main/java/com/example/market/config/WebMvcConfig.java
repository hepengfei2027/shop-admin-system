package com.example.market.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 映射本地上传目录到 /upload/ 访问路径
        // d:/APP/cursor/ku/cursor1/upload-images/ 下的文件可以通过
        // http://localhost:8081/upload/** 访问
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:d:/APP/cursor/ku/cursor1/upload-images/");
    }
}

