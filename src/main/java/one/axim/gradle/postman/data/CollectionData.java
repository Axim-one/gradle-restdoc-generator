package one.axim.gradle.postman.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CollectionData {
    private Map<String, Object> info;
    private List<GroupData> item;

    public Map<String, Object> getInfo() {
        return info;
    }

    public void setInfo(Map<String, Object> info) {
        this.info = info;
    }

    public List<GroupData> getItem() {
        return item;
    }

    public void setItem(List<GroupData> item) {
        this.item = item;
    }
}
