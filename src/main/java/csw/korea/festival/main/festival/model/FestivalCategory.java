package csw.korea.festival.main.festival.model;

import lombok.Getter;

@Getter
public enum FestivalCategory {
    MUSIC_PERFORMING_ARTS("Music & Performing Arts", "음악 & 공연 예술"),
    VISUAL_ARTS_EXHIBITIONS("Visual Arts & Exhibitions", "시각 예술 & 전시회"),
    CULTURAL_HERITAGE("Cultural & Heritage", "문화 & 유산"),
    FOOD_CULINARY("Food & Culinary", "음식 & 요리"),
    FAMILY_CHILDREN("Family & Children", "가족 & 어린이"),
    SPORTS_RECREATION("Sports & Recreation", "스포츠 & 레크리에이션"),
    TECHNOLOGY_INNOVATION("Technology & Innovation", "기술 & 혁신"),
    LITERATURE_EDUCATION("Literature & Education", "문학 & 교육"),
    SEASONAL_HOLIDAY("Seasonal & Holiday", "계절 & 휴일"),
    COMMUNITY_SOCIAL("Community & Social", "커뮤니티 & 사회"),
    OTHER("Other", "기타"); // For uncategorized festivals

    private final String displayNameEn;
    private final String displayNameKo;

    FestivalCategory(String displayNameEn, String displayNameKo) {
        this.displayNameEn = displayNameEn;
        this.displayNameKo = displayNameKo;
    }

    /**
     * Returns the enum constant matching the given display name.
     *
     * @param displayName The display name to match.
     * @return The matching FestivalCategory enum constant, or OTHER if no match is found.
     */
    public static FestivalCategory fromDisplayName(String displayName) {
        for (FestivalCategory category : FestivalCategory.values()) {
            if (category.getDisplayNameEn().equalsIgnoreCase(displayName)) {
                return category;
            }
        }
        return OTHER; // Default category
    }
}
