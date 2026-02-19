package one.axim.gradle.generator.data;

public class FieldData {
    private String name;
    private String type;

    private String comment;
    private Boolean isOptional;

    private boolean isEnum = false;
    private String serverClassName;

    private boolean isObject = false;
    private boolean isArray = false;

    private String mapperType;

    private String ext;

    private String classPath;

    private String enumTypeComment;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Boolean getIsOptional() {
        return isOptional;
    }

    public void setOptional(Boolean optional) {
        isOptional = optional;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getServerClassName() {
        return serverClassName;
    }

    public void setServerClassName(String serverClassName) {
        this.serverClassName = serverClassName;
    }

    public boolean isObject() {
        return isObject;
    }

    public void setObject(boolean object) {
        isObject = object;
    }

    public boolean isArray() {
        return isArray;
    }

    public void setArray(boolean array) {
        isArray = array;
    }

    public String getMapperType() {
        return mapperType;
    }

    public void setMapperType(String mapperType) {
        this.mapperType = mapperType;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public boolean isEnum() {
        return isEnum;
    }

    public void setEnum(boolean anEnum) {
        isEnum = anEnum;
    }

    public Boolean getOptional() {
        return isOptional;
    }

    public String getClassPath() {
        return classPath;
    }

    public void setClassPath(String classPath) {
        this.classPath = classPath;
    }

    public String getEnumTypeComment() {
        return enumTypeComment;
    }

    public void setEnumTypeComment(String enumTypeComment) {
        this.enumTypeComment = enumTypeComment;
    }
}
