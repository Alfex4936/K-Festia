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
    @Transactional
    public void onApplicationEvent(@NotNull ApplicationReadyEvent event) {
        // 트랜잭션이 완료되도록 명시적으로 분리
        boolean dataUpdated = festivalService.updateFestivalsDataOnStartup();

        // 데이터가 업데이트되었다면 약간의 지연을 주어 트랜잭션이 완전히 커밋되도록 함
        if (dataUpdated) {
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        // Lucene 인덱스 빌드
        MassIndexer massIndexer = Search.session(entityManager).massIndexer(Festival.class);
        try {
            massIndexer.startAndWait();
            log.info("Lucene 인덱스가 성공적으로 생성되었습니다.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Lucene 인덱스 생성이 중단되었습니다.", e);
        }
    }
}