/*
 * Copyright ⓒ 2017 Brand X Corp. All Rights Reserved
 */
package one.axim.gradle.postman.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BodyData {
    private String mode;
    private String raw;
    private Map<String, Object> options;

    public String getMode() {
        return mode;
    }

    public BodyData setMode(String mode) {
        this.mode = mode;
        return this;
    }

    public String getRaw() {
        return raw;
    }

    public BodyData setRaw(String raw) {
        this.raw = raw;
        return this;
    }

    public Map<String, Object> getOptions() {
        return options;
    }

    public BodyData setOptions(Map<String, Object> options) {
        this.options = options;
        return this;
    }
}