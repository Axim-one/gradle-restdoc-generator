package one.axim.gradle.data;

public class APIParameter {

    private String name;

    private String type;

    private String classPath;

    private String description;

    private boolean isOptional = false;

    private String defaultValue;

    private APIParameterKind parameterKind;

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
