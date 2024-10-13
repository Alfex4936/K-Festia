package csw.korea.festival.main.common.util;

import csw.dkssud.HangulMapper;

public class Korean {

    /**
     * 주어진 텍스트가 QWERTY 한국어 입력인지 확인합니다.
     *
     * @param text 검사할 텍스트
     * @return QWERTY 한국어 입력이면 true, 아니면 false
     */
    public static boolean isQwertyKorean(String text) {
        // QWERTY로 입력된 텍스트를 한글로 변환
        String hangul = toHangul(text);
        // 변환된 한글이 실제 한글 문자를 포함하고 있는지 확인
        return hangul != null && hangul.matches(".*[가-힣]+.*");
    }

    /**
     * 주어진 텍스트가 오로지 알파벳(a-zA-Z)으로만 구성되어 있는지 확인합니다.
     *
     * @param text 검사할 텍스트
     * @return 오로지 알파벳으로만 구성되면 true, 아니면 false
     */
    public static boolean isPureEnglish(String text) {
        return text.matches("^[a-zA-Z]+$");
    }

    /**
     * 한글 텍스트를 QWERTY로 변환합니다.
     *
     * @param text 한글 텍스트
     * @return QWERTY로 변환된 텍스트
     */
    public static String toQwerty(String text) {
        return HangulMapper.hangulToQwerty(text);
    }

    /**
     * QWERTY로 입력된 한국어 텍스트를 한글로 변환합니다.
     *
     * @param text QWERTY로 입력된 텍스트
     * @return 변환된 한글 텍스트
     */
    public static String toHangul(String text) {
        return HangulMapper.qwertyToHangul(text);
    }
}
