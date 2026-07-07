package br.com.finlumia.docs.support.config;

import java.util.List;

import br.com.finlumia.docs.support.services.ExternalApiAuthenticationFilter;
import br.com.finlumia.docs.support.services.InternalServiceTokenFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.intercept.AuthorizationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@ConditionalOnProperty(
        prefix = "finlumia.security.module-api",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class ModuleSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain supportApiSecurityFilterChain(
            HttpSecurity http,
            InternalServiceTokenFilter internalServiceTokenFilter,
            ObjectProvider<ExternalApiAuthenticationFilter> jwtFilterProvider,
            PublicApiProperties publicApiProperties) throws Exception {
        http
                .securityMatcher(ApiPaths.EXTERNAL_API_PREFIX + "/**", ApiPaths.INTERNAL_API_PREFIX + "/**")
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.ignoringRequestMatchers(
                        ApiPaths.EXTERNAL_API_PREFIX + "/**",
                        ApiPaths.INTERNAL_API_PREFIX + "/**"))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/error").permitAll()
                        .requestMatchers(publicApiProperties.getPathsArray()).permitAll()
                        .requestMatchers(ApiPaths.INTERNAL_DOCS_EXTERNAL + "/**").permitAll()
                        .requestMatchers(ApiPaths.INTERNAL_DOCS_PREFIX + "/**").permitAll()
                        .requestMatchers(ApiPaths.INTERNAL_API_PREFIX + "/**").permitAll()
                        .requestMatchers(ApiPaths.EXTERNAL_API_PREFIX + "/**").authenticated()
                        .anyRequest().denyAll())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .addFilterBefore(internalServiceTokenFilter, AuthorizationFilter.class);

        ExternalApiAuthenticationFilter jwtFilter = jwtFilterProvider.getIfAvailable();
        if (jwtFilter != null) {
            http.addFilterBefore(jwtFilter, AuthorizationFilter.class);
            http.httpBasic(httpBasic -> httpBasic.disable());
        } else {
            http.httpBasic(Customizer.withDefaults());
        }

        http.formLogin(formLogin -> formLogin.disable());
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(ModuleSecurityProperties props) {
        CorsConfiguration docsConfig = new CorsConfiguration();
        docsConfig.setAllowedOrigins(List.of(props.getDocsOrigin()));
        docsConfig.setAllowedMethods(List.of("GET", "OPTIONS"));
        docsConfig.setAllowedHeaders(List.of("*"));
        docsConfig.setAllowCredentials(false);

        CorsConfiguration apiConfig = new CorsConfiguration();
        apiConfig.setAllowedOrigins(props.getAllowedOrigins());
        apiConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        apiConfig.setAllowedHeaders(List.of("Content-Type", "Authorization"));
        apiConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration(ApiPaths.INTERNAL_DOCS_PREFIX + "/**", docsConfig);
        source.registerCorsConfiguration("/**", apiConfig);
        return source;
    }
}
