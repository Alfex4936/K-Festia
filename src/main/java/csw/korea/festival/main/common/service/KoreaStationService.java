package csw.korea.festival.main.common.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class KoreaStationService {

    private Map<String, Station> stationMap;

    @PostConstruct
    public void init() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        InputStream inputStream = getClass().getResourceAsStream("/data/stations.json");
        if (inputStream == null) {
            throw new IllegalStateException("stations.json not found in classpath");
        }
        JsonNode rootNode = objectMapper.readTree(inputStream);
        stationMap = new HashMap<>();

        // Parse the DATA array
        JsonNode dataArray = rootNode.get("DATA");
        if (dataArray == null || !dataArray.isArray()) {
            throw new IllegalStateException("Invalid stations.json format: 'DATA' array not found");
        }

        for (JsonNode node : dataArray) {
            String stationName = node.get("bldn_nm").asText();
            String latStr = node.get("lat").asText();
            String lonStr = node.get("lot").asText();

            // Parse latitude and longitude
            double latitude;
            double longitude;
            try {
                latitude = Double.parseDouble(latStr);
                longitude = Double.parseDouble(lonStr);
            } catch (NumberFormatException e) {
                // Skip this station if coordinates are invalid
                continue;
            }

            // Normalize station name
            String normalizedName = normalizeStationName(stationName);

            Station station = new Station(stationName, normalizedName, latitude, longitude);
            stationMap.put(normalizedName, station);
        }
    }

    /**
     * Retrieves a station by name, handling variations in input.
     *
     * @param name The station name input by the user.
     * @return An Optional containing the Station if found.
     */
    public Optional<Station> getStationByName(String name) {
        String normalizedName = normalizeStationName(name);

        Station station = stationMap.get(normalizedName);

        return Optional.ofNullable(station);
    }

    /**
     * Normalizes station names by stripping parentheses content and ensuring the "역" suffix.
     *
     * @param name The original station name.
     * @return The normalized station name.
     */
    private String normalizeStationName(String name) {
        // Strip parentheses and their content
        int idx = name.indexOf("(");
        if (idx != -1) {
            name = name.substring(0, idx);
        }

        // Remove trailing whitespace
        name = name.trim();

        // Ensure the station name ends with "역"
        if (!name.endsWith("역")) {
            name = name + "역";
        }

        return name;
    }

    @Getter
    @Setter
    public static class Station {
        private String displayName;     // Original station name from data
        private String name;  // Normalized name used for matching
        private Double latitude;
        private Double longitude;

        public Station(String displayName, String name, Double latitude, Double longitude) {
            this.displayName = displayName;
            this.name = name;
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
