package com.truist.batch.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI/Swagger Configuration for Fabric Platform API Documentation
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI fabricOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Fabric Platform API")
                        .description("Enterprise Data Loading and Batch Processing Platform REST API")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("Fabric Platform Team")
                                .email("fabric-support@company.com"))
                        .license(new License()
                                .name("Internal Use Only")
                                .url("https://company.com/license")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080/api")
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api.fabric.company.com/api")
                                .description("Production Server")
                ));
    }
}