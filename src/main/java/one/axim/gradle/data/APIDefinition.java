package one.axim.gradle.data;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import one.axim.gradle.data.ErrorGroupDefinition;

/**
 * Represents a single REST API endpoint definition.
 *
 * <p>Each instance maps to one controller method annotated with
 * {@code @GetMapping}, {@code @PostMapping}, etc. Generated JSON
 * files under {@code api/} contain arrays of this type.
 *
 * <h3>JSON example:</h3>
 * <pre>{@code
 * {
 *   "id": "getUserById",
 *   "name": "사용자 상세 조회",
 *   "className": "SampleController",
 *   "method": "GET",
 *   "urlMapping": "/users/{userId}",
 *   "returnClass": "com.example.dto.UserDto",
 *   "isArrayReturn": false,
 *   "isPaging": false,
 *   "pagingType": null,
 *   "isNeedsSession": true,
 *   "parameters": [...],
 *   "responseStatus": { "200": "성공", "404": "사용자를 찾을 수 없음" },
 *   "errors": [ ErrorGroupDefinition, ... ]
 * }
 * }</pre>
 *
 * @see APIParameter
 * @see ErrorGroupDefinition
 */
public class APIDefinition {

    /** Unique API identifier (typically the method name). */
    private String id;

    /** Display name of the API (from {@code @GetMapping(name=...)} or Javadoc first line). */
    private String name;

    /** Controller class simple name (e.g., {@code "SampleController"}). */
    private String className;

    /** API description (from Javadoc body text). */
    private String description;

    /** API group name (from {@code @group} Javadoc tag or {@code @XRestGroupName} annotation). */
    private String group;

    /** Group identifier for categorization. */
    private String groupId;

    /** List of request parameters (path, query, body fields). */
    private List<APIParameter> parameters;

    /** List of custom headers specific to this endpoint. */
    private List<APIHeader> hearders;

    /** Fully qualified class name of the return type (unwrapped from generics like {@code ApiResult<T>}). */
    private String returnClass;

    /** Return value description (from {@code @return} Javadoc tag). */
    private String returnDescription;

    /** Whether the return type is an array/list (e.g., {@code List<UserDto>}). */
    @JsonProperty("isArrayReturn")
    private boolean isArrayReturn = false;

    /** URL path mapping (e.g., {@code "/users/{userId}"}). */
    private String urlMapping;

    /** HTTP method (GET, POST, PUT, DELETE, PATCH). */
    private String method;

    /** Whether this endpoint requires authentication (from {@code @auth true} Javadoc tag). */
    @JsonProperty("isNeedsSession")
    private boolean isNeedsSession;

    /**
     * HTTP response status codes and their descriptions.
     * Populated from {@code @response} Javadoc tags and linked error groups.
     * Default: {@code {"200": "성공"}}.
     */
    private Map<String, String> responseStatus;

    /** Whether this endpoint uses pagination (Spring {@code Pageable} or XPage). */
    @JsonProperty("isPaging")
    private boolean isPaging;

    /**
     * Pagination type: {@code "spring"} for Spring Data {@code Pageable},
     * {@code "xpage"} for framework XPage. {@code null} if not paged.
     */
    private String pagingType;

    /**
     * Error groups linked to this API via {@code @error}/{@code @throws} tags
     * or method {@code throws} clauses. {@code null} if no errors are linked.
     */
    private List<ErrorGroupDefinition> errors;

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

    public String getPagingType() {
        return pagingType;
    }

    public void setPagingType(String pagingType) {
        this.pagingType = pagingType;
    }

    /**
     * pagingType을 반환하되, null이면서 isPaging=true인 경우 하위호환을 위해 "xpage"를 반환한다.
     */
    public String getEffectivePagingType() {
        if (pagingType != null) return pagingType;
        return isPaging ? PagingType.XPAGE : null;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public List<ErrorGroupDefinition> getErrors() {
        return errors;
    }

    public void setErrors(List<ErrorGroupDefinition> errors) {
        this.errors = errors;
    }
}
