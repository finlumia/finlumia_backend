package br.com.finlumia.docs.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DocsOpenApiConfig {

    @Bean
    public OpenAPI docsOpenAPI(@Value("${spring.application.name:docs}") String applicationName) {
        return new OpenAPI()
                .info(new Info()
                        .title("Finlumia - " + applicationName)
                        .description("Agregador de documentacao OpenAPI dos microservicos")
                        .version("v1"));
    }

    @Bean
    public GroupedOpenApi docsAggregatorOpenApiGroup() {
        return GroupedOpenApi.builder()
                .group("docs")
                .pathsToMatch("/docs/**")
                .build();
    }
}
