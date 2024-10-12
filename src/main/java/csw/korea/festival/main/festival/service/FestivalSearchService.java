package csw.korea.festival.main.festival.service;

import csw.korea.festival.main.festival.model.Festival;
import csw.korea.festival.main.festival.model.FestivalPage;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FestivalSearchService {

    private final EntityManager entityManager;

    private final FestivalWeatherService festivalWeatherService;

    public FestivalPage searchFestivals(String query, int page, int size) {
        SearchSession searchSession = Search.session(entityManager);

        SearchResult<Festival> result = searchSession.search(Festival.class)
                .where(f -> f.bool()
                        .should(f.match()
                                .fields("name", "nameEn")
                                .matching(query)
                                .boost(4.0f))
                        .should(f.match()
                                .fields("summary", "summaryEn")
                                .matching(query)
                                .boost(2.0f))
                        .should(f.match()
                                .field("categoryDisplayNames")
                                .matching(query)
                                .boost(1.0f))
                        .should(f.wildcard()
                                .field("address")
                                .matching(STR."*\{query}*")
                                .boost(5.0f)
                        )
                        .should(f.phrase()
                                .fields("province", "city", "district", "town", "street")
                                .matching(query)
                                .boost(6.0f))
                        .minimumShouldMatchNumber(1) // at least one should clause must match for a document to be included.
                )
                .fetch(page * size, size);

        int totalHits = (int) result.total().hitCount();
        int start = Math.min(page * size, totalHits);
        int end = Math.min(start + size, totalHits);
        List<Festival> paginatedFestivals = result.hits().subList(start, end);

        // Fetch weather for paginated festivals
        paginatedFestivals = festivalWeatherService.processFestivalsWeather(paginatedFestivals);

        FestivalPage festivalPage = new FestivalPage();
        festivalPage.setContent(paginatedFestivals);
        festivalPage.setPageNumber(page);
        festivalPage.setPageSize(size);
        festivalPage.setTotalElements(totalHits);
        festivalPage.setTotalPages((totalHits + size - 1) / size); // Ceiling division

        return festivalPage;
    }

    public List<String> autocompleteCity(String prefix) {
        SearchSession searchSession = Search.session(entityManager);

        List<String> suggestions = searchSession.search(Festival.class)
                .select(f -> f.field("city", String.class))
                .where(f -> f.wildcard()
                        .field("city")
                        .matching(STR."\{prefix}*")
                )
                .fetchHits(10);

        return suggestions.stream().distinct().collect(Collectors.toList());
    }
}
