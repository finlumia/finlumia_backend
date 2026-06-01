package br.com.finlumia.movement.config;

import br.com.finlumia.movement.services.KeyUserInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final KeyUserInterceptor keyUserInterceptor;
    private final PublicApiProperties publicApiProperties;

    public WebMvcConfig(KeyUserInterceptor keyUserInterceptor, PublicApiProperties publicApiProperties) {
        this.keyUserInterceptor = keyUserInterceptor;
        this.publicApiProperties = publicApiProperties;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(keyUserInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(publicApiProperties.getPathsArray());
    }
}
