package csw.korea.festival.main.translation;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import csw.korea.festival.main.festival.model.FestivalCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategorizationService {

    private final OpenAiClient openAiClient;

    // Initialize Caffeine cache
    private final Cache<String, List<FestivalCategory>> categorizationCache = Caffeine.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS) // Cache entries expire after 24 hours
            .maximumSize(10_000) // Maximum number of entries in the cache
            .build();

    /**
     * Categorizes the given festival summary into predefined categories.
     *
     * @param summary The festival summary in English.
     * @return A list of FestivalCategory enums.
     */
    public List<FestivalCategory> categorize(String summary) {
        if (summary == null || summary.isEmpty()) {
            return List.of(FestivalCategory.OTHER);
        }

        // Check if the categorization result is already cached
        List<FestivalCategory> cachedCategories = categorizationCache.getIfPresent(summary);
        if (cachedCategories != null) {
            return cachedCategories;
        }

        // If not cached, perform categorization using OpenAiClient
        List<FestivalCategory> categories = openAiClient.categorize(summary);

        // Cache the result for future use
        categorizationCache.put(summary, categories);
        return categories;
    }
}
