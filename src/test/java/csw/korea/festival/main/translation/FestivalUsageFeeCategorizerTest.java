package csw.korea.festival.main.translation;

import csw.korea.festival.main.festival.model.FestivalUsageFeeCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FestivalUsageFeeCategorizerTest {

    @Test
    public void testCategorizeUsageFee_Free() {
        String input = "무료";
        assertEquals(FestivalUsageFeeCategory.FREE, CategorizationService.categorizeUsageFee(input));
    }

    @Test
    public void testCategorizeUsageFee_Paid() {
        String input = "유료(1인 26,000원)";
        assertEquals(FestivalUsageFeeCategory.PAID, CategorizationService.categorizeUsageFee(input));
    }

    @Test
    public void testCategorizeUsageFee_FreeWithPaidExtras() {
        String input1 = "입장 무료<br>이용료 별도 5,000원";
        String input2 = "무료 / 유료(30,000원~)";
        String input3 = "무료(유료 프로그램 有)";
        assertEquals(FestivalUsageFeeCategory.FREE_WITH_PAID_EXTRAS, CategorizationService.categorizeUsageFee(input1));
        assertEquals(FestivalUsageFeeCategory.FREE_WITH_PAID_EXTRAS, CategorizationService.categorizeUsageFee(input2));
        assertEquals(FestivalUsageFeeCategory.FREE_WITH_PAID_EXTRAS, CategorizationService.categorizeUsageFee(input3));
    }

    @Test
    public void testCategorizeUsageFee_Unknown() {
        String input = "";
        assertEquals(FestivalUsageFeeCategory.UNKNOWN, CategorizationService.categorizeUsageFee(input));

        String input2 = "상세 정보 없음";
        assertEquals(FestivalUsageFeeCategory.UNKNOWN, CategorizationService.categorizeUsageFee(input2));
    }
}