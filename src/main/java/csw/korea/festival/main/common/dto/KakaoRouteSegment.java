package csw.korea.festival.main.common.dto;

import csw.korea.festival.main.festival.model.Festival;

import java.time.Duration;

public class KakaoRouteSegment {
    private Festival fromFestival;
    private Festival toFestival;
    private double distance; // in kilometers
    private Duration duration;
}
