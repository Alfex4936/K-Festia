package csw.korea.festival.main.common.util;

import csw.korea.festival.main.common.dto.AddressComponents;

import java.util.Set;

public class KoreanAddressParser {

    // Helper sets for identifying which token belongs where.
    private static final Set<String> PROVINCE_SUFFIXES = Set.of("도", "특별자치시", "광역시", "특별시");
    private static final Set<String> CITY_SUFFIXES = Set.of("시", "군"); // ex: 안성시, 영양군
    private static final Set<String> DISTRICT_SUFFIXES = Set.of("구", "군"); // ex: 동래구, 강화군
    private static final Set<String> TOWN_SUFFIXES = Set.of("읍", "면", "동");  // ex: 죽산면, 보람동

    public AddressComponents parseAddress(String address) {
        // Return an empty object if input is invalid
        if (address == null || address.trim().isEmpty()) {
            return new AddressComponents();
        }

        AddressComponents components = new AddressComponents();

        // Split and trim
        String[] parts = address.trim().split("\\s+");

        boolean provinceSet = false;
        boolean citySet = false;
        boolean districtSet = false;
        boolean townSet = false;

        StringBuilder streetBuilder = new StringBuilder();

        // Single pass over tokens
        for (String part : parts) {
            if (!provinceSet && endsWithAny(part, PROVINCE_SUFFIXES)) {
                components.setProvince(part);
                provinceSet = true;
            } else if (!citySet && endsWithAny(part, CITY_SUFFIXES)) {
                components.setCity(part);
                citySet = true;
            } else if (!districtSet && endsWithAny(part, DISTRICT_SUFFIXES)) {
                components.setDistrict(part);
                districtSet = true;
            } else if (!townSet && endsWithAny(part, TOWN_SUFFIXES)) {
                components.setTown(part);
                townSet = true;
            } else {
                // Anything else goes into street
                streetBuilder.append(part).append(" ");
            }
        }

        // Trim trailing space
        String street = streetBuilder.toString().trim();
        components.setStreet(street);

        return components;
    }

    /**
     * Utility method to check if a string ends with any of the given suffixes.
     */
    private boolean endsWithAny(String input, Set<String> suffixes) {
        for (String suffix : suffixes) {
            if (input.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }
}
