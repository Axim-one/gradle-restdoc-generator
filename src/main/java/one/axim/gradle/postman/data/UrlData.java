/*
 * Copyright ⓒ 2017 Brand X Corp. All Rights Reserved
 */
package one.axim.gradle.postman.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.ArrayList;

/**
 * 포스트맨 URL 데이터
 *
 * @author 황예원
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class UrlData {
    private String raw;
    private String[] host;
    private String[] path;
    private ArrayList<PathVariableData> variable;
    private ArrayList<QueryData> query;

    public String getRaw() {
        return raw;
    }

    public UrlData setRaw(String raw) {
        this.raw = raw;
        return this;
    }

    public String[] getHost() {
        return host;
    }

    public UrlData setHost(String[] host) {
        this.host = host;
        return this;
    }

    public String[] getPath() {
        return path;
    }

    public UrlData setPath(String[] path) {
        this.path = path;
        return this;
    }

    public ArrayList<PathVariableData> getVariable() {
        return variable;
    }

    public UrlData setVariable(ArrayList<PathVariableData> variable) {
        this.variable = variable;
        return this;
    }

    public ArrayList<QueryData> getQuery() {
        return query;
    }

    public UrlData setQuery(ArrayList<QueryData> query) {
        this.query = query;
        return this;
    }
}