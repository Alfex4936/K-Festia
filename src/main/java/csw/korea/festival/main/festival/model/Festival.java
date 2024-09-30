package csw.korea.festival.main.festival.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Data
public class Festival {

    @JsonProperty("cntntsNm")
    private String name;       // Festival Name

    @JsonProperty("fstvlOutlCn")
    private String summary;    // Festival Summary

    @JsonProperty("fstvlBgngDe")
    private String startDate;  // Start Date

    @JsonProperty("fstvlEndDe")
    private String endDate;    // End Date

    @JsonProperty("adres")
    private String address;    // Address

    @JsonProperty("fstvlUtztFareInfo")
    private String usageFeeInfo; // Festival Usage Fee Information

    @JsonProperty("areaNm")
    private String areaName;   // Area Name

    @JsonProperty("distance")
    private Double distance;   // Distance from the user's location (ex, 2.970000000000000 in km)

    @JsonProperty("xcrdVal")
    private Double latitude;

    @JsonProperty("ycrdVal")
    private Double longitude;

    // Fields for English translations
    private String nameEn;     // Festival Name (English)
    private String summaryEn;  // Festival Summary (English)
    private String naverUrl;   // Naver Map URL

    private List<FestivalCategory> categories;

    // Transient field for parsed endDate
    @JsonIgnore
    private LocalDate parsedEndDate;

    /**
     * Parses the endDate string to LocalDate.
     * the endDate must be "yyyy.MM.dd" format.
     */
    public LocalDate getParsedEndDate() {
        if (parsedEndDate == null && endDate != null && !endDate.isEmpty()) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
                this.parsedEndDate = LocalDate.parse(endDate, formatter);
            } catch (DateTimeParseException e) {
                // set parsedEndDate to a default value
                this.parsedEndDate = LocalDate.MIN; // Represents an expired festival
            }
        }
        return parsedEndDate;
    }
}
