package one.axim.gradle.data;

/**
 * A single error code entry within an {@link ErrorGroupDefinition}.
 *
 * <p>Represents one {@code public static final ErrorCode} field in an exception class.
 * The {@code message} field is resolved from {@code .properties} files using the {@code messageKey}.
 *
 * @see ErrorGroupDefinition
 */
public class ErrorCodeEntry {

    /** Error code string (e.g., {@code "USER_001"}). */
    private String code;
    /** Field name in the exception class (e.g., {@code "USER_NOT_FOUND"}). */
    private String name;
    /** i18n message key for resolving the display message (e.g., {@code "error.user.notfound"}). */
    private String messageKey;
    /** Resolved human-readable message. Falls back to messageKey if not resolved. */
    private String message;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public void setMessageKey(String messageKey) {
        this.messageKey = messageKey;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
