package one.axim.gradle.generator.data;

import java.util.ArrayList;
import java.util.List;

public class ModelData {
    private String className;
    private ArrayList<FieldData> fields;

    private List<String> imports;

    public ModelData() {
        this.fields = new ArrayList<>();
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public ArrayList<FieldData> getFields() {
        return fields;
    }

    public void setFields(ArrayList<FieldData> fields) {
        this.fields = fields;
    }

    public void addField(FieldData field) {
        this.fields.add(field);
    }

    public List<String> getImports() {
        return imports;
    }

    public void setImports(List<String> imports) {
        this.imports = imports;
    }

    public void addImport(String cls) {

        if (!this.imports.contains(cls))
            this.imports.add(cls);
    }
}
