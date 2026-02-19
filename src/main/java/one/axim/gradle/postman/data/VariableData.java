/*
 * Copyright ⓒ 2017 Brand X Corp. All Rights Reserved
 */
package one.axim.gradle.postman.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 포스트맨 Variable 데이터
 *
 * @author 황예원
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class VariableData {
    private String id;
    private String key;
    private String value;
    // string, boolean, number
    private String type;
    private String description;
    private boolean disabled;

    public String getId() {
        return id;
    }

    public VariableData setId(String id) {
        this.id = id;
        return this;
    }

    public String getKey() {
        return key;
    }

    public VariableData setKey(String key) {
        this.key = key;
        return this;
    }

    public String getValue() {
        return value;
    }

    public VariableData setValue(String value) {
        this.value = value;
        return this;
    }

    public String getType() {
        return type;
    }

    public VariableData setType(String type) {
        this.type = type;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public VariableData setDescription(String description) {
        this.description = description;
        return this;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public VariableData setDisabled(boolean disabled) {
        this.disabled = disabled;
        return this;
    }
}
