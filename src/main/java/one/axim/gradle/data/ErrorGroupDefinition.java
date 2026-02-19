package one.axim.gradle.data;

import java.util.List;

/**
 * An error group representing one exception class and its associated error codes.
 *
 * <p>Each exception class that contains {@code public static final ErrorCode} fields
 * produces one {@code ErrorGroupDefinition}. Groups are serialized to {@code error/errors.json}
 * and linked to individual APIs via {@code @error}/{@code @throws} Javadoc tags.
 *
 * <h3>JSON example:</h3>
 * <pre>{@code
 * {
 *   "group": "User Not Found",
 *   "exception": "UserNotFoundException",
 *   "status": 404,
 *   "codes": [
 *     { "name": "USER_NOT_FOUND", "code": "USER_001", "messageKey": "error.user.notfound", "message": "사용자를 찾을 수 없습니다" }
 *   ]
 * }
 * }</pre>
 *
 * @see ErrorCodeEntry
 * @see ErrorCodeScanner
 */
public class ErrorGroupDefinition {

    /** Human-readable group name derived from exception class (e.g., {@code "User Not Found"}). */
    private String group;
    /** Simple class name of the exception (e.g., {@code "UserNotFoundException"}). */
    private String exception;
    /** HTTP status code associated with this exception (e.g., 404). */
    private int status;
    /** List of error codes defined as static fields in the exception class. */
    private List<ErrorCodeEntry> codes;

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public List<ErrorCodeEntry> getCodes() {
        return codes;
    }

    public void setCodes(List<ErrorCodeEntry> codes) {
        this.codes = codes;
    }
}
