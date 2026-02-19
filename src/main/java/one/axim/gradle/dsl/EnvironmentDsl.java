package one.axim.gradle.dsl;

import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Optional;

import java.util.ArrayList;
import java.util.List;

public class EnvironmentDsl {

    @Input
    private String name;

    @Input @Optional
    private List<String[]> variables = new ArrayList<>();

    public EnvironmentDsl(String name) {
        this.name = name;
    }

    public void variable(String name, String value) {
        this.variables.add(new String[]{name, value});
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String[]> getVariables() {
        return variables;
    }
}
