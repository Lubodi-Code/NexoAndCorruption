package nexo.beta.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatColor;

public class Utils {

    public static String colorize(String msg) {
        Matcher match = Pattern.compile("#[a-fA-F0-9]{6}").matcher(msg);
        while (match.find()) {
            String color = msg.substring(match.start(), match.end());
            msg = msg.replace(color, String.valueOf(ChatColor.of(color)));
            match = Pattern.compile("#[a-fA-F0-9]{6}").matcher(msg);
        }
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    /**
     * Formatea una cantidad de segundos a una cadena legible
     * por ejemplo "1h 5m" o "30s".
     */
    public static String formatTime(int seconds) {
        int days = seconds / 86400;
        seconds %= 86400;
        int hours = seconds / 3600;
        seconds %= 3600;
        int minutes = seconds / 60;
        seconds %= 60;

        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.length() == 0) sb.append(seconds).append("s");
        return sb.toString().trim();
    }

    // Add more utility methods here
}