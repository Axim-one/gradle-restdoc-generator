/*
 * Copyright ⓒ 2017 Brand X Corp. All Rights Reserved
 */
package one.axim.gradle.postman.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;

/**
 * 포스트맨 Response 데이터
 *
 * @author 황예원
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponseData {
    private String id;
    private ArrayList<HeaderData> header;
    private String body;
    private String status;
    private Integer code;
    private RequestData originalRequest;

    private String name;

    private String _postman_previewlanguage = "json";

    public String getId() {
        return id;
    }

    public ResponseData setId(String id) {
        this.id = id;
        return this;
    }

    public ArrayList<HeaderData> getHeader() {
        return header;
    }

    public ResponseData setHeader(ArrayList<HeaderData> header) {
        this.header = header;
        return this;
    }

    public String getBody() {
        return body;
    }

    public ResponseData setBody(String body) {
        this.body = body;
        return this;
    }

    public String getStatus() {
        return status;
    }

    public ResponseData setStatus(String status) {
        this.status = status;
        return this;
    }

    public Integer getCode() {
        return code;
    }

    public ResponseData setCode(Integer code) {
        this.code = code;
        return this;
    }

    public RequestData getOriginalRequest() {
        return originalRequest;
    }

    public ResponseData setOriginalRequest(RequestData originalRequest) {
        this.originalRequest = originalRequest;
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String get_postman_previewlanguage() {
        return _postman_previewlanguage;
    }

    public void set_postman_previewlanguage(String _postman_previewlanguage) {
        this._postman_previewlanguage = _postman_previewlanguage;
    }
}