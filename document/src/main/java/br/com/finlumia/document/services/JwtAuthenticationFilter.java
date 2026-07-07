package br.com.finlumia.document.services;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import br.com.finlumia.document.config.ApiPaths;
import br.com.finlumia.document.config.PublicApiProperties;
import br.com.finlumia.shared.exception.FinlumiaException;
import br.com.finlumia.shared.views.DialogDefault;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter implements ExternalApiAuthenticationFilter {

    public static final String REQUEST_ATTR_USER_KEY = "usersKey";
    public static final String REQUEST_ATTR_USER_EMAIL = "usersEmail";

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final PublicApiProperties publicApiProperties;
    private final ObjectMapper objectMapper;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthenticationFilter(
            JwtService jwtService,
            PublicApiProperties publicApiProperties,
            ObjectMapper objectMapper) {
        this.jwtService = jwtService;
        this.publicApiProperties = publicApiProperties;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        if (requestUri == null || !requestUri.startsWith(ApiPaths.EXTERNAL_API_PREFIX + "/")) {
            return true;
        }

        for (String pattern : publicApiProperties.getPaths()) {
            if (pathMatcher.match(pattern, requestUri)) {
                return true;
            }
        }

        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Nao autorizado", "Bearer token is required");
            return;
        }

        String accessToken = authorization.substring(BEARER_PREFIX.length()).trim();
        if (accessToken.isBlank()) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "Nao autorizado", "Bearer token is required");
            return;
        }

        try {
            Claims claims = jwtService.parseClaims(accessToken);
            JwtAuthentication authentication = new JwtAuthentication(
                    jwtService.extractUserKey(claims),
                    claims.get("email", String.class));

            SecurityContextHolder.getContext().setAuthentication(authentication);
            request.setAttribute(REQUEST_ATTR_USER_KEY, authentication.getUserKey());
            request.setAttribute(REQUEST_ATTR_USER_EMAIL, authentication.getEmail());
            filterChain.doFilter(request, response);
        } catch (FinlumiaException exception) {
            writeFinlumiaError(response, exception);
        }
    }

    private void writeFinlumiaError(HttpServletResponse response, FinlumiaException exception) throws IOException {
        int status = exception.getCode() > 0 ? exception.getCode() : HttpServletResponse.SC_UNAUTHORIZED;
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        DialogDefault body = new DialogDefault(exception.getCode(), exception.getTitle(), exception.getMessage());
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private void writeError(HttpServletResponse response, int status, String title, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), new DialogDefault(status, title, message));
    }
}
