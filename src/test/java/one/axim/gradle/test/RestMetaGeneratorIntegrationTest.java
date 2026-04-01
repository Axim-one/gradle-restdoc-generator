package one.axim.gradle.test;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import one.axim.gradle.data.PagingType;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.*;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class RestMetaGeneratorIntegrationTest {

    private static final Gson gson = new Gson();
    private static Path tempDir;
    private static BuildResult buildResult;

    @BeforeAll
    static void setUp() throws Exception {
        // 테스트 프로젝트를 임시 디렉토리로 복사
        tempDir = Files.createTempDirectory("restdoc-test-");
        Path source = Paths.get(RestMetaGeneratorIntegrationTest.class.getClassLoader()
                .getResource("test-project").toURI());
        copyDirectory(source, tempDir);

        // restMetaGenerator 태스크 실행
        buildResult = GradleRunner.create()
                .withProjectDir(tempDir.toFile())
                .withArguments("restMetaGenerator", "--stacktrace")
                .withPluginClasspath()
                .forwardOutput()
                .build();
    }

    @AfterAll
    static void tearDown() throws Exception {
        if (tempDir != null) {
            deleteDirectory(tempDir);
        }
    }

    @Test
    void testTaskSucceeds() {
        assertEquals(TaskOutcome.SUCCESS,
                buildResult.task(":restMetaGenerator").getOutcome());
    }

    @Test
    void testApiJsonGenerated() {
        Path apiDir = tempDir.resolve("build/docs/api");
        assertTrue(Files.isDirectory(apiDir), "api directory should exist");

        File[] jsonFiles = apiDir.toFile().listFiles((dir, name) -> name.endsWith(".json"));
        assertNotNull(jsonFiles);
        assertTrue(jsonFiles.length > 0, "at least one API JSON file should be generated");

        // SampleController에 대한 JSON 파일 확인
        boolean found = false;
        for (File f : jsonFiles) {
            if (f.getName().contains("SampleController")) {
                found = true;
                break;
            }
        }
        assertTrue(found, "SampleController API JSON should exist");
    }

    @Test
    void testModelJsonGenerated() {
        Path modelDir = tempDir.resolve("build/docs/model");
        assertTrue(Files.isDirectory(modelDir), "model directory should exist");

        File[] jsonFiles = modelDir.toFile().listFiles((dir, name) -> name.endsWith(".json"));
        assertNotNull(jsonFiles);
        assertTrue(jsonFiles.length > 0, "at least one model JSON file should be generated");
    }

    @Test
    void testInheritedFieldsInModel() throws Exception {
        Path modelDir = tempDir.resolve("build/docs/model");
        File userDtoJson = findFile(modelDir, "UserDto");
        assertNotNull(userDtoJson, "UserDto model JSON should exist");

        String json = Files.readString(userDtoJson.toPath());
        Map<String, Object> model = gson.fromJson(json, Map.class);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fields = (List<Map<String, Object>>) model.get("fields");
        assertNotNull(fields, "fields should not be null");

        List<String> fieldNames = fields.stream()
                .map(f -> (String) f.get("name"))
                .toList();

        // UserDto 자체 필드
        assertTrue(fieldNames.contains("name"), "should contain UserDto.name");
        // BaseDto (부모) 상속 필드
        assertTrue(fieldNames.contains("id"), "should contain inherited BaseDto.id");
        assertTrue(fieldNames.contains("createdAt"), "should contain inherited BaseDto.createdAt");
    }

    @Test
    void testEnumModelGenerated() throws Exception {
        Path modelDir = tempDir.resolve("build/docs/model");
        File enumJson = findFile(modelDir, "UserStatus");
        assertNotNull(enumJson, "UserStatus enum model JSON should exist");

        String json = Files.readString(enumJson.toPath());
        Map<String, Object> model = gson.fromJson(json, Map.class);

        assertEquals("Enum", model.get("type"), "UserStatus type should be 'Enum'");
    }

    @Test
    void testSpringPageApiDefinition() throws Exception {
        Path apiDir = tempDir.resolve("build/docs/api");
        File controllerJson = findFile(apiDir, "SampleController");
        assertNotNull(controllerJson, "SampleController API JSON should exist");

        String json = Files.readString(controllerJson.toPath());
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> apis = gson.fromJson(json, listType);

        // getUsersPaged 메서드 찾기
        Map<String, Object> pagedApi = null;
        for (Map<String, Object> api : apis) {
            if ("사용자 페이징 조회".equals(api.get("name"))) {
                pagedApi = api;
                break;
            }
        }
        assertNotNull(pagedApi, "사용자 페이징 조회 API should exist");

        // isPaging=true, pagingType=spring 확인
        assertTrue((Boolean) pagedApi.get("isPaging"), "isPaging should be true");
        assertEquals(PagingType.SPRING, pagedApi.get("pagingType"), "pagingType should be 'spring'");

        // page, size, sort 파라미터 확인
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> params = (List<Map<String, Object>>) pagedApi.get("parameters");
        assertNotNull(params, "parameters should not be null");

        List<String> paramNames = params.stream()
                .map(p -> (String) p.get("name"))
                .toList();
        assertTrue(paramNames.contains("page"), "should contain page parameter");
        assertTrue(paramNames.contains("size"), "should contain size parameter");
        assertTrue(paramNames.contains("sort"), "should contain sort parameter");

        // classPath가 FQN인지 확인
        for (Map<String, Object> param : params) {
            String name = (String) param.get("name");
            String classPath = (String) param.get("classPath");
            if ("page".equals(name) || "size".equals(name)) {
                assertEquals("java.lang.Integer", classPath, name + " classPath should be FQN");
            } else if ("sort".equals(name)) {
                assertEquals("java.lang.String", classPath, "sort classPath should be FQN");
            }
        }
    }

    @Test
    void testSpringPageReturnClassIsContentType() throws Exception {
        Path apiDir = tempDir.resolve("build/docs/api");
        File controllerJson = findFile(apiDir, "SampleController");
        assertNotNull(controllerJson);

        String json = Files.readString(controllerJson.toPath());
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> apis = gson.fromJson(json, listType);

        Map<String, Object> pagedApi = null;
        for (Map<String, Object> api : apis) {
            if ("사용자 페이징 조회".equals(api.get("name"))) {
                pagedApi = api;
                break;
            }
        }
        assertNotNull(pagedApi);

        // returnClass가 Page가 아닌 내부 타입(UserDto)이어야 함
        String returnClass = (String) pagedApi.get("returnClass");
        assertTrue(returnClass.contains("UserDto"),
                "returnClass should be the content type (UserDto), not Page. Got: " + returnClass);
        assertFalse(returnClass.contains("Page"),
                "returnClass should NOT contain 'Page'. Got: " + returnClass);
    }

    @Test
    void testInnerClassEnumParameter() throws Exception {
        Path apiDir = tempDir.resolve("build/docs/api");
        File controllerJson = findFile(apiDir, "SampleController");
        assertNotNull(controllerJson, "SampleController API JSON should exist");

        String json = Files.readString(controllerJson.toPath());
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> apis = gson.fromJson(json, listType);

        // 주문 상태별 조회 메서드 찾기
        Map<String, Object> orderApi = null;
        for (Map<String, Object> api : apis) {
            if ("주문 상태별 조회".equals(api.get("name"))) {
                orderApi = api;
                break;
            }
        }
        assertNotNull(orderApi, "주문 상태별 조회 API should exist (inner class enum should not cause failure)");

        // status 파라미터가 enum으로 처리되었는지 확인
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> params = (List<Map<String, Object>>) orderApi.get("parameters");
        assertNotNull(params, "parameters should not be null");

        Map<String, Object> statusParam = null;
        for (Map<String, Object> param : params) {
            if ("status".equals(param.get("name"))) {
                statusParam = param;
                break;
            }
        }
        assertNotNull(statusParam, "status parameter should exist");
        assertTrue(((Boolean) statusParam.get("isEnum")), "status should be recognized as enum");
    }

    @Test
    void testAllApiMethodsProcessed() throws Exception {
        // Bug 2 검증: 모든 메서드가 처리되었는지 확인 (한 메서드 실패로 나머지가 스킵되지 않음)
        Path apiDir = tempDir.resolve("build/docs/api");
        File controllerJson = findFile(apiDir, "SampleController");
        assertNotNull(controllerJson);

        String json = Files.readString(controllerJson.toPath());
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> apis = gson.fromJson(json, listType);

        List<String> apiNames = apis.stream()
                .map(api -> (String) api.get("name"))
                .toList();

        // 모든 API 메서드가 생성되었는지 확인
        assertTrue(apiNames.contains("사용자 목록 조회"), "사용자 목록 조회 should be processed");
        assertTrue(apiNames.contains("사용자 상세 조회"), "사용자 상세 조회 should be processed");
        assertTrue(apiNames.contains("사용자 상태 조회"), "사용자 상태 조회 should be processed");
        assertTrue(apiNames.contains("사용자 페이징 조회"), "사용자 페이징 조회 should be processed");
        assertTrue(apiNames.contains("주문 상태별 조회"), "주문 상태별 조회 should be processed");
        assertTrue(apiNames.contains("사용자 단건 조회 래핑"), "사용자 단건 조회 래핑 should be processed");
        assertTrue(apiNames.contains("사용자 목록 조회 래핑"), "사용자 목록 조회 래핑 should be processed");
        assertTrue(apiNames.contains("사용자 페이징 조회 래핑"), "사용자 페이징 조회 래핑 should be processed");
        assertTrue(apiNames.contains("사용자 검색"), "사용자 검색 should be processed");
        assertTrue(apiNames.contains("파트너 조회"), "파트너 조회 should be processed");
    }

    @Test
    void testApiResultWrappedSingleObject() throws Exception {
        // ApiResult<UserDto> → returnClass가 UserDto여야 함
        Path apiDir = tempDir.resolve("build/docs/api");
        File controllerJson = findFile(apiDir, "SampleController");
        assertNotNull(controllerJson);

        String json = Files.readString(controllerJson.toPath());
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> apis = gson.fromJson(json, listType);

        Map<String, Object> api = findApiByName(apis, "사용자 단건 조회 래핑");
        assertNotNull(api, "사용자 단건 조회 래핑 API should exist");

        String returnClass = (String) api.get("returnClass");
        assertTrue(returnClass.contains("UserDto"),
                "returnClass should be UserDto, not ApiResult. Got: " + returnClass);
        assertFalse(returnClass.contains("ApiResult"),
                "returnClass should NOT contain ApiResult. Got: " + returnClass);
    }

    @Test
    void testApiResultWrappedList() throws Exception {
        // ApiResult<List<UserDto>> → returnClass가 UserDto이고 arrayReturn이 true
        Path apiDir = tempDir.resolve("build/docs/api");
        File controllerJson = findFile(apiDir, "SampleController");
        assertNotNull(controllerJson);

        String json = Files.readString(controllerJson.toPath());
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> apis = gson.fromJson(json, listType);

        Map<String, Object> api = findApiByName(apis, "사용자 목록 조회 래핑");
        assertNotNull(api, "사용자 목록 조회 래핑 API should exist");

        String returnClass = (String) api.get("returnClass");
        assertTrue(returnClass.contains("UserDto"),
                "returnClass should be UserDto. Got: " + returnClass);
        assertTrue((Boolean) api.get("isArrayReturn"),
                "isArrayReturn should be true for ApiResult<List<UserDto>>");
    }

    @Test
    void testApiResultWrappedPage() throws Exception {
        // ApiResult<Page<UserDto>> → returnClass가 UserDto이고 isPaging=true, pagingType=spring
        Path apiDir = tempDir.resolve("build/docs/api");
        File controllerJson = findFile(apiDir, "SampleController");
        assertNotNull(controllerJson);

        String json = Files.readString(controllerJson.toPath());
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> apis = gson.fromJson(json, listType);

        Map<String, Object> api = findApiByName(apis, "사용자 페이징 조회 래핑");
        assertNotNull(api, "사용자 페이징 조회 래핑 API should exist");

        String returnClass = (String) api.get("returnClass");
        assertTrue(returnClass.contains("UserDto"),
                "returnClass should be UserDto. Got: " + returnClass);
        assertFalse(returnClass.contains("Page"),
                "returnClass should NOT contain Page. Got: " + returnClass);
        assertTrue((Boolean) api.get("isPaging"),
                "isPaging should be true for ApiResult<Page<UserDto>>");
        assertEquals(PagingType.SPRING, api.get("pagingType"),
                "pagingType should be 'spring'");
    }

    @Test
    void testServiceJsonGenerated() {
        Path docsDir = tempDir.resolve("build/docs");
        File[] jsonFiles = docsDir.toFile().listFiles(
                (dir, name) -> name.endsWith(".json"));
        assertNotNull(jsonFiles);

        boolean found = false;
        for (File f : jsonFiles) {
            if (f.getName().contains("test-service")) {
                found = true;
                break;
            }
        }
        assertTrue(found, "Service JSON (test-service.json) should exist in docs root");
    }

    // --- OpenAPI tests ---

    @Test
    void testOpenApiJsonGenerated() {
        Path openapiFile = tempDir.resolve("build/docs/openapi.json");
        assertTrue(Files.exists(openapiFile), "openapi.json should be generated");
    }

    @Test
    void testOpenApiStructure() throws Exception {
        Map<String, Object> spec = readOpenApiJson();

        assertEquals("3.0.3", spec.get("openapi"), "openapi version should be 3.0.3");

        @SuppressWarnings("unchecked")
        Map<String, Object> info = (Map<String, Object>) spec.get("info");
        assertNotNull(info, "info should not be null");
        assertEquals("Test Service", info.get("title"), "info.title should match serviceName");
        assertEquals("v1.0", info.get("version"), "info.version should match serviceVersion");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> servers = (List<Map<String, Object>>) spec.get("servers");
        assertNotNull(servers, "servers should not be null");
        assertFalse(servers.isEmpty(), "servers should not be empty");
        assertEquals("http://localhost:8080", servers.get(0).get("url"), "server url should match apiServerUrl");
    }

    @Test
    void testOpenApiPaths() throws Exception {
        Map<String, Object> spec = readOpenApiJson();

        @SuppressWarnings("unchecked")
        Map<String, Object> paths = (Map<String, Object>) spec.get("paths");
        assertNotNull(paths, "paths should not be null");
        assertFalse(paths.isEmpty(), "paths should not be empty");

        // Check that at least one path with a path parameter exists
        boolean hasPathParam = false;
        for (Map.Entry<String, Object> entry : paths.entrySet()) {
            if (entry.getKey().contains("{")) {
                hasPathParam = true;

                @SuppressWarnings("unchecked")
                Map<String, Object> pathItem = (Map<String, Object>) entry.getValue();
                // Find the operation (get, post, etc.)
                for (Object opObj : pathItem.values()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> operation = (Map<String, Object>) opObj;
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> parameters = (List<Map<String, Object>>) operation.get("parameters");
                    if (parameters != null) {
                        boolean foundPathIn = parameters.stream()
                                .anyMatch(p -> "path".equals(p.get("in")));
                        assertTrue(foundPathIn, "path parameter should have in=path");
                    }
                }
                break;
            }
        }
        assertTrue(hasPathParam, "at least one path should contain a path parameter");
    }

    @Test
    void testOpenApiComponentSchemas() throws Exception {
        Map<String, Object> spec = readOpenApiJson();

        @SuppressWarnings("unchecked")
        Map<String, Object> components = (Map<String, Object>) spec.get("components");
        assertNotNull(components, "components should not be null");

        @SuppressWarnings("unchecked")
        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");
        assertNotNull(schemas, "schemas should not be null");

        // UserDto schema should exist
        assertTrue(schemas.containsKey("UserDto"), "UserDto schema should exist");

        @SuppressWarnings("unchecked")
        Map<String, Object> userSchema = (Map<String, Object>) schemas.get("UserDto");
        assertEquals("object", userSchema.get("type"), "UserDto should be type object");

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) userSchema.get("properties");
        assertNotNull(properties, "UserDto properties should not be null");
        assertTrue(properties.containsKey("name"), "UserDto should have name property");

        // Enum schema should exist (UserStatus or OrderDto_OrderStatus)
        boolean hasEnumSchema = schemas.keySet().stream()
                .anyMatch(key -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> s = (Map<String, Object>) schemas.get(key);
                    return s.containsKey("enum");
                });
        assertTrue(hasEnumSchema, "at least one enum schema should exist in components");
    }

    @Test
    void testOpenApiPaginationSchema() throws Exception {
        Map<String, Object> spec = readOpenApiJson();

        @SuppressWarnings("unchecked")
        Map<String, Object> components = (Map<String, Object>) spec.get("components");
        @SuppressWarnings("unchecked")
        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");

        // SpringPage_UserDto wrapper schema should exist
        assertTrue(schemas.containsKey("SpringPage_UserDto"),
                "SpringPage_UserDto schema should exist for paged APIs");

        @SuppressWarnings("unchecked")
        Map<String, Object> pageSchema = (Map<String, Object>) schemas.get("SpringPage_UserDto");
        assertEquals("object", pageSchema.get("type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) pageSchema.get("properties");
        assertNotNull(props, "SpringPage schema should have properties");
        assertTrue(props.containsKey("content"), "should have content property");
        assertTrue(props.containsKey("totalElements"), "should have totalElements property");
        assertTrue(props.containsKey("totalPages"), "should have totalPages property");
        assertTrue(props.containsKey("sort"), "should have sort property");

        // content should be an array referencing UserDto
        @SuppressWarnings("unchecked")
        Map<String, Object> contentProp = (Map<String, Object>) props.get("content");
        assertEquals("array", contentProp.get("type"), "content should be type array");

        @SuppressWarnings("unchecked")
        Map<String, Object> items = (Map<String, Object>) contentProp.get("items");
        assertNotNull(items, "content items should not be null");
        assertTrue(((String) items.get("$ref")).contains("UserDto"),
                "content items should reference UserDto");
    }

    @Test
    void testOpenApiArrayReturn() throws Exception {
        Map<String, Object> spec = readOpenApiJson();

        @SuppressWarnings("unchecked")
        Map<String, Object> paths = (Map<String, Object>) spec.get("paths");

        // Find the list endpoint (사용자 목록 조회) - should have array response
        boolean foundArrayResponse = false;
        for (Object pathItemObj : paths.values()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> pathItem = (Map<String, Object>) pathItemObj;
            for (Object opObj : pathItem.values()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> operation = (Map<String, Object>) opObj;
                @SuppressWarnings("unchecked")
                Map<String, Object> responses = (Map<String, Object>) operation.get("responses");
                if (responses == null) continue;

                for (Object respObj : responses.values()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> resp = (Map<String, Object>) respObj;
                    @SuppressWarnings("unchecked")
                    Map<String, Object> content = (Map<String, Object>) resp.get("content");
                    if (content == null) continue;

                    @SuppressWarnings("unchecked")
                    Map<String, Object> jsonContent = (Map<String, Object>) content.get("application/json");
                    if (jsonContent == null) continue;

                    @SuppressWarnings("unchecked")
                    Map<String, Object> schema = (Map<String, Object>) jsonContent.get("schema");
                    if (schema != null && "array".equals(schema.get("type"))) {
                        foundArrayResponse = true;
                        @SuppressWarnings("unchecked")
                        Map<String, Object> arrayItems = (Map<String, Object>) schema.get("items");
                        assertNotNull(arrayItems, "array items should not be null");
                        assertNotNull(arrayItems.get("$ref"), "array items should have $ref");
                        break;
                    }
                }
                if (foundArrayResponse) break;
            }
            if (foundArrayResponse) break;
        }
        assertTrue(foundArrayResponse, "at least one endpoint should have an array response schema");
    }

    // --- Spec Bundle tests ---

    @Test
    void testSpecBundleGenerated() {
        Path bundleFile = tempDir.resolve("build/docs/spec-bundle.json");
        assertTrue(Files.exists(bundleFile), "spec-bundle.json should be generated");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSpecBundleStructure() throws Exception {
        Map<String, Object> bundle = readSpecBundle();

        // service
        Map<String, Object> service = (Map<String, Object>) bundle.get("service");
        assertNotNull(service, "service should not be null");
        assertEquals("test-service", service.get("serviceId"));
        assertEquals("Test Service", service.get("name"));
        assertEquals("http://localhost:8080", service.get("apiServerUrl"));

        // apis
        List<Map<String, Object>> apis = (List<Map<String, Object>>) bundle.get("apis");
        assertNotNull(apis, "apis should not be null");
        assertTrue(apis.size() >= 12, "should have at least 12 APIs, got: " + apis.size());

        // models
        Map<String, Object> models = (Map<String, Object>) bundle.get("models");
        assertNotNull(models, "models should not be null");
        assertTrue(models.containsKey("com.example.dto.UserDto"), "should contain UserDto model");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSpecBundleModelsContainEnums() throws Exception {
        Map<String, Object> bundle = readSpecBundle();
        Map<String, Object> models = (Map<String, Object>) bundle.get("models");

        boolean hasEnum = models.values().stream().anyMatch(m -> {
            Map<String, Object> model = (Map<String, Object>) m;
            return "Enum".equals(model.get("type"));
        });
        assertTrue(hasEnum, "models should contain at least one Enum type");
    }

    // --- Error Code tests ---

    @Test
    void testErrorJsonGenerated() {
        Path errorsFile = tempDir.resolve("build/docs/error/errors.json");
        assertTrue(Files.exists(errorsFile), "error/errors.json should be generated");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testErrorGroups() throws Exception {
        List<Map<String, Object>> errors = readErrorsJson();
        assertEquals(2, errors.size(), "should have 2 error groups (AuthException, UserNotFoundException)");

        List<String> exceptionNames = errors.stream()
                .map(g -> (String) g.get("exception"))
                .sorted()
                .toList();
        assertEquals(List.of("AuthException", "UserNotFoundException"), exceptionNames);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testErrorCodesInGroup() throws Exception {
        List<Map<String, Object>> errors = readErrorsJson();

        Map<String, Object> userGroup = errors.stream()
                .filter(g -> "UserNotFoundException".equals(g.get("exception")))
                .findFirst().orElse(null);
        assertNotNull(userGroup, "UserNotFoundException group should exist");

        List<Map<String, Object>> codes = (List<Map<String, Object>>) userGroup.get("codes");
        assertNotNull(codes, "codes should not be null");
        assertEquals(2, codes.size(), "UserNotFoundException should have 2 error codes");

        List<String> codeValues = codes.stream()
                .map(c -> (String) c.get("code"))
                .sorted()
                .toList();
        assertTrue(codeValues.contains("USER_001"), "should contain USER_001");
        assertTrue(codeValues.contains("USER_002"), "should contain USER_002");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testErrorHttpStatus() throws Exception {
        List<Map<String, Object>> errors = readErrorsJson();

        for (Map<String, Object> group : errors) {
            String exception = (String) group.get("exception");
            int status = ((Number) group.get("status")).intValue();
            if ("UserNotFoundException".equals(exception)) {
                assertEquals(404, status, "UserNotFoundException should have status 404");
            } else if ("AuthException".equals(exception)) {
                assertEquals(401, status, "AuthException should have status 401");
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void testErrorMessageResolution() throws Exception {
        List<Map<String, Object>> errors = readErrorsJson();

        Map<String, Object> userGroup = errors.stream()
                .filter(g -> "UserNotFoundException".equals(g.get("exception")))
                .findFirst().orElse(null);
        assertNotNull(userGroup);

        List<Map<String, Object>> codes = (List<Map<String, Object>>) userGroup.get("codes");
        Map<String, Object> userNotFound = codes.stream()
                .filter(c -> "USER_001".equals(c.get("code")))
                .findFirst().orElse(null);
        assertNotNull(userNotFound, "USER_001 code should exist");

        String message = (String) userNotFound.get("message");
        assertNotNull(message, "message should not be null");
        assertNotEquals(userNotFound.get("messageKey"), message,
                "message should be resolved, not the raw messageKey");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSpecBundleContainsErrors() throws Exception {
        Map<String, Object> bundle = readSpecBundle();

        List<Map<String, Object>> errors = (List<Map<String, Object>>) bundle.get("errors");
        assertNotNull(errors, "spec-bundle should contain errors");
        assertFalse(errors.isEmpty(), "errors should not be empty");
        assertEquals(2, errors.size(), "should have 2 error groups in spec-bundle");
    }

    // --- @error / throws → API errors tests ---

    @Test
    @SuppressWarnings("unchecked")
    void testErrorTagPopulatesResponseStatus() throws Exception {
        // @error UserNotFoundException → responseStatus에 "404" 포함
        Path apiDir = tempDir.resolve("build/docs/api");
        File controllerJson = findFile(apiDir, "SampleController");
        assertNotNull(controllerJson);

        String json = Files.readString(controllerJson.toPath());
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> apis = gson.fromJson(json, listType);

        Map<String, Object> api = findApiByName(apis, "사용자 상세 조회");
        assertNotNull(api, "사용자 상세 조회 API should exist");

        Map<String, String> responseStatus = (Map<String, String>) api.get("responseStatus");
        assertNotNull(responseStatus, "responseStatus should not be null");
        assertTrue(responseStatus.containsKey("404"),
                "responseStatus should contain 404 from @error UserNotFoundException");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testErrorTagPopulatesErrorsList() throws Exception {
        // @error UserNotFoundException → errors 필드에 ErrorGroupDefinition 포함
        Path apiDir = tempDir.resolve("build/docs/api");
        File controllerJson = findFile(apiDir, "SampleController");
        assertNotNull(controllerJson);

        String json = Files.readString(controllerJson.toPath());
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> apis = gson.fromJson(json, listType);

        Map<String, Object> api = findApiByName(apis, "사용자 상세 조회");
        assertNotNull(api);

        List<Map<String, Object>> errors = (List<Map<String, Object>>) api.get("errors");
        assertNotNull(errors, "errors field should not be null for @error tagged method");
        assertFalse(errors.isEmpty(), "errors should not be empty");

        boolean hasUserNotFound = errors.stream()
                .anyMatch(e -> "UserNotFoundException".equals(e.get("exception")));
        assertTrue(hasUserNotFound, "errors should contain UserNotFoundException");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testThrowsAutoDetection() throws Exception {
        // throws AuthException → errors에 AuthException 포함, responseStatus에 "401"
        Path apiDir = tempDir.resolve("build/docs/api");
        File controllerJson = findFile(apiDir, "SampleController");
        assertNotNull(controllerJson);

        String json = Files.readString(controllerJson.toPath());
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> apis = gson.fromJson(json, listType);

        Map<String, Object> api = findApiByName(apis, "사용자 상태 조회");
        assertNotNull(api, "사용자 상태 조회 API should exist");

        List<Map<String, Object>> errors = (List<Map<String, Object>>) api.get("errors");
        assertNotNull(errors, "errors should not be null for method with throws clause");

        boolean hasAuth = errors.stream()
                .anyMatch(e -> "AuthException".equals(e.get("exception")));
        assertTrue(hasAuth, "errors should contain AuthException from throws clause");

        Map<String, String> responseStatus = (Map<String, String>) api.get("responseStatus");
        assertNotNull(responseStatus);
        assertTrue(responseStatus.containsKey("401"),
                "responseStatus should contain 401 from throws AuthException");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testMethodWithoutErrorsHasNoErrorsField() throws Exception {
        // 에러 없는 메서드는 errors=null
        Path apiDir = tempDir.resolve("build/docs/api");
        File controllerJson = findFile(apiDir, "SampleController");
        assertNotNull(controllerJson);

        String json = Files.readString(controllerJson.toPath());
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> apis = gson.fromJson(json, listType);

        Map<String, Object> api = findApiByName(apis, "사용자 목록 조회");
        assertNotNull(api, "사용자 목록 조회 API should exist");

        assertNull(api.get("errors"),
                "errors should be null for method without @error or throws");
    }

    // --- Error Response tests ---

    @Test
    void testErrorResponseJsonGenerated() {
        Path errorResponseFile = tempDir.resolve("build/docs/error/error-response.json");
        assertTrue(Files.exists(errorResponseFile), "error/error-response.json should be generated");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testErrorResponseFields() throws Exception {
        Path errorResponseFile = tempDir.resolve("build/docs/error/error-response.json");
        String json = Files.readString(errorResponseFile);
        Map<String, Object> model = gson.fromJson(json, Map.class);

        assertEquals("ApiErrorResponse", model.get("name"),
                "error response model name should be ApiErrorResponse");

        List<Map<String, Object>> fields = (List<Map<String, Object>>) model.get("fields");
        assertNotNull(fields, "fields should not be null");

        List<String> fieldNames = fields.stream()
                .map(f -> (String) f.get("name"))
                .toList();
        assertTrue(fieldNames.contains("status"), "should contain status field");
        assertTrue(fieldNames.contains("code"), "should contain code field");
        assertTrue(fieldNames.contains("message"), "should contain message field");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSpecBundleContainsErrorResponse() throws Exception {
        Map<String, Object> bundle = readSpecBundle();

        Map<String, Object> errorResponse = (Map<String, Object>) bundle.get("errorResponse");
        assertNotNull(errorResponse, "spec-bundle should contain errorResponse");
        assertEquals("ApiErrorResponse", errorResponse.get("name"),
                "errorResponse name should be ApiErrorResponse");

        List<Map<String, Object>> fields = (List<Map<String, Object>>) errorResponse.get("fields");
        assertNotNull(fields, "errorResponse fields should not be null");

        List<String> fieldNames = fields.stream()
                .map(f -> (String) f.get("name"))
                .toList();
        assertTrue(fieldNames.contains("status"), "should contain status field");
        assertTrue(fieldNames.contains("code"), "should contain code field");
        assertTrue(fieldNames.contains("message"), "should contain message field");
    }

    // --- Item 1: @RequestParam 복합 객체 → 개별 쿼리 파라미터 전개 ---

    @Test
    @SuppressWarnings("unchecked")
    void testComplexQueryParameterExpansion() throws Exception {
        Path apiDir = tempDir.resolve("build/docs/api");
        File controllerJson = findFile(apiDir, "SampleController");
        assertNotNull(controllerJson);

        String json = Files.readString(controllerJson.toPath());
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> apis = gson.fromJson(json, listType);

        Map<String, Object> api = findApiByName(apis, "사용자 검색");
        assertNotNull(api, "사용자 검색 API should exist");

        List<Map<String, Object>> params = (List<Map<String, Object>>) api.get("parameters");
        assertNotNull(params, "parameters should not be null");

        List<String> paramNames = params.stream()
                .map(p -> (String) p.get("name"))
                .toList();

        // UserSearchRequest의 필드들이 개별 쿼리 파라미터로 전개되어야 함
        assertTrue(paramNames.contains("name"), "should contain 'name' query param from UserSearchRequest");
        assertTrue(paramNames.contains("status"), "should contain 'status' query param from UserSearchRequest");
        assertTrue(paramNames.contains("networkId"), "should contain 'networkId' query param from UserSearchRequest");
        assertTrue(paramNames.contains("active"), "should contain 'active' query param from UserSearchRequest");

        // "search"라는 단일 Object 파라미터가 아닌 개별 필드로 전개됨을 확인
        boolean hasSearchObject = paramNames.contains("search");
        assertFalse(hasSearchObject, "should NOT have single 'search' Object parameter");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testComplexQueryParamEnumDetection() throws Exception {
        Path apiDir = tempDir.resolve("build/docs/api");
        File controllerJson = findFile(apiDir, "SampleController");
        assertNotNull(controllerJson);

        String json = Files.readString(controllerJson.toPath());
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> apis = gson.fromJson(json, listType);

        Map<String, Object> api = findApiByName(apis, "사용자 검색");
        assertNotNull(api);

        List<Map<String, Object>> params = (List<Map<String, Object>>) api.get("parameters");
        Map<String, Object> statusParam = params.stream()
                .filter(p -> "status".equals(p.get("name")))
                .findFirst().orElse(null);
        assertNotNull(statusParam, "status parameter should exist");
        assertTrue((Boolean) statusParam.get("isEnum"), "status field should be detected as enum");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testComplexQueryParamRequiredFromValidation() throws Exception {
        Path apiDir = tempDir.resolve("build/docs/api");
        File controllerJson = findFile(apiDir, "SampleController");
        assertNotNull(controllerJson);

        String json = Files.readString(controllerJson.toPath());
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> apis = gson.fromJson(json, listType);

        Map<String, Object> api = findApiByName(apis, "사용자 검색");
        assertNotNull(api);

        List<Map<String, Object>> params = (List<Map<String, Object>>) api.get("parameters");

        // @NotNull status → required (isOptional=false)
        Map<String, Object> statusParam = params.stream()
                .filter(p -> "status".equals(p.get("name")))
                .findFirst().orElse(null);
        assertNotNull(statusParam);
        assertFalse((Boolean) statusParam.get("isOptional"), "@NotNull field should not be optional");

        // primitive boolean active → required (isOptional=false)
        Map<String, Object> activeParam = params.stream()
                .filter(p -> "active".equals(p.get("name")))
                .findFirst().orElse(null);
        assertNotNull(activeParam);
        assertFalse((Boolean) activeParam.get("isOptional"), "primitive field should not be optional");

        // String name → optional (isOptional=true)
        Map<String, Object> nameParam = params.stream()
                .filter(p -> "name".equals(p.get("name")))
                .findFirst().orElse(null);
        assertNotNull(nameParam);
        assertTrue((Boolean) nameParam.get("isOptional"), "unannotated String field should be optional");
    }

    // --- Item 2: operationId 의미있는 이름 ---

    @Test
    @SuppressWarnings("unchecked")
    void testOperationIdIsMeaningfulMethodName() throws Exception {
        Path apiDir = tempDir.resolve("build/docs/api");
        File controllerJson = findFile(apiDir, "SampleController");
        assertNotNull(controllerJson);

        String json = Files.readString(controllerJson.toPath());
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> apis = gson.fromJson(json, listType);

        for (Map<String, Object> api : apis) {
            String id = (String) api.get("id");
            assertNotNull(id, "operationId should not be null");
            // operationId는 MD5 해시가 아닌 의미있는 이름이어야 함
            assertFalse(id.matches("[0-9a-f]{32}"),
                    "operationId should not be MD5 hash. Got: " + id + " for API: " + api.get("name"));
        }

        // 특정 메서드명 확인
        Map<String, Object> getUserApi = findApiByName(apis, "사용자 상세 조회");
        assertNotNull(getUserApi);
        assertEquals("getUser", getUserApi.get("id"), "operationId should be method name");
    }

    // --- Item 2: OpenAPI operationId ---

    @Test
    @SuppressWarnings("unchecked")
    void testOpenApiOperationIdMeaningful() throws Exception {
        Map<String, Object> spec = readOpenApiJson();
        Map<String, Object> paths = (Map<String, Object>) spec.get("paths");

        for (Object pathItemObj : paths.values()) {
            Map<String, Object> pathItem = (Map<String, Object>) pathItemObj;
            for (Object opObj : pathItem.values()) {
                Map<String, Object> operation = (Map<String, Object>) opObj;
                String operationId = (String) operation.get("operationId");
                assertNotNull(operationId, "operationId should not be null in OpenAPI spec");
                assertFalse(operationId.matches("[0-9a-f]{32}"),
                        "OpenAPI operationId should not be MD5 hash. Got: " + operationId);
            }
        }
    }

    // --- Item 5: 에러 응답에 ApiError 스키마 자동 첨부 ---

    @Test
    @SuppressWarnings("unchecked")
    void testOpenApiErrorResponseHasSchema() throws Exception {
        Map<String, Object> spec = readOpenApiJson();
        Map<String, Object> paths = (Map<String, Object>) spec.get("paths");

        // 사용자 상세 조회는 @error UserNotFoundException → 404 응답에 ApiError 스키마 첨부
        boolean found404WithSchema = false;
        for (Map.Entry<String, Object> pathEntry : paths.entrySet()) {
            Map<String, Object> pathItem = (Map<String, Object>) pathEntry.getValue();
            for (Object opObj : pathItem.values()) {
                Map<String, Object> operation = (Map<String, Object>) opObj;
                Map<String, Object> responses = (Map<String, Object>) operation.get("responses");
                if (responses == null) continue;

                Map<String, Object> resp404 = (Map<String, Object>) responses.get("404");
                if (resp404 != null) {
                    Map<String, Object> content = (Map<String, Object>) resp404.get("content");
                    if (content != null) {
                        Map<String, Object> jsonContent = (Map<String, Object>) content.get("application/json");
                        if (jsonContent != null) {
                            Map<String, Object> schema = (Map<String, Object>) jsonContent.get("schema");
                            if (schema != null && schema.containsKey("$ref")) {
                                assertTrue(((String) schema.get("$ref")).contains("ApiError"),
                                        "404 response should reference ApiError schema");
                                found404WithSchema = true;
                            }
                        }
                    }
                }
            }
        }
        assertTrue(found404WithSchema, "should have at least one 404 response with ApiError schema");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testOpenApiApiErrorSchemaExists() throws Exception {
        Map<String, Object> spec = readOpenApiJson();
        Map<String, Object> components = (Map<String, Object>) spec.get("components");
        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");

        assertTrue(schemas.containsKey("ApiError"), "ApiError schema should exist in components");

        Map<String, Object> apiErrorSchema = (Map<String, Object>) schemas.get("ApiError");
        Map<String, Object> properties = (Map<String, Object>) apiErrorSchema.get("properties");
        assertNotNull(properties);
        assertTrue(properties.containsKey("code"), "ApiError should have code property");
        assertTrue(properties.containsKey("message"), "ApiError should have message property");
        assertTrue(properties.containsKey("status"), "ApiError should have status property");
    }

    // --- Item 6: LocalDateTime 인라인, Item 7: BigDecimal format ---

    @Test
    @SuppressWarnings("unchecked")
    void testOpenApiDateTimeInline() throws Exception {
        Map<String, Object> spec = readOpenApiJson();
        Map<String, Object> components = (Map<String, Object>) spec.get("components");
        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");

        // LocalDateTime이 별도 스키마로 등록되지 않아야 함
        assertFalse(schemas.containsKey("LocalDateTime"),
                "LocalDateTime should NOT exist as a separate schema — it should be inlined");
        assertFalse(schemas.containsKey("LocalDate"),
                "LocalDate should NOT exist as a separate schema — it should be inlined");

        // UserDto의 createdAt 필드 (BaseDto 상속)가 인라인 date-time으로 처리되어야 함
        Map<String, Object> userSchema = (Map<String, Object>) schemas.get("UserDto");
        if (userSchema != null) {
            Map<String, Object> properties = (Map<String, Object>) userSchema.get("properties");
            if (properties != null && properties.containsKey("createdAt")) {
                Map<String, Object> createdAt = (Map<String, Object>) properties.get("createdAt");
                // $ref가 아닌 인라인 string/date-time이어야 함
                assertNull(createdAt.get("$ref"), "createdAt should not use $ref");
                assertNull(createdAt.get("allOf"), "createdAt should not use allOf wrapper");
            }
        }
    }

    // --- Item 8: example 값 타입 기반 자동 생성 ---

    @Test
    @SuppressWarnings("unchecked")
    void testOpenApiEnumSchemaHasExample() throws Exception {
        Map<String, Object> spec = readOpenApiJson();
        Map<String, Object> components = (Map<String, Object>) spec.get("components");
        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");

        // enum 스키마에 example이 첫 번째 값으로 설정되어야 함
        boolean foundEnumWithExample = false;
        for (Map.Entry<String, Object> entry : schemas.entrySet()) {
            Map<String, Object> schema = (Map<String, Object>) entry.getValue();
            if (schema.containsKey("enum")) {
                assertNotNull(schema.get("example"),
                        "enum schema '" + entry.getKey() + "' should have example value");
                List<String> enumValues = (List<String>) schema.get("enum");
                assertEquals(enumValues.get(0), schema.get("example"),
                        "enum example should be the first enum value");
                foundEnumWithExample = true;
            }
        }
        assertTrue(foundEnumWithExample, "should have at least one enum schema with example");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testOpenApiFieldExampleValues() throws Exception {
        Map<String, Object> spec = readOpenApiJson();
        Map<String, Object> components = (Map<String, Object>) spec.get("components");
        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");

        Map<String, Object> userSchema = (Map<String, Object>) schemas.get("UserDto");
        assertNotNull(userSchema, "UserDto schema should exist");

        Map<String, Object> properties = (Map<String, Object>) userSchema.get("properties");

        // Long 타입 id 필드에 example 1이 있어야 함
        if (properties.containsKey("id")) {
            Map<String, Object> idProp = (Map<String, Object>) properties.get("id");
            // id가 인라인 타입인 경우만 검증 (Object $ref인 경우 example 없음)
            if (idProp.containsKey("type")) {
                assertNotNull(idProp.get("example"), "Long field 'id' should have example value");
            }
        }
    }

    // --- List<String> 반환 타입 처리 ---

    @Test
    @SuppressWarnings("unchecked")
    void testListStringReturnIsArray() throws Exception {
        // List<String> → OpenAPI에서 array + items: string (not $ref)
        Map<String, Object> spec = readOpenApiJson();
        Map<String, Object> paths = (Map<String, Object>) spec.get("paths");

        boolean found = false;
        for (Map.Entry<String, Object> pathEntry : paths.entrySet()) {
            if (!pathEntry.getKey().contains("tags")) continue;
            Map<String, Object> pathItem = (Map<String, Object>) pathEntry.getValue();
            for (Object opObj : pathItem.values()) {
                Map<String, Object> operation = (Map<String, Object>) opObj;
                Map<String, Object> responses = (Map<String, Object>) operation.get("responses");
                for (Object respObj : responses.values()) {
                    Map<String, Object> resp = (Map<String, Object>) respObj;
                    Map<String, Object> content = (Map<String, Object>) resp.get("content");
                    if (content == null) continue;
                    Map<String, Object> json = (Map<String, Object>) content.get("application/json");
                    if (json == null) continue;
                    Map<String, Object> schema = (Map<String, Object>) json.get("schema");
                    if (schema != null && "array".equals(schema.get("type"))) {
                        Map<String, Object> items = (Map<String, Object>) schema.get("items");
                        assertNotNull(items);
                        assertEquals("string", items.get("type"),
                                "List<String> items should be inline string, not $ref");
                        assertNull(items.get("$ref"),
                                "List<String> items should NOT use $ref");
                        found = true;
                    }
                }
            }
        }
        assertTrue(found, "should find List<String> array response for tags endpoint");
    }

    // --- JSON Sample 자동 생성 ---

    @Test
    @SuppressWarnings("unchecked")
    void testSpecBundleContainsResponseSample() throws Exception {
        Map<String, Object> bundle = readSpecBundle();
        List<Map<String, Object>> apis = (List<Map<String, Object>>) bundle.get("apis");

        // 사용자 목록 조회 (List<UserDto>) → responseSample이 배열 형태
        Map<String, Object> listApi = findApiByName(apis, "사용자 목록 조회");
        assertNotNull(listApi);
        String responseSample = (String) listApi.get("responseSample");
        assertNotNull(responseSample, "List return should have responseSample");
        assertTrue(responseSample.trim().startsWith("["), "List return responseSample should be array");
        assertTrue(responseSample.contains("name"), "responseSample should contain model fields");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSpecBundleContainsRequestSample() throws Exception {
        Map<String, Object> bundle = readSpecBundle();
        List<Map<String, Object>> apis = (List<Map<String, Object>>) bundle.get("apis");

        // POST 사용자 생성 → requestSample 존재
        Map<String, Object> createApi = findApiByName(apis, "사용자 생성");
        assertNotNull(createApi, "사용자 생성 API should exist");
        String requestSample = (String) createApi.get("requestSample");
        assertNotNull(requestSample, "POST API should have requestSample");
        assertTrue(requestSample.contains("name"), "requestSample should contain model fields");

        // responseSample도 존재
        String responseSample = (String) createApi.get("responseSample");
        assertNotNull(responseSample, "POST API should have responseSample");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSpecBundleGetApiNoRequestSample() throws Exception {
        Map<String, Object> bundle = readSpecBundle();
        List<Map<String, Object>> apis = (List<Map<String, Object>>) bundle.get("apis");

        // GET 사용자 목록 조회 → requestSample은 null
        Map<String, Object> getApi = findApiByName(apis, "사용자 목록 조회");
        assertNotNull(getApi);
        assertNull(getApi.get("requestSample"), "GET API should not have requestSample");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSpecBundlePagingResponseSample() throws Exception {
        Map<String, Object> bundle = readSpecBundle();
        List<Map<String, Object>> apis = (List<Map<String, Object>>) bundle.get("apis");

        // 사용자 페이징 조회 → SpringPage 래퍼로 감싸진 responseSample
        Map<String, Object> pagedApi = findApiByName(apis, "사용자 페이징 조회");
        assertNotNull(pagedApi);
        String responseSample = (String) pagedApi.get("responseSample");
        assertNotNull(responseSample, "Paging API should have responseSample");
        assertTrue(responseSample.contains("content"), "Spring Page responseSample should contain 'content'");
        assertTrue(responseSample.contains("totalElements"), "Spring Page responseSample should contain 'totalElements'");
    }

    // --- Set<String> → array 처리 ---

    @Test
    @SuppressWarnings("unchecked")
    void testSetFieldMappedAsArray() throws Exception {
        // UserDto.permissions (Set<String>) → OpenAPI에서 array + items: string
        Map<String, Object> spec = readOpenApiJson();
        Map<String, Object> components = (Map<String, Object>) spec.get("components");
        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");

        Map<String, Object> userSchema = (Map<String, Object>) schemas.get("UserDto");
        assertNotNull(userSchema);
        Map<String, Object> properties = (Map<String, Object>) userSchema.get("properties");
        assertNotNull(properties);

        Map<String, Object> permissionsProp = (Map<String, Object>) properties.get("permissions");
        assertNotNull(permissionsProp, "permissions property should exist in UserDto");
        assertEquals("array", permissionsProp.get("type"),
                "Set<String> should be mapped as array, not string");

        Map<String, Object> items = (Map<String, Object>) permissionsProp.get("items");
        assertNotNull(items, "array should have items");
        assertEquals("string", items.get("type"), "Set<String> items should be string type");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testSetReturnTypeMappedAsArray() throws Exception {
        // Set<String> 반환 타입 → isArrayReturn: true
        Path apiDir = tempDir.resolve("build/docs/api");
        File controllerJson = findFile(apiDir, "SampleController");
        assertNotNull(controllerJson);

        String json = Files.readString(controllerJson.toPath());
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> apis = gson.fromJson(json, listType);

        Map<String, Object> api = findApiByName(apis, "사용자 권한 조회");
        assertNotNull(api, "사용자 권한 조회 API should exist");
        assertTrue((Boolean) api.get("isArrayReturn"),
                "Set<String> return type should be detected as array return");
    }

    // --- 쿼리 파라미터 enum 모델 생성 ---

    @Test
    @SuppressWarnings("unchecked")
    void testQueryParamEnumModelGenerated() throws Exception {
        // UserSearchRequest의 status 필드(UserStatus enum)가
        // 쿼리 파라미터 전개 시에도 enum 모델이 생성되어야 함
        Path modelDir = tempDir.resolve("build/docs/model");
        File enumModel = findFile(modelDir, "UserStatus");
        assertNotNull(enumModel, "UserStatus enum model should be generated from query param expansion");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testOpenApiQueryParamEnumHasValues() throws Exception {
        Map<String, Object> spec = readOpenApiJson();
        Map<String, Object> paths = (Map<String, Object>) spec.get("paths");

        // /search 엔드포인트의 status 파라미터에 enum 배열이 있어야 함
        for (Map.Entry<String, Object> pathEntry : paths.entrySet()) {
            if (!pathEntry.getKey().contains("search")) continue;
            Map<String, Object> pathItem = (Map<String, Object>) pathEntry.getValue();
            for (Object opObj : pathItem.values()) {
                Map<String, Object> operation = (Map<String, Object>) opObj;
                List<Map<String, Object>> parameters = (List<Map<String, Object>>) operation.get("parameters");
                if (parameters == null) continue;
                for (Map<String, Object> param : parameters) {
                    if ("status".equals(param.get("name"))) {
                        Map<String, Object> schema = (Map<String, Object>) param.get("schema");
                        assertNotNull(schema.get("enum"),
                                "status query param should have enum values from UserStatus");
                        return;
                    }
                }
            }
        }
        fail("Should have found status parameter in search endpoint");
    }

    // --- 외부 패키지 참조 모델 생성 ---

    @Test
    @SuppressWarnings("unchecked")
    void testExternalPackageModelGenerated() throws Exception {
        // basePackage('com.example') 밖의 com.external.entity.PartnerEntity가
        // 반환 타입으로 참조되면 model JSON이 생성되어야 함
        Path modelDir = tempDir.resolve("build/docs/model");
        File partnerModel = findFile(modelDir, "PartnerEntity");
        assertNotNull(partnerModel,
                "PartnerEntity model should be generated even though it's outside basePackage");

        String json = Files.readString(partnerModel.toPath());
        Map<String, Object> model = gson.fromJson(json, Map.class);
        assertEquals("Object", model.get("type"));

        List<Map<String, Object>> fields = (List<Map<String, Object>>) model.get("fields");
        List<String> fieldNames = fields.stream()
                .map(f -> (String) f.get("name")).toList();
        assertTrue(fieldNames.contains("id"), "should contain id field");
        assertTrue(fieldNames.contains("name"), "should contain name field");
        assertTrue(fieldNames.contains("email"), "should contain email field");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testOpenApiExternalSchemaNotMissing() throws Exception {
        // OpenAPI spec에서 PartnerEntity schema가 $ref로 참조되고,
        // components/schemas에도 존재해야 함 (broken $ref 방지)
        Map<String, Object> spec = readOpenApiJson();
        Map<String, Object> components = (Map<String, Object>) spec.get("components");
        Map<String, Object> schemas = (Map<String, Object>) components.get("schemas");

        assertTrue(schemas.containsKey("PartnerEntity"),
                "PartnerEntity schema should exist in components/schemas (not broken $ref)");

        Map<String, Object> partnerSchema = (Map<String, Object>) schemas.get("PartnerEntity");
        assertEquals("object", partnerSchema.get("type"));
        Map<String, Object> properties = (Map<String, Object>) partnerSchema.get("properties");
        assertNotNull(properties);
        assertTrue(properties.containsKey("id"), "should have id property");
        assertTrue(properties.containsKey("name"), "should have name property");
    }

    // --- @XApiIgnore 어노테이션 기반 제외 ---

    @Test
    @SuppressWarnings("unchecked")
    void testXApiIgnoreClassExcludesEntireController() throws Exception {
        // @XApiIgnore가 붙은 InternalController는 API JSON이 생성되지 않아야 함
        Path apiDir = tempDir.resolve("build/docs/api");
        File internalJson = findFile(apiDir, "InternalController");
        assertNull(internalJson, "InternalController should be excluded by @XApiIgnore on class");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testXApiIgnoreMethodExcludesEndpoint() throws Exception {
        Path apiDir = tempDir.resolve("build/docs/api");
        File controllerJson = findFile(apiDir, "SampleController");
        assertNotNull(controllerJson);

        String json = Files.readString(controllerJson.toPath());
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        List<Map<String, Object>> apis = gson.fromJson(json, listType);

        // @XApiIgnore가 붙은 debug 메서드는 제외되어야 함
        Map<String, Object> debugApi = findApiByName(apis, "디버그");
        assertNull(debugApi, "debug method should be excluded by @XApiIgnore on method");

        // 다른 메서드들은 정상 존재
        Map<String, Object> getUserApi = findApiByName(apis, "사용자 상세 조회");
        assertNotNull(getUserApi, "non-ignored methods should still be included");
    }

    // --- helpers ---

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> readErrorsJson() throws Exception {
        Path errorsFile = tempDir.resolve("build/docs/error/errors.json");
        String json = Files.readString(errorsFile);
        Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
        return gson.fromJson(json, listType);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readSpecBundle() throws Exception {
        Path bundleFile = tempDir.resolve("build/docs/spec-bundle.json");
        String json = Files.readString(bundleFile);
        return gson.fromJson(json, Map.class);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readOpenApiJson() throws Exception {
        Path openapiFile = tempDir.resolve("build/docs/openapi.json");
        String json = Files.readString(openapiFile);
        return gson.fromJson(json, Map.class);
    }


    private static Map<String, Object> findApiByName(List<Map<String, Object>> apis, String name) {
        for (Map<String, Object> api : apis) {
            if (name.equals(api.get("name"))) {
                return api;
            }
        }
        return null;
    }

    private static File findFile(Path dir, String nameContains) {
        File[] files = dir.toFile().listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.getName().contains(nameContains) && f.getName().endsWith(".json")) {
                return f;
            }
        }
        return null;
    }

    private static void copyDirectory(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Files.createDirectories(target.resolve(source.relativize(dir)));
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    private static void deleteDirectory(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        Files.walkFileTree(dir, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
                Files.delete(d);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
