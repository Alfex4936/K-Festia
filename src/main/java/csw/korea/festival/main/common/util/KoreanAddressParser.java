package csw.korea.festival.main.common.util;

import csw.korea.festival.main.common.dto.AddressComponents;

public class KoreanAddressParser {

    public AddressComponents parseAddress(String address) {
        AddressComponents components = new AddressComponents();

        // Split the address by spaces
        String[] parts = address.split("\\s+");

        // Province City District [Town] [Street and Number] [(Additional Info)]
        int index = 0;

        if (index < parts.length) {
            components.setProvince(parts[index++]);
        }
        if (index < parts.length) {
            components.setCity(parts[index++]);
        }
        if (index < parts.length && parts[index].endsWith("구") || parts[index].endsWith("군")) {
            components.setDistrict(parts[index++]);
        }
        if (index < parts.length && parts[index].endsWith("읍") || parts[index].endsWith("면") || parts[index].endsWith("동")) {
            components.setTown(parts[index++]);
        }
        if (index < parts.length) {
            StringBuilder streetBuilder = new StringBuilder();
            while (index < parts.length) {
                streetBuilder.append(parts[index++]).append(" ");
            }
            components.setStreet(streetBuilder.toString().trim());
        }

        return components;
    }
}
