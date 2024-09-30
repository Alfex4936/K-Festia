package csw.korea.festival.main.translation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import csw.korea.festival.main.festival.model.FestivalCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.fluent.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiClient {

    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private final ObjectMapper objectMapper;
    @Value("${openai.api.key}")
    private String apiKey;

    // Constants for the translation method
    private static final String TRANSLATE_SYSTEM_PROMPT = "Translates Korean text to English. Answer only translated text.";
    private static final String TRANSLATE_EXAMPLE_USER = "백두대간 봉자페스티벌";
    private static final String TRANSLATE_EXAMPLE_ASSISTANT = "The Baekdu Daegan Bongja Festival";

    // Constant prompt for the category method
    private static final String CATEGORIZE_PROMPT = """
            You are an assistant that categorizes festival descriptions into predefined categories. Assign each festival to one or more of the following categories:

            1. Music & Performing Arts
            2. Visual Arts & Exhibitions
            3. Cultural & Heritage
            4. Food & Culinary
            5. Family & Children
            6. Sports & Recreation
            7. Technology & Innovation
            8. Literature & Education
            9. Seasonal & Holiday
            10. Community & Social

            If a festival doesn't fit any of the above categories, assign it to "Other".

            Provide the categories as a comma-separated list without any additional text.

            Example:
            Festival Description: "A grand music festival featuring various artists and live performances."
            Categories: Music & Performing Arts

            Festival Description: "An art exhibition showcasing modern sculptures and paintings."
            Categories: Visual Arts & Exhibitions

            Festival Description: "A community gathering with food stalls and live entertainment."
            Categories: Community & Social, Food & Culinary
            """;

    public String translate(String text) {
        if (text == null || text.isEmpty()) {
            log.warn("Empty or null text received for translation.");
            return text;
        }

        try {
            // Construct the request body using String Templates
            String requestBody = STR."""
                    {
                        "model": "gpt-4o-mini",
                        "messages": [
                            {"role": "system", "content": "\{TRANSLATE_SYSTEM_PROMPT}"},
                            {"role": "user", "content": "\{TRANSLATE_EXAMPLE_USER}"},
                            {"role": "assistant", "content": "\{TRANSLATE_EXAMPLE_ASSISTANT}"},
                            {"role": "user", "content": "\{text.strip()}"}
                        ]
                    }
                    """;

            String response = Request.post(OPENAI_API_URL)
                    .addHeader("Authorization", STR."Bearer \{apiKey}")
                    .addHeader("Content-Type", "application/json")
                    .bodyString(requestBody, org.apache.hc.core5.http.ContentType.APPLICATION_JSON)
                    .execute()
                    .returnContent()
                    .asString(StandardCharsets.UTF_8);

            JsonNode root = objectMapper.readTree(response);

            // Check for errors in the response
            if (root.has("error")) {
                String errorMessage = root.path("error").path("message").asText();
                log.error("OpenAI API Error: {}", errorMessage);
                return text; // Return original text or handle accordingly
            }

            return root.path("choices").get(0).path("message").path("content").asText().trim();

        } catch (Exception e) {
            log.error("Error during translation: {}", e.getMessage(), e);
            return text; // Return original text in case of failure
        }
    }

    /**
     * Categorizes the festival summary into predefined categories.
     *
     * @param summary The festival summary in English.
     * @return A list of FestivalCategory enums.
     */
    public List<FestivalCategory> categorize(String summary) {
        if (summary == null || summary.isEmpty()) {
            log.warn("Empty or null summary received for categorization.");
            return List.of(FestivalCategory.OTHER);
        }

        try {
            // Construct the request body using ObjectMapper
            Map<String, Object> requestBodyMap = new HashMap<>();
            requestBodyMap.put("model", "gpt-4o-mini");
            requestBodyMap.put("max_tokens", 50);
            requestBodyMap.put("temperature", 0.3);

            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "system", "content", CATEGORIZE_PROMPT));
            messages.add(Map.of("role", "user", "content", summary.strip()));

            requestBodyMap.put("messages", messages);

            String requestBody = objectMapper.writeValueAsString(requestBodyMap);

            String response = Request.post(OPENAI_API_URL)
                    .addHeader("Authorization", STR."Bearer \{apiKey}")
                    .addHeader("Content-Type", "application/json")
                    .bodyString(requestBody, org.apache.hc.core5.http.ContentType.APPLICATION_JSON)
                    .execute()
                    .returnContent()
                    .asString(StandardCharsets.UTF_8);

            JsonNode root = objectMapper.readTree(response);

            // Check for errors in the response
            if (root.has("error")) {
                String errorMessage = root.path("error").path("message").asText();
                log.error("OpenAI API Error: {}", errorMessage);
                return List.of(FestivalCategory.OTHER);
            }

            String categoriesStr = root.path("choices").get(0).path("message").path("content").asText().trim();
            if (categoriesStr.isEmpty()) {
                return List.of(FestivalCategory.OTHER);
            }

            // Split the categories by comma and map to enum
            String[] categoriesArray = categoriesStr.split(",");
            List<FestivalCategory> categories = Arrays.stream(categoriesArray)
                    .map(String::trim)
                    .map(FestivalCategory::fromDisplayName)
                    .filter(Objects::nonNull)
                    .toList();

            // Ensure at least one category
            if (categories.isEmpty()) {
                categories = List.of(FestivalCategory.OTHER);
            }

            return categories;

        } catch (Exception e) {
            log.error("Error during categorization: {}", e.getMessage(), e);
            return List.of(FestivalCategory.OTHER);
        }
    }
}
