package one.axim.gradle.data;

/**
 * A field within an {@link APIModelDefinition}.
 *
 * <p>Represents a Java class field or enum constant. Used in both model JSON
 * and error-response JSON generation.
 *
 * @see APIModelDefinition
 */
public class APIField {

    /** Field name (e.g., {@code "userId"}). */
    private String name;
    /** Simple type name (e.g., {@code "String"}, {@code "Long"}, {@code "List"}). */
    private String type;
    /** Fully qualified class path (e.g., {@code "java.lang.String"}). */
    private String classPath;
    /** Field description (from Javadoc or annotation). */
    private String description;
    /** Whether this field is optional (default: {@code true}). */
    private boolean isOptional = true;

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

    public String getClassPath() {

        return classPath;
    }

    public void setClassPath(String classPath) {

        this.classPath = classPath;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }

    public boolean isOptional() {

        return isOptional;
    }

    public void setOptional(boolean optional) {

        isOptional = optional;
    }
}
