package csw.korea.festival.main.translation;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import csw.korea.festival.main.festival.model.FestivalCategory;
import csw.korea.festival.main.festival.model.FestivalUsageFeeCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategorizationService {

    private final OpenAiClient openAiClient;

    private static final Pattern FREE_PATTERN = Pattern.compile("무료");
    private static final Pattern PAID_PATTERN = Pattern.compile("유료|이용료|입장료|비용|요금|가격");

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

    /**
     * Categorizes the usage fee information into predefined categories.
     *
     * @param usageFeeInfo The raw usage fee information string.
     * @return The corresponding UsageFeeCategory.
     */
    public static FestivalUsageFeeCategory categorizeUsageFee(String usageFeeInfo) {
        if (usageFeeInfo == null || usageFeeInfo.trim().isEmpty()) {
            return FestivalUsageFeeCategory.UNKNOWN;
        }

        boolean containsFree = FREE_PATTERN.matcher(usageFeeInfo).find();
        boolean containsPaid = PAID_PATTERN.matcher(usageFeeInfo).find();

        if (containsFree && !containsPaid) {
            return FestivalUsageFeeCategory.FREE;
        } else if (containsPaid && !containsFree) {
            return FestivalUsageFeeCategory.PAID;
        } else if (containsFree) {
            return FestivalUsageFeeCategory.FREE_WITH_PAID_EXTRAS;
        } else {
            return FestivalUsageFeeCategory.UNKNOWN;
        }
    }
}
