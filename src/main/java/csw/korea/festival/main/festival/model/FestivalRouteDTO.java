package csw.korea.festival.main.festival.model;

import csw.korea.festival.main.common.dto.KakaoRouteSegment;
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
    private List<KakaoRouteSegment> routeSegments;
}
