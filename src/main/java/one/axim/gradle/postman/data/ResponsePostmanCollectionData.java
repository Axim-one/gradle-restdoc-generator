package one.axim.gradle.postman.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;


@JsonIgnoreProperties(ignoreUnknown = true)
public class ResponsePostmanCollectionData {

    private List<ResponseEnvData> collections;

    public List<ResponseEnvData> getCollections() {
        return collections;
    }

    public void setCollections(List<ResponseEnvData> collections) {
        this.collections = collections;
    }
}
