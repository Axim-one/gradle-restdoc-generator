package one.axim.gradle.data;

public class APIField {

    private String name;
    private String type;
    private String classPath;
    private String description;
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
