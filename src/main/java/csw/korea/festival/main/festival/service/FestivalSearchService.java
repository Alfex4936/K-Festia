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
                                .fields("address", "categoryDisplayNames")
                                .matching(query)
                                .boost(1.0f))
                        .should(f.match()
                                .fields("province", "city", "district", "town", "street")
                                .matching(query)
                                .boost(0.5f))
                )
                .fetch(page * size, size);

        List<Festival> festivals = result.hits();
        long totalHits = result.total().hitCount();
        int totalPages = (int) ((totalHits + size - 1) / size); // Ceiling division


        FestivalPage festivalPage = new FestivalPage();
        festivalPage.setContent(festivals);
        festivalPage.setPageNumber(page);
        festivalPage.setPageSize(size);
        festivalPage.setTotalElements((int) totalHits);
        festivalPage.setTotalPages(totalPages);

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
