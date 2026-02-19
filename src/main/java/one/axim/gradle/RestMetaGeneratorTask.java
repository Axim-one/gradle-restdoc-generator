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

public class RestMetaGeneratorTask extends DefaultTask {

    public static final String TAG = RestMetaGeneratorTask.class.getSimpleName();

    public static final String TASK_NAME = "restMetaGenerator";

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Input
    private String documentPath;

    @Input
    private String basePackage;

    @Input
    private boolean deploy = true;

    @Input
    private String serverUrl;

    @Input
    private String serviceId;

    @Input
    private boolean debug;

    @Input
    private String postmanApiKey = "";

    @Input
    private String postmanWorkSpaceId = "";

    // --- 서비스 정보 (x_service_info.json에서 이동) ---

    @Input @Optional
    private String serviceName = "";

    @Input @Optional
    private String apiServerUrl = "";

    @Input @Optional
    private String serviceVersion = "v1.0";

    @Input @Optional
    private String introductionFile = "";

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

    public boolean getDeploy() {
        return deploy;
    }

    public void setDeploy(boolean deploy) {
        this.deploy = deploy;
    }

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
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

    public void auth(Action<AuthDsl> action) {
        action.execute(this.authConfig);
    }

    public void header(String name, String defaultValue, String description) {
        this.headerConfigs.add(new HeaderDsl(name, defaultValue, description));
    }

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
        Log.i(TAG, String.format("  deploy: [%b]%n", deploy));
        Log.i(TAG, String.format("  serverUrl: [%s]%n", serverUrl));
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

            // API 문서 생성
            doGenerate(docDir, baseClassUtils, classUtils);
            Log.i(TAG, "API document generate complete");

            // API Doc 서버 저장
            doDeploy(docDir);
            Log.i(TAG, "API SERVER Save Complete");

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

    private void doGenerate(File docDir, ClassUtils baseClassUtils, List<ClassUtils> classUtils) throws Exception {

        RestApiDocGenerator generator =
                new RestApiDocGenerator(baseClassUtils, classUtils, docDir, basePackage);
        generator.generate();

    }

    private void doDeploy(File docDir) throws Exception {

        RestApiDocDeployer deployer =
                new RestApiDocDeployer(docDir, serverUrl, serviceId);
        deployer.deploy();

    }

    private void makeServiceJson(File docDir, ServiceDefinition serviceDefinition) throws Exception {
        if (!docDir.exists())
            docDir.mkdirs();

        File file = new File(docDir, serviceDefinition.getServiceId() + ".json");
        String json = gson.toJson(serviceDefinition);
        Files.write(file.toPath(), json.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }


}
