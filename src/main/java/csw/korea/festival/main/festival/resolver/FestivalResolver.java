package csw.korea.festival.main.festival.resolver;

import csw.korea.festival.main.festival.model.FestivalPage;
import csw.korea.festival.main.festival.service.FestivalService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class FestivalResolver {

    private final FestivalService festivalService;

    /**
     * Fetches a paginated list of festivals based on the provided month and location.
     *
     * @param month     Optional month in "MM" format. Defaults to current month if not provided.
     * @param latitude  Optional latitude for location-based filtering. Defaults to predefined value if not provided.
     * @param longitude Optional longitude for location-based filtering. Defaults to predefined value if not provided.
     * @param page      Optional page number (0-indexed). Defaults to 0 if not provided.
     * @param size      Optional page size. Defaults to 10 if not provided.
     * @return Paginated list of festivals matching the criteria.
     */
    @QueryMapping
    public FestivalPage festivals(
            @Argument String month,
            @Argument Float latitude,
            @Argument Float longitude,
            @Argument Integer page,
            @Argument Integer size
    ) {
        return festivalService.getFestivals(month, latitude, longitude, page, size);
    }
}
