/*
 * Copyright ⓒ 2017 Brand X Corp. All Rights Reserved
 */
package one.axim.gradle.postman.data;

/**
 * 포스트맨 헤더 데이터
 *
 * @author 황예원
 */
public class HeaderData {
    private String key;
    private String value;
    private String description;

    public String getKey() {
        return key;
    }

    public HeaderData setKey(String key) {
        this.key = key;
        return this;
    }

    public String getValue() {
        return value;
    }

    public HeaderData setValue(String value) {
        this.value = value;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public HeaderData setDescription(String description) {
        this.description = description;
        return this;
    }
}