package csw.korea.festival.main.festival.service;

import csw.korea.festival.main.common.service.KoreaStationService;
import csw.korea.festival.main.festival.model.Festival;
import csw.korea.festival.main.festival.model.FestivalCategory;
import csw.korea.festival.main.festival.model.FestivalRouteDTO;
import csw.korea.festival.main.festival.repository.FestivalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;

import static csw.korea.festival.main.common.util.CoordinatesConverter.calculateDistance;

@RequiredArgsConstructor
@Service
public class FestivalRouteService {

    private final FestivalRepository festivalRepository;
    private final KoreaStationService koreaStationService;

    public FestivalRouteDTO planFestivalRoute(String startStationName, LocalDate startDate, LocalDate endDate,
                                              List<FestivalCategory> preferredCategories, int maxFestivals) {
        // 1. Get the start station coordinates
        KoreaStationService.Station startStation = koreaStationService.getStationByName(startStationName)
                .orElseThrow(() -> new IllegalArgumentException("Station not found: " + startStationName));

        // 2. Fetch festivals within date range and categories
        List<Festival> candidateFestivals = festivalRepository.findFestivalsByDateRangeAndCategories(
                startDate, endDate, preferredCategories);

        // 3. Calculate distances from the start station to each festival
        Map<Festival, Double> distancesFromStart = new HashMap<>();
        for (Festival festival : candidateFestivals) {
            double distance = calculateDistance(startStation.getLatitude(), startStation.getLongitude(),
                    festival.getLatitude(), festival.getLongitude());
            distancesFromStart.put(festival, distance);
        }

        // 4. Initialize the route
        List<Festival> route = new ArrayList<>();
        Set<Festival> visited = new HashSet<>();

        // 5. Start from the nearest festival to the start station
        Festival currentFestival = candidateFestivals.stream()
                .min(Comparator.comparing(distancesFromStart::get))
                .orElse(null);

        if (currentFestival == null) {
            // No festivals found
            return new FestivalRouteDTO(Collections.emptyList(), 0, Duration.ZERO);
        }

        route.add(currentFestival);
        visited.add(currentFestival);

        // 6. Iteratively find the nearest unvisited festival
        while (route.size() < maxFestivals && visited.size() < candidateFestivals.size()) {
            Festival lastFestival = route.getLast();
            Festival nextFestival = candidateFestivals.stream()
                    .filter(f -> !visited.contains(f))
                    .min(Comparator.comparing(f -> calculateDistance(
                            lastFestival.getLatitude(), lastFestival.getLongitude(),
                            f.getLatitude(), f.getLongitude())))
                    .orElse(null);

            if (nextFestival == null) {
                break;
            }

            route.add(nextFestival);
            visited.add(nextFestival);
        }

        // 7. Calculate total distance and duration
        double totalDistance = calculateTotalDistance(startStation, route);
        Duration totalDuration = estimateTotalDurationByWalking(totalDistance);

        return new FestivalRouteDTO(route, totalDistance, totalDuration);
    }

    private double calculateTotalDistance(KoreaStationService.Station startStation, List<Festival> route) {
        double totalDistance = 0;
        double prevLat = startStation.getLatitude();
        double prevLon = startStation.getLongitude();

        for (Festival festival : route) {
            // KM
            double distance = calculateDistance(prevLat, prevLon, festival.getLatitude(), festival.getLongitude());
            totalDistance += distance;
            prevLat = festival.getLatitude();
            prevLon = festival.getLongitude();
        }

        return totalDistance;
    }

    private Duration estimateTotalDurationByWalking(double totalDistance) {
        double averageSpeedKmPerHour = 4.5; // km/h
        double hours = totalDistance / averageSpeedKmPerHour;
        return Duration.ofMinutes((long) (hours * 60));
    }
}
