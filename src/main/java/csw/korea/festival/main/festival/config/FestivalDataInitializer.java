package csw.korea.festival.main.festival.config;

import csw.korea.festival.main.festival.model.Festival;
import csw.korea.festival.main.festival.service.FestivalService;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Transactional
public class FestivalDataInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private final FestivalService festivalService;
    private final EntityManager entityManager;

    public FestivalDataInitializer(FestivalService festivalService, EntityManager entityManager) {
        this.festivalService = festivalService;
        this.entityManager = entityManager;
    }

    @Override
    public void onApplicationEvent(@NotNull ApplicationReadyEvent event) {
        // Update the festivals data
        festivalService.updateFestivalsDataOnStartup();

        // Build Lucene index
        MassIndexer massIndexer = Search.session(entityManager).massIndexer(Festival.class);
        try {
            massIndexer.startAndWait();
            log.info("Lucene index built successfully.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Lucene index building was interrupted.", e);
        }
    }
}