package one.axim.gradle.data;

import java.util.List;

public class ServiceDefinition {

    private String serviceId;
    private String name;
    private String introduction;
    private String apiServerUrl;
    private String version = "v1.0";
    private List<ENVVariableDefinition> envVariable;
    private List<APIHeader> headers;

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