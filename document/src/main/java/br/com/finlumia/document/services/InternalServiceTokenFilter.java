package br.com.finlumia.document.services;

import java.io.IOException;

import br.com.finlumia.document.config.ApiPaths;
import br.com.finlumia.document.config.InternalSecurityProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@ConditionalOnProperty(
        prefix = "finlumia.security.module-api",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class InternalServiceTokenFilter extends OncePerRequestFilter {

    private final InternalSecurityProperties internalSecurityProperties;

    public InternalServiceTokenFilter(InternalSecurityProperties internalSecurityProperties) {
        this.internalSecurityProperties = internalSecurityProperties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !requiresInternalServiceToken(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String configuredToken = internalSecurityProperties.getServiceToken();
        if (configuredToken == null || configuredToken.isBlank()) {
            response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Internal service token is not configured");
            return;
        }

        String providedToken = request.getHeader(internalSecurityProperties.getHeaderName());
        if (providedToken == null || !configuredToken.equals(providedToken)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid internal service token");
            return;
        }

        filterChain.doFilter(request, response);
    }

    static boolean requiresInternalServiceToken(String requestUri) {
        if (requestUri == null || requestUri.isBlank()) {
            return false;
        }

        if (requestUri.startsWith(ApiPaths.INTERNAL_DOCS_INTERNAL)) {
            return true;
        }

        if (!requestUri.startsWith(ApiPaths.INTERNAL_API_PREFIX + "/")) {
            return false;
        }

        return !requestUri.startsWith(ApiPaths.INTERNAL_DOCS_PREFIX);
    }
}
