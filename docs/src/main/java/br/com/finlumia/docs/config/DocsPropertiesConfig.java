package br.com.finlumia.docs.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(DocsModuleProperties.class)
public class DocsPropertiesConfig {
}
