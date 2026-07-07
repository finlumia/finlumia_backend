package br.com.finlumia.configurator.config;

import br.com.finlumia.configurator.services.JwtAuthenticationFilter;
import br.com.finlumia.configurator.services.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class ConfiguratorJwtConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public JwtService jwtService(JwtProperties jwtProperties) {
        return new JwtService(jwtProperties);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtService jwtService,
            PublicApiProperties publicApiProperties,
            ObjectMapper objectMapper) {
        return new JwtAuthenticationFilter(jwtService, publicApiProperties, objectMapper);
    }
}
