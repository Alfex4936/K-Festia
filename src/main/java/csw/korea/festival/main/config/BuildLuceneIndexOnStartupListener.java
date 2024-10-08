package csw.korea.festival.main.config;

import csw.korea.festival.main.festival.model.Festival;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
@Transactional
public class BuildLuceneIndexOnStartupListener implements ApplicationListener<ApplicationReadyEvent> {

    private final EntityManager entityManager;

    public BuildLuceneIndexOnStartupListener(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public void onApplicationEvent(@NotNull ApplicationReadyEvent event) {
        MassIndexer massIndexer = Search.session(entityManager).massIndexer(Festival.class);
        try {
            massIndexer.startAndWait();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}