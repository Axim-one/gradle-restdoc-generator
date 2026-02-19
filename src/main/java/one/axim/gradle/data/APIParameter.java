package one.axim.gradle.data;

/**
 * A request parameter in an {@link APIDefinition}.
 *
 * <p>Represents path variables, query parameters, request body fields,
 * or auto-generated pagination parameters.
 *
 * @see APIDefinition
 * @see APIParameterKind
 */
public class APIParameter {

    /** Parameter name (e.g., {@code "userId"}, {@code "page"}). */
    private String name;

    /** Simple type name (e.g., {@code "Long"}, {@code "String"}). */
    private String type;

    /** Fully qualified class path (e.g., {@code "java.lang.Long"}). */
    private String classPath;

    /** Parameter description (from {@code @param} Javadoc tag). */
    private String description;

    /** Whether this parameter is optional (default: {@code false}). */
    private boolean isOptional = false;

    /** Default value for this parameter (e.g., {@code "0"} for page). */
    private String defaultValue;

    /** Parameter location: PATH, QUERY, or BODY. */
    private APIParameterKind parameterKind;

    /** Whether this parameter is an enum type. */
    private boolean isEnum = false;

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

    public boolean getIsOptional() {

        return isOptional;
    }

    public void setIsOptional(boolean optional) {

        isOptional = optional;
    }

    public String getDefaultValue() {

        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {

        this.defaultValue = defaultValue;
    }

    public APIParameterKind getParameterKind() {

        return parameterKind;
    }

    public void setParameterKind(APIParameterKind parameterKind) {

        this.parameterKind = parameterKind;
    }

    public boolean getIsEnum() {
        return isEnum;
    }

    public void setEnum(boolean anEnum) {
        isEnum = anEnum;
    }
}
