package csw.korea.festival.main.festival.service;

import csw.korea.festival.main.common.service.KoreaStationService;
import csw.korea.festival.main.common.util.Korean;
import csw.korea.festival.main.festival.model.Festival;
import csw.korea.festival.main.festival.model.FestivalPage;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.search.engine.search.predicate.dsl.BooleanPredicateClausesStep;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.spatial.DistanceUnit;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FestivalSearchService {

    private final EntityManager entityManager;

    private final FestivalWeatherService festivalWeatherService;
    private final KoreaStationService koreaStationService;

    /**
     * 다중 단어 쿼리를 처리하여 관련 페스티벌을 검색합니다.
     *
     * @param query 검색 쿼리 문자열 (예: "수원 세화")
     * @param page  현재 페이지 번호
     * @param size  페이지당 결과 개수
     * @return 페스티벌 페이지 결과
     */
    public FestivalPage searchFestivals(String query, int page, int size) {
        SearchSession searchSession = Search.session(entityManager);

        // 1. Split the query into individual terms
        String[] terms = query.split("\\s+");

        // 2. Convert each term using qwerty to Korean conversion
        String[] qwertyTerms = Arrays.stream(terms)
                .map(Korean::toHangul)
                .toArray(String[]::new);

        // 3. Build the boolean query dynamically
        SearchResult<Festival> result = searchSession.search(Festival.class)
                .where(f -> {
                    // Start a boolean predicate
                    BooleanPredicateClausesStep<?> boolQuery = f.bool();

                    // For each term, add 'should' clauses
                    for (String term : terms) {
                        boolQuery.should(f.match()
                                .fields("name", "nameEn")
                                .matching(term)
                                .boost(9.0f));
                        boolQuery.should(f.match()
                                .fields("summary", "summaryEn")
                                .matching(term)
                                .boost(7.0f));
                        boolQuery.should(f.match()
                                .field("categoryDisplayNames")
                                .matching(term)
                                .boost(6.0f));
                        boolQuery.should(f.wildcard()
                                .field("address")
                                .matching(STR."*\{term}*")
                                .boost(10.0f));
                        boolQuery.should(f.phrase()
                                .fields("province", "city", "district", "town", "street")
                                .matching(term)
                                // .slop(2)
                                .boost(15.0f));
                    }

                    //  Require a percentage of terms to match.
                    boolQuery.minimumShouldMatchPercent(50);

                    return boolQuery;
                })
                // .highlighter(f -> f.field("name").field("summary"))
                .fetch(page * size, size);

        int totalHits = (int) result.total().hitCount();
        int start = Math.min(page * size, totalHits);
        int end = Math.min(start + size, totalHits);
        List<Festival> paginatedFestivals = result.hits().subList(start, end);

        // Process the festivals to include weather information
        paginatedFestivals = festivalWeatherService.processFestivalsWeather(paginatedFestivals);

        // Create and return the festival page object
        FestivalPage festivalPage = new FestivalPage();
        festivalPage.setContent(paginatedFestivals);
        festivalPage.setPageNumber(page);
        festivalPage.setPageSize(size);
        festivalPage.setTotalElements(totalHits);
        festivalPage.setTotalPages((totalHits + size - 1) / size);


        return festivalPage;
    }

    /**
     * 다중 단어 쿼리를 처리하여 관련 페스티벌을 검색합니다. (지정된 역 Station 주변)
     *
     * @param query 검색 쿼리 문자열 (예: "음식")
     * @param station 역 이름 (예: "서울대입구역")
     * @param page  현재 페이지 번호
     * @param size  페이지당 결과 개수
     * @return 페스티벌 페이지 결과
     */
    public FestivalPage searchFestivals(String query, int page, int size, Double latitude, Double longitude) {
        SearchSession searchSession = Search.session(entityManager);

        // 3. Build the boolean query dynamically
        SearchResult<Festival> result = searchSession.search(Festival.class)
                .where(f -> {
                    // Start a boolean predicate
                    BooleanPredicateClausesStep<?> boolQuery = f.bool();

                    // For each term, add 'should' clauses
                    if (query != null && !query.trim().isEmpty()) {
                        String[] terms = query.split("\\s+");

                        for (String term : terms) {
                            boolQuery.should(f.match()
                                    .fields("name", "nameEn")
                                    .matching(term)
                                    .boost(9.0f));
                            boolQuery.should(f.match()
                                    .fields("summary", "summaryEn")
                                    .matching(term)
                                    .boost(7.0f));
                            boolQuery.should(f.match()
                                    .field("categoryDisplayNames")
                                    .matching(term)
                                    .boost(6.0f));
                            boolQuery.should(f.wildcard()
                                    .field("address")
                                    .matching(STR."*\{term}*")
                                    .boost(10.0f));
                            boolQuery.should(f.phrase()
                                    .fields("province", "city", "district", "town", "street")
                                    .matching(term)
                                    // .slop(2)
                                    .boost(15.0f));
                        }

                        //  Require a percentage of terms to match.
                        boolQuery.minimumShouldMatchPercent(50);
                    }

                    // Add spatial predicate
                    if (latitude != null && longitude != null) {
                        boolQuery.must(f.spatial().within()
                                .field("festivalLocation")
                                .circle(latitude, longitude, 5, DistanceUnit.KILOMETERS));
                    }

                    return boolQuery;
                })
                .sort(f -> {
                    if (latitude != null && longitude != null) {
                        return f.distance("festivalLocation", latitude, longitude);
                    } else {
                        return f.score();
                    }
                })
                .fetch(page * size, size);

        int totalHits = (int) result.total().hitCount();
        int start = Math.min(page * size, totalHits);
        int end = Math.min(start + size, totalHits);
        List<Festival> paginatedFestivals = result.hits().subList(start, end);

        // Process the festivals to include weather information
        paginatedFestivals = festivalWeatherService.processFestivalsWeather(paginatedFestivals);

        // Create and return the festival page object
        FestivalPage festivalPage = new FestivalPage();
        festivalPage.setContent(paginatedFestivals);
        festivalPage.setPageNumber(page);
        festivalPage.setPageSize(size);
        festivalPage.setTotalElements(totalHits);
        festivalPage.setTotalPages((totalHits + size - 1) / size);

        return festivalPage;
    }

    public FestivalPage searchFestivalsNearStation(String query, String stationName, int page, int size) {
        // Get the station coordinates
        Optional<KoreaStationService.Station> stationOpt = koreaStationService.getStationByName(stationName);

        if (stationOpt.isEmpty()) {
            throw new IllegalArgumentException(STR."Station not found: \{stationName}");
        }

        KoreaStationService.Station station = stationOpt.get();
        return searchFestivals(query, page, size, station.getLatitude(), station.getLongitude());
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
