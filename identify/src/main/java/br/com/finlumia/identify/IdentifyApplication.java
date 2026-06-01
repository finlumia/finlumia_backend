package br.com.finlumia.identify;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"br.com.finlumia.identify", "br.com.finlumia.shared"})
public class IdentifyApplication {
    public static void main(String[] args) {
        SpringApplication.run(IdentifyApplication.class, args);
    }
}
