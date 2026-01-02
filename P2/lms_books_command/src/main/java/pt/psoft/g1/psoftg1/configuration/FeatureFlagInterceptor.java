package pt.psoft.g1.psoftg1.configuration;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor to check feature flags and kill switch before processing requests
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FeatureFlagInterceptor implements HandlerInterceptor {

    private final FeatureFlagService featureFlagService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {

        String method = request.getMethod();
        String path = request.getRequestURI();

        // Allow GET requests and health checks always
        if (method.equals("GET") || path.contains("/actuator") || path.contains("/api/admin/feature-flags")) {
            return true;
        }

        // Check master kill switch for write operations
        if (featureFlagService.isMasterKillSwitchActive()) {
            log.warn("Request blocked by MASTER KILL SWITCH: {} {}", method, path);
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"Service temporarily unavailable\",\"reason\":\"Master kill switch is active\"}"
            );
            return false;
        }

        return true;
    }
}

