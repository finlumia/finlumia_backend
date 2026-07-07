package br.com.finlumia.docs.support.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        InternalSecurityProperties.class,
        ModuleSecurityProperties.class,
        PublicApiProperties.class,
        JwtProperties.class
})
public class SupportPropertiesConfig {
}
