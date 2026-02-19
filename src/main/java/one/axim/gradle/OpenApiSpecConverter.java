package one.axim.gradle;

import one.axim.gradle.data.*;
import one.axim.gradle.utils.SpringPageSchema;
import one.axim.gradle.utils.SpringPageSortSchema;
import one.axim.gradle.utils.XPageSchema;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.gson.Gson;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

public class OpenApiSpecConverter {

    private static final Gson gson = new Gson();
    private static final ObjectMapper objectMapper = new ObjectMapper() {{
        enable(SerializationFeature.INDENT_OUTPUT);
        configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
    }};

    private final String documentPath;
    private final ServiceDefinition serviceDefinition;
    private final ArrayList<APIDefinition> apiList;

    public OpenApiSpecConverter(ServiceDefinition serviceDefinition, String docPath) {
        this.serviceDefinition = serviceDefinition;
        this.documentPath = docPath;
        this.apiList = new ArrayList<>();
        loadApiJson(docPath);
    }

    private void loadApiJson(String docPath) {
        File file = new File(docPath + File.separator + "api");
        File[] files = file.listFiles();
        if (files == null) return;

        for (File json : files) {
            try {
                APIDefinition[] api = gson.fromJson(FileUtils.readFileToString(json, "UTF-8"), APIDefinition[].class);
                if (api != null) {
                    Collections.addAll(this.apiList, api);
                }
            } catch (IOException io) {
                // skip unreadable files
            }
        }
    }

    private APIModelDefinition loadModel(String path) {
        try {
            File modelFile = new File(this.documentPath + File.separator + "model" + File.separator + path + ".json");
            return gson.fromJson(FileUtils.readFileToString(modelFile, "UTF-8"), APIModelDefinition.class);
        } catch (IOException io) {
            return null;
        }
    }

    public void build() throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("openapi", "3.0.3");
        root.put("info", buildInfo());
        root.put("servers", buildServers());

        List<Map<String, String>> tags = buildTags();
        if (!tags.isEmpty()) {
            root.put("tags", tags);
        }

        root.put("paths", buildPaths());
        root.put("components", buildComponents());

        File outputFile = new File(this.documentPath, "openapi.json");
        String json = objectMapper.writeValueAsString(root);
        Files.write(outputFile.toPath(), json.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        System.out.println("OpenAPI spec generated: " + outputFile.getAbsolutePath());
    }

    private Map<String, Object> buildInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("title", StringUtils.defaultIfEmpty(serviceDefinition.getName(), serviceDefinition.getServiceId()));
        info.put("version", StringUtils.defaultIfEmpty(serviceDefinition.getVersion(), "v1.0"));
        if (!StringUtils.isEmpty(serviceDefinition.getIntroduction())) {
            info.put("description", serviceDefinition.getIntroduction());
        }
        return info;
    }

    private List<Map<String, String>> buildServers() {
        List<Map<String, String>> servers = new ArrayList<>();
        if (!StringUtils.isEmpty(serviceDefinition.getApiServerUrl())) {
            Map<String, String> server = new LinkedHashMap<>();
            server.put("url", serviceDefinition.getApiServerUrl());
            servers.add(server);
        }
        return servers;
    }

    private List<Map<String, String>> buildTags() {
        Set<String> seen = new LinkedHashSet<>();
        for (APIDefinition api : this.apiList) {
            if (!StringUtils.isEmpty(api.getGroup())) {
                seen.add(api.getGroup());
            }
        }
        List<Map<String, String>> tags = new ArrayList<>();
        for (String name : seen) {
            Map<String, String> tag = new LinkedHashMap<>();
            tag.put("name", name);
            tags.add(tag);
        }
        return tags;
    }

    private Map<String, Object> buildPaths() {
        Map<String, Object> paths = new LinkedHashMap<>();

        for (APIDefinition api : this.apiList) {
            // Convert URL to OpenAPI path format: /v1.0/users/{id}
            String path = "/" + serviceDefinition.getVersion() + api.getUrlMapping();

            @SuppressWarnings("unchecked")
            Map<String, Object> pathItem = (Map<String, Object>) paths.computeIfAbsent(path, k -> new LinkedHashMap<>());

            String method = api.getMethod().toLowerCase();
            pathItem.put(method, buildOperation(api));
        }

        return paths;
    }

    private Map<String, Object> buildOperation(APIDefinition api) {
        Map<String, Object> operation = new LinkedHashMap<>();

        operation.put("summary", api.getName());
        if (!StringUtils.isEmpty(api.getDescription())) {
            operation.put("description", api.getDescription());
        }
        if (!StringUtils.isEmpty(api.getGroup())) {
            operation.put("tags", Collections.singletonList(api.getGroup()));
        }
        operation.put("operationId", api.getId());

        List<Map<String, Object>> parameters = buildParameters(api);
        if (!parameters.isEmpty()) {
            operation.put("parameters", parameters);
        }

        Map<String, Object> requestBody = buildRequestBody(api);
        if (requestBody != null) {
            operation.put("requestBody", requestBody);
        }

        operation.put("responses", buildResponses(api));

        // Security: if needs session and auth is configured
        if (api.isNeedsSession() && serviceDefinition.getAuth() != null
                && "token".equals(serviceDefinition.getAuth().getType())) {
            operation.put("security", Collections.singletonList(
                    Collections.singletonMap("ApiKeyAuth", Collections.emptyList())));
        }

        return operation;
    }

    private List<Map<String, Object>> buildParameters(APIDefinition api) {
        List<Map<String, Object>> parameters = new ArrayList<>();

        if (api.getParameters() != null) {
            for (APIParameter param : api.getParameters()) {
                if (param.getParameterKind() == APIParameterKind.URL_PATH) {
                    parameters.add(buildParameter(param, "path"));
                } else if (param.getParameterKind() == APIParameterKind.REQUEST_PARAMETER) {
                    // XPageNation expansion
                    if ("one.axim.framework.core.data.XPageNation".equals(param.getClassPath())) {
                        parameters.add(buildSimpleQueryParam("size", "integer", "int32", "페이지당 데이터 크기", false));
                        parameters.add(buildSimpleQueryParam("page", "integer", "int32", "조회 하고자 하는 페이지 넘버", false));
                    } else {
                        parameters.add(buildParameter(param, "query"));
                    }
                }
            }
        }

        // Common headers from ServiceDefinition
        if (serviceDefinition.getHeaders() != null) {
            for (APIHeader header : serviceDefinition.getHeaders()) {
                Map<String, Object> headerParam = new LinkedHashMap<>();
                headerParam.put("name", header.getName());
                headerParam.put("in", "header");
                if (!StringUtils.isEmpty(header.getDescription())) {
                    headerParam.put("description", header.getDescription());
                }
                headerParam.put("required", !header.getIsOptional());
                Map<String, Object> schema = new LinkedHashMap<>();
                schema.put("type", "string");
                if (!StringUtils.isEmpty(header.getDefaultValue())) {
                    schema.put("default", header.getDefaultValue());
                }
                headerParam.put("schema", schema);
                parameters.add(headerParam);
            }
        }

        // Per-API headers
        if (api.getHearders() != null) {
            for (APIHeader header : api.getHearders()) {
                Map<String, Object> headerParam = new LinkedHashMap<>();
                headerParam.put("name", header.getName());
                headerParam.put("in", "header");
                if (!StringUtils.isEmpty(header.getDescription())) {
                    headerParam.put("description", header.getDescription());
                }
                headerParam.put("required", !header.getIsOptional());
                Map<String, Object> schema = new LinkedHashMap<>();
                schema.put("type", "string");
                if (!StringUtils.isEmpty(header.getDefaultValue())) {
                    schema.put("default", header.getDefaultValue());
                }
                headerParam.put("schema", schema);
                parameters.add(headerParam);
            }
        }

        return parameters;
    }

    private Map<String, Object> buildParameter(APIParameter param, String in) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("name", param.getName());
        p.put("in", in);
        if (!StringUtils.isEmpty(param.getDescription())) {
            p.put("description", param.getDescription());
        }
        p.put("required", "path".equals(in) || !param.getIsOptional());

        Map<String, Object> schema;
        if (param.getIsEnum() || "Enum".equals(param.getType())) {
            schema = buildEnumSchema(param.getClassPath());
        } else {
            schema = mapOpenApiType(param.getType());
        }

        if (!StringUtils.isEmpty(param.getDefaultValue())) {
            schema.put("default", param.getDefaultValue());
        }
        p.put("schema", schema);
        return p;
    }

    private Map<String, Object> buildSimpleQueryParam(String name, String type, String format, String description, boolean required) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("name", name);
        p.put("in", "query");
        p.put("description", description);
        p.put("required", required);
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", type);
        if (format != null) {
            schema.put("format", format);
        }
        p.put("schema", schema);
        return p;
    }

    private Map<String, Object> buildRequestBody(APIDefinition api) {
        if (api.getParameters() == null) return null;

        for (APIParameter param : api.getParameters()) {
            if (param.getParameterKind() == APIParameterKind.REQUEST_BODY) {
                String schemaName = toSchemaName(param.getClassPath());
                Map<String, Object> body = new LinkedHashMap<>();
                body.put("required", true);
                Map<String, Object> content = new LinkedHashMap<>();
                Map<String, Object> jsonType = new LinkedHashMap<>();
                Map<String, Object> schema = new LinkedHashMap<>();
                schema.put("$ref", "#/components/schemas/" + schemaName);
                jsonType.put("schema", schema);
                content.put("application/json", jsonType);
                body.put("content", content);
                return body;
            }
        }
        return null;
    }

    private Map<String, Object> buildResponses(APIDefinition api) {
        Map<String, Object> responses = new LinkedHashMap<>();

        if (api.getResponseStatus() != null && !api.getResponseStatus().isEmpty()) {
            for (Map.Entry<String, String> entry : api.getResponseStatus().entrySet()) {
                Map<String, Object> responseObj = new LinkedHashMap<>();
                responseObj.put("description", entry.getValue());

                // Only add schema for success responses with a return type
                if (entry.getKey().startsWith("2") && api.getReturnClass() != null
                        && !"void".equalsIgnoreCase(api.getReturnClass())) {
                    Map<String, Object> schema = buildResponseSchema(api);
                    if (schema != null) {
                        Map<String, Object> content = new LinkedHashMap<>();
                        Map<String, Object> jsonType = new LinkedHashMap<>();
                        jsonType.put("schema", schema);
                        content.put("application/json", jsonType);
                        responseObj.put("content", content);
                    }
                }

                responses.put(entry.getKey(), responseObj);
            }
        } else {
            // Default 200 response
            Map<String, Object> defaultResponse = new LinkedHashMap<>();
            defaultResponse.put("description", "성공");
            responses.put("200", defaultResponse);
        }

        return responses;
    }

    private Map<String, Object> buildResponseSchema(APIDefinition api) {
        String returnClass = api.getReturnClass();
        String pagingType = api.getEffectivePagingType();
        String schemaName = toSchemaName(returnClass);

        if (PagingType.SPRING.equals(pagingType)) {
            // Reference to wrapper schema
            Map<String, Object> ref = new LinkedHashMap<>();
            ref.put("$ref", "#/components/schemas/SpringPage_" + schemaName);
            return ref;
        } else if (PagingType.XPAGE.equals(pagingType)) {
            Map<String, Object> ref = new LinkedHashMap<>();
            ref.put("$ref", "#/components/schemas/XPage_" + schemaName);
            return ref;
        } else if (api.isArrayReturn()) {
            Map<String, Object> arraySchema = new LinkedHashMap<>();
            arraySchema.put("type", "array");
            Map<String, Object> items = new LinkedHashMap<>();
            items.put("$ref", "#/components/schemas/" + schemaName);
            arraySchema.put("items", items);
            return arraySchema;
        } else {
            Map<String, Object> ref = new LinkedHashMap<>();
            ref.put("$ref", "#/components/schemas/" + schemaName);
            return ref;
        }
    }

    private Map<String, Object> buildComponents() {
        Map<String, Object> components = new LinkedHashMap<>();

        Map<String, Object> schemas = new LinkedHashMap<>();
        Set<String> processedSet = new HashSet<>();

        // Collect all referenced models
        for (APIDefinition api : this.apiList) {
            // Request body models
            if (api.getParameters() != null) {
                for (APIParameter param : api.getParameters()) {
                    if (param.getParameterKind() == APIParameterKind.REQUEST_BODY) {
                        collectSchema(param.getClassPath(), schemas, processedSet);
                    }
                }
            }

            // Response models
            if (api.getReturnClass() != null && !"void".equalsIgnoreCase(api.getReturnClass())) {
                collectSchema(api.getReturnClass(), schemas, processedSet);

                // Paging wrapper schemas
                String pagingType = api.getEffectivePagingType();
                String schemaName = toSchemaName(api.getReturnClass());

                if (PagingType.SPRING.equals(pagingType)) {
                    String wrapperName = "SpringPage_" + schemaName;
                    if (!schemas.containsKey(wrapperName)) {
                        schemas.put(wrapperName, buildSpringPageSchema(api.getReturnClass()));
                    }
                } else if (PagingType.XPAGE.equals(pagingType)) {
                    String wrapperName = "XPage_" + schemaName;
                    if (!schemas.containsKey(wrapperName)) {
                        schemas.put(wrapperName, buildXPageSchema(api.getReturnClass()));
                    }
                }
            }

            // Enum parameters
            if (api.getParameters() != null) {
                for (APIParameter param : api.getParameters()) {
                    if (param.getIsEnum() || "Enum".equals(param.getType())) {
                        collectSchema(param.getClassPath(), schemas, processedSet);
                    }
                }
            }
        }

        components.put("schemas", schemas);

        // Security schemes
        Map<String, Object> securitySchemes = buildSecuritySchemes();
        if (!securitySchemes.isEmpty()) {
            components.put("securitySchemes", securitySchemes);
        }

        return components;
    }

    private void collectSchema(String classPath, Map<String, Object> schemas, Set<String> processedSet) {
        if (classPath == null || processedSet.contains(classPath)) return;
        processedSet.add(classPath);

        APIModelDefinition model = loadModel(classPath);
        if (model == null) return;

        String schemaName = toSchemaName(classPath);

        if ("Enum".equals(model.getType())) {
            schemas.put(schemaName, buildEnumSchemaFromModel(model));
        } else {
            schemas.put(schemaName, buildSchemaForModel(model));

            // Recurse into Object/Array/Enum fields
            if (model.getFields() != null) {
                for (APIField field : model.getFields()) {
                    if ("Object".equals(field.getType()) || "Array".equals(field.getType()) || "Enum".equals(field.getType())) {
                        collectSchema(field.getClassPath(), schemas, processedSet);
                    }
                }
            }
        }
    }

    private Map<String, Object> buildSchemaForModel(APIModelDefinition model) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");

        if (!StringUtils.isEmpty(model.getDescription())) {
            schema.put("description", model.getDescription());
        }

        Map<String, Object> properties = new LinkedHashMap<>();
        List<String> required = new ArrayList<>();

        if (model.getFields() != null) {
            for (APIField field : model.getFields()) {
                properties.put(field.getName(), buildSchemaForField(field));
                if (!field.isOptional()) {
                    required.add(field.getName());
                }
            }
        }

        schema.put("properties", properties);
        if (!required.isEmpty()) {
            schema.put("required", required);
        }

        return schema;
    }

    private Map<String, Object> buildSchemaForField(APIField field) {
        switch (field.getType()) {
            case "Object": {
                Map<String, Object> ref = new LinkedHashMap<>();
                ref.put("$ref", "#/components/schemas/" + toSchemaName(field.getClassPath()));
                if (!StringUtils.isEmpty(field.getDescription())) {
                    // $ref doesn't allow sibling properties in OpenAPI 3.0, wrap in allOf
                    Map<String, Object> wrapper = new LinkedHashMap<>();
                    wrapper.put("description", field.getDescription());
                    wrapper.put("allOf", Collections.singletonList(ref));
                    return wrapper;
                }
                return ref;
            }
            case "Array": {
                Map<String, Object> arraySchema = new LinkedHashMap<>();
                arraySchema.put("type", "array");
                if (!StringUtils.isEmpty(field.getDescription())) {
                    arraySchema.put("description", field.getDescription());
                }
                Map<String, Object> items = new LinkedHashMap<>();
                // Check if array items are a model or a primitive
                APIModelDefinition itemModel = loadModel(field.getClassPath());
                if (itemModel != null) {
                    items.put("$ref", "#/components/schemas/" + toSchemaName(field.getClassPath()));
                } else {
                    Map<String, Object> mapped = mapOpenApiType(field.getClassPath());
                    items.putAll(mapped);
                }
                arraySchema.put("items", items);
                return arraySchema;
            }
            case "Enum": {
                Map<String, Object> enumSchema = buildEnumSchema(field.getClassPath());
                if (!StringUtils.isEmpty(field.getDescription())) {
                    enumSchema.put("description", field.getDescription());
                }
                return enumSchema;
            }
            default: {
                Map<String, Object> schema = mapOpenApiType(field.getType());
                if (!StringUtils.isEmpty(field.getDescription())) {
                    schema.put("description", field.getDescription());
                }
                return schema;
            }
        }
    }

    private Map<String, Object> buildEnumSchema(String classPath) {
        APIModelDefinition model = loadModel(classPath);
        if (model != null) {
            return buildEnumSchemaFromModel(model);
        }
        // Fallback: just string type
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "string");
        return schema;
    }

    private Map<String, Object> buildEnumSchemaFromModel(APIModelDefinition model) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "string");
        if (model.getFields() != null && !model.getFields().isEmpty()) {
            List<String> values = new ArrayList<>();
            for (APIField field : model.getFields()) {
                values.add(field.getName());
            }
            schema.put("enum", values);
        }
        if (!StringUtils.isEmpty(model.getDescription())) {
            schema.put("description", model.getDescription());
        }
        return schema;
    }

    private Map<String, Object> buildSpringPageSchema(String contentClassPath) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("description", "Spring Page wrapper for " + toSchemaName(contentClassPath));

        Map<String, Object> properties = new LinkedHashMap<>();

        // content array
        Map<String, Object> contentProp = new LinkedHashMap<>();
        contentProp.put("type", "array");
        Map<String, Object> items = new LinkedHashMap<>();
        items.put("$ref", "#/components/schemas/" + toSchemaName(contentClassPath));
        contentProp.put("items", items);
        properties.put("content", contentProp);

        // Other fields from SpringPageSchema
        for (Field field : SpringPageSchema.class.getDeclaredFields()) {
            if ("content".equals(field.getName())) continue;
            properties.put(field.getName(), mapJavaFieldType(field));
        }

        // sort object
        Map<String, Object> sortSchema = new LinkedHashMap<>();
        sortSchema.put("type", "object");
        Map<String, Object> sortProps = new LinkedHashMap<>();
        for (Field sortField : SpringPageSortSchema.class.getDeclaredFields()) {
            sortProps.put(sortField.getName(), mapJavaFieldType(sortField));
        }
        sortSchema.put("properties", sortProps);
        properties.put("sort", sortSchema);

        schema.put("properties", properties);
        return schema;
    }

    private Map<String, Object> buildXPageSchema(String contentClassPath) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("description", "XPage wrapper for " + toSchemaName(contentClassPath));

        Map<String, Object> properties = new LinkedHashMap<>();

        for (Field field : XPageSchema.class.getDeclaredFields()) {
            if ("pageRows".equals(field.getName())) {
                Map<String, Object> pageRowsProp = new LinkedHashMap<>();
                pageRowsProp.put("type", "array");
                Map<String, Object> items = new LinkedHashMap<>();
                items.put("$ref", "#/components/schemas/" + toSchemaName(contentClassPath));
                pageRowsProp.put("items", items);
                properties.put("pageRows", pageRowsProp);
            } else if ("orders".equals(field.getName())) {
                Map<String, Object> ordersProp = new LinkedHashMap<>();
                ordersProp.put("type", "array");
                Map<String, Object> orderItems = new LinkedHashMap<>();
                orderItems.put("type", "string");
                ordersProp.put("items", orderItems);
                properties.put("orders", ordersProp);
            } else {
                properties.put(field.getName(), mapJavaFieldType(field));
            }
        }

        schema.put("properties", properties);
        return schema;
    }

    private Map<String, Object> buildSecuritySchemes() {
        Map<String, Object> schemes = new LinkedHashMap<>();

        if (serviceDefinition.getAuth() != null && "token".equals(serviceDefinition.getAuth().getType())) {
            Map<String, Object> apiKeyScheme = new LinkedHashMap<>();
            apiKeyScheme.put("type", "apiKey");
            apiKeyScheme.put("in", "header");
            apiKeyScheme.put("name", serviceDefinition.getAuth().getHeaderKey());
            if (!StringUtils.isEmpty(serviceDefinition.getAuth().getDescription())) {
                apiKeyScheme.put("description", serviceDefinition.getAuth().getDescription());
            }
            schemes.put("ApiKeyAuth", apiKeyScheme);
        }

        return schemes;
    }

    // --- Type mapping ---

    private Map<String, Object> mapOpenApiType(String internalType) {
        Map<String, Object> schema = new LinkedHashMap<>();

        if (internalType == null) {
            schema.put("type", "string");
            return schema;
        }

        switch (internalType) {
            case "String":
            case "java.lang.String":
                schema.put("type", "string");
                break;
            case "int":
            case "Integer":
            case "java.lang.Integer":
                schema.put("type", "integer");
                schema.put("format", "int32");
                break;
            case "long":
            case "Long":
            case "java.lang.Long":
                schema.put("type", "integer");
                schema.put("format", "int64");
                break;
            case "float":
            case "Float":
            case "java.lang.Float":
                schema.put("type", "number");
                schema.put("format", "float");
                break;
            case "double":
            case "Double":
            case "java.lang.Double":
                schema.put("type", "number");
                schema.put("format", "double");
                break;
            case "boolean":
            case "Boolean":
            case "java.lang.Boolean":
                schema.put("type", "boolean");
                break;
            case "BigDecimal":
            case "java.math.BigDecimal":
                schema.put("type", "number");
                break;
            case "Date":
            case "java.util.Date":
            case "LocalDateTime":
            case "java.time.LocalDateTime":
            case "LocalDate":
            case "java.time.LocalDate":
            case "Instant":
            case "java.time.Instant":
            case "ZonedDateTime":
            case "java.time.ZonedDateTime":
            case "OffsetDateTime":
            case "java.time.OffsetDateTime":
                schema.put("type", "string");
                schema.put("format", "date-time");
                break;
            default:
                schema.put("type", "string");
                break;
        }

        return schema;
    }

    private Map<String, Object> mapJavaFieldType(Field field) {
        Class<?> type = field.getType();
        Map<String, Object> schema = new LinkedHashMap<>();

        if (type == int.class || type == Integer.class) {
            schema.put("type", "integer");
            schema.put("format", "int32");
        } else if (type == long.class || type == Long.class) {
            schema.put("type", "integer");
            schema.put("format", "int64");
        } else if (type == boolean.class || type == Boolean.class) {
            schema.put("type", "boolean");
        } else if (type == String.class) {
            schema.put("type", "string");
        } else if (type == List.class) {
            schema.put("type", "array");
            Map<String, Object> items = new LinkedHashMap<>();
            items.put("type", "object");
            schema.put("items", items);
        } else {
            schema.put("type", "string");
        }

        return schema;
    }

    // --- Naming ---

    private String toSchemaName(String classPath) {
        if (classPath == null) return "Unknown";
        // Take last segment of FQN
        String name = classPath;
        int lastDot = classPath.lastIndexOf('.');
        if (lastDot >= 0) {
            name = classPath.substring(lastDot + 1);
        }
        // Inner class $ → _
        name = name.replace('$', '_');
        return name;
    }
}
