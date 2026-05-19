package br.com.finlumia.shared.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import br.com.finlumia.shared.security.HeaderKeyUserResolver;

@Component
public class KeyUserInterceptor implements HandlerInterceptor {

    private static final String HEADER_NAME = "keyUser";
    private static final String ATTRIBUTE_NAME = "keyUser";
    private final HeaderKeyUserResolver headerKeyUserResolver;

    public KeyUserInterceptor(HeaderKeyUserResolver headerKeyUserResolver) {
        this.headerKeyUserResolver = headerKeyUserResolver;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String keyUserHeader = request.getHeader(HEADER_NAME);
        if (keyUserHeader == null || keyUserHeader.isBlank()) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Header 'keyUser' is required");
            return false;
        }

        return headerKeyUserResolver.resolve(keyUserHeader)
                .map(keyUser -> {
            request.setAttribute(ATTRIBUTE_NAME, keyUser);
            return true;
                })
                .orElseGet(() -> {
                    try {
                        response.sendError(
                                HttpServletResponse.SC_BAD_REQUEST,
                                "Header 'keyUser' must be a mapped key or a numeric value greater than zero");
                    } catch (Exception ignored) {
                        return false;
                    }
                    return false;
                });
    }
}
