package csw.korea.festival.main.festival.resolver;

import csw.korea.festival.main.LuceneConfigurationTest;
import csw.korea.festival.main.config.web.RateLimitExceededException;
import csw.korea.festival.main.festival.model.FestivalPage;
import csw.korea.festival.main.festival.service.FestivalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.graphql.test.tester.HttpGraphQlTester;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(LuceneConfigurationTest.class)
public class FestivalResolverIntegrationTest {

    @LocalServerPort
    int port;
    private HttpGraphQlTester graphQlTester;
    @MockBean
    private FestivalService festivalService;

    @BeforeEach
    public void setUp() {
        WebTestClient client = WebTestClient.bindToServer()
                .baseUrl(String.format("http://localhost:%s/graphql", port))
                .build();

        graphQlTester = HttpGraphQlTester.create(client);

        // Mocking service for GraphQL queries
        when(festivalService.getFestivals("01", null, null, 0, 10))
                .thenReturn(new FestivalPage()); // Mocked response
    }

    @Test
    void contextLoads() {
        assertNotNull(graphQlTester);
    }

    @Test
    public void testGraphQlRateLimitExceeded() {
        String query = "{ festivals(month: \"01\", page: 0, size: 10) { totalPages } }";

        // Simulate rate limit exceeded by throwing RateLimitExceededException
        when(festivalService.getFestivals(any(), any(), any(), any(), any()))
                .thenThrow(new RateLimitExceededException("Rate limit exceeded"));

        graphQlTester.document(query)
                .execute()
                .errors()
                .satisfy(errors -> {
                    assert errors.stream().anyMatch(error -> Objects.requireNonNull(error.getMessage()).contains("Rate limit exceeded"));
                });
    }

    @Test
    public void testGraphQlSuccess() {
        String query = "{ festivals(month: \"01\", page: 0, size: 10) { totalPages } }";

        graphQlTester.document(query)
                .execute()
                .path("festivals.totalPages")
                .hasValue();
    }
}
