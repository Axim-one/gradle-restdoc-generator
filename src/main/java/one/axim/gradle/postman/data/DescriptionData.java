/*
 * Copyright ⓒ 2017 Brand X Corp. All Rights Reserved
 */
package one.axim.gradle.postman.data;

/**
 * 포스트맨 decription 데이터
 *
 * @author 황예원
 */
public class DescriptionData {
    private String content;
    private String type;

    public String getContent() {
        return content;
    }

    public DescriptionData setContent(String content) {
        this.content = content;
        return this;
    }

    public String getType() {
        return type;
    }

    public DescriptionData setType(String type) {
        this.type = type;
        return this;
    }
}