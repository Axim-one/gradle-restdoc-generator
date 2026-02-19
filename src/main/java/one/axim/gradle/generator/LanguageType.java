package one.axim.gradle.generator;

public enum LanguageType {
    JAVA("java"),
    POSTMAN("postman");

    private String value;

    LanguageType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
