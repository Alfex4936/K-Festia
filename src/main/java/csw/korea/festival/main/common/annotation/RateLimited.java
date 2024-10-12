package csw.korea.festival.main.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to apply rate limiting to GraphQL resolver methods.
 *
 * <p>
 * Parameters:
 * <ul>
 *   <li><b>key</b>: A unique identifier for the rate limit (e.g., method name).</li>
 *   <li><b>capacity</b>: The maximum number of requests allowed in the specified duration.</li>
 *   <li><b>refillTokens</b>: Number of tokens to add at each refill interval.</li>
 *   <li><b>refillDurationMillis</b>: Duration in milliseconds for token refill intervals.</li>
 * </ul>
 * </p>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RateLimited {
    String key();

    long capacity();

    long refillTokens();

    long refillDurationMillis(); // Duration in milliseconds
}