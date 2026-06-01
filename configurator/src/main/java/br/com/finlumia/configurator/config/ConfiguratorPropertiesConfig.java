package br.com.finlumia.configurator.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        InternalSecurityProperties.class,
        ModuleSecurityProperties.class,
        KeyUserResolutionProperties.class,
        PublicApiProperties.class
})
public class ConfiguratorPropertiesConfig {
}
