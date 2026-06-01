package br.com.finlumia.movement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {"br.com.finlumia.movement", "br.com.finlumia.shared"})
public class MovementApplication {
    public static void main(String[] args) {
        SpringApplication.run(MovementApplication.class, args);
    }
}
