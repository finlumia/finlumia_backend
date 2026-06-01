package br.com.finlumia.configurator.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI identifyOpenAPI(@Value("${spring.application.name:identify}") String applicationName) {
        return new OpenAPI()
                .info(new Info()
                        .title("Finlumia - " + applicationName)
                        .version("v1"));
    }

    @Bean
    public GroupedOpenApi externalOpenApiGroup() {
        return GroupedOpenApi.builder()
                .group("external")
                .pathsToMatch(ApiPaths.EXTERNAL_API_PREFIX + "/**")
                .build();
    }

    @Bean
    public GroupedOpenApi internalOpenApiGroup() {
        return GroupedOpenApi.builder()
                .group("internal")
                .pathsToMatch(ApiPaths.INTERNAL_API_PREFIX + "/**")
                .pathsToExclude(ApiPaths.INTERNAL_DOCS_PREFIX + "/**")
                .build();
    }
}
