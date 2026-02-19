/*
 * Copyright ⓒ 2017 Brand X Corp. All Rights Reserved
 */
package one.axim.gradle.generator.data;

/**
 * 파라미터 Data 모델
 *
 * @author 황예원
 */
public class ParameterData {
    private String name;
    private String type;
    private String comment;
    private Boolean isOptional;

    private Boolean isEnum;

    private String classPath;

    private String enumTypeComment;

    public String getName() {
        return name;
    }

    public ParameterData setName(String name) {
        this.name = name;
        return this;
    }

    public String getType() {
        return type;
    }

    public ParameterData setType(String type) {
        this.type = type;
        return this;
    }

    public String getComment() {
        return comment;
    }

    public ParameterData setComment(String comment) {
        this.comment = comment;
        return this;
    }

    public Boolean getIsOptional() {
        return isOptional;
    }

    public ParameterData setOptional(Boolean optional) {
        isOptional = optional;
        return this;
    }

    public Boolean getIsEnum() {
        return isEnum;
    }

    public void setEnum(Boolean anEnum) {
        isEnum = anEnum;
    }

    public String getClassPath() {
        return classPath;
    }

    public void setClassPath(String classPath) {
        this.classPath = classPath;
    }

    public String getEnumTypeComment() {
        return enumTypeComment;
    }

    public void setEnumTypeComment(String enumTypeComment) {
        this.enumTypeComment = enumTypeComment;
    }
}