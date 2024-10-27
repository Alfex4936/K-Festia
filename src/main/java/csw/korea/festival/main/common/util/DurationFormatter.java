package csw.korea.festival.main.common.util;

import java.time.Duration;
import java.util.Locale;

public class DurationFormatter {

    /**
     * Formats a Duration into a user-friendly string.
     *
     * @param duration The Duration to format.
     * @param locale   The Locale for formatting (e.g., Locale.ENGLISH or Locale.KOREAN).
     * @return A formatted duration string.
     */
    public static String formatDuration(Duration duration, Locale locale) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();

        String formatted = "";

        if (locale.equals(Locale.KOREAN)) {
            if (hours > 0) {
                formatted += hours + "시간 ";
            }
            if (minutes > 0) {
                formatted += minutes + "분";
            }
        } else { // Default to English
            if (hours > 0) {
                formatted += hours + " hour" + (hours > 1 ? "s " : " ");
            }
            if (minutes > 0) {
                formatted += minutes + " minute" + (minutes > 1 ? "s" : "");
            }
        }

        // Handle cases where duration is less than a minute
        if (formatted.isEmpty()) {
            if (locale.equals(Locale.KOREAN)) {
                formatted = "0분";
            } else {
                formatted = "0 minutes";
            }
        }

        return formatted.trim();
    }
}

