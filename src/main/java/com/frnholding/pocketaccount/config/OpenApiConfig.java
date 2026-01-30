package com.frnholding.pocketaccount.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("Development Server"),
                        new Server()
                                .url("https://api.pocketaccount.com")
                                .description("Production Server")
                ))
                .info(new Info()
                        .title("PocketAccount API")
                        .version("1.0.0")
                        .description("Document intelligence system for automated PDF extraction and interpretation. " +
                                "Supports invoices, bank statements, and receipts using PDFBox, OCR (Tesseract), and AI (OpenAI) extraction methods.")
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT")));
    }
}
