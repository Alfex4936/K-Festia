package csw.korea.festival.main.festival.service;

import csw.korea.festival.main.festival.model.Festival;
import csw.korea.festival.main.festival.model.FestivalPage;
import jakarta.persistence.EntityManager;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FestivalSearchService {

    private final EntityManager entityManager;

    public FestivalSearchService(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    public FestivalPage searchFestivals(String query, int page, int size) {
        SearchSession searchSession = Search.session(entityManager);

        SearchResult<Festival> result = searchSession.search(Festival.class)
                .where(
                        f -> f.match()
                                .fields("name", "nameEn", "summary",
                                        "summaryEn", "address", "categoryDisplayNames")
                                .matching(query))
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
}
