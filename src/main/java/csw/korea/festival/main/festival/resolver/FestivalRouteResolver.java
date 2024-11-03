package csw.korea.festival.main.festival.resolver;

import csw.korea.festival.main.common.util.DurationFormatter;
import csw.korea.festival.main.festival.model.FestivalCategory;
import csw.korea.festival.main.festival.model.FestivalRoute;
import csw.korea.festival.main.festival.model.FestivalRouteDTO;
import csw.korea.festival.main.festival.service.FestivalRouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import static csw.korea.festival.main.common.util.CoordinatesConverter.MAX_DISTANCE_KM;

@Controller
@RequiredArgsConstructor
public class FestivalRouteResolver {
    private final FestivalRouteService festivalRouteService;

    @QueryMapping
    public FestivalRoute planFestivalRoute(
            @Argument String startStation,
            @Argument String startDate,
            @Argument String endDate,
            @Argument List<FestivalCategory> preferredCategories,
            @Argument Integer maxFestivals,
            @Argument String locale) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        int max = (maxFestivals != null) ? maxFestivals : 5; // Default to 5 festivals

        FestivalRouteDTO routeDTO = festivalRouteService.planFestivalRoute(
                startStation, start, end, preferredCategories, max);

        Locale userLocale = Locale.ENGLISH; // Default locale
        if ("ko".equalsIgnoreCase(locale)) {
            userLocale = Locale.KOREAN;
        }

        return convertToGraphQLType(routeDTO, userLocale);
    }

    @QueryMapping
    public FestivalRoute planFestivalRouteByCar(
            @Argument String startStation,
            @Argument String startDate,
            @Argument String endDate,
            @Argument List<FestivalCategory> preferredCategories,
            @Argument Integer maxFestivals,
            @Argument Double maxDistance,
            @Argument String locale) {
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);
        int max = (maxFestivals != null) ? maxFestivals : 5; // Default to 5 festivals
        double maxDistanceKm = (maxDistance != null) ? maxDistance : MAX_DISTANCE_KM;

        FestivalRouteDTO routeDTO = festivalRouteService.planFestivalRouteByCar(
                startStation, start, end, preferredCategories, max, maxDistanceKm);

        Locale userLocale = Locale.ENGLISH; // Default locale
        if ("ko".equalsIgnoreCase(locale)) {
            userLocale = Locale.KOREAN;
        }

        return convertToGraphQLType(routeDTO, userLocale);
    }

    private FestivalRoute convertToGraphQLType(FestivalRouteDTO dto, Locale locale) {
        FestivalRoute route = new FestivalRoute();
        route.setFestivals(dto.getFestivals());
        route.setTotalDistance(dto.getTotalDistance());
        route.setTotalDuration(DurationFormatter.formatDuration(dto.getTotalDuration(), locale));
        return route;
    }

    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();
        return STR."\{hours} hours \{minutes} minutes";
    }
}
