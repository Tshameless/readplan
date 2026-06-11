package com.readplan.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@EnableConfigurationProperties(BookStorageProperties.class)
public class WebStorageConfig implements WebMvcConfigurer {

    private final BookStorageProperties bookStorageProperties;

    public WebStorageConfig(BookStorageProperties bookStorageProperties) {
        this.bookStorageProperties = bookStorageProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path storagePath = Path.of(bookStorageProperties.bookDir()).toAbsolutePath().normalize();
        String resourceLocation = storagePath.toUri().toString();
        if (!resourceLocation.endsWith("/")) {
            resourceLocation = resourceLocation + "/";
        }
        registry.addResourceHandler("/files/books/**")
            .addResourceLocations(resourceLocation);
    }
}
