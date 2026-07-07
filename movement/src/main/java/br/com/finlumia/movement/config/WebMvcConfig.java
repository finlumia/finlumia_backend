package br.com.finlumia.movement.config;

import br.com.finlumia.movement.models.CategoryId;
import br.com.finlumia.movement.models.DeleteMode;
import br.com.finlumia.movement.models.InstitutionId;
import br.com.finlumia.movement.models.PaymentMethod;
import br.com.finlumia.movement.models.SortBy;
import br.com.finlumia.movement.models.SortOrder;
import br.com.finlumia.movement.models.TransactionType;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// KeyUserInterceptor removido: o user_key é derivado do JWT (atributo "usersKey")
// e não mais de um header HTTP controlado pelo cliente, eliminando o vetor de IDOR.
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    // Sem isso, o Spring usa Enum.valueOf() (que exige o nome exato da constante,
    // ex.: "DATE") para converter @RequestParam em enum, mas a API e o frontend
    // trocam os valores em lowercase definidos via @JsonValue/fromValue (ex.:
    // "date"). Isso fazia todo GET /api/v1/transactions retornar 400, já que o
    // frontend sempre envia sortBy=date&sortOrder=desc.
    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, TransactionType.class, TransactionType::fromValue);
        registry.addConverter(String.class, PaymentMethod.class, PaymentMethod::fromValue);
        registry.addConverter(String.class, InstitutionId.class, InstitutionId::fromValue);
        registry.addConverter(String.class, CategoryId.class, CategoryId::fromValue);
        registry.addConverter(String.class, SortBy.class, SortBy::fromValue);
        registry.addConverter(String.class, SortOrder.class, SortOrder::fromValue);
        registry.addConverter(String.class, DeleteMode.class, DeleteMode::fromValue);
    }
}
