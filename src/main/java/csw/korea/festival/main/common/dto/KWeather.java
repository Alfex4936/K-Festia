package csw.korea.festival.main.common.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

public class KWeather {
    @Data
    public static class WeatherRequest {
        private String temperature;
        private String desc;
        private String iconImage;
        private String humidity;
        private String rainfall;
        private String snowfall;
    }

    @Data
    public static class WeatherInfo {
        private String type;
        private String rcode;

        @JsonProperty("iconId")
        private String iconId;

        private String temperature;
        private String desc;
        private String humidity;
        private String rainfall;
        private String snowfall;
    }

    @Data
    public static class WeatherInfos {
        private WeatherInfo current;
        private WeatherInfo forecast;
    }

    @Data
    public static class Code {
        @JsonProperty("childcount")
        private double childCount;

        private double x;
        private double y;
        private String type;
        private String code;
        private String name;
        private String fullName;
        private String regionId;

        @JsonProperty("name0")
        private String name0;

        @JsonProperty("code1")
        private String code1;

        @JsonProperty("name1")
        private String name1;

        @JsonProperty("code2")
        private String code2;

        @JsonProperty("name2")
        private String name2;

        @JsonProperty("code3")
        private String code3;

        @JsonProperty("name3")
        private String name3;
    }

    @Data
    public static class Codes {
        private Code hcode;
        private Code bcode;
        private String resultCode;
    }

    @Data
    public static class WeatherResponse {
        private Codes codes;

        @JsonProperty("weatherInfos")
        private WeatherInfos weatherInfos;
    }
}
