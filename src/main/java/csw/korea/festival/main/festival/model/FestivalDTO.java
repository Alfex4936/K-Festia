package csw.korea.festival.main.festival.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FestivalDTO {
    @JsonProperty("cntntsNm")
    private String name;       // Festival Name

    @JsonProperty("fstvlOutlCn")
    private String summary;    // Festival Summary

    @JsonProperty("fstvlBgngDe")
    private String startDate;  // Start Date as String

    @JsonProperty("fstvlEndDe")
    private String endDate;    // End Date as String

    @JsonProperty("adres")
    private String address;    // Address

    @JsonProperty("fstvlUtztFareInfo")
    private String usageFeeInfo; // Festival Usage Fee Information

    @JsonProperty("areaNm")
    private String areaName;   // Area Name

    @JsonProperty("xcrdVal")
    private Double latitude;

    @JsonProperty("ycrdVal")
    private Double longitude;

    @JsonProperty("distance")
    private Double distance;   // Distance from the user's location

    @JsonProperty("fstvlCntntsId")
    private String festivalId; // Festival ID
}
