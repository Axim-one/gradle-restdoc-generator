/*
 * Copyright ⓒ 2017 Brand X Corp. All Rights Reserved
 */
package one.axim.gradle.postman.data;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 포스트맨 Path Variable 데이터
 *
 * @author 황예원
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PathVariableData {
    private String id;
    private String key;
    private String value;
    // string, boolean, number
    private String type;
    private String name;
    private String description;

    @JsonIgnore
    private boolean isOptional;

    @JsonIgnore
    private boolean isEnum;

    @JsonIgnore
    private String originType;

    @JsonIgnore
    private String classPath;

    public String getId() {
        return id;
    }

    public PathVariableData setId(String id) {
        this.id = id;
        return this;
    }

    public String getKey() {
        return key;
    }

    public PathVariableData setKey(String key) {
        this.key = key;
        return this;
    }

    public String getValue() {
        return value;
    }

    public PathVariableData setValue(String value) {
        this.value = value;
        return this;
    }

    public String getType() {
        return type;
    }

    public PathVariableData setType(String type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public PathVariableData setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public PathVariableData setDescription(String description) {
        this.description = description;
        return this;
    }

    public boolean isOptional() {
        return isOptional;
    }

    public PathVariableData setOptional(boolean optional) {
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