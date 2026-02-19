package one.axim.gradle.data;

import java.util.List;

/**
 * Service-level metadata for the API documentation.
 *
 * <p>Configured via the {@code restMetaGenerator} DSL block and serialized
 * as {@code {serviceId}.json} in the output directory. Also included in
 * {@code spec-bundle.json} and OpenAPI {@code info} section.
 *
 * @see RestMetaGeneratorTask
 */
public class ServiceDefinition {

    /** Unique service identifier. Used as the JSON filename. */
    private String serviceId;
    /** Display name of the service. */
    private String name;
    /** Service introduction text (supports Markdown, loaded from {@code introductionFile}). */
    private String introduction;
    /** Base URL of the API server (e.g., {@code "https://api.example.com"}). */
    private String apiServerUrl;
    /** API version (default: {@code "v1.0"}). */
    private String version = "v1.0";
    /** Postman environment variable definitions. */
    private List<ENVVariableDefinition> envVariable;
    /** Common HTTP headers applied to all endpoints. */
    private List<APIHeader> headers;
    /** Authentication configuration. */
    private APIAuthData auth;

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getIntroduction() {
        return introduction;
    }

    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public List<ENVVariableDefinition> getEnvVariable() {
        return envVariable;
    }

    public void setEnvVariable(List<ENVVariableDefinition> envVariable) {
        this.envVariable = envVariable;
    }

    public List<APIHeader> getHeaders() {
        return headers;
    }

    public ServiceDefinition setHeaders(List<APIHeader> headers) {
        this.headers = headers;
        return this;
    }

    public String getApiServerUrl() {
        return apiServerUrl;
    }

    public void setApiServerUrl(String apiServerUrl) {
        this.apiServerUrl = apiServerUrl;
    }

    public APIAuthData getAuth() {
        return auth;
    }

    public void setAuth(APIAuthData auth) {
        this.auth = auth;
    }
}