package csw.korea.festival.main.festival.model;

import lombok.Getter;

@Getter
public enum FestivalCategory {
    MUSIC_PERFORMING_ARTS("Music & Performing Arts"),
    VISUAL_ARTS_EXHIBITIONS("Visual Arts & Exhibitions"),
    CULTURAL_HERITAGE("Cultural & Heritage"),
    FOOD_CULINARY("Food & Culinary"),
    FAMILY_CHILDREN("Family & Children"),
    SPORTS_RECREATION("Sports & Recreation"),
    TECHNOLOGY_INNOVATION("Technology & Innovation"),
    LITERATURE_EDUCATION("Literature & Education"),
    SEASONAL_HOLIDAY("Seasonal & Holiday"),
    COMMUNITY_SOCIAL("Community & Social"),
    OTHER("Other"); // For uncategorized festivals

    private final String displayName;

    FestivalCategory(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the enum constant matching the given display name.
     *
     * @param displayName The display name to match.
     * @return The matching FestivalCategory enum constant, or OTHER if no match is found.
     */
    public static FestivalCategory fromDisplayName(String displayName) {
        for (FestivalCategory category : FestivalCategory.values()) {
            if (category.getDisplayName().equalsIgnoreCase(displayName)) {
                return category;
            }
        }
        return OTHER; // Default category
    }
}
