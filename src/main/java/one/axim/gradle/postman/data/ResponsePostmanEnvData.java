package one.axim.gradle.postman.data;

import java.util.List;

public class ResponsePostmanEnvData implements IResponsePostman {

    private List<ResponseEnvData> environments;

    public List<ResponseEnvData> getEnvironments() {
        return environments;
    }

    public void setEnvironments(List<ResponseEnvData> environments) {
        this.environments = environments;
    }
}
