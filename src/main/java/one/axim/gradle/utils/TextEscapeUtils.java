package one.axim.gradle.utils;

public class TextEscapeUtils {

    /**
     * Escapes text for safe inclusion in a markdown table cell.
     * <ul>
     *   <li>{@code |} → {@code \|} (prevents cell boundary break)</li>
     *   <li>{@code \n} → {@code <br>} (line break within cell)</li>
     *   <li>{@code \r} → removed</li>
     * </ul>
     *
     * @param text raw text (may be null)
     * @return escaped text safe for markdown table cells, or empty string if null
     */
    public static String escapeForMarkdownTable(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replace("\r", "")
                .replace("|", "\\|")
                .replace("\n", "<br>");
    }
}
