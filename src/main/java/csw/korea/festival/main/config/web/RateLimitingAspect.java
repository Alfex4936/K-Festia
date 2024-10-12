package csw.korea.festival.main.config.web;

import csw.korea.festival.main.common.annotation.RateLimited;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.*;

import java.time.Duration;

/**
 * Aspect to handle rate limiting for methods annotated with @RateLimited.
 */
@Aspect
@Component
public class RateLimitingAspect {

    private final RateLimiterService rateLimiterService;

    public RateLimitingAspect(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    /**
     * Around advice to apply rate limiting before method execution.
     *
     * @param pjp          The proceeding join point.
     * @param rateLimited  The RateLimited annotation instance.
     * @return The result of the method execution.
     * @throws Throwable If the method execution throws an exception.
     */
    @Around("@annotation(rateLimited)")
    public Object rateLimit(ProceedingJoinPoint pjp, RateLimited rateLimited) throws Throwable {
        String clientId = getClientId();
        String key = STR."\{rateLimited.key()}:\{clientId}";
        long capacity = rateLimited.capacity();
        long refillTokens = rateLimited.refillTokens();
        Duration refillDuration = Duration.ofMillis(rateLimited.refillDurationMillis());

        Bucket bucket = rateLimiterService.resolveBucket(key, capacity, refillTokens, refillDuration);

        if (bucket.tryConsume(1)) {
            return pjp.proceed();
        } else {
            throw new RateLimitExceededException("Too many requests. Please try again later.");
        }
    }

    private String getClientId() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes == null) {
            return "anonymous";
        }

        HttpServletRequest request = ((ServletRequestAttributes) requestAttributes).getRequest();
        String clientIp = extractClientIp(request);
        return clientIp != null ? clientIp : "anonymous";
    }

    private String extractClientIp(HttpServletRequest request) {
        String[] headerNames = { "X-Forwarded-For", "Proxy-Client-IP", "WL-Proxy-Client-IP",
                "HTTP_CLIENT_IP", "HTTP_X_FORWARDED_FOR", "Fly-Client-IP", "X-Real-IP" };

        for (String header : headerNames) {
            String ipList = request.getHeader(header);
            if (ipList != null && !ipList.isEmpty() && !"unknown".equalsIgnoreCase(ipList)) {
                return ipList.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }
}
