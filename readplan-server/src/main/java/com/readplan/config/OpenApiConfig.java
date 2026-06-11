package com.readplan.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI readPlanOpenApi() {
        return new OpenAPI().info(new Info()
            .title("ReadPlan API")
            .version("v1.0.0")
            .description("个人阅读计划与笔记系统接口骨架"));
    }
}
