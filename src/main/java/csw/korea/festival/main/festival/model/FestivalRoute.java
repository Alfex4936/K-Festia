package csw.korea.festival.main.festival.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class FestivalRoute {
    @JsonProperty("festivals")
    private List<Festival> festivals;

    @JsonProperty("totalDistance")
    private double totalDistance;

    @JsonProperty("totalDuration")
    private String totalDuration;

    public FestivalRoute() {

    }
}
