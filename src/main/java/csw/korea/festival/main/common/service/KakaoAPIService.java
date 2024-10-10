package csw.korea.festival.main.common.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import csw.korea.festival.main.common.dto.KWeather;
import csw.korea.festival.main.common.util.CoordinatesConverter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoAPIService {

    private final Cache<String, KWeather.WeatherRequest> weatherCache = Caffeine.newBuilder()
            .expireAfterWrite(30, TimeUnit.MINUTES) // expires after 30 mins
            .maximumSize(1000) // Maximum number of entries in the cache
            .recordStats() // Enable statistics collection
            .build();

    private final WebClient webClient;

    @Value("${kakao.weather.url}")
    private String kakaoWeatherUrl;

    @Value("${kakao.weather.icon-url}")
    private String kakaoWeatherIconUrl;

    private KWeather.@NotNull WeatherRequest getWeatherRequest(KWeather.WeatherResponse apiResp) {
        String icon = String.format(kakaoWeatherIconUrl,
                apiResp.getWeatherInfos().getCurrent().getIconId());

        // Create the WeatherRequest object
        KWeather.WeatherRequest weatherRequest = new KWeather.WeatherRequest();
        weatherRequest.setTemperature(apiResp.getWeatherInfos().getCurrent().getTemperature());
        weatherRequest.setDesc(apiResp.getWeatherInfos().getCurrent().getDesc());
        weatherRequest.setIconImage(icon);
        weatherRequest.setHumidity(apiResp.getWeatherInfos().getCurrent().getHumidity());
        weatherRequest.setRainfall(apiResp.getWeatherInfos().getCurrent().getRainfall());
        weatherRequest.setSnowfall(apiResp.getWeatherInfos().getCurrent().getSnowfall());
        return weatherRequest;
    }

    /**
     * Fetches weather information based on latitude and longitude.
     *
     * @param latitude  Latitude in WGS84 format.
     * @param longitude Longitude in WGS84 format.
     * @return WeatherRequest object containing weather details.
     * @throws Exception if an error occurs during the process.
     */
    // @Cacheable(value = "weatherCache", key = "#latitude + ',' + #longitude")
    public KWeather.WeatherRequest getWeatherFromCoordinates(double latitude, double longitude) throws Exception {
        // Check if the weather result is already cached
        var cacheKey = String.format("%f,%f", latitude, longitude);
        var cachedWeather = weatherCache.getIfPresent(cacheKey);
        if (cachedWeather != null) {
            return cachedWeather;
        }

        // Convert coordinates from WGS84 to WCONGNAMUL
        var wcongnamul = CoordinatesConverter.convertWGS84ToWCONGNAMUL(latitude, longitude);

        // Construct the request URL
        String reqURL = String.format("%s&x=%f&y=%f", kakaoWeatherUrl, wcongnamul.latitude(), wcongnamul.longitude());

        // Perform the GET request
        Mono<KWeather.WeatherResponse> responseMono = webClient.get()
                .uri(reqURL)
                .header("Referer", reqURL)
                .retrieve()
                .bodyToMono(KWeather.WeatherResponse.class);

        KWeather.WeatherResponse apiResp;
        try {
            apiResp = responseMono.block();
        } catch (Exception e) {
            log.error("Error executing request: {}", e.getMessage());
            throw new IOException("Error executing weather API request", e);
        }

        if (apiResp == null) {
            throw new IOException("Empty response from weather API");
        }

        if (!"OK".equalsIgnoreCase(apiResp.getCodes().getResultCode())) {
            throw new IOException("No weather found for this address");
        }

        var result = getWeatherRequest(apiResp);
        weatherCache.put(cacheKey, result);
        return result;
    }
}
