package one.axim.gradle.postman.data;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * 포스트맨 Info 데이터
 *
 * @author 황예원
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class InfoData {
    private String _postman_id;
    private String name;
    private DescriptionData description;
    private String schema;
    private String version;

    public String get_postman_id() {
        return _postman_id;
    }

    public void set_postman_id(String _postman_id) {
        this._postman_id = _postman_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public DescriptionData getDescription() {
        return description;
    }

    public InfoData setDescription(DescriptionData description) {
        this.description = description;
        return this;
    }

    public void setDescriptionBody(String body) {
        this.description = new DescriptionData();
        this.description.setContent(body);
        this.description.setType("text/markdown");
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
