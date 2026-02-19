package one.axim.gradle.data;

import java.util.List;

public class APIModelDefinition {

    private String name;
    private String type;
    private String description;
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
