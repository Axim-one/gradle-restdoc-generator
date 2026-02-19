/*
 * Copyright ⓒ 2017 Brand X Corp. All Rights Reserved
 */
package one.axim.gradle.postman.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;
import java.util.List;

/**
 * 포스트맨 Item 데이터
 *
 * @author 황예원
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ItemData {

    private String id;
    private String name;
    private RequestData request;
    private ArrayList<ResponseData> response;
    private String description;

    public ItemData() {
        this.response = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public ItemData setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return name;
    }

    public ItemData setName(String name) {
        this.name = name;
        return this;
    }

    public RequestData getRequest() {
        return request;
    }

    public ItemData setRequest(RequestData request) {
        this.request = request;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public ItemData setDescription(String description) {
        this.description = description;
        return this;
    }

    public List<ResponseData> getResponse() {
        return response;
    }

    public ItemData addResponse(ResponseData response) {
        this.response.add(response);
        return this;
    }

    public void setResponse(ArrayList<ResponseData> response) {
        this.response = response;
    }
}