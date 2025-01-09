package csw.korea.festival.main.festival.service;

import csw.korea.festival.main.common.dto.AddressComponents;
import csw.korea.festival.main.common.util.KoreanAddressParser;
import csw.korea.festival.main.config.LimitedThreadFactory;
import csw.korea.festival.main.festival.model.Festival;
import csw.korea.festival.main.festival.model.FestivalCategory;
import csw.korea.festival.main.festival.model.FestivalDTO;
import csw.korea.festival.main.festival.model.FestivalPage;
import csw.korea.festival.main.festival.repository.FestivalRepository;
import csw.korea.festival.main.translation.CategorizationService;
import csw.korea.festival.main.translation.TranslationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.safety.Cleaner;
import org.jsoup.safety.Safelist;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FestivalProcessingService {

    private final FestivalRepository festivalRepository;
    private final TranslationService translationService;
    private final CategorizationService categorizationService;
    private final KoreanAddressParser addressParser = new KoreanAddressParser();

    /**
     * Translates and categorizes a festival.
     *
     * @param festival The festival to process.
     * @return The processed festival with translated fields and assigned categories.
     */
    private Festival translateAndCategorizeFestival(Festival festival) {
        if (Thread.currentThread().isInterrupted()) {
            // Handle interruption, e.g., return early
            return null; // FIXME: or throw an exception
        }

        // Clean the summary by removing HTML tags and unwanted characters
        String sourceBodyHtml = Jsoup.clean(festival.getSummary(), Safelist.none()); // preserves text within angle brackets if they are not valid HTML tags.
        Cleaner cleaner = new Cleaner(Safelist.none());
        String cleanSummary = cleaner.clean(Jsoup.parse(sourceBodyHtml)).text();

        // Remove unwanted whitespace characters
        cleanSummary = cleanSummary.replace("\r", "")
                .replace("\n", "")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("\"", "'")
                .trim();

        // Translate name and summary from Korean to English
        String translatedName = translationService.translateText(festival.getName());
        String translatedSummary = translationService.translateText(cleanSummary);

        festival.setNameEn(translatedName);
        festival.setSummary(cleanSummary);
        festival.setSummaryEn(translatedSummary);
        festival.setAddress(festival.getAddress().trim());

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


    @SuppressWarnings(value = "UnstableApiUsage")
    public List<Festival> processFestivals(List<FestivalDTO> festivalDTOs, LocalDateTime freshnessThreshold) {
        if (festivalDTOs.isEmpty()) {
            return Collections.emptyList();
        }

        // Map DTOs to entities
        Map<String, Festival> festivalMap = festivalDTOs.stream().map(this::mapDtoToEntity).collect(Collectors.toMap(Festival::getFestivalId, Function.identity()));

        // Extract unique identifiers
        Set<String> festivalIds = festivalMap.keySet();

        // Fetch existing festivals from the database
        List<Festival> existingFestivals = festivalRepository.findByFestivalIdIn(festivalIds);

        // Create a map for easy lookup
        Map<String, Festival> existingFestivalMap = existingFestivals.stream().collect(Collectors.toMap(Festival::getFestivalId, Function.identity()));

        // Lists to hold festivals to process
        List<Festival> festivalsToProcess = new ArrayList<>();

        for (Map.Entry<String, Festival> entry : festivalMap.entrySet()) {
            String festivalId = entry.getKey();
            Festival festival = entry.getValue();

            existingFestivalMap.computeIfAbsent(festivalId, id -> {
                festivalsToProcess.add(festival);
                return festival;
            });

            Festival existingFestival = existingFestivalMap.get(festivalId);
            if (existingFestival != null && existingFestival.getLastUpdated().isBefore(freshnessThreshold)) {
                // Existing festival is outdated
                festival.setId(existingFestival.getId()); // Ensure we're updating the same entity
                festivalsToProcess.add(festival);
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
        festival.setSummary(dto.getSummary().replace("<br>", " ").strip());
        festival.setAddress(dto.getAddress().strip());
        festival.setUsageFeeInfo(dto.getUsageFeeInfo());
        festival.setAreaName(dto.getAreaName());
        festival.setLatitude(dto.getLatitude());
        festival.setLongitude(dto.getLongitude());
        festival.setLastUpdated(LocalDateTime.now());
        festival.setImageUrl(dto.getImageUrl());

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

        // Parse address components
        AddressComponents components = addressParser.parseAddress(dto.getAddress());
        festival.setProvince(components.getProvince());
        festival.setCity(components.getCity());
        festival.setDistrict(components.getDistrict());
        festival.setTown(components.getTown());
        festival.setStreet(components.getStreet());

        return festival;
    }

    public void updateFestivalAddresses() {
        List<Festival> festivals = festivalRepository.findAll();
        for (Festival festival : festivals) {
            AddressComponents components = addressParser.parseAddress(festival.getAddress());
            festival.setProvince(components.getProvince());
            festival.setCity(components.getCity());
            festival.setDistrict(components.getDistrict());
            festival.setTown(components.getTown());
            festival.setStreet(components.getStreet());
            festivalRepository.save(festival);
        }
    }
}
