package csw.korea.festival.main.festival.service;

import csw.korea.festival.main.common.dto.KWeather;
import csw.korea.festival.main.common.service.KakaoAPIService;
import csw.korea.festival.main.config.LimitedThreadFactory;
import csw.korea.festival.main.festival.model.Festival;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.ThreadFactory;

@Slf4j
@Service
public class FestivalWeatherService {

    private final KakaoAPIService kakaoAPIService;

    public FestivalWeatherService(KakaoAPIService kakaoAPIService) {
        this.kakaoAPIService = kakaoAPIService;
    }

    /**
     * Fetch weather data.
     *
     * @param festivalsToProcess The Festivals to process.
     * @return The processed Festival.
     */
    public List<Festival> processFestivalsWeather(List<Festival> festivalsToProcess) {
        int maxConcurrency = 10;
        ThreadFactory baseFactory = Thread.ofVirtual().factory();
        ThreadFactory limitedFactory = new LimitedThreadFactory(baseFactory, maxConcurrency);

        List<Festival> processedFestivals = new ArrayList<>();
        try (var scope = new StructuredTaskScope.ShutdownOnFailure(null, limitedFactory)) {
            List<StructuredTaskScope.Subtask<Festival>> futures = new ArrayList<>();
            for (Festival festival : festivalsToProcess) {
                StructuredTaskScope.Subtask<Festival> future = scope.fork(() -> {
                    // Fetch Weather Data Concurrently
                    try {
                        KWeather.WeatherRequest weather = kakaoAPIService.getWeatherFromCoordinates(
                                festival.getLatitude(), festival.getLongitude());
                        festival.setWeather(weather);
                        log.info("Fetched weather for festival '{}' ({})", festival.getName(), weather);
                    } catch (Exception e) {
                        log.error("Error fetching weather for festival '{}': {}", festival.getName(), e.getMessage());
                        festival.setWeather(null);
                    }
                    return festival;
                });
                futures.add(future);
            }

            scope.join();
            scope.throwIfFailed();

            for (StructuredTaskScope.Subtask<Festival> future : futures) {
                Festival result = future.get();
                if (result != null) {
                    processedFestivals.add(result);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error fetching festival weather", e);
        }
        return processedFestivals;
    }
}
