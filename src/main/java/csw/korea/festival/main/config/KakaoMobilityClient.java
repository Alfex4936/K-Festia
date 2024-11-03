package csw.korea.festival.main.config;

import csw.korea.festival.main.common.dto.KakaoRouteRequest;
import csw.korea.festival.main.common.dto.KakaoRouteResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class KakaoMobilityClient {

    private final WebClient webClient;

    public KakaoMobilityClient(@Value("${kakao.rest-api.key}") String kakaoApiKey) {
        int size = 16 * 1024 * 1024; // 16MB

        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(size))
                .build();

        this.webClient = WebClient.builder()
                .baseUrl("https://apis-navi.kakaomobility.com")
                .defaultHeader("Authorization", STR."KakaoAK \{kakaoApiKey}")
                .exchangeStrategies(strategies)
                .build();
    }

    public Mono<KakaoRouteResponse> getMultiWaypointRoute(KakaoRouteRequest request) {
        return webClient.post()
                .uri("/v1/waypoints/directions")
                .header("Content-Type", "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(KakaoRouteResponse.class);
    }
}
