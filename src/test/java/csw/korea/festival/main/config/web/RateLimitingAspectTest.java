package csw.korea.festival.main.config.web;

import csw.korea.festival.main.common.annotation.RateLimited;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

public class RateLimitingAspectTest {

    private RateLimitingAspect rateLimitingAspect;

    @Mock
    private RateLimiterService rateLimiterService;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        rateLimitingAspect = new RateLimitingAspect(rateLimiterService);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenAnswer(invocation -> this.getClass().getDeclaredMethod("dummyMethod"));
    }

    @RateLimited(key = "testKey", capacity = 3, refillTokens = 1, refillDurationMillis = 60000)
    public void dummyMethod() {
    }

    @Test
    public void testRateLimitingPass() throws Throwable {
        when(rateLimiterService.resolveBucket(anyString(), anyLong(), anyLong(), any(Duration.class)))
                .thenReturn(Bucket.builder().addLimit(Bandwidth.classic(3, Refill.greedy(1, Duration.ofMinutes(1)))).build());

        rateLimitingAspect.rateLimit(joinPoint, methodSignature.getMethod().getAnnotation(RateLimited.class));
        verify(joinPoint, times(1)).proceed();
    }

    @Test
    public void testRateLimitingExceeded() {
        Bandwidth limit = Bandwidth
                .builder()
                .capacity(1)
                .refillGreedy(1, Duration.ofMinutes(1))
                .build();
        Bucket bucket = Bucket.builder().addLimit(limit).build();
        when(rateLimiterService.resolveBucket(anyString(), anyLong(), anyLong(), any(Duration.class)))
                .thenReturn(bucket);

        bucket.tryConsume(1); // Consume the only token available

        assertThrows(RateLimitExceededException.class, () ->
                rateLimitingAspect.rateLimit(joinPoint, methodSignature.getMethod().getAnnotation(RateLimited.class))
        );
    }
}
