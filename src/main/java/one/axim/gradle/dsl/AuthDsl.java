package one.axim.gradle.dsl;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

public class AuthDsl {

    @Input @Optional
    private String type = "";

    @Input @Optional
    private String headerKey = "";

    @Input @Optional
    private String value = "";

    @Input @Optional
    private String descriptionFile = "";

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getHeaderKey() {
        return headerKey;
    }

    public void setHeaderKey(String headerKey) {
        this.headerKey = headerKey;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescriptionFile() {
        return descriptionFile;
    }

    public void setDescriptionFile(String descriptionFile) {
        this.descriptionFile = descriptionFile;
    }
}
