package csw.korea.festival.main;

import csw.korea.festival.main.festival.model.Festival;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

@TestConfiguration
public class LuceneConfigurationTest {
    @Bean
    @Transactional
    public MassIndexer indexLuceneData(EntityManager entityManager) {
        MassIndexer massIndexer = Search.session(entityManager).massIndexer(Festival.class);
        try {
            massIndexer.startAndWait();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return massIndexer;
    }
}
