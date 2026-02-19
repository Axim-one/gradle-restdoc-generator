package one.axim.gradle.data;

public class ErrorDefinition {

    private String name;

    private String code;

    private String description;

    public ErrorDefinition(String name, String[] error) {

        this.name = name;
        this.code = error[0];
        this.description = error[1];
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getCode() {

        return code;
    }

    public void setCode(String code) {

        this.code = code;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }
}
