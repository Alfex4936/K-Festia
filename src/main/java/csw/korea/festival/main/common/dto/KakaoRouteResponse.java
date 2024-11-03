package csw.korea.festival.main.common.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class KakaoRouteResponse {

    private String trans_id;
    private List<Route> routes;

    @Getter
    @Setter
    public static class Route {
        private int result_code;
        private String result_msg;
        private Summary summary;
        // more

        @Getter
        @Setter
        public static class Summary {
            private Origin origin;
            private Destination destination;
            private int distance;
            private int duration;
            private Fare fare;

            @Getter
            @Setter
            public static class Origin {
                private String name;
                private double x;
                private double y;
            }

            @Getter
            @Setter
            public static class Destination {
                private String name;
                private double x;
                private double y;
            }

            @Getter
            @Setter
            public static class Fare {
                private int taxi;
                private int toll;
            }
        }
    }
}
