package csw.korea.festival.main.config;

import csw.korea.festival.main.config.web.RateLimitExceededException;
import csw.korea.festival.main.festival.exception.NoNearbyFestivalException;
import graphql.ErrorClassification;
import graphql.ErrorType;
import graphql.GraphQLError;
import graphql.language.SourceLocation;
import graphql.schema.DataFetchingEnvironment;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;

import java.util.List;
import java.util.Map;


/**
 * GraphQL configuration to handle custom exceptions.
 */
@Configuration
public class GraphQLConfig {

    @Bean
    public DataFetcherExceptionResolverAdapter dataFetcherExceptionResolver() {
        return new DataFetcherExceptionResolverAdapter() {

            @Override
            protected GraphQLError resolveToSingleError(@NotNull Throwable ex, @NotNull DataFetchingEnvironment env) {
                if (ex instanceof RateLimitExceededException) {
                    return new CustomGraphQLRateLimitError(
                            ex.getMessage(),
                            env.getField().getSourceLocation(),
                            env.getExecutionStepInfo().getPath().toList(),
                            Map.of("code", "RATE_LIMIT_EXCEEDED")
                    );
                } else if (ex instanceof NoNearbyFestivalException) {
                    return new CustomGraphQLRateLimitError(
                            ex.getMessage(),
                            env.getField().getSourceLocation(),
                            env.getExecutionStepInfo().getPath().toList(),
                            Map.of("code", "NO_NEARBY_FESTIVAL")
                    );
                }
                return super.resolveToSingleError(ex, env);
            }
        };
    }

    /**
     * Custom GraphQLError implementation to include additional information.
     */
    public static class CustomGraphQLRateLimitError implements GraphQLError {
        private final String message;
        private final List<SourceLocation> locations;
        private final List<Object> path;
        private final Map<String, Object> extensions;

        public CustomGraphQLRateLimitError(String message, SourceLocation location, List<Object> path, Map<String, Object> extensions) {
            this.message = message;
            this.locations = List.of(location);
            this.path = path;
            this.extensions = extensions;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public List<SourceLocation> getLocations() {
            return locations;
        }

        @Override
        public ErrorClassification getErrorType() {
            return ErrorType.DataFetchingException;
        }

        @Override
        public List<Object> getPath() {
            return path;
        }

        @Override
        public Map<String, Object> getExtensions() {
            return extensions;
        }
    }
}