package br.com.finlumia.configurator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"br.com.finlumia.configurator", "br.com.finlumia.shared"})
public class ConfiguratorApplication {
    public static void main(String[] args) {
        SpringApplication.run(ConfiguratorApplication.class, args);
    }
}