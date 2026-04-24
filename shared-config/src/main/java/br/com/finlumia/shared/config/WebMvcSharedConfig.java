package br.com.finlumia.shared.config;

import br.com.finlumia.shared.interceptor.KeyUserInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcSharedConfig implements WebMvcConfigurer {

    private final KeyUserInterceptor keyUserInterceptor;

    public WebMvcSharedConfig(KeyUserInterceptor keyUserInterceptor) {
        this.keyUserInterceptor = keyUserInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(keyUserInterceptor).addPathPatterns("/api/**");
    }
}
