package br.com.finlumia.document.config;

import br.com.finlumia.document.services.ExternalApiAuthenticationFilter;
import br.com.finlumia.document.services.JwtAuthenticationFilter;
import br.com.finlumia.document.services.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class DocumentConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public ExternalApiAuthenticationFilter jwtAuthenticationFilter(
            JwtService jwtService,
            PublicApiProperties publicApiProperties,
            ObjectMapper objectMapper) {
        return new JwtAuthenticationFilter(jwtService, publicApiProperties, objectMapper);
    }
}
