package csw.korea.festival.main.festival.service;

import csw.korea.festival.main.festival.model.Festival;
import csw.korea.festival.main.festival.model.FestivalCategory;
import csw.korea.festival.main.festival.model.FestivalPage;
import csw.korea.festival.main.festival.model.FestivalResponse;
import csw.korea.festival.main.translation.CategorizationService;
import csw.korea.festival.main.translation.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class FestivalService {

    // Predefined default latitude and longitude (e.g., Seoul Yongsan-gu coordinates)
    private static final float DEFAULT_LATITUDE = 37.53000974602071f;
    private static final float DEFAULT_LONGITUDE = 126.98068787026715f;
    // Precompile the regex pattern for performance
    private static final Pattern HTML_TAG_PATTERN = Pattern.compile("<[^>]+>");
    private final WebClient webClient;

    private final TranslationService translationService;
    private final CategorizationService categorizationService;

    /**
     * Fetches and translates festival data based on the provided month and location with pagination.
     *
     * @param month     Optional month in "MM" format. Defaults to current month if not provided.
     * @param latitude  Optional latitude for location-based filtering. Defaults to predefined value if not provided.
     * @param longitude Optional longitude for location-based filtering. Defaults to predefined value if not provided.
     * @param page      Optional page number (0-indexed). Defaults to 0 if not provided.
     * @param size      Optional page size. Defaults to 10 if not provided.
     * @return Paginated list of translated and sorted festivals.
     */
    public FestivalPage getFestivals(String month, Float latitude, Float longitude, Integer page, Integer size) {
        // Default to current month if month is not provided
        if (month == null || month.isEmpty()) {
            month = LocalDate.now().format(DateTimeFormatter.ofPattern("MM"));
        } else {
            // Validate month format
            if (!month.matches("^(0[1-9]|1[0-2])$")) {
                throw new IllegalArgumentException("Invalid month format. Use MM format (e.g., '01' for January).");
            }
        }

        // Default to predefined latitude and longitude if not provided
        if (latitude == null) {
            latitude = DEFAULT_LATITUDE;
        } else {
            // Validate latitude range
            if (latitude < -90.0f || latitude > 90.0f) {
                throw new IllegalArgumentException("Invalid latitude. Must be between -90 and 90.");
            }
        }

        if (longitude == null) {
            longitude = DEFAULT_LONGITUDE;
        } else {
            // Validate longitude range
            if (longitude < -180.0f || longitude > 180.0f) {
                throw new IllegalArgumentException("Invalid longitude. Must be between -180 and 180.");
            }
        }

        List<Festival> festivals = fetchFestivalsInKorean(month, latitude, longitude);

        // Filter out expired festivals (endDate < today)
        // Sort festivals by distance in ascending order
        LocalDate today = LocalDate.now();
        List<Festival> activeFestivals = festivals.stream()
                .filter(festival -> {
                    LocalDate festivalEndDate = festival.getParsedEndDate();
                    // Ensure that festivalEndDate is not null
                    return festivalEndDate != null && !festivalEndDate.isBefore(today);
                }).sorted(Comparator.comparing(Festival::getDistance)).toList();

        // Translate festival names and summaries
        List<Festival> translatedFestivals = activeFestivals.stream()
                .map(this::translateAndCategorizeFestival)
                .sorted(Comparator.comparing(Festival::getDistance))
                .toList();

        // Pagination logic
        int totalElements = translatedFestivals.size();
        int pageNumber = (page != null && page >= 0) ? page : 0;
        int pageSize = (size != null && size > 0) ? size : 10;
        int totalPages = (int) Math.ceil((double) totalElements / pageSize);

        int fromIndex = pageNumber * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, totalElements);

        List<Festival> pagedFestivals = (fromIndex < toIndex) ? translatedFestivals.subList(fromIndex, toIndex) : List.of();

        FestivalPage festivalPage = new FestivalPage();
        festivalPage.setContent(pagedFestivals);
        festivalPage.setPageNumber(pageNumber);
        festivalPage.setPageSize(pageSize);
        festivalPage.setTotalElements(totalElements);
        festivalPage.setTotalPages(totalPages);

        return festivalPage;
    }

    /**
     * Fetches festival data in Korean from the external API based on month and location.
     *
     * @param month     Month in "MM" format.
     * @param latitude  Latitude for location-based filtering.
     * @param longitude Longitude for location-based filtering.
     * @return List of festivals.
     */
    private List<Festival> fetchFestivalsInKorean(String month, float latitude, float longitude) {
        // Construct the payload with month, latitude, and longitude
        String payload = "startIdx=0&searchType=A&searchDate=" + month +
                "&searchArea=&searchCate=&locationx=" + latitude +
                "&locationy=" + longitude;

        String primaryUri = "https://korean.visitkorea.or.kr/kfes/list/selectWntyFstvlList.do";
        String secondaryUri = "https://kfes.ktovisitkorea.com/list/selectWntyFstvlList.do";

        return webClient.post()
                .uri(primaryUri)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(FestivalResponse.class)
                // Fallback to secondary URI upon error
                .onErrorResume(e -> webClient.post()
                        .uri(secondaryUri)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .bodyValue(payload)
                        .retrieve()
                        .bodyToMono(FestivalResponse.class))
                // both URIs fail
//                .doOnError(e -> logger.error("Both URIs failed: {}", e.toString()))
                .blockOptional()
                .map(FestivalResponse::getResultList)
                .orElse(List.of()); // Return empty list if both fail
    }

    /**
     * Translates and categorizes a festival.
     *
     * @param festival The festival to process.
     * @return The processed festival with translated fields and assigned categories.
     */
    private Festival translateAndCategorizeFestival(Festival festival) {
        // Clean the summary by removing HTML tags and unwanted characters
        String cleanSummary = HTML_TAG_PATTERN.matcher(festival.getSummary()).replaceAll("").trim()
                .replace("\r", "").replace("\n", "");

        // Translate name and summary from Korean to English
        String translatedName = translationService.translateText(festival.getName());
        String translatedSummary = translationService.translateText(cleanSummary);

        festival.setNameEn(translatedName);
        festival.setSummary(cleanSummary);
        festival.setSummaryEn(translatedSummary);

        // Assign categories using OpenAI
        List<FestivalCategory> categories = categorizationService.categorize(translatedSummary);
        festival.setCategories(categories);

        // Set Naver URL with proper encoding
        String encodedAddress = URLEncoder.encode(festival.getAddress(), StandardCharsets.UTF_8);
        String naverUrl = "https://map.naver.com/?query=" + encodedAddress + "&type=SITE_1&queryRank=0";
        festival.setNaverUrl(naverUrl);

        return festival;
    }
}
