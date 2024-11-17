package csw.korea.festival.main.festival.service;

import csw.korea.festival.main.festival.model.FestivalDTO;
import csw.korea.festival.main.festival.model.FestivalResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class FestivalFetchingService {

    // import org.springframework.web.reactive.function.client.WebClient;
    private final WebClient webClient;

    public FestivalFetchingService(WebClient webClient) {
        this.webClient = webClient;
    }

    /**
     * Fetches festival data in Korean from the external API based on month and location.
     *
     * @return List of festivals.
     */
    public List<FestivalDTO> fetchFestivalsInKorean() {
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
     * Filters out expired FestivalDTOs based on their endDate.
     *
     * @param festivalDTOs The list of FestivalDTOs to filter.
     * @return A list of FestivalDTOs that are not expired.
     */
    public List<FestivalDTO> filterExpiredFestivals(List<FestivalDTO> festivalDTOs) {
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
