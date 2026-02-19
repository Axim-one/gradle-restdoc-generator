package one.axim.gradle;

import one.axim.gradle.data.*;
import one.axim.gradle.dsl.AuthDsl;
import one.axim.gradle.dsl.EnvironmentDsl;
import one.axim.gradle.dsl.HeaderDsl;
import one.axim.gradle.utils.ClassUtils;
import one.axim.gradle.utils.Log;
import com.google.gson.GsonBuilder;
import com.google.gson.Gson;
import org.apache.commons.lang3.StringUtils;
import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.Nested;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.TaskAction;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import one.axim.gradle.data.ErrorGroupDefinition;

/**
 * Gradle task that generates REST API documentation from Spring Boot controllers.
 *
 * <p>This task is the core of the {@code gradle-restdoc-generator} plugin.
 * It scans {@code @RestController} classes via reflection and Javadoc parsing,
 * then produces JSON metadata, OpenAPI 3.0 spec, a spec-bundle, and optionally
 * syncs to Postman.
 *
 * <h3>Execution pipeline:</h3>
 * <ol>
 *   <li>Error code scanning ({@link ErrorCodeScanner}) — finds exception classes with ErrorCode fields</li>
 *   <li>API document generation ({@link RestApiDocGenerator}) — per-controller and per-model JSON</li>
 *   <li>Error JSON output — {@code errors.json} and {@code error-response.json}</li>
 *   <li>OpenAPI 3.0 spec generation ({@link OpenApiSpecConverter})</li>
 *   <li>Spec bundle generation ({@link SpecBundleGenerator})</li>
 *   <li>Postman sync ({@link PostmanSpecConverter}) — if {@code postmanApiKey} is set</li>
 * </ol>
 *
 * <h3>DSL configuration example:</h3>
 * <pre>{@code
 * restMetaGenerator {
 *     documentPath = 'build/docs'
 *     basePackage  = 'com.example'
 *     serviceId    = 'my-service'
 *
 *     serviceName     = 'My Service'
 *     apiServerUrl    = 'https://api.example.com'
 *     serviceVersion  = 'v1.0'
 *     introductionFile = 'docs/introduction.md'
 *
 *     errorCodeClass     = 'com.example.exception.ErrorCode'
 *     errorResponseClass = 'com.example.dto.ApiErrorResponse'
 *
 *     auth {
 *         type = 'token'
 *         headerKey = 'Authorization'
 *         value = 'Bearer {{token}}'
 *         descriptionFile = 'docs/auth.md'
 *     }
 *
 *     header('X-Custom-Header', 'default-value', 'description')
 *
 *     environment('DEV') {
 *         variable('base_url', 'https://dev.api.example.com')
 *     }
 * }
 * }</pre>
 *
 * <h3>Output directory structure:</h3>
 * <pre>
 * {documentPath}/
 * ├── {serviceId}.json          — service definition
 * ├── openapi.json              — OpenAPI 3.0.3 spec
 * ├── spec-bundle.json          — unified bundle (service + APIs + models + errors)
 * ├── api/
 * │   └── {Controller}.json     — per-controller API definitions
 * ├── model/
 * │   └── {ClassName}.json      — per-model (DTO) definitions
 * └── error/
 *     ├── errors.json           — error code groups
 *     └── error-response.json   — error response model definition
 * </pre>
 *
 * @see DocumentPlugin
 * @see ErrorCodeScanner
 * @see RestApiDocGenerator
 */
public class RestMetaGeneratorTask extends DefaultTask {

    public static final String TAG = RestMetaGeneratorTask.class.getSimpleName();

    /** Gradle task name used to register this task: {@code "restMetaGenerator"}. */
    public static final String TASK_NAME = "restMetaGenerator";

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /** Output directory path for generated documentation. Relative paths are resolved against the project directory. */
    @Input
    private String documentPath;

    /**
     * Base package to scan for {@code @RestController} classes.
     * Supports comma-separated multiple packages (e.g., {@code "com.example.api,com.example.admin"}).
     */
    @Input
    private String basePackage;

    /** Unique service identifier. Used as the filename for the service JSON ({@code {serviceId}.json}). */
    @Input
    private String serviceId;

    /** Enable debug logging. */
    @Input
    private boolean debug;

    /** Postman API key for collection sync. Leave empty to skip Postman integration. */
    @Input
    private String postmanApiKey = "";

    /** Postman workspace ID for collection sync. */
    @Input
    private String postmanWorkSpaceId = "";

    /** Display name of the service (appears in OpenAPI info.title and spec-bundle). */
    @Input @Optional
    private String serviceName = "";

    /** Base URL of the API server (appears in OpenAPI servers[0].url). */
    @Input @Optional
    private String apiServerUrl = "";

    /** API version prefix (default: {@code "v1.0"}). Appears in OpenAPI info.version. */
    @Input @Optional
    private String serviceVersion = "v1.0";

    /** Path to a markdown file whose content becomes the service introduction/description. */
    @Input @Optional
    private String introductionFile = "";

    /**
     * Fully qualified class name of the ErrorCode enum/class to scan.
     * If empty, falls back to the framework default ({@code one.axim.framework.rest.exception.ErrorCode}).
     */
    @Input @Optional
    private String errorCodeClass = "";

    /**
     * Fully qualified class name of the error response DTO.
     * Used to generate {@code error/error-response.json} with the model's field structure.
     * If empty, falls back to the framework default ({@code one.axim.framework.rest.model.ApiError}).
     *
     * <p>Example:
     * <pre>{@code
     * errorResponseClass = 'com.example.dto.ApiErrorResponse'
     * }</pre>
     *
     * @since 2.0.5
     */
    @Input @Optional
    private String errorResponseClass = "";

    @Nested
    private AuthDsl authConfig = new AuthDsl();

    private List<HeaderDsl> headerConfigs = new ArrayList<>();

    private List<EnvironmentDsl> environmentConfigs = new ArrayList<>();

    public RestMetaGeneratorTask() {
        this.dependsOn("compileJava");
    }

    private static void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            try (var paths = Files.walk(dir.toPath())) {
                paths.sorted((a, b) -> b.compareTo(a))
                        .forEach(p -> {
                            try {
                                Files.delete(p);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // --- 기존 프로퍼티 getter/setter ---

    public String getPostmanApiKey() {
        return postmanApiKey;
    }

    public void setPostmanApiKey(String postmanApiKey) {
        this.postmanApiKey = postmanApiKey;
    }

    public String getPostmanWorkSpaceId() {
        return postmanWorkSpaceId;
    }

    public void setPostmanWorkSpaceId(String postmanWorkSpaceId) {
        this.postmanWorkSpaceId = postmanWorkSpaceId;
    }

    public String getDocumentPath() {
        return documentPath;
    }

    public void setDocumentPath(String documentPath) {
        this.documentPath = documentPath;
    }

    public String getBasePackage() {
        return basePackage;
    }

    public void setBasePackage(String basePackage) {
        this.basePackage = basePackage;
    }

    public String getServiceId() {
        return serviceId;
    }

    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }

    public boolean getDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    // --- 새 서비스 프로퍼티 getter/setter ---

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getApiServerUrl() {
        return apiServerUrl;
    }

    public void setApiServerUrl(String apiServerUrl) {
        this.apiServerUrl = apiServerUrl;
    }

    public String getServiceVersion() {
        return serviceVersion;
    }

    public void setServiceVersion(String serviceVersion) {
        this.serviceVersion = serviceVersion;
    }

    public String getIntroductionFile() {
        return introductionFile;
    }

    public void setIntroductionFile(String introductionFile) {
        this.introductionFile = introductionFile;
    }

    public String getErrorCodeClass() {
        return errorCodeClass;
    }

    public void setErrorCodeClass(String errorCodeClass) {
        this.errorCodeClass = errorCodeClass;
    }

    public String getErrorResponseClass() {
        return errorResponseClass;
    }

    public void setErrorResponseClass(String errorResponseClass) {
        this.errorResponseClass = errorResponseClass;
    }

    public AuthDsl getAuthConfig() {
        return authConfig;
    }

    @Internal
    public List<HeaderDsl> getHeaderConfigs() {
        return headerConfigs;
    }

    @Internal
    public List<EnvironmentDsl> getEnvironmentConfigs() {
        return environmentConfigs;
    }

    // --- DSL 메서드 ---

    /**
     * Configures authentication settings for the API documentation.
     *
     * <pre>{@code
     * auth {
     *     type = 'token'
     *     headerKey = 'Authorization'
     *     value = 'Bearer {{token}}'
     *     descriptionFile = 'docs/auth.md'
     * }
     * }</pre>
     *
     * @param action configuration action for {@link AuthDsl}
     */
    public void auth(Action<AuthDsl> action) {
        action.execute(this.authConfig);
    }

    /**
     * Adds a common HTTP header that applies to all API endpoints.
     *
     * <pre>{@code
     * header('X-Custom-Header', 'default-value', 'Header description')
     * }</pre>
     *
     * @param name         header name
     * @param defaultValue default header value
     * @param description  header description for documentation
     */
    public void header(String name, String defaultValue, String description) {
        this.headerConfigs.add(new HeaderDsl(name, defaultValue, description));
    }

    /**
     * Adds a Postman environment configuration with named variables.
     *
     * <pre>{@code
     * environment('DEV') {
     *     variable('base_url', 'https://dev.api.example.com')
     *     variable('token', 'dev-token')
     * }
     * }</pre>
     *
     * @param name   environment name (e.g., "DEV", "PROD")
     * @param action configuration action for {@link EnvironmentDsl}
     */
    public void environment(String name, Action<EnvironmentDsl> action) {
        EnvironmentDsl env = new EnvironmentDsl(name);
        action.execute(env);
        this.environmentConfigs.add(env);
    }

    // --- Task Action ---

    @TaskAction
    public void deplyTask() {
        Log.i(TAG, "-------------------------------------------------");
        Log.i(TAG, "REST API Generator");
        Log.i(TAG, "-------------------------------------------------");
        Log.i(TAG, String.format("  documentPath: [%s]", documentPath));
        Log.i(TAG, String.format("  basePackage: [%s]%n", basePackage));
        Log.i(TAG, String.format("  serviceId: [%s]%n", serviceId));
        Log.i(TAG, "-------------------------------------------------");

        try {

            List<ClassUtils> classUtils = new ArrayList<>();

            Set<Project> projects = getProject().getRootProject().getAllprojects();

            for (Project project : projects) {
                try {
                    Log.i(TAG, "Load Project :: " + project.getName());
                    classUtils.add(new ClassUtils(project));

                } catch (Exception e) {
                }
            }


            ClassUtils baseClassUtils = new ClassUtils(getProject());

            File docDir = documentPath.startsWith("/") ? new File(documentPath) : new File(getProject().getProjectDir(), documentPath);

            // DSL 프로퍼티로부터 ServiceDefinition 빌드
            ServiceDefinition serviceDefinition = buildServiceDefinition();

            Log.i(TAG, "-------------------------------------------------");
            Log.i(TAG, "Service info");
            Log.i(TAG, "-------------------------------------------------");
            Log.i(TAG, String.format("  serviceId: [%s]", serviceDefinition.getServiceId()));
            Log.i(TAG, String.format("  name: [%s]%n", serviceDefinition.getName()));
            Log.i(TAG, String.format("  introduction: [%s]%n", serviceDefinition.getIntroduction()));
            Log.i(TAG, "-------------------------------------------------");

            // 문서 디렉터리를 먼저 삭제한다.
            Log.i(TAG, "delete document directory.");
            deleteDirectory(docDir);

            // Service 문서 생성
            makeServiceJson(docDir, serviceDefinition);

            // 1. Error Code 스캐닝 (API 생성 전에 수행)
            Log.i(TAG, "Error code scanning start");
            ErrorCodeScanner errorScanner = new ErrorCodeScanner(
                    baseClassUtils.getAllClassLoader(),
                    classUtils,
                    basePackage,
                    docDir.getPath(),
                    this.errorCodeClass,
                    this.errorResponseClass
            );
            List<ErrorGroupDefinition> errorGroups = errorScanner.scanAndReturn();
            Log.i(TAG, "Error code scanning end");

            // 2. API 문서 생성 (errorGroups 전달)
            doGenerate(docDir, baseClassUtils, classUtils, errorGroups);
            Log.i(TAG, "API document generate complete");

            // 3. Error JSON 파일 출력
            errorScanner.writeResults(errorGroups);

            // OpenAPI 3.0 스펙 생성
            Log.i(TAG, "OpenAPI spec generation start");
            OpenApiSpecConverter openApiConverter = new OpenApiSpecConverter(serviceDefinition, docDir.getPath());
            openApiConverter.build();
            Log.i(TAG, "OpenAPI spec generation end");

            // Spec Bundle JSON 생성
            Log.i(TAG, "Spec bundle generation start");
            SpecBundleGenerator bundleGenerator = new SpecBundleGenerator(serviceDefinition, docDir.getPath());
            bundleGenerator.build();
            Log.i(TAG, "Spec bundle generation end");

            if (!StringUtils.isEmpty(this.postmanApiKey)) {
                Log.i(TAG, "postman import start");
                PostmanSpecConverter postmanSpecConverter = new PostmanSpecConverter(this.postmanApiKey, this.postmanWorkSpaceId, serviceDefinition, docDir.getPath());
                postmanSpecConverter.build();
                Log.i(TAG, "postman import end");
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, e);
        }
    }

    private ServiceDefinition buildServiceDefinition() throws IOException {
        ServiceDefinition sd = new ServiceDefinition();
        sd.setServiceId(this.serviceId);
        sd.setName(this.serviceName);
        sd.setApiServerUrl(this.apiServerUrl);
        sd.setVersion(this.serviceVersion);

        // introductionFile → 파일 읽어서 introduction에 설정
        if (!StringUtils.isEmpty(this.introductionFile)) {
            File introFile = new File(getProject().getProjectDir(), this.introductionFile);
            if (introFile.exists()) {
                sd.setIntroduction(Files.readString(introFile.toPath()));
            }
        }

        // auth 설정
        if (!StringUtils.isEmpty(this.authConfig.getType())) {
            APIAuthData authData = new APIAuthData();
            authData.setType(this.authConfig.getType());
            authData.setHeaderKey(this.authConfig.getHeaderKey());
            authData.setValue(this.authConfig.getValue());

            // descriptionFile → 파일 읽어서 description에 설정
            if (!StringUtils.isEmpty(this.authConfig.getDescriptionFile())) {
                File descFile = new File(getProject().getProjectDir(), this.authConfig.getDescriptionFile());
                if (descFile.exists()) {
                    authData.setDescription(Files.readString(descFile.toPath()));
                }
            }

            sd.setAuth(authData);
        }

        // headers 설정
        if (!this.headerConfigs.isEmpty()) {
            List<APIHeader> headers = new ArrayList<>();
            for (HeaderDsl headerDsl : this.headerConfigs) {
                APIHeader header = new APIHeader();
                header.setName(headerDsl.getName());
                header.setDefaultValue(headerDsl.getDefaultValue());
                header.setDescription(headerDsl.getDescription());
                header.setIsOptional(headerDsl.isOptional());
                headers.add(header);
            }
            sd.setHeaders(headers);
        }

        // environment 설정
        if (!this.environmentConfigs.isEmpty()) {
            List<ENVVariableDefinition> envDefs = new ArrayList<>();
            for (EnvironmentDsl envDsl : this.environmentConfigs) {
                ENVVariableDefinition envDef = new ENVVariableDefinition();
                envDef.setName(envDsl.getName());

                List<ENVVariable> variables = new ArrayList<>();
                for (String[] pair : envDsl.getVariables()) {
                    ENVVariable var = new ENVVariable();
                    var.setName(pair[0]);
                    var.setValue(pair[1]);
                    variables.add(var);
                }
                envDef.setVariables(variables);
                envDefs.add(envDef);
            }
            sd.setEnvVariable(envDefs);
        }

        return sd;
    }

    private void doGenerate(File docDir, ClassUtils baseClassUtils, List<ClassUtils> classUtils,
                            List<ErrorGroupDefinition> errorGroups) throws Exception {

        RestApiDocGenerator generator =
                new RestApiDocGenerator(baseClassUtils, classUtils, docDir, basePackage);
        generator.setErrorGroups(errorGroups);
        generator.generate();

    }

    private void makeServiceJson(File docDir, ServiceDefinition serviceDefinition) throws Exception {
        if (!docDir.exists())
            docDir.mkdirs();

        File file = new File(docDir, serviceDefinition.getServiceId() + ".json");
        String json = gson.toJson(serviceDefinition);
        Files.write(file.toPath(), json.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }


}
