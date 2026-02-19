/*
 * Copyright ⓒ 2017 Brand X Corp. All Rights Reserved
 */
package one.axim.gradle.postman.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;

/**
 * 포스트맨 Request 데이터
 *
 * @author 황예원
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class RequestData {
    private UrlData url;
    private String method;
    private BodyData body;
    private ArrayList<HeaderData> header;
    private String description;

    public UrlData getUrl() {
        return url;
    }

    public RequestData setUrl(UrlData url) {
        this.url = url;
        return this;
    }

    public String getMethod() {
        return method;
    }

    public RequestData setMethod(String method) {
        this.method = method;
        return this;
    }

    public BodyData getBody() {
        return body;
    }

    public RequestData setBody(BodyData body) {
        this.body = body;
        return this;
    }

    public ArrayList<HeaderData> getHeader() {
        return header;
    }

    public RequestData setHeader(ArrayList<HeaderData> header) {
        this.header = header;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public RequestData setDescription(String description) {
        this.description = description;
        return this;
    }
}