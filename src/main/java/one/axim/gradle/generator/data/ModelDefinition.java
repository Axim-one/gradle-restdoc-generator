package one.axim.gradle.generator.data;

import java.util.ArrayList;
import java.util.List;

public class ModelDefinition {
    private String packageName;
    private String type;
    private String className;
    private ArrayList<ModelData> models;

    private List<String> imports;
    private ModelData keepModel;

    public ModelDefinition(String packageName, String className) {
        this.models = new ArrayList<>();
        this.packageName = packageName;
        this.className = className;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public ArrayList<ModelData> getModels() {

        if (this.keepModel != null) {
            models.add(0, this.keepModel);
            this.keepModel = null;
        }
        return models;
    }

    public void setModels(ArrayList<ModelData> models) {
        this.models = models;
    }

    public void addModel(ModelData modelData) {

        this.models.add(0, modelData);
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getType() {
        return type;
    }

    public ModelDefinition setType(String type) {
        this.type = type;
        return this;
    }

    public List<String> getImports() {
        if (imports == null) {

            imports = new ArrayList<>();
            if (this.getModels() != null) {
                for (ModelData model : this.getModels()) {
                    if (model.getImports() != null) {
                        for (String anImport : model.getImports()) {

                            if (!imports.contains(anImport)) {
                                imports.add(anImport);
                            }
                        }
                    }
                }
            }
        }
        return imports;
    }

    public void setImports(List<String> imports) {
        this.imports = imports;
    }

    public ModelData getKeepModel() {
        return keepModel;
    }

    public void setKeepModel(ModelData modelData) {
        this.keepModel = modelData;
    }
}
