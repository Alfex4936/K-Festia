package csw.korea.festival.main.common.util;

import csw.dkssud.HangulMapper;

public class Korean {

    public static boolean isQwerty(String text) {
        return text.matches("^[a-zA-Z]*$");
    }

    public static String toQwerty(String text) {
        return HangulMapper.hangulToQwerty(text);
    }

    public static String toHangul(String text) {
        return HangulMapper.qwertyToHangul(text);
    }
}
