package one.axim.gradle.dsl;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

public class HeaderDsl {

    @Input
    private String name;

    @Input @Optional
    private String defaultValue = "";

    @Input @Optional
    private String description = "";

    @Input @Optional
    private boolean optional = false;

    public HeaderDsl(String name, String defaultValue, String description) {
        this.name = name;
        this.defaultValue = defaultValue;
        this.description = description;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }
}
