package csw.korea.festival.main.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.ratelimiter.operator.RateLimiterOperator;
import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.util.retry.Retry;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
public class WebClientConfig {

    @Value("${webclient.user-agent}")
    private String userAgent;

    @Bean
    public WebClient webClient() {
        // Connection Pool Configuration
        ConnectionProvider connectionProvider = ConnectionProvider.builder("visitKoreaPool")
                .maxConnections(100)
                .pendingAcquireTimeout(Duration.ofSeconds(60))
                .maxIdleTime(Duration.ofSeconds(60))
                .build();

        // SSL Context Configuration
        SslContext sslContext;
        try {
            sslContext = SslContextBuilder.forClient()
                    .trustManager(InsecureTrustManagerFactory.INSTANCE) // TODO: on production
                    .build();
        } catch (Exception e) {
            sslContext = null;
        }

        // HttpClient Configuration
        SslContext finalSslContext = sslContext;
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15000) // 15 seconds
                .responseTimeout(Duration.ofSeconds(30)) // 30 seconds
                .secure(spec -> {
                    if (finalSslContext != null) {
                        spec.sslContext(finalSslContext);
                    }
                })
                .keepAlive(true)
                .compress(true)
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(30, TimeUnit.SECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(30, TimeUnit.SECONDS))
                );
//                .wiretap("reactor.netty.http.client.HttpClient", LogLevel.INFO, AdvancedByteBufFormat.TEXTUAL);

        // Circuit Breaker Configuration
        CircuitBreaker circuitBreaker = CircuitBreaker.of("visitKoreaCB",
                CircuitBreakerConfig.custom()
                        .failureRateThreshold(50) // Percentage to open circuit
                        .waitDurationInOpenState(Duration.ofSeconds(30)) // Time to wait before attempting to close circuit
                        .slidingWindowSize(20) // Number of calls to evaluate
                        .build()
        );

        // Rate Limiter Configuration
        RateLimiter rateLimiter = RateLimiter.of("visitKoreaRateLimiter",
                RateLimiterConfig.custom()
                        .limitForPeriod(10) // Number of permits per period
                        .limitRefreshPeriod(Duration.ofSeconds(1)) // Refresh period
                        .timeoutDuration(Duration.ofSeconds(2)) // Wait time for a permit
                        .build()
        );

        // Build WebClient with Filters
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                // .baseUrl("https://korean.visitkorea.or.kr")
                .defaultHeader(HttpHeaders.USER_AGENT, userAgent)
                .filter((request, next) ->
                        next.exchange(request)
                                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                                .transformDeferred(RateLimiterOperator.of(rateLimiter))
                                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2))
                                        .filter(throwable -> throwable instanceof IOException))
                )
                .build();
    }
}
