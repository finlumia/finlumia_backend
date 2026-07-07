package br.com.finlumia.document.config;

import br.com.finlumia.document.models.InsightType;
import br.com.finlumia.document.models.Period;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// KeyUserInterceptor removido: o user_key é derivado do JWT (atributo "usersKey")
// e não mais de um header HTTP controlado pelo cliente, eliminando o vetor de IDOR
// (mesma migração já aplicada no módulo movement).
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    // Sem isso, o Spring usa Enum.valueOf() (que exige o nome exato da constante,
    // ex.: "SIX_MONTHS") para converter @RequestParam em enum, mas a API e o
    // frontend trocam os valores em lowercase definidos via @JsonValue/fromValue
    // (ex.: "6m"). Isso fazia todo GET /api/v1/reports/* retornar 400/502.
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, Period.class, Period::fromValue);
        registry.addConverter(String.class, InsightType.class, InsightType::fromValue);
    }
}
