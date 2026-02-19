/*
 * Copyright ⓒ 2017 Brand X Corp. All Rights Reserved
 */
package one.axim.gradle.postman.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 포스트맨 Query 데이터
 *
 * @author 황예원
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class QueryData {
    private String key;
    private String value;
    private String description;

    @JsonIgnore
    private String type;
    @JsonIgnore
    private String defaultValue;
    @JsonIgnore
    private boolean isOptional;

    @JsonInclude
    private boolean isEnum;

    @JsonIgnore
    private String originType;

    @JsonIgnore
    private String classPath;

    public String getKey() {
        return key;
    }

    public QueryData setKey(String key) {
        this.key = key;
        return this;
    }

    public String getValue() {
        return value;
    }

    public QueryData setValue(String value) {
        this.value = value;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public QueryData setDescription(String description) {
        this.description = description;
        return this;
    }

    public String getType() {
        return type;
    }

    public QueryData setType(String type) {
        this.type = type;
        return this;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public QueryData setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
        return this;
    }

    public boolean isOptional() {
        return isOptional;
    }

    public QueryData setOptional(boolean optional) {
        isOptional = optional;
        return this;
    }

    public boolean isEnum() {
        return isEnum;
    }

    public void setEnum(boolean anEnum) {
        isEnum = anEnum;
    }

    public String getOriginType() {
        return originType;
    }

    public void setOriginType(String originType) {
        this.originType = originType;
    }

    public String getClassPath() {
        return classPath;
    }

    public void setClassPath(String classPath) {
        this.classPath = classPath;
    }
}