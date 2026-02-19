package one.axim.gradle.postman.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CollectionResponse {

    private CollectionData collection;

    public CollectionData getCollection() {
        return collection;
    }

    public void setCollection(CollectionData collection) {
        this.collection = collection;
    }
}
