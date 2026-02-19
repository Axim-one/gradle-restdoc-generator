package one.axim.gradle;

import one.axim.gradle.data.APIDefinition;
import one.axim.gradle.data.APIModelDefinition;
import one.axim.gradle.data.ServiceDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class SpecBundleGenerator {

    private static final Gson gson = new Gson();
    private static final ObjectMapper objectMapper = new ObjectMapper() {{
        enable(SerializationFeature.INDENT_OUTPUT);
        configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }};

    private final String documentPath;
    private final ServiceDefinition serviceDefinition;

    public SpecBundleGenerator(ServiceDefinition serviceDefinition, String docPath) {
        this.serviceDefinition = serviceDefinition;
        this.documentPath = docPath;
    }

    public void build() throws IOException {
        Map<String, Object> bundle = new LinkedHashMap<>();
        bundle.put("service", buildServiceMap());
        bundle.put("apis", loadAllApis());
        bundle.put("models", loadAllModels());

        File outputFile = new File(this.documentPath, "spec-bundle.json");
        String json = objectMapper.writeValueAsString(bundle);
        Files.write(outputFile.toPath(), json.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        System.out.println("Spec bundle generated: " + outputFile.getAbsolutePath());
    }

    private Map<String, Object> buildServiceMap() {
        Map<String, Object> service = new LinkedHashMap<>();
        service.put("serviceId", serviceDefinition.getServiceId());
        service.put("name", serviceDefinition.getName());
        service.put("apiServerUrl", serviceDefinition.getApiServerUrl());
        service.put("version", serviceDefinition.getVersion());
        service.put("introduction", serviceDefinition.getIntroduction());
        service.put("auth", serviceDefinition.getAuth());
        service.put("headers", serviceDefinition.getHeaders());
        return service;
    }

    private List<APIDefinition> loadAllApis() {
        List<APIDefinition> allApis = new ArrayList<>();
        File apiDir = new File(this.documentPath + File.separator + "api");
        File[] files = apiDir.listFiles();
        if (files == null) return allApis;

        for (File json : files) {
            try {
                APIDefinition[] apis = gson.fromJson(FileUtils.readFileToString(json, "UTF-8"), APIDefinition[].class);
                if (apis != null) {
                    Collections.addAll(allApis, apis);
                }
            } catch (IOException e) {
                // skip
            }
        }
        return allApis;
    }

    private Map<String, APIModelDefinition> loadAllModels() {
        Map<String, APIModelDefinition> models = new LinkedHashMap<>();
        File modelDir = new File(this.documentPath + File.separator + "model");
        File[] files = modelDir.listFiles();
        if (files == null) return models;

        for (File json : files) {
            try {
                APIModelDefinition model = gson.fromJson(FileUtils.readFileToString(json, "UTF-8"), APIModelDefinition.class);
                if (model != null) {
                    // key = filename without .json extension (= FQCN)
                    String key = json.getName().replace(".json", "");
                    models.put(key, model);
                }
            } catch (IOException e) {
                // skip
            }
        }
        return models;
    }
}
