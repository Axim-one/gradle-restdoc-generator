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

        // Global security: all endpoints require auth by default when auth is configured
        String globalSchemeName = getSecuritySchemeName(serviceDefinition.getAuth());
        if (globalSchemeName != null) {
            root.put("security", Collections.singletonList(
                    Collections.singletonMap(globalSchemeName, Collections.emptyList())));
        }

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
            String version = serviceDefinition.getVersion();
            String path = (version != null && !version.isEmpty())
                    ? "/" + version + api.getUrlMapping()
                    : api.getUrlMapping();

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

        // Security opt-out: endpoints without @auth or with @auth false override global security
        if (!api.isNeedsSession() && getSecuritySchemeName(serviceDefinition.getAuth()) != null) {
            operation.put("security", Collections.emptyList());
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
        if (api.getHeaders() != null) {
            for (APIHeader header : api.getHeaders()) {
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

                // 성공 응답에는 반환 타입 스키마 첨부
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
                // 4xx/5xx 에러 응답에는 ApiError 스키마 자동 첨부
                else if (entry.getKey().startsWith("4") || entry.getKey().startsWith("5")) {
                    Map<String, Object> content = new LinkedHashMap<>();
                    Map<String, Object> jsonType = new LinkedHashMap<>();
                    Map<String, Object> schema = new LinkedHashMap<>();
                    schema.put("$ref", "#/components/schemas/ApiError");
                    jsonType.put("schema", schema);
                    content.put("application/json", jsonType);
                    responseObj.put("content", content);
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
            Map<String, Object> items;
            // primitive/known 타입은 인라인 매핑, 그 외는 $ref
            if (isPrimitiveOrKnownType(returnClass)) {
                items = mapOpenApiType(returnClass);
            } else {
                items = new LinkedHashMap<>();
                items.put("$ref", "#/components/schemas/" + schemaName);
            }
            arraySchema.put("items", items);
            return arraySchema;
        } else {
            // 단일 반환: primitive 타입은 인라인 매핑
            if (isPrimitiveOrKnownType(returnClass)) {
                return mapOpenApiType(returnClass);
            }
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

        // 에러 응답이 있는 경우 ApiError 스키마 자동 등록
        boolean hasErrorResponse = this.apiList.stream().anyMatch(api ->
                api.getResponseStatus() != null && api.getResponseStatus().keySet().stream()
                        .anyMatch(code -> code.startsWith("4") || code.startsWith("5")));
        if (hasErrorResponse) {
            schemas.put("ApiError", buildApiErrorSchema());
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
        // 알려진 인라인 타입은 별도 스키마로 등록하지 않음
        if (isKnownInlineType(classPath)) return;
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

    // TODO: @XApiDoc(example = "...") 어노테이션 기반 example 지원 추가 예정
    //       현재는 타입 기반 기본값만 자동 생성. 어노테이션 정의 후 명시적 example 우선 적용으로 확장.

    private Map<String, Object> buildSchemaForField(APIField field) {
        switch (field.getType()) {
            case "Object": {
                // 알려진 인라인 타입은 $ref 대신 직접 매핑 (LocalDateTime, BigDecimal 등)
                if (isKnownInlineType(field.getClassPath())) {
                    Map<String, Object> schema = mapOpenApiType(field.getClassPath());
                    if (!StringUtils.isEmpty(field.getDescription())) {
                        schema.put("description", field.getDescription());
                    }
                    applyDefaultExample(schema, field.getClassPath());
                    return schema;
                }
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
                // enum example: 첫 번째 값 사용
                @SuppressWarnings("unchecked")
                List<String> enumValues = (List<String>) enumSchema.get("enum");
                if (enumValues != null && !enumValues.isEmpty()) {
                    enumSchema.put("example", enumValues.get(0));
                }
                return enumSchema;
            }
            default: {
                Map<String, Object> schema = mapOpenApiType(field.getType());
                if (!StringUtils.isEmpty(field.getDescription())) {
                    schema.put("description", field.getDescription());
                }
                applyDefaultExample(schema, field.getType());
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
            schema.put("example", values.get(0));
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

    private String getSecuritySchemeName(APIAuthData auth) {
        if (auth == null) return null;
        String type = auth.getType();
        if ("apiKey".equals(type) || "token".equals(type)) {
            return "ApiKeyAuth";
        } else if ("http".equals(type)) {
            if ("basic".equals(auth.getScheme())) {
                return "BasicAuth";
            }
            return "BearerAuth";
        }
        return null;
    }

    private Map<String, Object> buildSecuritySchemes() {
        Map<String, Object> schemes = new LinkedHashMap<>();
        APIAuthData auth = serviceDefinition.getAuth();
        if (auth == null) return schemes;

        String schemeName = getSecuritySchemeName(auth);
        if (schemeName == null) return schemes;

        String type = auth.getType();
        Map<String, Object> scheme = new LinkedHashMap<>();

        if ("apiKey".equals(type) || "token".equals(type)) {
            scheme.put("type", "apiKey");
            scheme.put("in", StringUtils.defaultIfEmpty(auth.getIn(), "header"));
            scheme.put("name", auth.getHeaderKey());
        } else if ("http".equals(type)) {
            scheme.put("type", "http");
            scheme.put("scheme", auth.getScheme());
            if (!StringUtils.isEmpty(auth.getBearerFormat())) {
                scheme.put("bearerFormat", auth.getBearerFormat());
            }
        }

        if (!StringUtils.isEmpty(auth.getDescription())) {
            scheme.put("description", auth.getDescription());
        }

        schemes.put(schemeName, scheme);
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
            case "short":
            case "Short":
            case "java.lang.Short":
                schema.put("type", "integer");
                schema.put("format", "int32");
                break;
            case "byte":
            case "Byte":
            case "java.lang.Byte":
                schema.put("type", "integer");
                schema.put("format", "int32");
                break;
            case "char":
            case "Character":
            case "java.lang.Character":
                schema.put("type", "string");
                break;
            case "boolean":
            case "Boolean":
            case "java.lang.Boolean":
                schema.put("type", "boolean");
                break;
            case "Number":
            case "java.lang.Number":
                schema.put("type", "number");
                break;
            case "BigDecimal":
            case "java.math.BigDecimal":
                schema.put("type", "number");
                schema.put("format", "decimal");
                break;
            case "LocalDate":
            case "java.time.LocalDate":
                schema.put("type", "string");
                schema.put("format", "date");
                break;
            case "Date":
            case "java.util.Date":
            case "LocalDateTime":
            case "java.time.LocalDateTime":
            case "Instant":
            case "java.time.Instant":
            case "ZonedDateTime":
            case "java.time.ZonedDateTime":
            case "OffsetDateTime":
            case "java.time.OffsetDateTime":
                schema.put("type", "string");
                schema.put("format", "date-time");
                break;
            case "LocalTime":
            case "java.time.LocalTime":
                schema.put("type", "string");
                schema.put("format", "time");
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

    private Map<String, Object> buildApiErrorSchema() {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("description", "API 에러 응답");

        Map<String, Object> properties = new LinkedHashMap<>();

        Map<String, Object> codeProp = new LinkedHashMap<>();
        codeProp.put("type", "string");
        codeProp.put("description", "에러 코드");
        properties.put("code", codeProp);

        Map<String, Object> messageProp = new LinkedHashMap<>();
        messageProp.put("type", "string");
        messageProp.put("description", "에러 메시지");
        properties.put("message", messageProp);

        Map<String, Object> statusProp = new LinkedHashMap<>();
        statusProp.put("type", "integer");
        statusProp.put("description", "HTTP 상태 코드");
        properties.put("status", statusProp);

        schema.put("properties", properties);
        return schema;
    }

    /**
     * primitive, wrapper, 날짜, BigDecimal 등 model이 아닌 알려진 타입인지 확인한다.
     * 이들은 $ref가 아닌 인라인 type/format으로 매핑해야 한다.
     */
    private boolean isPrimitiveOrKnownType(String classPath) {
        if (classPath == null) return false;
        return classPath.startsWith("java.lang.")
                || classPath.startsWith("java.math.")
                || classPath.startsWith("java.time.")
                || classPath.equals("java.util.Date")
                || classPath.equals("int") || classPath.equals("long")
                || classPath.equals("float") || classPath.equals("double")
                || classPath.equals("boolean") || classPath.equals("short")
                || classPath.equals("byte") || classPath.equals("char");
    }

    /**
     * 알려진 Java 타입을 인라인 OpenAPI 타입으로 변환할 수 있는지 확인한다.
     * LocalDateTime, BigDecimal 등은 별도 $ref 스키마 대신 직접 타입/포맷으로 매핑된다.
     */
    private boolean isKnownInlineType(String classPath) {
        if (classPath == null) return false;
        return classPath.startsWith("java.time.")
                || classPath.equals("java.util.Date")
                || classPath.equals("java.math.BigDecimal");
    }

    /**
     * 타입 기반 기본 example 값을 스키마에 추가한다.
     * TODO: @XApiDoc(example = "...") 어노테이션 지원 시, 명시적 값이 있으면 그것을 우선 사용하도록 확장
     */
    private void applyDefaultExample(Map<String, Object> schema, String typeName) {
        if (typeName == null) return;
        switch (typeName) {
            case "String":
            case "java.lang.String":
                // String은 필드명에 따라 달라지므로 빈 문자열은 넣지 않음
                break;
            case "int":
            case "Integer":
            case "java.lang.Integer":
                schema.put("example", 1);
                break;
            case "long":
            case "Long":
            case "java.lang.Long":
                schema.put("example", 1);
                break;
            case "short":
            case "Short":
            case "java.lang.Short":
            case "byte":
            case "Byte":
            case "java.lang.Byte":
                schema.put("example", 1);
                break;
            case "char":
            case "Character":
            case "java.lang.Character":
                schema.put("example", "A");
                break;
            case "boolean":
            case "Boolean":
            case "java.lang.Boolean":
                schema.put("example", true);
                break;
            case "Number":
            case "java.lang.Number":
                schema.put("example", 0);
                break;
            case "float":
            case "Float":
            case "java.lang.Float":
            case "double":
            case "Double":
            case "java.lang.Double":
                schema.put("example", 0.0);
                break;
            case "BigDecimal":
            case "java.math.BigDecimal":
                schema.put("example", "100.00");
                break;
            case "LocalDateTime":
            case "java.time.LocalDateTime":
            case "Instant":
            case "java.time.Instant":
            case "ZonedDateTime":
            case "java.time.ZonedDateTime":
            case "OffsetDateTime":
            case "java.time.OffsetDateTime":
            case "Date":
            case "java.util.Date":
                schema.put("example", "2026-01-01T00:00:00");
                break;
            case "LocalDate":
            case "java.time.LocalDate":
                schema.put("example", "2026-01-01");
                break;
            case "LocalTime":
            case "java.time.LocalTime":
                schema.put("example", "12:00:00");
                break;
            default:
                break;
        }
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
