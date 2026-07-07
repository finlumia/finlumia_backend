package br.com.finlumia.docs.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@SecurityScheme(
        name = DocsOpenApiConfig.BEARER_AUTH,
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT")
public class DocsOpenApiConfig {

    public static final String BEARER_AUTH = "bearerAuth";

    @Bean
    public OpenAPI docsOpenAPI(@Value("${spring.application.name:docs}") String applicationName) {
        return new OpenAPI()
                .info(new Info()
                        .title("Finlumia - " + applicationName)
                        .description("Documentacao e suporte da plataforma Finlumia")
                        .version("v1"));
    }

    @Bean
    public GroupedOpenApi docsAggregatorOpenApiGroup() {
        return GroupedOpenApi.builder()
                .group("docs")
                .pathsToMatch("/docs/**")
                .build();
    }

    @Bean
    public GroupedOpenApi supportExternalOpenApiGroup() {
        return GroupedOpenApi.builder()
                .group("external")
                .pathsToMatch("/api/**")
                .build();
    }

    @Bean
    public GroupedOpenApi supportInternalOpenApiGroup() {
        return GroupedOpenApi.builder()
                .group("internal")
                .pathsToMatch("/internal/**")
                .pathsToExclude("/internal/docs/**")
                .build();
    }
}
