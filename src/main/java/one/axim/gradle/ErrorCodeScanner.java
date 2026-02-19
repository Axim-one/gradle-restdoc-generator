package one.axim.gradle;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.ConstructorDeclaration;
import com.github.javaparser.ast.expr.FieldAccessExpr;
import com.github.javaparser.ast.expr.IntegerLiteralExpr;
import com.github.javaparser.ast.stmt.ExplicitConstructorInvocationStmt;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import one.axim.gradle.data.APIField;
import one.axim.gradle.data.APIModelDefinition;
import one.axim.gradle.data.ErrorCodeEntry;
import one.axim.gradle.data.ErrorGroupDefinition;
import one.axim.gradle.utils.ClassUtils;
import one.axim.gradle.utils.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.*;

/**
 * Scans exception classes for {@code ErrorCode} fields and generates error documentation.
 *
 * <p>This scanner performs two-phase execution:</p>
 * <ol>
 *   <li>{@link #scanAndReturn()} — scans exception classes and returns error groups (no file I/O)</li>
 *   <li>{@link #writeResults(List)} — writes {@code errors.json} and {@code error-response.json}</li>
 * </ol>
 *
 * <h3>Error code class resolution (two-tier):</h3>
 * <ol>
 *   <li>DSL override via {@code errorCodeClass} property</li>
 *   <li>Framework default: {@code one.axim.framework.rest.exception.ErrorCode}</li>
 * </ol>
 *
 * <h3>Error response class resolution (two-tier):</h3>
 * <ol>
 *   <li>DSL override via {@code errorResponseClass} property</li>
 *   <li>Framework default: {@code one.axim.framework.rest.model.ApiError}</li>
 * </ol>
 *
 * <h3>HTTP status resolution order:</h3>
 * <ol>
 *   <li>Known framework exception mapping (e.g., {@code NotFoundException} → 404)</li>
 *   <li>Source code analysis — parses {@code super(HttpStatus.XXX)} in constructors</li>
 *   <li>Default: 500</li>
 * </ol>
 *
 * <h3>Output files:</h3>
 * <ul>
 *   <li>{@code error/errors.json} — array of {@link ErrorGroupDefinition}</li>
 *   <li>{@code error/error-response.json} — {@link APIModelDefinition} of the error response DTO</li>
 * </ul>
 *
 * @see ErrorGroupDefinition
 * @see ErrorCodeEntry
 * @see RestMetaGeneratorTask
 */
public class ErrorCodeScanner {

    private static final String TAG = ErrorCodeScanner.class.getSimpleName();
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /** Default ErrorCode class from axim framework. */
    private static final String DEFAULT_ERROR_CODE_CLASS = "one.axim.framework.rest.exception.ErrorCode";
    private static final String DEFAULT_FRAMEWORK_EXCEPTION_PACKAGE = "one.axim.framework.rest.exception";
    /** Default error response model class from axim framework. */
    private static final String API_ERROR_CLASS = "one.axim.framework.rest.model.ApiError";

    private static final Map<String, Integer> HTTP_STATUS_MAP = new LinkedHashMap<>();

    static {
        HTTP_STATUS_MAP.put("OK", 200);
        HTTP_STATUS_MAP.put("CREATED", 201);
        HTTP_STATUS_MAP.put("NO_CONTENT", 204);
        HTTP_STATUS_MAP.put("BAD_REQUEST", 400);
        HTTP_STATUS_MAP.put("UNAUTHORIZED", 401);
        HTTP_STATUS_MAP.put("FORBIDDEN", 403);
        HTTP_STATUS_MAP.put("NOT_FOUND", 404);
        HTTP_STATUS_MAP.put("METHOD_NOT_ALLOWED", 405);
        HTTP_STATUS_MAP.put("CONFLICT", 409);
        HTTP_STATUS_MAP.put("UNPROCESSABLE_ENTITY", 422);
        HTTP_STATUS_MAP.put("INTERNAL_SERVER_ERROR", 500);
        HTTP_STATUS_MAP.put("BAD_GATEWAY", 502);
        HTTP_STATUS_MAP.put("SERVICE_UNAVAILABLE", 503);
        HTTP_STATUS_MAP.put("GATEWAY_TIMEOUT", 504);
    }

    private static final Map<String, Integer> KNOWN_EXCEPTION_STATUS = new LinkedHashMap<>();

    static {
        KNOWN_EXCEPTION_STATUS.put("UnAuthorizedException", 401);
        KNOWN_EXCEPTION_STATUS.put("NotFoundException", 404);
        KNOWN_EXCEPTION_STATUS.put("InvalidRequestParameterException", 400);
        KNOWN_EXCEPTION_STATUS.put("UnavailableServerException", 504);
        KNOWN_EXCEPTION_STATUS.put("UnknownServerException", 500);
    }

    private final ClassLoader classLoader;
    private final List<ClassUtils> classUtils;
    private final String basePackage;
    private final String documentPath;
    private final String errorCodeClassName;
    private final String errorResponseClassName;

    /**
     * Creates a new ErrorCodeScanner.
     *
     * @param classLoader            classloader that includes the project's compiled classes
     * @param classUtils             list of ClassUtils for source file resolution across subprojects
     * @param basePackage            base package(s) to scan (comma-separated)
     * @param documentPath           output directory for generated JSON files
     * @param errorCodeClassName     FQCN of the ErrorCode class (empty string to use framework default)
     * @param errorResponseClassName FQCN of the error response DTO class (empty string to use framework default)
     */
    public ErrorCodeScanner(ClassLoader classLoader, List<ClassUtils> classUtils,
                            String basePackage, String documentPath,
                            String errorCodeClassName, String errorResponseClassName) {
        this.classLoader = classLoader;
        this.classUtils = classUtils;
        this.basePackage = basePackage;
        this.documentPath = documentPath;
        this.errorCodeClassName = errorCodeClassName;
        this.errorResponseClassName = errorResponseClassName;
    }

    public void scan() {
        List<ErrorGroupDefinition> groups = scanAndReturn();
        writeResults(groups);
    }

    /**
     * ErrorCode 클래스를 스캔하여 에러 그룹 목록을 반환한다. (파일 쓰기 없음)
     */
    public List<ErrorGroupDefinition> scanAndReturn() {
        try {
            Class<?> errorCodeClass = loadErrorCodeClass();
            if (errorCodeClass == null) {
                Log.i(TAG, "ErrorCode class not found, skipping error code scanning");
                return Collections.emptyList();
            }
            Log.i(TAG, "ErrorCode class loaded: " + errorCodeClass.getName());

            Properties messages = loadMessageProperties();
            Log.i(TAG, "Loaded " + messages.size() + " message properties");

            List<ErrorGroupDefinition> groups = scanExceptionClasses(errorCodeClass, messages);
            Log.i(TAG, "Found " + groups.size() + " error groups");
            return groups;
        } catch (Exception e) {
            Log.w(TAG, "Error code scanning failed: " + e.getMessage());
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    /**
     * 스캔 결과를 JSON 파일로 출력한다.
     */
    public void writeResults(List<ErrorGroupDefinition> groups) {
        try {
            writeErrorsJson(groups);
            writeApiErrorModel();
        } catch (Exception e) {
            Log.w(TAG, "Error code writing failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private Class<?> loadErrorCodeClass() {
        // DSL override 시도
        if (errorCodeClassName != null && !errorCodeClassName.isEmpty()) {
            try {
                return classLoader.loadClass(errorCodeClassName);
            } catch (ClassNotFoundException e) {
                Log.w(TAG, "Custom ErrorCode class not found: " + errorCodeClassName);
            }
        }
        // 기본 프레임워크 클래스 시도
        try {
            return classLoader.loadClass(DEFAULT_ERROR_CODE_CLASS);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private Properties loadMessageProperties() {
        Properties merged = new Properties();

        // 프로젝트 resources에서 properties 파일 탐색
        for (ClassUtils cu : classUtils) {
            try {
                File resourceDir = findResourceDir(cu);
                if (resourceDir != null && resourceDir.exists()) {
                    loadPropertiesFromDir(resourceDir, merged);
                }
            } catch (Exception e) {
                // skip
            }
        }

        // 클래스패스 JAR에서 framework messages 로드
        try {
            var resources = classLoader.getResources("messages_ko.properties");
            while (resources.hasMoreElements()) {
                try (var is = resources.nextElement().openStream()) {
                    Properties p = new Properties();
                    p.load(is);
                    merged.putAll(p);
                }
            }
        } catch (IOException e) {
            // skip
        }

        return merged;
    }

    private File findResourceDir(ClassUtils cu) {
        try {
            File sourceDir = cu.getMainSourceSet().getResources().getSrcDirs().iterator().next();
            return sourceDir;
        } catch (Exception e) {
            return null;
        }
    }

    private void loadPropertiesFromDir(File dir, Properties target) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".properties")) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    Properties p = new Properties();
                    p.load(fis);
                    target.putAll(p);
                } catch (IOException e) {
                    // skip
                }
            }
        }
    }

    private List<ErrorGroupDefinition> scanExceptionClasses(Class<?> errorCodeClass, Properties messages) throws IOException {
        List<ErrorGroupDefinition> groups = new ArrayList<>();
        Set<String> processedClasses = new HashSet<>();

        // 스캔 대상 패키지 목록
        Set<String> scanPackages = new HashSet<>();
        if (basePackage.contains(",")) {
            for (String pkg : basePackage.split(",")) {
                scanPackages.add(pkg.trim());
            }
        } else {
            scanPackages.add(basePackage);
        }
        scanPackages.add(DEFAULT_FRAMEWORK_EXCEPTION_PACKAGE);

        ImmutableSet<ClassPath.ClassInfo> allClasses = ClassPath.from(classLoader).getAllClasses();

        for (ClassPath.ClassInfo classInfo : allClasses) {
            boolean match = false;
            for (String pkg : scanPackages) {
                if (classInfo.getPackageName().startsWith(pkg)) {
                    match = true;
                    break;
                }
            }
            if (!match) continue;

            try {
                Class<?> cls = classInfo.load();
                if (processedClasses.contains(cls.getName())) continue;
                processedClasses.add(cls.getName());

                // ErrorCode 타입의 public static final 필드가 있는지 확인
                List<Field> errorCodeFields = getErrorCodeFields(cls, errorCodeClass);
                if (errorCodeFields.isEmpty()) continue;

                Log.i(TAG, "Found exception class: " + cls.getSimpleName() + " with " + errorCodeFields.size() + " error codes");

                ErrorGroupDefinition group = new ErrorGroupDefinition();
                group.setException(cls.getSimpleName());
                group.setGroup(deriveGroupName(cls.getSimpleName()));
                group.setStatus(resolveHttpStatus(cls));

                List<ErrorCodeEntry> entries = new ArrayList<>();
                for (Field field : errorCodeFields) {
                    try {
                        Object errorCode = field.get(null);
                        ErrorCodeEntry entry = new ErrorCodeEntry();
                        entry.setName(field.getName());
                        entry.setCode(invokeStringMethod(errorCode, "code"));
                        entry.setMessageKey(invokeStringMethod(errorCode, "messageKey"));

                        // 메시지 해석
                        String resolvedMessage = messages.getProperty(entry.getMessageKey());
                        entry.setMessage(resolvedMessage != null ? resolvedMessage : entry.getMessageKey());

                        entries.add(entry);
                    } catch (Exception e) {
                        Log.w(TAG, "Failed to extract error code from field: " + field.getName() + " — " + e.getMessage());
                    }
                }

                group.setCodes(entries);
                groups.add(group);

            } catch (Throwable e) {
                // 클래스 로드 실패 시 스킵
            }
        }

        // exception 이름 순 정렬
        groups.sort(Comparator.comparing(ErrorGroupDefinition::getException));
        return groups;
    }

    private List<Field> getErrorCodeFields(Class<?> cls, Class<?> errorCodeClass) {
        List<Field> result = new ArrayList<>();
        for (Field field : cls.getDeclaredFields()) {
            if (Modifier.isPublic(field.getModifiers())
                    && Modifier.isStatic(field.getModifiers())
                    && Modifier.isFinal(field.getModifiers())
                    && errorCodeClass.isAssignableFrom(field.getType())) {
                result.add(field);
            }
        }
        return result;
    }

    private String invokeStringMethod(Object obj, String methodName) throws Exception {
        Method method = obj.getClass().getMethod(methodName);
        Object result = method.invoke(obj);
        return result != null ? result.toString() : "";
    }

    private int resolveHttpStatus(Class<?> exceptionClass) {
        // 1. 알려진 프레임워크 예외 매핑
        String simpleName = exceptionClass.getSimpleName();
        if (KNOWN_EXCEPTION_STATUS.containsKey(simpleName)) {
            return KNOWN_EXCEPTION_STATUS.get(simpleName);
        }

        // 2. 소스 분석으로 HttpStatus 추출
        int statusFromSource = resolveHttpStatusFromSource(exceptionClass);
        if (statusFromSource > 0) {
            return statusFromSource;
        }

        // 3. 기본값
        return 500;
    }

    private int resolveHttpStatusFromSource(Class<?> exceptionClass) {
        File sourceFile = findSourceFile(exceptionClass);
        if (sourceFile == null) return -1;

        try {
            JavaParser parser = new JavaParser();
            CompilationUnit cu = parser.parse(sourceFile).getResult().orElse(null);
            if (cu == null) return -1;

            // 클래스 선언 탐색
            Optional<ClassOrInterfaceDeclaration> classDecl = cu.findFirst(ClassOrInterfaceDeclaration.class);
            if (classDecl.isEmpty()) return -1;

            // constructor에서 super() 호출 탐색
            List<ConstructorDeclaration> constructors = classDecl.get().getConstructors();
            for (ConstructorDeclaration constructor : constructors) {
                List<ExplicitConstructorInvocationStmt> superCalls =
                        constructor.findAll(ExplicitConstructorInvocationStmt.class);
                for (ExplicitConstructorInvocationStmt superCall : superCalls) {
                    if (!superCall.isThis()) { // super() call
                        // 인자에서 HttpStatus.XXX 패턴 탐색
                        List<FieldAccessExpr> fieldAccesses = superCall.findAll(FieldAccessExpr.class);
                        for (FieldAccessExpr fa : fieldAccesses) {
                            String scope = fa.getScope().toString();
                            if (scope.equals("HttpStatus") || scope.endsWith(".HttpStatus")) {
                                String statusName = fa.getNameAsString();
                                Integer statusCode = HTTP_STATUS_MAP.get(statusName);
                                if (statusCode != null) {
                                    return statusCode;
                                }
                            }
                        }
                        // 숫자 리터럴 인자 탐색
                        List<IntegerLiteralExpr> intLiterals = superCall.findAll(IntegerLiteralExpr.class);
                        for (IntegerLiteralExpr lit : intLiterals) {
                            int val = lit.asNumber().intValue();
                            if (val >= 100 && val < 600) {
                                return val;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.w(TAG, "Failed to parse source for HttpStatus: " + exceptionClass.getSimpleName());
        }

        return -1;
    }

    private File findSourceFile(Class<?> cls) {
        for (ClassUtils cu : classUtils) {
            try {
                File f = cu.getSourceFile(cls);
                if (f != null && f.exists()) return f;
            } catch (Exception e) {
                // skip
            }
        }
        return null;
    }

    static String deriveGroupName(String exceptionClassName) {
        // "UserNotFoundException" → "UserNotFound" → "User Not Found"
        String base = exceptionClassName;
        if (base.endsWith("Exception")) {
            base = base.substring(0, base.length() - "Exception".length());
        }
        // CamelCase → 공백 분리
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < base.length(); i++) {
            char c = base.charAt(i);
            if (i > 0 && Character.isUpperCase(c) && !Character.isUpperCase(base.charAt(i - 1))) {
                sb.append(' ');
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private void writeErrorsJson(List<ErrorGroupDefinition> groups) throws IOException {
        File errorDir = new File(documentPath, "error");
        if (!errorDir.exists()) errorDir.mkdirs();

        File outputFile = new File(errorDir, "errors.json");
        String json = gson.toJson(groups);
        Files.write(outputFile.toPath(), json.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        Log.i(TAG, "Error codes written to: " + outputFile.getAbsolutePath());
    }

    /**
     * Resolves the error response class with two-tier priority:
     * DSL override ({@code errorResponseClassName}) first, then framework default ({@code ApiError}).
     *
     * @return the resolved Class, or {@code null} if neither is available
     */
    private Class<?> loadErrorResponseClass() {
        // 1. DSL override
        if (errorResponseClassName != null && !errorResponseClassName.isEmpty()) {
            try {
                return classLoader.loadClass(errorResponseClassName);
            } catch (ClassNotFoundException e) {
                Log.w(TAG, "Custom error response class not found: " + errorResponseClassName);
            }
        }
        // 2. 프레임워크 기본값
        try {
            return classLoader.loadClass(API_ERROR_CLASS);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private void writeApiErrorModel() {
        try {
            Class<?> apiErrorClass = loadErrorResponseClass();
            if (apiErrorClass == null) {
                Log.i(TAG, "Error response class not found, skipping");
                return;
            }

            APIModelDefinition model = new APIModelDefinition();
            model.setName(apiErrorClass.getSimpleName());
            model.setType("Object");

            List<APIField> fields = new ArrayList<>();
            for (Field field : apiErrorClass.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;

                APIField apiField = new APIField();
                apiField.setName(field.getName());
                apiField.setType(field.getType().getSimpleName());
                fields.add(apiField);
            }
            model.setFields(fields);

            File errorDir = new File(documentPath, "error");
            if (!errorDir.exists()) errorDir.mkdirs();

            File outputFile = new File(errorDir, "error-response.json");
            String json = gson.toJson(model);
            Files.write(outputFile.toPath(), json.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            Log.i(TAG, "Error response model written to: " + outputFile.getAbsolutePath());
        } catch (Exception e) {
            Log.w(TAG, "Failed to write error response model: " + e.getMessage());
        }
    }
}
