package csw.korea.festival.main.festival.service;

import csw.korea.festival.main.common.dto.KakaoRouteRequest;
import csw.korea.festival.main.common.dto.KakaoRouteResponse;
import csw.korea.festival.main.common.service.KoreaStationService;
import csw.korea.festival.main.config.KakaoMobilityClient;
import csw.korea.festival.main.festival.exception.NoNearbyFestivalException;
import csw.korea.festival.main.festival.model.Festival;
import csw.korea.festival.main.festival.model.FestivalCategory;
import csw.korea.festival.main.festival.model.FestivalRouteDTO;
import csw.korea.festival.main.festival.repository.FestivalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

import static csw.korea.festival.main.common.util.CoordinatesConverter.calculateDistance;

@RequiredArgsConstructor
@Service
public class FestivalRouteService {

    private final FestivalRepository festivalRepository;
    private final KoreaStationService koreaStationService;
    private final KakaoMobilityClient kakaoMobilityClient;

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
            return new FestivalRouteDTO(Collections.emptyList(), 0, Duration.ZERO, null);
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

        return new FestivalRouteDTO(route, totalDistance, totalDuration, null);
    }

    public FestivalRouteDTO planFestivalRouteByCar(
            String startStationName, LocalDate startDate, LocalDate endDate,
            List<FestivalCategory> preferredCategories, int maxAllowedFestivals, double maxDistanceKm) {
        // Get the start station coordinates
        KoreaStationService.Station startStation = koreaStationService.getStationByName(startStationName)
                .orElseThrow(() -> new IllegalArgumentException("Station not found: " + startStationName));

        double startLat = startStation.getLatitude();
        double startLon = startStation.getLongitude();

        // Fetch festivals within date range and categories
        List<Festival> allFestivals = festivalRepository.findFestivalsByDateRangeAndCategories(
                startDate, endDate, preferredCategories);

        // Filter festivals within the maximum distance
        List<Festival> nearbyFestivals = allFestivals.stream()
                .map(festival -> {
                    double festivalLat = festival.getLatitude();
                    double festivalLon = festival.getLongitude();
                    double distance = calculateDistance(startLat, startLon, festivalLat, festivalLon);
                    festival.setDistance(distance); // Add a field to store distance
                    return festival;
                })
                .filter(festival -> festival.getDistance() <= maxDistanceKm)
                .sorted(Comparator.comparingDouble(Festival::getDistance))
                .limit(maxAllowedFestivals)
                .collect(Collectors.toList());

        if (nearbyFestivals.isEmpty()) {
            throw new NoNearbyFestivalException(STR."No festivals found within \{maxDistanceKm} km of the start station.");
        }

        // Optimize festival order
        List<Festival> optimizedFestivals = optimizeFestivalOrder(startStation, nearbyFestivals);

        // Build the Kakao Route Request
        KakaoRouteRequest routeRequest = buildKakaoRouteRequest(startStation, optimizedFestivals);

        // Call Kakao API
        KakaoRouteResponse routeResponse = kakaoMobilityClient.getMultiWaypointRoute(routeRequest).block();

        // Handle response and map to FestivalRouteDTO
        return mapToFestivalRouteDTO(nearbyFestivals, routeResponse);
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

    private KakaoRouteRequest buildKakaoRouteRequest(KoreaStationService.Station startStation, List<Festival> festivals) {
        // Set origin using start station coordinates
        KakaoRouteRequest.Location origin = new KakaoRouteRequest.Location();
        origin.setName(startStation.getName());
        origin.setX(startStation.getLongitude());
        origin.setY(startStation.getLatitude());

        // Set destination as the last festival
        Festival lastFestival = festivals.getLast();
        KakaoRouteRequest.Location destination = new KakaoRouteRequest.Location();
        destination.setName(lastFestival.getName());
        destination.setX(lastFestival.getLongitude());
        destination.setY(lastFestival.getLatitude());

        // Set waypoints as intermediate festivals (excluding the last one)
        List<KakaoRouteRequest.Location> waypoints = festivals.subList(0, festivals.size() - 1).stream()
                .map(festival -> {
                    KakaoRouteRequest.Location loc = new KakaoRouteRequest.Location();
                    loc.setName(festival.getName());
                    loc.setX(festival.getLongitude());
                    loc.setY(festival.getLatitude());
                    return loc;
                })
                .collect(Collectors.toList());

        // Build request
        KakaoRouteRequest request = new KakaoRouteRequest();
        request.setOrigin(origin);
        request.setDestination(destination);
        request.setWaypoints(waypoints);
        request.setPriority("RECOMMEND"); // or "TIME", "DISTANCE"
        request.setCar_fuel("GASOLINE");
        request.setCar_hipass(false);
        request.setAlternatives(false);
        request.setRoad_details(false);
        request.setSummary(true); // to reduce response size

        return request;
    }

    private FestivalRouteDTO mapToFestivalRouteDTO(List<Festival> festivals, KakaoRouteResponse routeResponse) {
        if (routeResponse == null || routeResponse.getRoutes() == null || routeResponse.getRoutes().isEmpty()) {
            throw new RuntimeException("Failed to retrieve route from Kakao API");
        }

        KakaoRouteResponse.Route route = routeResponse.getRoutes().getFirst();
        KakaoRouteResponse.Route.Summary summary = route.getSummary();

        double totalDistance = summary.getDistance() / 1000.0; // Convert meters to kilometers
        Duration totalDuration = Duration.ofSeconds(summary.getDuration());

        return new FestivalRouteDTO(festivals, totalDistance, totalDuration, null);
    }

    public List<Festival> optimizeFestivalOrder(KoreaStationService.Station startStation, List<Festival> festivals) {
        List<Festival> optimizedRoute = new ArrayList<>();
        Set<Festival> unvisited = new HashSet<>(festivals);

        // Start from the start station
        double currentLat = startStation.getLatitude();
        double currentLon = startStation.getLongitude();

        while (!unvisited.isEmpty()) {
            Festival nearestFestival = null;
            double nearestDistance = Double.MAX_VALUE;

            for (Festival festival : unvisited) {
                double distance = calculateDistance(currentLat, currentLon, festival.getLatitude(), festival.getLongitude());
                if (distance < nearestDistance) {
                    nearestDistance = distance;
                    nearestFestival = festival;
                }
            }

            optimizedRoute.add(nearestFestival);
            unvisited.remove(nearestFestival);
            currentLat = Objects.requireNonNull(nearestFestival).getLatitude();
            currentLon = nearestFestival.getLongitude();
        }

        return optimizedRoute;
    }

}
