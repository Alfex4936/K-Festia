package csw.korea.festival.main.festival.resolver;


import csw.korea.festival.main.common.annotation.RateLimited;
import csw.korea.festival.main.common.util.Korean;
import csw.korea.festival.main.festival.model.FestivalPage;
import csw.korea.festival.main.festival.service.FestivalSearchService;
import csw.korea.festival.main.festival.service.FestivalService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class FestivalResolver {

    private final FestivalService festivalService;
    private final FestivalSearchService festivalSearchService;

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
    @RateLimited(key = "festivals", capacity = 100, refillTokens = 100, refillDurationMillis = 60000)
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

    /**
     * Searches for festivals based on a query string.
     *
     * @param query The search query.
     * @param page  Optional page number.
     * @param size  Optional page size.
     * @return A paginated list of festivals matching the query.
     */
    @RateLimited(key = "searchFestivals", capacity = 50, refillTokens = 50, refillDurationMillis = 60000)
    @QueryMapping
    public FestivalPage searchFestivals(
            @Argument String query,
            @Argument Integer page,
            @Argument Integer size
    ) {
        int pageNumber = page != null ? page : 0;
        int pageSize = size != null ? size : 10;

        // When the query is empty, return all festivals
        if (query == null || query.isBlank()) {
            return festivalService.getFestivals(null, null, null, pageNumber, pageSize);
        }

        // When the query is QWERTY Korean (e.g. "rudrl" -> "경기")
        if (Korean.isQwerty(query)) {
            query = Korean.toHangul(query);
        }
        return festivalSearchService.searchFestivals(query, pageNumber, pageSize);
    }
}