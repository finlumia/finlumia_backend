package br.com.finlumia.identify.config;

import java.util.List;

import br.com.finlumia.identify.services.InternalServiceTokenFilter;
import br.com.finlumia.identify.services.JwtAuthenticationFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
@ConditionalOnProperty(
        prefix = "finlumia.security.module-api",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ModuleSecurityConfig {

    @Bean
    public InternalServiceTokenFilter internalServiceTokenFilter(InternalSecurityProperties properties) {
        return new InternalServiceTokenFilter(properties);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            br.com.finlumia.identify.services.TokenService tokenService,
            br.com.finlumia.identify.services.JwtService jwtService,
            PublicApiProperties publicApiProperties,
            com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        return new JwtAuthenticationFilter(tokenService, jwtService, publicApiProperties, objectMapper);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            InternalServiceTokenFilter internalServiceTokenFilter,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            PublicApiProperties publicApiProperties) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(
                                "/api/identify/token",
                                "/api/identify/token/**",
                                "/api/identify/auth/login/google",
                                "/api/identify/auth/register",
                                "/api/identify/auth/verify-email",
                                "/api/identify/auth/resend-verification",
                                "/api/identify/auth/forgot-password",
                                "/api/identify/auth/verify-reset-token",
                                "/api/identify/auth/reset-password")
                        .permitAll()
                        .requestMatchers(publicApiProperties.getPathsArray()).permitAll()
                        .requestMatchers(ApiPaths.INTERNAL_DOCS_EXTERNAL + "/**").permitAll()
                        .requestMatchers(ApiPaths.INTERNAL_DOCS_PREFIX + "/**").permitAll()
                        .requestMatchers(ApiPaths.INTERNAL_API_PREFIX + "/**").permitAll()
                        .requestMatchers(ApiPaths.EXTERNAL_API_PREFIX + "/**").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(internalServiceTokenFilter, AuthorizationFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, AuthorizationFilter.class)
                .httpBasic(httpBasic -> httpBasic.disable())
                .formLogin(formLogin -> formLogin.disable());
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(ModuleSecurityProperties moduleSecurityProperties) {
        CorsConfiguration docsConfig = new CorsConfiguration();
        docsConfig.setAllowedOrigins(List.of(moduleSecurityProperties.getDocsOrigin()));
        docsConfig.setAllowedMethods(List.of("GET", "OPTIONS"));
        docsConfig.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        docsConfig.setAllowCredentials(false);

        CorsConfiguration apiConfig = new CorsConfiguration();
        apiConfig.setAllowedOrigins(moduleSecurityProperties.getAllowedOrigins());
        apiConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        apiConfig.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With"));
        apiConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(ApiPaths.INTERNAL_DOCS_PREFIX + "/**", docsConfig);
        source.registerCorsConfiguration("/**", apiConfig);
        return source;
    }
}
