package csw.korea.festival.main.common.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
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

        // Initialize the station map
        stationMap = new HashMap<>();

        // Load the stations.json file
        try (InputStream inputStream = getClass().getResourceAsStream("/data/stations.json")) {
            if (inputStream == null) {
                throw new IllegalStateException("stations.json not found in classpath");
            }

            // Create a JsonParser
            JsonFactory jsonFactory = objectMapper.getFactory();
            try (JsonParser parser = jsonFactory.createParser(inputStream)) {
                // Move to the start of the object
                if (parser.nextToken() != JsonToken.START_OBJECT) {
                    throw new IllegalStateException("Expected data to start with an Object");
                }

                // Iterate over the fields of the root object
                while (parser.nextToken() != JsonToken.END_OBJECT) {
                    String fieldName = parser.currentName();
                    parser.nextToken(); // Move to the value

                    if ("DATA".equals(fieldName)) {
                        if (parser.currentToken() == JsonToken.START_ARRAY) {
                            // Create an ObjectReader for StationData
                            ObjectReader reader = objectMapper.readerFor(StationData.class);

                            // Process each element in the "DATA" array
                            while (parser.nextToken() != JsonToken.END_ARRAY) {
                                StationData stationData = reader.readValue(parser);

                                // Process the station data
                                String stationName = stationData.getBldn_nm();
                                String latStr = stationData.getLat();
                                String lonStr = stationData.getLot();

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
                        } else {
                            throw new IllegalStateException("DATA field is not an array");
                        }
                    } else {
                        // Skip any other fields
                        parser.skipChildren();
                    }
                }
            }
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

        name = name.intern(); // Some stations have the same name, so intern the string
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

    @Getter
    @Setter
    public static class StationData {
        private String bldn_id;
        private String bldn_nm;
        private String route;
        private String lat;
        private String lot;
    }
}
