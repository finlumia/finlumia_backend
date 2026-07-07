package br.com.finlumia.docs.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                                "/docs/swagger-ui.html",
                                "/docs/swagger-ui/**",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                DocsOpenApiPaths.PUBLIC_API_DOCS_PREFIX + "/**")
                        .permitAll()
                        .requestMatchers("/docs/admin/**")
                        .hasRole("ADMIN")
                        .anyRequest().permitAll())
                .httpBasic(httpBasic -> {
                })
                .formLogin(formLogin -> formLogin.disable());

        return http.build();
    }

    @Bean
    public UserDetailsManager userDetailsManager(
            @Value("${docs.security.admin.username}") String adminUsername,
            @Value("${docs.security.admin.password}") String adminPassword,
            PasswordEncoder passwordEncoder) {
        UserDetails adminUser = User.withUsername(adminUsername)
                .password(passwordEncoder.encode(adminPassword))
                .roles("ADMIN")
                .build();

        return new InMemoryUserDetailsManager(adminUser);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
}
