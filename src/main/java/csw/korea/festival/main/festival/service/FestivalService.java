package csw.korea.festival.main.festival.service;

import csw.korea.festival.main.festival.model.Festival;
import csw.korea.festival.main.festival.model.FestivalDTO;
import csw.korea.festival.main.festival.model.FestivalPage;
import csw.korea.festival.main.festival.repository.FestivalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Comparator;
import java.util.List;
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

    private final FestivalRepository festivalRepository;
    private final FestivalWeatherService festivalWeatherService;
    private final FestivalProcessingService festivalProcessingService;
    private final FestivalFetchingService festivalFetchingService;

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

        // Define the freshness threshold (e.g., data updated within the last 14 days)
        LocalDateTime freshnessThreshold = LocalDateTime.now().minusWeeks(3);

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
            List<FestivalDTO> festivalDTOs = festivalFetchingService.filterExpiredFestivals(festivalFetchingService.fetchFestivalsInKorean()); // FETCH ALL FESTIVALS

            // Process festivals (translation and categorization)
            List<Festival> processedFestivals = festivalProcessingService.processFestivals(festivalDTOs, freshnessThreshold);

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

        // Option1: Set isFinished for each festival
//        LocalDate today = LocalDate.now();
//        for (Festival festival : festivals) {
//            if (festival.getEndDate() != null) {
//                festival.setIsFinished(festival.getEndDate().isBefore(today));
//            } else {
//                festival.setIsFinished(false);
//            }
//        }
//
        // Filter out expired festivals from the entity list
        festivals = festivals.stream().filter(festival -> festival.getEndDate() != null && !festival.getEndDate().isBefore(today)).collect(Collectors.toList());

        // Calculate distances and sort
        Float finalLatitude = latitude;
        Float finalLongitude = longitude;
        festivals.forEach(festival -> {
            double calculatedDistance = calculateDistance(finalLatitude, finalLongitude, festival.getLatitude(), festival.getLongitude());
            festival.setDistance(calculatedDistance);
        });

        festivals.sort(Comparator.comparing(Festival::getDistance));

        // Apply pagination
        int start = Math.min(page * size, festivals.size());
        int end = Math.min(start + size, festivals.size());
        List<Festival> paginatedFestivals = festivals.subList(start, end);

        // Fetch weather for paginated festivals
        festivals = festivalWeatherService.processFestivalsWeather(paginatedFestivals);

        // Create and return FestivalPage
        FestivalPage festivalPage = new FestivalPage();
        festivalPage.setContent(paginatedFestivals);
        festivalPage.setPageNumber(page);
        festivalPage.setPageSize(size);
        festivalPage.setTotalElements(festivals.size());
        festivalPage.setTotalPages((int) Math.ceil((double) festivals.size() / size));

        return festivalPage;
    }

    public List<Festival> getFestivalsByProvince(String province) {
        return festivalRepository.findByProvince(province);
    }

    public List<Festival> getFestivalsByCity(String city) {
        return festivalRepository.findByCity(city);
    }

    public List<Festival> getFestivalsByProvinceAndCity(String province, String city) {
        return festivalRepository.findByProvinceAndCity(province, city);
    }

    /**
     * Updates the festivals data during application startup.
     * @return true if new festivals were added, false otherwise
     */
    public boolean updateFestivalsDataOnStartup() {
        boolean dataUpdated = false;
        // 기존 임계값 정의
        LocalDateTime freshnessThreshold = LocalDateTime.now().minusWeeks(3);

        // 임계값 이후 업데이트된 축제 확인
        List<Festival> festivals = festivalRepository.findFestivalsUpdatedAfter(freshnessThreshold);

        if (festivals.isEmpty()) {
            log.info("데이터베이스에 최신 축제 정보가 없습니다. 외부 API에서 가져오는 중...");

            // 외부 API에서 가져오기
            List<FestivalDTO> festivalDTOs = festivalFetchingService.filterExpiredFestivals(
                    festivalFetchingService.fetchFestivalsInKorean()
            );

            // 축제 정보 처리 (번역 및 분류)
            List<Festival> processedFestivals = festivalProcessingService.processFestivals(
                    festivalDTOs, freshnessThreshold
            );

            if (!processedFestivals.isEmpty()) {
                // 최종 업데이트 타임스탬프 갱신
                processedFestivals.forEach(festival -> festival.setLastUpdated(LocalDateTime.now()));

                // 처리된 축제 정보를 데이터베이스에 저장
                festivalRepository.saveAll(processedFestivals);
                log.info("{} 개의 새로운 축제 정보가 데이터베이스에 저장되었습니다.", processedFestivals.size());
                dataUpdated = true;
            } else {
                log.info("처리 후 저장할 새 축제 정보가 없습니다.");
            }
        } else {
            log.info("데이터베이스에 최신 축제 정보가 이미 있습니다.");
        }

        return dataUpdated;
    }
}
