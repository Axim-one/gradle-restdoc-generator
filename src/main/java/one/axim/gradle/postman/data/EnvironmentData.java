package one.axim.gradle.postman.data;

import java.util.ArrayList;

public class EnvironmentData {
    private String name;
    private ArrayList<EnvironmentKeyValueData> values;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<EnvironmentKeyValueData> getValues() {
        return values;
    }

    public void setValues(ArrayList<EnvironmentKeyValueData> values) {
        this.values = values;
    }
}
