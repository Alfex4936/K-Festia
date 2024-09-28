package csw.korea.festival.main.translation;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TranslationService {

    private final OpenAiClient openAiClient;

    // TODO: maybe redis?
    // Cache to store translations and reduce API calls
    private final Cache<String, String> translationCache = Caffeine.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .maximumSize(10_000)
            .build();

    /**
     * Translates the given text from Korean to English.
     *
     * @param text Korean text to translate.
     * @return Translated English text or the original text in case of failure.
     */
    public String translateText(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String cachedTranslation = translationCache.getIfPresent(text);
        if (cachedTranslation != null) {
            return cachedTranslation;
        }

        String translatedText = openAiClient.translate(text);
        translationCache.put(text, translatedText);
        return translatedText;
    }
}