package csw.korea.festival.main.festival.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.Duration;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class FestivalRouteDTO {
    private List<Festival> festivals;
    private double totalDistance;
    private Duration totalDuration;
}
