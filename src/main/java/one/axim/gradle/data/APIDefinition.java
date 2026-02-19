package one.axim.gradle.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class APIDefinition {

    private String id;

    private String name;
    private String className;

    private String description;

    private String group;

    private String groupId;

    private List<APIParameter> parameters;

    private List<APIHeader> hearders;

    private String returnClass;

    private String returnDescription;

    @JsonProperty("isArrayReturn")
    private boolean isArrayReturn = false;

    private String urlMapping;

    private String method;

    @JsonProperty("isNeedsSession")
    private boolean isNeedsSession;

    private Map<String, String> responseStatus;

    @JsonProperty("isPaging")
    private boolean isPaging;

    public APIDefinition() {
        responseStatus = new HashMap<>();
        responseStatus.put("200", "성공");
        isArrayReturn = false;
    }

    public String getName() {

        return name;
    }

    public void setName(String name) {

        this.name = name;
    }

    public String getDescription() {

        return description;
    }

    public void setDescription(String description) {

        this.description = description;
    }

    public String getGroup() {

        return group;
    }

    public void setGroup(String group) {

        this.group = group;
    }

    public List<APIParameter> getParameters() {

        return parameters;
    }

    public void setParameters(List<APIParameter> parameters) {

        this.parameters = parameters;
    }

    public List<APIHeader> getHearders() {

        return hearders;
    }

    public void setHearders(List<APIHeader> hearders) {

        this.hearders = hearders;
    }

    public String getReturnClass() {

        return returnClass;
    }

    public void setReturnClass(String returnClass) {

        this.returnClass = returnClass;
    }

    public String getReturnDescription() {

        return returnDescription;
    }

    public void setReturnDescription(String returnDescription) {

        this.returnDescription = returnDescription;
    }

    public String getUrlMapping() {

        return urlMapping;
    }

    public void setUrlMapping(String urlMapping) {

        this.urlMapping = urlMapping;
    }

    public String getMethod() {

        return method;
    }

    public void setMethod(String method) {

        this.method = method;
    }

    public boolean isNeedsSession() {

        return isNeedsSession;
    }

    public void setNeedsSession(boolean needsSession) {

        isNeedsSession = needsSession;
    }

    public void addResponseStatus(String statusCode, String message) {
        responseStatus.put(statusCode, message);
    }

    public Map<String, String> getResponseStatus() {

        return responseStatus;
    }

    public void setResponseStatus(Map<String, String> responseStatus) {

        this.responseStatus = responseStatus;
    }

    public boolean isArrayReturn() {

        return isArrayReturn;
    }

    public void setArrayReturn(boolean arrayReturn) {

        isArrayReturn = arrayReturn;
    }

    public String getId() {

        return id;
    }

    public void setId(String id) {

        this.id = id;
    }

    public boolean getIsPaging() {
        return isPaging;
    }

    public void setIsPaging(boolean paging) {
        isPaging = paging;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
}
