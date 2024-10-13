package csw.korea.festival.main.festival.service;

import csw.korea.festival.main.common.util.Korean;
import csw.korea.festival.main.festival.model.Festival;
import csw.korea.festival.main.festival.model.FestivalPage;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FestivalSearchService {

    private final EntityManager entityManager;

    private final FestivalWeatherService festivalWeatherService;

    /**
     * 다중 단어 쿼리를 처리하여 관련 페스티벌을 검색합니다.
     *
     * @param query 검색 쿼리 문자열 (예: "수원 세화")
     * @param page  현재 페이지 번호
     * @param size  페이지당 결과 개수
     * @return 페스티벌 페이지 결과
     */
    // TODO: Support multiple search terms
    public FestivalPage searchFestivals(String query, int page, int size) {
        SearchSession searchSession = Search.session(entityManager);

        // 1. 쿼리를 개별 단어로 분할
        // String[] terms = query.split("\\s+");

        String qwertyKorean = Korean.toHangul(query); // can be just normal English words but also trying to convert to Korean

        // 2. 부울 쿼리 구성
        SearchResult<Festival> result = searchSession.search(Festival.class)
                .where(f -> f.bool()
                        .should(f.match()
                                .fields("name", "nameEn")
                                .matching(query)
                                .boost(4.0f))
                        .should(f.match()
                                .fields("name", "nameEn")
                                .matching(qwertyKorean)
                                .boost(3.0f))

                        .should(f.match()
                                .fields("summary", "summaryEn")
                                .matching(query)
                                .boost(2.0f))
                        .should(f.match()
                                .fields("summary", "summaryEn")
                                .matching(qwertyKorean)
                                .boost(1.0f))

                        .should(f.match()
                                .field("categoryDisplayNames")
                                .matching(query)
                                .boost(1.0f))

                        .should(f.wildcard()
                                .field("address")
                                .matching(STR."*\{query}*")
                                .boost(5.0f))
                        .should(f.wildcard()
                                .field("address")
                                .matching(STR."*\{qwertyKorean}*")
                                .boost(2.0f))

                        .should(f.phrase()
                                .fields("province", "city", "district", "town", "street")
                                .matching(query)
                                .boost(6.0f))
                        .should(f.phrase()
                                .fields("province", "city", "district", "town", "street")
                                .matching(qwertyKorean)
                                .boost(3.0f))
                        .minimumShouldMatchNumber(1) // at least one should clause must match for a document to be included.
                )
                .fetch(page * size, size);

        int totalHits = (int) result.total().hitCount();
        int start = Math.min(page * size, totalHits);
        int end = Math.min(start + size, totalHits);
        List<Festival> paginatedFestivals = result.hits().subList(start, end);

        // 페이지네이션된 페스티벌의 날씨 정보 가져오기
        paginatedFestivals = festivalWeatherService.processFestivalsWeather(paginatedFestivals);

        // 페스티벌 페이지 객체 생성
        FestivalPage festivalPage = new FestivalPage();
        festivalPage.setContent(paginatedFestivals);
        festivalPage.setPageNumber(page);
        festivalPage.setPageSize(size);
        festivalPage.setTotalElements(totalHits);
        festivalPage.setTotalPages((totalHits + size - 1) / size); // 올림 계산

        return festivalPage;
    }

    /**
     * 도시 자동 완성 기능을 제공합니다.
     *
     * @param prefix 입력된 접두사 문자열
     * @return 자동 완성된 도시 이름 목록
     */
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
