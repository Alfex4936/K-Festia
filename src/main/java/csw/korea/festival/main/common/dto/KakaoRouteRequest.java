package csw.korea.festival.main.common.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class KakaoRouteRequest {

    private Location origin;
    private Location destination;
    private List<Location> waypoints;
    private String priority; // "RECOMMEND", "TIME", or "DISTANCE"
    private String car_fuel; // "GASOLINE", "DIESEL", "LPG"
    private boolean car_hipass;
    private boolean alternatives;
    private boolean road_details;
    private boolean summary;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    @ToString
    public static class Location {
        private String name; // Optional
        private double x; // Longitude
        private double y; // Latitude
    }
}
