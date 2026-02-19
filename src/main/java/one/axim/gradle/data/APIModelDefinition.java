package one.axim.gradle.data;

import java.util.List;

/**
 * Model (DTO/Enum) definition used in API request/response bodies.
 *
 * <p>Generated JSON files under {@code model/} contain one instance per file,
 * keyed by fully qualified class name. Also used for {@code error/error-response.json}.
 *
 * <h3>JSON example:</h3>
 * <pre>{@code
 * {
 *   "name": "UserDto",
 *   "type": "Object",
 *   "fields": [
 *     { "name": "id", "type": "Long", "classPath": "java.lang.Long" },
 *     { "name": "name", "type": "String", "classPath": "java.lang.String" }
 *   ]
 * }
 * }</pre>
 *
 * @see APIField
 */
public class APIModelDefinition {

    /** Simple class name (e.g., {@code "UserDto"}). */
    private String name;
    /** Type category: {@code "Object"} for classes, {@code "Enum"} for enums. */
    private String type;
    /** Model description (from Javadoc class comment). */
    private String description;
    /** List of fields in this model (for Object type) or enum constants (for Enum type). */
    private List<APIField> fields;

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getType() {

        return type;
    }

    public void setType(String type) {

        this.type = type;
    }

    public List<APIField> getFields() {

        return fields;
    }

    public void setFields(List<APIField> fields) {

        this.fields = fields;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }
}
