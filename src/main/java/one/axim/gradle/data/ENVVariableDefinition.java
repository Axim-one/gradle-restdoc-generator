package one.axim.gradle.data;

import java.util.List;

public class ENVVariableDefinition {

    private String name;
    private String postmanUid;
    private List<ENVVariable> variables;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ENVVariable> getVariables() {
        return variables;
    }

    public void setVariables(List<ENVVariable> variables) {
        this.variables = variables;
    }

    public String getPostmanUid() {
        return postmanUid;
    }

    public void setPostmanUid(String postmanUid) {
        this.postmanUid = postmanUid;
    }
}
