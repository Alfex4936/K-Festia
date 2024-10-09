package csw.korea.festival.main.festival.service;

import csw.korea.festival.main.common.dto.KWeather;
import csw.korea.festival.main.common.service.ExternalAPIService;
import csw.korea.festival.main.config.LimitedThreadFactory;
import csw.korea.festival.main.festival.model.*;
import csw.korea.festival.main.festival.repository.FestivalRepository;
import csw.korea.festival.main.translation.CategorizationService;
import csw.korea.festival.main.translation.TranslationService;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.jsoup.safety.Cleaner;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static csw.korea.festival.main.common.util.CoordinatesConverter.calculateDistance;

@Slf4j
@Service
@RequiredArgsConstructor
public class FestivalService {

    // Predefined default latitude and longitude (e.g., Seoul Yongsan-gu coordinates)
    private static final float DEFAULT_LATITUDE = 37.53000974602071f;
    private static final float DEFAULT_LONGITUDE = 126.98068787026715f;
    // Precompile the regex pattern for performance
    private static final Pattern VALID_MONTH_PATTERN = Pattern.compile("^(0[1-9]|1[0-2])$");

    private final WebClient webClient;
    private final FestivalRepository festivalRepository;

    private final TranslationService translationService;
    private final CategorizationService categorizationService;
    private final ExternalAPIService externalAPIService;
    private final EntityManager entityManager;

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
            if (!VALID_MONTH_PATTERN.matcher(month).matches()) {
                throw new IllegalArgumentException("Invalid month format. Use MM format (e.g., '01' for January).");
            }
        }

        // Validate and set default values for latitude and longitude
        latitude = (latitude != null) ? latitude : DEFAULT_LATITUDE;
        if (latitude < 32.0f || latitude > 39.0f) {
            throw new IllegalArgumentException("Invalid latitude. Must be between 33 and 38.");
        }

        longitude = (longitude != null) ? longitude : DEFAULT_LONGITUDE;
        if (longitude < 123.0f || longitude > 133.0f) {
            throw new IllegalArgumentException("Invalid longitude. Must be between 124 and 132.");
        }

        // Define the freshness threshold (e.g., data updated within the last 7 days)
        LocalDateTime freshnessThreshold = LocalDateTime.now().minusDays(7);

        // Compute start and end dates of the month
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        LocalDate today = LocalDate.now();
        int currentYear = today.getYear();
        int monthInt = Integer.parseInt(month);
        LocalDate startOfMonthDate = LocalDate.of(currentYear, monthInt, 1);
        LocalDate endOfMonthDate = startOfMonthDate.with(TemporalAdjusters.lastDayOfMonth());
        String startOfMonth = startOfMonthDate.format(dateFormatter);
        String endOfMonth = endOfMonthDate.format(dateFormatter);

        // Fetch festivals overlapping the month
        List<Festival> festivals = festivalRepository.findFestivalsOverlappingMonth(startOfMonth, endOfMonth, freshnessThreshold);

        if (festivals.isEmpty()) {
            // Fetch from external API
            // Filter out expired festivals
            List<FestivalDTO> festivalDTOs = filterExpiredFestivals(fetchFestivalsInKorean()); // FETCH ALL FESTIVALS

            // Process festivals (translation and categorization)
            List<Festival> processedFestivals = processFestivals(festivalDTOs, freshnessThreshold);

            if (!processedFestivals.isEmpty()) {
                // Update the lastUpdated timestamp
                processedFestivals.forEach(festival -> festival.setLastUpdated(LocalDateTime.now()));

                // Save processed festivals to the database
                festivalRepository.saveAll(processedFestivals);
                log.info("Saved {} new festivals to the database.", processedFestivals.size());
            }

            // Fetch all festivals again after inserting new ones
            festivals = festivalRepository.findFestivalsOverlappingMonth(startOfMonth, endOfMonth, freshnessThreshold);
        }

        // Filter out expired festivals from the entity list
        festivals = festivals.stream()
                .filter(festival -> festival.getEndDate() != null && !festival.getEndDate().isBefore(today))
                .collect(Collectors.toList());

        // Calculate distances and sort
        Float finalLatitude = latitude;
        Float finalLongitude = longitude;
        festivals.forEach(festival -> {
            double calculatedDistance = calculateDistance(finalLatitude, finalLongitude, festival.getLatitude(), festival.getLongitude());
            festival.setDistance(calculatedDistance);
        });

        festivals = processFestivalsWeather(festivals);

        festivals.sort(Comparator.comparing(Festival::getDistance));

        // Apply pagination
        int start = Math.min(page * size, festivals.size());
        int end = Math.min(start + size, festivals.size());
        List<Festival> paginatedFestivals = festivals.subList(start, end);

        // Create and return FestivalPage
        FestivalPage festivalPage = new FestivalPage();
        festivalPage.setContent(paginatedFestivals);
        festivalPage.setPageNumber(page);
        festivalPage.setPageSize(size);
        festivalPage.setTotalElements(festivals.size());
        festivalPage.setTotalPages((int) Math.ceil((double) festivals.size() / size));

        return festivalPage;
    }

    public FestivalPage searchFestivals(String query, int page, int size) {
        SearchSession searchSession = Search.session(entityManager);

        SearchResult<Festival> result = searchSession.search(Festival.class)
                .where(f -> f.match()
                        .fields("name", "nameEn", "summary", "summaryEn", "address", "categoryDisplayNames")
                        .matching(query)
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

    /**
     * Fetches festival data in Korean from the external API based on month and location.
     *
     * @return List of festivals.
     */
    private List<FestivalDTO> fetchFestivalsInKorean() {
        List<FestivalDTO> allFestivals = new ArrayList<>();
        int startIdx = 0;
        int pageSize = 12;
        int totalCnt = Integer.MAX_VALUE; // Initialize to a large number

        String primaryUri = "https://korean.visitkorea.or.kr/kfes/list/selectWntyFstvlList.do";
        String secondaryUri = "https://kfes.ktovisitkorea.com/list/selectWntyFstvlList.do";

        while (startIdx < totalCnt) {
            // &searchDate=\{month}&locationx=\{latitude}&locationy=\{longitude}
            String payload = STR."startIdx=\{startIdx}&searchType=A&searchArea=&searchCate=";

            FestivalResponse response = webClient.post()
                    .uri(primaryUri)
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(FestivalResponse.class)
                    // Fallback to secondary URI upon error
                    .onErrorResume(ex -> {
                        log.error("Primary URI failed: {}, attempting secondary URI.", ex.getMessage());
                        return webClient.post()
                                .uri(secondaryUri)
                                .header("Content-Type", "application/x-www-form-urlencoded")
                                .bodyValue(payload)
                                .retrieve()
                                .bodyToMono(FestivalResponse.class);
                    })
                    .block();

            if (response == null || response.getResultList() == null || response.getResultList().isEmpty()) {
                log.warn("No festivals fetched for startIdx: {}", startIdx);
                break; // Exit the loop if no data is returned
            }

            if (totalCnt == Integer.MAX_VALUE) {
                totalCnt = response.getTotalCnt(); // Set the total count from the first response
                pageSize = response.getResultList().size(); // Determine the page size
            }

            allFestivals.addAll(response.getResultList());
            startIdx += pageSize; // Increment startIdx by the page size
        }

        return allFestivals;
    }

    /**
     * Translates and categorizes a festival.
     *
     * @param festival The festival to process.
     * @return The processed festival with translated fields and assigned categories.
     */
    private Festival translateAndCategorizeFestival(Festival festival) throws Exception {
        if (Thread.currentThread().isInterrupted()) {
            // Handle interruption, e.g., return early
            return null; // FIXME: or throw an exception
        }

        // Clean the summary by removing HTML tags and unwanted characters
        String sourceBodyHtml = Jsoup.clean(festival.getSummary(), Safelist.none()); // preserves text within angle brackets if they are not valid HTML tags.
        Cleaner cleaner = new Cleaner(Safelist. none());
        String cleanSummary = cleaner.clean(Jsoup. parse(sourceBodyHtml)).text();

        // Remove unwanted whitespace characters
        cleanSummary = cleanSummary.replace("\r", "").replace("\n", "")
                .replace("&lt;","<").replace("&gt;", ">").trim();

        // Translate name and summary from Korean to English
        String translatedName = translationService.translateText(festival.getName());
        String translatedSummary = translationService.translateText(cleanSummary);

        festival.setNameEn(translatedName);
        festival.setSummary(cleanSummary);
        festival.setSummaryEn(translatedSummary);

        // Assign categories using OpenAI
        Set<FestivalCategory> categories = new HashSet<>(categorizationService.categorize(translatedSummary));
        festival.setCategories(categories);

        // Set Naver URL with proper encoding
        String encodedAddress = URLEncoder.encode(festival.getAddress(), StandardCharsets.UTF_8);
        String naverUrl = STR."https://map.naver.com/?query=\{encodedAddress}&type=SITE_1&queryRank=0";
        festival.setNaverUrl(naverUrl);

        if (festival.getUsageFeeInfo() != null) {
            festival.setUsageFeeInfo(festival.getUsageFeeInfo().trim());
        }

        // Current weather per location
//        try {
//            var weatherRequest = externalAPIService.getWeatherFromCoordinates(festival.getLatitude(), festival.getLongitude());
//            festival.setWeather(weatherRequest);
//        } catch (Exception e) {
//            festival.setWeather(null); // Set weather to null for now
//        }

        if (Thread.currentThread().isInterrupted()) {
            return null;
        }

        return festival;
    }

    /**
     * Fetch weather data.
     *
     * @param festivalsToProcess The Festivals to process.
     * @return The processed Festival.
     */
    private List<Festival> processFestivalsWeather(List<Festival> festivalsToProcess) {
        int maxConcurrency = 10;
        ThreadFactory baseFactory = Thread.ofVirtual().factory();
        ThreadFactory limitedFactory = new LimitedThreadFactory(baseFactory, maxConcurrency);

        List<Festival> processedFestivals = new ArrayList<>();
        try (var scope = new StructuredTaskScope.ShutdownOnFailure(null, limitedFactory)) {
            List<StructuredTaskScope.Subtask<Festival>> futures = new ArrayList<>();
            for (Festival festival : festivalsToProcess) {
                StructuredTaskScope.Subtask<Festival> future = scope.fork(() ->
                        {
                            // Fetch Weather Data Concurrently
                            try {
                                KWeather.WeatherRequest weather = externalAPIService.getWeatherFromCoordinates(
                                        festival.getLatitude(), festival.getLongitude());
                                festival.setWeather(weather);
                                log.info("Fetched weather for festival '{}' ({})", festival.getName(), weather);
                            } catch (Exception e) {
                                log.error("Error fetching weather for festival '{}': {}", festival.getName(), e.getMessage());
                                festival.setWeather(null);
                            }
                            return festival;
                        }
                );
                futures.add(future);
            }

            scope.join();
            scope.throwIfFailed();

            for (StructuredTaskScope.Subtask<Festival> future : futures) {
                Festival result = future.get();
                if (result != null) {
                    processedFestivals.add(result);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error fetching festival weather", e);
        }
        return processedFestivals;
    }

    @SuppressWarnings(value = "UnstableApiUsage")
    private List<Festival> processFestivals(List<FestivalDTO> festivalDTOs, LocalDateTime freshnessThreshold) {
        if (festivalDTOs.isEmpty()) {
            return Collections.emptyList();
        }

        // Map DTOs to entities
        Map<String, Festival> festivalMap = festivalDTOs.stream()
                .map(this::mapDtoToEntity)
                .collect(Collectors.toMap(Festival::getFestivalId, Function.identity()));

        // Extract unique identifiers
        Set<String> festivalIds = festivalMap.keySet();

        // Fetch existing festivals from the database
        List<Festival> existingFestivals = festivalRepository.findByFestivalIdIn(festivalIds);

        // Create a map for easy lookup
        Map<String, Festival> existingFestivalMap = existingFestivals.stream()
                .collect(Collectors.toMap(Festival::getFestivalId, Function.identity()));

        // Lists to hold festivals to process
        List<Festival> festivalsToProcess = new ArrayList<>();

        for (Map.Entry<String, Festival> entry : festivalMap.entrySet()) {
            String festivalId = entry.getKey();
            Festival festival = entry.getValue();

            if (!existingFestivalMap.containsKey(festivalId)) {
                // New festival
                festivalsToProcess.add(festival);
            } else {
                // Festival exists in database
                Festival existingFestival = existingFestivalMap.get(festivalId);
                if (existingFestival.getLastUpdated().isBefore(freshnessThreshold)) {
                    // Existing festival is outdated
                    festival.setId(existingFestival.getId()); // Ensure we're updating the same entity
                    festivalsToProcess.add(festival);
                }
                // Else, up-to-date festival, do not process
            }
        }

        log.info("Identified {} festivals to process (new or outdated).", festivalsToProcess.size());

        if (festivalsToProcess.isEmpty()) {
            return Collections.emptyList();
        }

        // Proceed to translate and categorize festivals
        int maxConcurrency = 10;
        ThreadFactory baseFactory = Thread.ofVirtual().factory();
        ThreadFactory limitedFactory = new LimitedThreadFactory(baseFactory, maxConcurrency);

        List<Festival> processedFestivals = new ArrayList<>();
        try (var scope = new StructuredTaskScope.ShutdownOnFailure(null, limitedFactory)) {
            List<StructuredTaskScope.Subtask<Festival>> futures = new ArrayList<>();
            for (Festival festival : festivalsToProcess) {
                StructuredTaskScope.Subtask<Festival> future = scope.fork(() -> translateAndCategorizeFestival(festival));
                futures.add(future);
            }

            scope.join();
            scope.throwIfFailed();

            for (StructuredTaskScope.Subtask<Festival> future : futures) {
                Festival result = future.get();
                if (result != null) {
                    processedFestivals.add(result);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error during festival processing", e);
        }

        return processedFestivals;
    }


    private FestivalPage convertToFestivalPage(Page<Festival> festivalPage) {
        FestivalPage page = new FestivalPage();
        page.setContent(festivalPage.getContent());
        page.setPageNumber(festivalPage.getNumber());
        page.setPageSize(festivalPage.getSize());
        page.setTotalElements(festivalPage.getTotalElements());
        page.setTotalPages(festivalPage.getTotalPages());
        return page;
    }

    private Festival mapDtoToEntity(FestivalDTO dto) {
        Festival festival = new Festival();
        festival.setFestivalId(dto.getFestivalId().strip());
        festival.setName(dto.getName().strip());
        festival.setSummary(dto.getSummary());
        festival.setAddress(dto.getAddress());
        festival.setUsageFeeInfo(dto.getUsageFeeInfo());
        festival.setAreaName(dto.getAreaName());
        festival.setLatitude(dto.getLatitude());
        festival.setLongitude(dto.getLongitude());
        festival.setLastUpdated(LocalDateTime.now());

        // Parse and set startDate and endDate
        // koreafestival.com returns date as "yyyy.MM.dd" but I will save as "yyyy-MM-dd"
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");
        if (dto.getStartDate() != null && !dto.getStartDate().isEmpty()) {
            festival.setStartDate(LocalDate.parse(dto.getStartDate(), formatter));
        }
        if (dto.getEndDate() != null && !dto.getEndDate().isEmpty()) {
            festival.setEndDate(LocalDate.parse(dto.getEndDate(), formatter));
        }

        festival.setUsageFeeCategory(CategorizationService.categorizeUsageFee(dto.getUsageFeeInfo()));

        return festival;
    }

    /**
     * Filters out expired FestivalDTOs based on their endDate.
     *
     * @param festivalDTOs The list of FestivalDTOs to filter.
     * @return A list of FestivalDTOs that are not expired.
     */
    private List<FestivalDTO> filterExpiredFestivals(List<FestivalDTO> festivalDTOs) {
        LocalDate today = LocalDate.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy.MM.dd");

        return festivalDTOs.stream()
                .filter(dto -> {
                    String endDateStr = dto.getEndDate();
                    if (endDateStr == null || endDateStr.isEmpty()) {
                        // If endDate is missing, consider it as not expired
                        return true;
                    }
                    try {
                        LocalDate endDate = LocalDate.parse(endDateStr, formatter);
                        return !endDate.isBefore(today);
                    } catch (DateTimeParseException e) {
                        log.warn("Invalid endDate format for festival '{}': {}", dto.getName(), endDateStr);
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

}
