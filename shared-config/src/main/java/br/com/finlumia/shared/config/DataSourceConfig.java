package br.com.finlumia.shared.config;

import javax.sql.DataSource;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSourceConfig {

    @Bean(name = "postgresDataSource")
    @ConfigurationProperties(prefix = "spring.datasource")
    public DataSource postgresDataSource() {
        return DataSourceBuilder.create().build();
    }
}
