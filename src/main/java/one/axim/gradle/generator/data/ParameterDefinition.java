/*
 * Copyright ⓒ 2017 Brand X Corp. All Rights Reserved
 */
package one.axim.gradle.generator.data;

import java.util.ArrayList;

/**
 * Parameter 정의 모델
 *
 * @author 황예원
 */
public class ParameterDefinition {
    private String type;
    private ArrayList<ParameterData> parameters;

    public ParameterDefinition() {
        this.parameters = new ArrayList<>();
    }

    public String getType() {
        return type;
    }

    public ParameterDefinition setType(String type) {
        this.type = type;
        return this;
    }

    public ArrayList<ParameterData> getParameters() {
        return parameters;
    }

    public ParameterDefinition setParameters(ArrayList<ParameterData> parameters) {
        this.parameters = parameters;
        return this;
    }

    public void addParameter(ParameterData parameterData) {
        this.parameters.add(parameterData);
    }
}