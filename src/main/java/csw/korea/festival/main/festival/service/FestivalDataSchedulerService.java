package csw.korea.festival.main.festival.service;

import org.springframework.scheduling.annotation.Scheduled;

// @Service
public class FestivalDataSchedulerService {
    private final FestivalService festivalService;

    public FestivalDataSchedulerService(FestivalService festivalService) {
        this.festivalService = festivalService;
    }

    @Scheduled(cron = "0 0 0 * * ?") // Runs every day at midnight
    public void updateFestivalsDataDaily() {
        festivalService.updateFestivalsDataOnStartup();
    }
}
