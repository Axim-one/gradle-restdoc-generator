package one.axim.gradle.data;

public class APIHeader {

    private String name;

    private String type;

    private String classPath;

    private String description;

    private boolean isOptional = false;

    private String defaultValue;

    /** 공통 인증 헤더 여부 (true: 전역 인증, false: API 고유 헤더) */
    private boolean isGlobal = false;

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

    public boolean getIsGlobal() {
        return isGlobal;
    }

    public void setIsGlobal(boolean global) {
        isGlobal = global;
    }

}
