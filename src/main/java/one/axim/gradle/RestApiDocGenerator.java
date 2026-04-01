package one.axim.gradle;

import one.axim.gradle.data.*;
import one.axim.gradle.generator.utils.TypeMapUtils;
import one.axim.gradle.utils.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableSet;
import com.google.common.reflect.ClassPath;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import java.util.*;
import java.util.stream.Collectors;
import one.axim.gradle.data.ErrorGroupDefinition;

public class RestApiDocGenerator {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private static final String SPRING_PAGE = "org.springframework.data.domain.Page";
    private static final String SPRING_PAGEABLE = "org.springframework.data.domain.Pageable";
    private static final String SPRING_SORT = "org.springframework.data.domain.Sort";
    private static final String XPAGE_PREFIX = "one.axim.framework.core.data.XPage";

    private File docDir;
    private String basePackage;
    private List<ClassUtils> classUtils;
    private ClassUtils baseClassUtil;

    private Set<String> basePaths;

    private Map<String, ErrorGroupDefinition> errorGroupMap = Collections.emptyMap();

    private static final String XAPI_IGNORE = "one.axim.gradle.annotation.XApiIgnore";

    /** operationId 중복 방지를 위한 사용된 ID 집합 */
    private Set<String> usedOperationIds = new HashSet<>();

    /** DSL에서 설정된 제외 패키지 목록 */
    private List<String> excludePackages = Collections.emptyList();

    /** DSL에서 설정된 제외 클래스 이름 목록 (simple name) */
    private List<String> excludeClasses = Collections.emptyList();

    private Map<Class<?>, Set<Method>> methodMap = new TreeMap<Class<?>, Set<Method>>(new Comparator<Class<?>>() {
        @Override
        public int compare(Class<?> c1, Class<?> c2) {
            return c1.getName().compareTo(c2.getName());
        }
    });

    public RestApiDocGenerator(ClassUtils baseClassUtil, List<ClassUtils> classUtils, File docDir, String basePackage) {
        this.docDir = docDir;
        this.basePackage = basePackage;
        this.classUtils = classUtils;
        this.baseClassUtil = baseClassUtil;

        this.basePaths = new HashSet<>();

        prepareClassesAndMethods();
    }

    public void setExcludePackages(List<String> excludePackages) {
        this.excludePackages = excludePackages != null ? excludePackages : Collections.emptyList();
    }

    public void setExcludeClasses(List<String> excludeClasses) {
        this.excludeClasses = excludeClasses != null ? excludeClasses : Collections.emptyList();
    }

    public void setErrorGroups(List<ErrorGroupDefinition> groups) {
        this.errorGroupMap = new HashMap<>();
        for (ErrorGroupDefinition g : groups) {
            errorGroupMap.put(g.getException(), g);
        }
    }

    /**
     * XRestAction 어노테이션을 갖는 모든 클래스와 메서드의 목록을 준비한다.
     */
    private void prepareClassesAndMethods() {
        System.out.println(getClass().getSimpleName() + " - Prepare classes and methods info...");

        if (!docDir.exists()) {
            try {
                Files.createDirectories(Path.of(docDir.getPath()));
            } catch (IOException e) {
            }
        }

        String basePath = basePackage;

        Set<String> paths = new HashSet<>();
        if (basePackage.contains(",")) {
            String[] pkgs = basePackage.split(",");
            if (pkgs != null) {
                for (int i = 0; i < pkgs.length; i++) {
                    String pkg = pkgs[i];
                    if (i == 0) {
                        basePath = pkg;
                    }
                    paths.add(pkg.trim());
                }
            }
        } else {
            paths.add(basePackage);
        }

        basePaths.addAll(paths);

        try {

            System.out.println(" package reflection start ... ");

            List<Method> apiMethods = new ArrayList<>();


            ImmutableSet<ClassPath.ClassInfo> clsSets = ClassPath.from(this.baseClassUtil.getAllClassLoader()).getAllClasses();

            System.out.println("package find class size :: " + clsSets.size());

            for (ClassPath.ClassInfo clsInfo : clsSets) {

                System.out.println("load class load :: " + clsInfo.getName());

                if (!clsInfo.getPackageName().startsWith(basePath)) {
                    continue;
                }

                try {

                    System.out.println("find class load :: " + clsInfo.getName());

                    Class<?> cls = clsInfo.load();

                    boolean isApiController = false;
                    Annotation[] annotations = cls.getDeclaredAnnotations();
                    for (Annotation annotation : annotations) {

                        if (annotation.annotationType().getName().equals("org.springframework.web.bind.annotation.RestController")) {

                            isApiController = true;
                            break;
                        }
                    }

                    if (isApiController) {

                        // DSL excludePackages / excludeClasses 필터
                        if (isExcludedClass(cls)) {
                            System.out.println("[SKIP] Excluded controller: " + cls.getName());
                            continue;
                        }

                        // @XApiIgnore 클래스 레벨 필터
                        if (isHasAnnotation(cls.getDeclaredAnnotations(), XAPI_IGNORE)) {
                            System.out.println("[SKIP] @XApiIgnore controller: " + cls.getName());
                            continue;
                        }

                        Method[] methods = cls.getDeclaredMethods();
                        for (Method method : methods) {

                            // @XApiIgnore 메서드 레벨 필터
                            if (isHasAnnotation(method.getDeclaredAnnotations(), XAPI_IGNORE)) {
                                System.out.println("[SKIP] @XApiIgnore method: " + cls.getSimpleName() + "." + method.getName());
                                continue;
                            }

                            annotations = method.getDeclaredAnnotations();

                            for (Annotation annotation : annotations) {

                                if (annotation.annotationType().getName().equals("org.springframework.web.bind.annotation.GetMapping") ||
                                        annotation.annotationType().getName().equals("org.springframework.web.bind.annotation.PostMapping") ||
                                        annotation.annotationType().getName().equals("org.springframework.web.bind.annotation.PutMapping") ||
                                        annotation.annotationType().getName().equals("org.springframework.web.bind.annotation.DeleteMapping") ||
                                        annotation.annotationType().getName().equals("org.springframework.web.bind.annotation.RequestMapping") ||
                                        annotation.annotationType().getName().equals("org.springframework.web.bind.annotation.PatchMapping")) {

                                    apiMethods.add(method);
                                    break;
                                }
                            }
                        }
                    }

                } catch (Throwable e) {
                    System.out.println("cls load exception :: " + clsInfo.getName() + " " + e.getMessage());
                }
            }

            for (Method method : apiMethods) {

                Class<?> clazz = method.getDeclaringClass();

                Log.i("METHOD", method.getDeclaringClass() + ":" + method.getName());

                if (!methodMap.containsKey(clazz)) {
                    methodMap.put(clazz, new TreeSet<Method>(new Comparator<Method>() {
                        @Override
                        public int compare(Method m1, Method m2) {
                            return m1.getName().compareTo(m2.getName());
                        }
                    }));
                }
                Set<Method> clsMethods = methodMap.get(clazz);
                clsMethods.add(method);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Generator 메인
    public void generate() throws Exception {
        Set<String> referenceClassSet = new HashSet<>();
        Iterator<Class<?>> iter = methodMap.keySet().iterator();
        while (iter.hasNext()) {
            Class<?> clazz = iter.next();
            Set<Method> methods = methodMap.get(clazz);
            try {
                processClassMethods(clazz, methods, referenceClassSet);
            } catch (Exception e) {
                System.err.println("[WARN] Failed to process controller: " + clazz.getName() + " — " + e.getMessage());
                e.printStackTrace();
            }
        }
        if (!referenceClassSet.isEmpty()) {
            generationReferenceClassJson(referenceClassSet);
        }
    }

    /**
     * 한 클래스의 메서드 모두 처리 (한클래스에 하나의 API 문서 파일을 쓴다.)
     */
    public void processClassMethods(Class<?> clazz, Set<Method> methods, Set<String> referenceClassSet) throws Exception {
        List<APIDefinition> apiDefinitionArrayList = new ArrayList<>();

        // 클래스의 소스파일 찾기
        File srcFile = this.baseClassUtil.getSourceFile(clazz);

        if (srcFile != null) {

            // 위에서 찾은 소스파일 파싱
            System.out.println("source file :: " + srcFile.toURI() + " parse ... ");
            JavaSourceParser srcParser = JavaSourceParser.parse(srcFile);

            // 메서드들의 API 문서 준비
            for (Method method : methods) {
                try {
                    if (method.getAnnotations() != null)
                        for (Annotation annotation : method.getAnnotations()) {
                            System.out.println("method :: " + method.getName() + " annotation :: " + annotation.annotationType() + " name :: " + SuperRequestMappingUtil.name(method));
                        }

                    processSingleMethod(clazz, method, apiDefinitionArrayList, referenceClassSet, srcParser);
                } catch (Exception e) {
                    System.err.println("[WARN] Failed to process method: " + clazz.getName() + "." + method.getName() + " — " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // 하나의 클래스와 이 클래스가 갖는 메서드들의 문서를 JSON 파일로 쓰기
            File dir = new File(docDir, "api");
            if (!dir.exists()) {
                dir.mkdirs();
            }
            File file = new File(dir, clazz.getName() + ".json");
            String json = gson.toJson(apiDefinitionArrayList);
            writeTo(json, file);
        }

    }

    public File getClassFile(Class<?> cls) {
        for (ClassUtils classUtil : classUtils) {
            try {

                File f = classUtil.getSourceFile(cls);
                if (f != null) {
                    return f;
                }
            } catch (Exception e) {
            }
        }

        return null;
    }

    /**
     * 하나의 메서드에 대한 처리
     */
    public void processSingleMethod(Class<?> clazz, Method method, List<APIDefinition> apiDefinitionArrayList, Set<String> referenceClassSet, JavaSourceParser srcParser) throws Exception {

        System.out.println("Class Method Parse :: " + method.getName() + " class :: " + clazz.getName());

        String groupId = getAnnotationValue(clazz.getAnnotations(), "one.axim.framework.rest.annotation.XRestGroupName");

        String prefixUrl = "";
        for (Annotation ann : clazz.getAnnotations()) {
            if (ann.annotationType().getName().equals("org.springframework.web.bind.annotation.RequestMapping")) {
                Object val = AnnotationHelper.getValue(ann);
                if (val instanceof String[] && ((String[]) val).length > 0) {
                    prefixUrl = ((String[]) val)[0];
                    if (prefixUrl.endsWith("/")) {
                        prefixUrl = prefixUrl.substring(0, prefixUrl.length() - 1);
                    }
                }
                break;
            }
        }
        String urlMapping = prefixUrl + SuperRequestMappingUtil.value(method);
        String name = SuperRequestMappingUtil.name(method);

        if (StringUtils.isEmpty(name)) return;

        String methodName = SuperRequestMappingUtil.methodName(method);

        Parameter[] parameters = method.getParameters();

        Map<String, APIParameter> parameterHashMap = new HashMap<>();
        Map<String, APIHeader> headerHashMap = new HashMap<>();
        APIParameterKind paramKind;

        for (int i = 0; i < parameters.length; i++) {

            Parameter parameter = parameters[i];
            String parameterName = srcParser.getMethodParameterName(method, i);
            String parameterType = parameter.getType().getCanonicalName();
            String tempParameterType;

            Log.i("API", "Parameter : " + parameterName + " annotation : " + parameter.getAnnotatedType());

            // Spring Pageable 파라미터 감지
            if (parameterType.equals(SPRING_PAGEABLE)) {
                parameterHashMap.put("page", createQueryParameter("page", "java.lang.Integer", "int", "Page number (0-based)", "0"));
                parameterHashMap.put("size", createQueryParameter("size", "java.lang.Integer", "int", "Page size", "20"));
                parameterHashMap.put("sort", createQueryParameter("sort", "java.lang.String", "String", "Sort criteria (e.g. property,asc|desc)", null));
                continue;
            }

            if (TypeMapUtils.isNormalDataType(parameterType)) {
                tempParameterType = getLastComponent(parameterType, "\\.");
            } else if (parameter.getType().isEnum()) {
                tempParameterType = "Enum";
                referenceClassSet.add(parameterType);
            } else {
                tempParameterType = "Object";
                referenceClassSet.add(parameterType);
            }

            // RequestHeader 일 경우 (2018.05.03 추가)
            if (isHasAnnotation(parameter.getAnnotations(), "org.springframework.web.bind.annotation.RequestHeader")) {

                Map<String, Object> values = getAnnotationValueMap(parameter.getDeclaredAnnotations(), "org.springframework.web.bind.annotation.RequestHeader");

                if (values != null) {

                    APIHeader apiHeader = new APIHeader();
                    apiHeader.setName(parameterName);
                    apiHeader.setClassPath(parameterType);
                    apiHeader.setType(tempParameterType);

                    String pathName = values.get("value").toString();
                    if (pathName != null && !pathName.isEmpty()) {
                        apiHeader.setName(pathName);
                    }
                    boolean required = Boolean.parseBoolean(values.get("required").toString());
                    apiHeader.setIsOptional(!required);
                    String defaultValue = values.get("defaultValue").toString();
                    if (defaultValue.equals("\n\t\t\n\t\t\n\uE000\uE001\uE002\n\t\t\t\t\n")) {
                        defaultValue = "";
                    }
                    apiHeader.setDefaultValue(defaultValue);

                    headerHashMap.put(parameterName, apiHeader);
                }
            } else if (isHasAnnotation(parameter.getAnnotations(), "one.axim.framework.core.annotation.XPageNationDefault")) {

                Map<String, Object> values = getAnnotationValueMap(parameter.getDeclaredAnnotations(), "one.axim.framework.core.annotation.XPageNationDefault");

                if (values != null && !values.isEmpty()) {

                    APIParameter page = new APIParameter();
                    page.setClassPath("Integer");
                    page.setType("int");
                    page.setDescription("페이지 수");
                    page.setIsOptional(true);
                    page.setParameterKind(APIParameterKind.REQUEST_PARAMETER);
                    page.setDefaultValue(values.get("page").toString());
                    page.setName("page");
                    parameterHashMap.put(page.getName(), page);

                    APIParameter size = new APIParameter();
                    size.setClassPath("Integer");
                    size.setType("int");
                    size.setDescription("페이지 크기");
                    size.setIsOptional(true);
                    size.setParameterKind(APIParameterKind.REQUEST_PARAMETER);
                    size.setDefaultValue(values.get("size").toString());
                    size.setName("size");
                    parameterHashMap.put(size.getName(), size);

                    APIParameter offset = new APIParameter();
                    offset.setClassPath("Integer");
                    offset.setType("int");
                    offset.setDescription("size * page == offset");
                    offset.setIsOptional(true);
                    offset.setParameterKind(APIParameterKind.REQUEST_PARAMETER);

                    if (values.get("offset") != null && !values.get("offset").toString().equals("0"))
                        offset.setDefaultValue(values.get("offset").toString());

                    offset.setName("offset");
                    parameterHashMap.put(offset.getName(), offset);

                    APIParameter sort = new APIParameter();
                    sort.setClassPath("String");
                    sort.setType("String");
                    sort.setDescription("정렬 필드 및 방식 설정");
                    sort.setIsOptional(true);
                    sort.setParameterKind(APIParameterKind.REQUEST_PARAMETER);

                    if (values.get("column") != null) {
                        sort.setDefaultValue(values.get("column").toString() + "," + values.get("direction"));
                    }

                    sort.setName("sort");
                    parameterHashMap.put(sort.getName(), sort);
                }
            } else if (isHasAnnotation(parameter.getAnnotations(), "one.axim.framework.rest.annotation.XQueryParams")) {

                // Query Parameter Object
                generateQueryParameterModel(parameter.getType(), parameterHashMap, referenceClassSet);
            }
            // RequestHeader 가 아닐경우
            else {

                boolean isRequestBody = isHasAnnotation(parameter.getAnnotations(), "org.springframework.web.bind.annotation.RequestBody");
                boolean isPathVariable = isHasAnnotation(parameter.getAnnotations(), "org.springframework.web.bind.annotation.PathVariable");

                // @RequestParam 복합 객체: 개별 쿼리 파라미터로 전개
                if (tempParameterType.equals("Object") && !isRequestBody && !isPathVariable) {
                    generateQueryParameterModel(parameter.getType(), parameterHashMap, referenceClassSet);
                } else {

                    APIParameter apiParameter = new APIParameter();
                    apiParameter.setName(parameterName);
                    apiParameter.setClassPath(parameterType);
                    apiParameter.setEnum(parameter.getType().isEnum());
                    apiParameter.setType(tempParameterType);

                    paramKind = APIParameterKind.REQUEST_PARAMETER;


                    if (isHasAnnotation(parameter.getAnnotations(), "org.springframework.web.bind.annotation.RequestParam")) {

                        Map<String, Object> values = getAnnotationValueMap(parameter.getAnnotations(), "org.springframework.web.bind.annotation.RequestParam");
                        boolean required = Boolean.parseBoolean(values.get("required").toString());
                        apiParameter.setIsOptional(!required);
                        String defaultValue = values.get("defaultValue").toString();
                        if (defaultValue.equals("\n\t\t\n\t\t\n\uE000\uE001\uE002\n\t\t\t\t\n")) {
                            defaultValue = "";
                        }

                        apiParameter.setDefaultValue(defaultValue);
                    }

                    if (isRequestBody) {
                        paramKind = APIParameterKind.REQUEST_BODY;
                    }

                    if (isPathVariable) {
                        Map<String, Object> values = getAnnotationValueMap(parameter.getAnnotations(), "org.springframework.web.bind.annotation.PathVariable");

                        paramKind = APIParameterKind.URL_PATH;

                        String pathName = values.get("value").toString();

                        if (!StringUtils.isEmpty(pathName)) {
                            apiParameter.setName(pathName);
                        } else {
                            apiParameter.setName(parameterName);
                        }

                        boolean required = Boolean.parseBoolean(values.get("required").toString());
                        apiParameter.setIsOptional(!required);
                    }

                    apiParameter.setParameterKind(paramKind);
                    parameterHashMap.put(parameterName, apiParameter);
                }
            }
        }

        MethodComment methodComment = srcParser.getMethodComment(method);

        String description = "";
        List<CommentTag> commentTags = null;
        if (methodComment != null) {
            description = methodComment.getDescription();
            commentTags = methodComment.getTags();
        }

        String returnValueDescription = "";
        String group = "";
        String className = "";
        boolean isAuth = false;
        Map<String, String> resStatusMap = new HashMap<>();
        List<ErrorGroupDefinition> methodErrors = new ArrayList<>();

        if (commentTags != null) {
            for (CommentTag commentTag : commentTags) {

                if ("param".equals(commentTag.getTag())) {                // 파라미터
                    String paramName = commentTag.getName() == null ? "" : commentTag.getName().trim();
                    String desc = commentTag.getValue() == null ? "" : commentTag.getValue().trim();
                    if (parameterHashMap.get(paramName) != null) {

                        if (parameterHashMap.get(paramName).getIsEnum()) {
                            // enum field add
                            Class cls = loadClassWithFallback(clazz.getClassLoader(), parameterHashMap.get(paramName).getClassPath());
                            if (cls != null) {
                                Field[] fields = cls.getDeclaredFields();
                                desc += " ( ";
                                desc += StringUtils.join(Arrays.stream(fields).filter(field -> {
                                    return !field.getName().startsWith("$");
                                }).map(field -> {
                                    return field.getName();
                                }).collect(Collectors.toList()), " | ");
                                desc += " )";
                            }
                            parameterHashMap.get(paramName).setDescription(desc);
                        } else {
                            parameterHashMap.get(paramName).setDescription(desc);
                        }
                    } else if (headerHashMap.get(paramName) != null) {
                        headerHashMap.get(paramName).setDescription(desc);
                    }
                } else if ("return".equals(commentTag.getTag())) {
                    returnValueDescription = commentTag.getValue() == null ? "" : commentTag.getValue().trim();
                } else if ("response".equals(commentTag.getTag())) {
                    String statusCode = commentTag.getName() == null ? "" : commentTag.getName().trim();
                    String desc = commentTag.getValue() == null ? "" : commentTag.getValue().trim();
                    if (!statusCode.equals("")) {
                        resStatusMap.put(statusCode, desc);
                    }
                } else if ("group".equals(commentTag.getTag())) {
                    group = commentTag.getValue() == null ? "" : commentTag.getValue().trim();
                } else if ("auth".equals(commentTag.getTag())) {

                    if (commentTag.getValue() != null && commentTag.getValue().trim().equals("true")) {

                        isAuth = true;
                    }

                } else if ("header".equals(commentTag.getTag())) {

                    APIHeader apiHeader = new APIHeader();
                    apiHeader.setName(commentTag.getName());
                    apiHeader.setClassPath("java.lang.String");
                    apiHeader.setType("String");

                    apiHeader.setIsOptional(false);
                    apiHeader.setDescription(commentTag.getValue());
                    apiHeader.setDefaultValue("");

                    headerHashMap.put(commentTag.getName(), apiHeader);
                } else if ("className".equals(commentTag.getTag())) {
                    className = commentTag.getValue() == null ? "" : commentTag.getValue().trim();

                    System.out.println("class name :: " + className);
                } else if ("error".equals(commentTag.getTag()) || "throws".equals(commentTag.getTag())) {
                    String exName = commentTag.getName();
                    if (exName != null && errorGroupMap.containsKey(exName)) {
                        ErrorGroupDefinition eg = errorGroupMap.get(exName);
                        methodErrors.add(eg);
                        resStatusMap.put(String.valueOf(eg.getStatus()), eg.getGroup());
                    }
                }
            }
        }

        // throws 자동 감지 (메서드 시그니처의 throws 절)
        for (Class<?> exType : method.getExceptionTypes()) {
            String simpleName = exType.getSimpleName();
            if (errorGroupMap.containsKey(simpleName)) {
                boolean alreadyAdded = methodErrors.stream()
                        .anyMatch(e -> e.getException().equals(simpleName));
                if (!alreadyAdded) {
                    ErrorGroupDefinition eg = errorGroupMap.get(simpleName);
                    methodErrors.add(eg);
                    resStatusMap.put(String.valueOf(eg.getStatus()), eg.getGroup());
                }
            }
        }

        String returnValueType = method.getReturnType().getCanonicalName();

        APIDefinition apiDefinition = new APIDefinition();
        apiDefinition.setName(name);
        apiDefinition.setDescription(description);
        apiDefinition.setClassName(className);
        apiDefinition.setMethod(methodName);
        apiDefinition.setParameters(new ArrayList<>(parameterHashMap.values()));

        // 헤더: isGlobal 판별 (Access-Token, Authorization → 전역 인증 헤더)
        List<APIHeader> headerList = new ArrayList<>(headerHashMap.values());
        for (APIHeader h : headerList) {
            h.setIsGlobal(isGlobalAuthHeader(h.getName()));
        }
        apiDefinition.setHeaders(headerList);
        apiDefinition.setUrlMapping(urlMapping);
        apiDefinition.setNeedsSession(isAuth);
        apiDefinition.setGroupId(groupId);

        if ("".equals(group)) {
            group = "그룹없음";
        }
        apiDefinition.setGroup(group);

        // operationId: 컨트롤러 메서드명 사용 (중복 시 컨트롤러 prefix 추가)
        String operationId = method.getName();
        if (usedOperationIds.contains(operationId)) {
            String prefix = clazz.getSimpleName();
            if (prefix.endsWith("Controller")) {
                prefix = prefix.substring(0, prefix.length() - "Controller".length());
            }
            prefix = Character.toLowerCase(prefix.charAt(0)) + prefix.substring(1);
            operationId = prefix + "_" + method.getName();
        }
        usedOperationIds.add(operationId);
        apiDefinition.setId(operationId);

        if (method.getReturnType() instanceof Class) {
            Class<?> returnType = method.getReturnType();
            if (TypeMapUtils.isCollectionType(returnType.getCanonicalName()) || returnType.getCanonicalName().indexOf("[]") > -1) {
                apiDefinition.setArrayReturn(true);
                if (method.getGenericReturnType() instanceof ParameterizedType) {
                    Type genericType = ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
                    returnValueType = genericType.getTypeName();
                } else if (returnType.getCanonicalName().indexOf("[]") > -1) {
                    returnValueType = returnType.getCanonicalName().substring(0, returnType.getCanonicalName().indexOf("[]"));
                } else {
                    return;
                }
            } else if (returnType.getCanonicalName().startsWith(XPAGE_PREFIX)) {
                apiDefinition.setIsPaging(true);
                apiDefinition.setPagingType(PagingType.XPAGE);
                Type genericType = ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
                returnValueType = genericType.getTypeName();
            } else if (returnType.getCanonicalName().equals(SPRING_PAGE)) {
                apiDefinition.setIsPaging(true);
                apiDefinition.setPagingType(PagingType.SPRING);
                Type genericType = ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments()[0];
                returnValueType = genericType.getTypeName();
            }
            // 제네릭 래퍼를 재귀적으로 언래핑 (ResponseEntity<ApiResponse<List<Dto>>> 등)
            else if (method.getGenericReturnType() instanceof ParameterizedType) {
                Type unwrapped = unwrapGenericType(method.getGenericReturnType());

                if (unwrapped instanceof ParameterizedType) {
                    ParameterizedType pt = (ParameterizedType) unwrapped;
                    Type rawType = pt.getRawType();
                    if (rawType instanceof Class) {
                        String rawName = ((Class<?>) rawType).getName();
                        if (TypeMapUtils.isCollectionType(rawName)) {
                            apiDefinition.setArrayReturn(true);
                            Type innerType = pt.getActualTypeArguments()[0];
                            if (!(innerType instanceof TypeVariable)) {
                                returnValueType = innerType.getTypeName();
                            }
                        } else if (rawName.startsWith(XPAGE_PREFIX)) {
                            apiDefinition.setIsPaging(true);
                            apiDefinition.setPagingType(PagingType.XPAGE);
                            Type innerType = pt.getActualTypeArguments()[0];
                            if (!(innerType instanceof TypeVariable)) {
                                returnValueType = innerType.getTypeName();
                            }
                        } else if (rawName.equals(SPRING_PAGE)) {
                            apiDefinition.setIsPaging(true);
                            apiDefinition.setPagingType(PagingType.SPRING);
                            Type innerType = pt.getActualTypeArguments()[0];
                            if (!(innerType instanceof TypeVariable)) {
                                returnValueType = innerType.getTypeName();
                            }
                        }
                    }
                } else if (unwrapped instanceof Class) {
                    String typeName = ((Class<?>) unwrapped).getTypeName();
                    if (!typeName.equals("java.lang.Void")) {
                        returnValueType = typeName;
                    }
                }
                // TypeVariable인 경우 skip — erasure 타입이 이미 설정됨
            }
        }

        apiDefinition.setReturnClass(returnValueType);
        apiDefinition.setReturnDescription(returnValueDescription);

        if (!resStatusMap.isEmpty()) {
            apiDefinition.setResponseStatus(resStatusMap);
        }

        if (!methodErrors.isEmpty()) {
            apiDefinition.setErrors(methodErrors);
        }

        apiDefinitionArrayList.add(apiDefinition);

        if (!TypeMapUtils.isNormalDataType(apiDefinition.getReturnClass())) {
            String cleanReturnClass = extractInnerType(apiDefinition.getReturnClass());
            apiDefinition.setReturnClass(cleanReturnClass);
            referenceClassSet.add(cleanReturnClass);
        }
    }

    private String getLastComponent(String path, String pathDiv) {
        String[] paths = path.split(pathDiv);
        if (paths.length > 0) {
            return paths[Math.max(paths.length - 1, 0)];
        } else {
            return path;
        }
    }

    /**
     * json을 파일로 쓰기
     */
    private void writeTo(String json, File file) throws Exception {
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        Files.write(file.toPath(), json.getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    /**
     * 참조 클래스 문서 생성
     */
    private void generationReferenceClassJson(final Set<String> refClss) throws Exception {
        refClss.forEach(s -> {
            Class<?> cls = null;

            if (s.equals("T")) {
                return;
            }

            // 참조 모델 생성: JDK/프레임워크 클래스만 제외, basePackage 필터 적용하지 않음
            // (컨트롤러 스캔은 basePackage로 제한하지만, 참조된 DTO/Entity는 패키지 무관하게 생성)
            if (isFrameworkOrLibraryClass(s)) return;

            try {
                cls = loadClassWithFallback(this.baseClassUtil.getClassLoader(), s);
            } catch (ClassNotFoundException e1) {
                e1.printStackTrace();
                return;
            }

            String clsName = cls.getName();
            if (clsName.startsWith(XPAGE_PREFIX) ||
                clsName.equals("one.axim.framework.core.data.XPageNation") ||
                clsName.equals("one.axim.framework.core.data.XOrder") ||
                clsName.equals(SPRING_PAGE) ||
                clsName.equals(SPRING_PAGEABLE) ||
                clsName.equals(SPRING_SORT) ||
                cls.isAssignableFrom(List.class) || cls.isAssignableFrom(ArrayList.class)) {
                return;
            }

            APIModelDefinition modelDefinition = new APIModelDefinition();

            List<APIField> apiFields = new ArrayList<>();
            Set<String> referenceClassSet = new HashSet<>();

            modelDefinition.setName(cls.getName());
            modelDefinition.setType("Object");

            boolean isEnum = cls.isEnum();

            JavaSourceParser srcParser = null;

            if (isEnum) {
                modelDefinition.setType("Enum");
            }

            System.out.println("source find :: " + cls.getName());

            // 파서 캐시: 선언 클래스별로 JavaSourceParser를 캐싱하여 부모 필드 코멘트도 조회
            Map<Class<?>, JavaSourceParser> parserCache = new HashMap<>();

            for (ClassUtils classUtil : classUtils) {
                try {

                    File srcFile = classUtil.getSourceFile(cls);

                    if (srcFile != null) {

                        System.out.println("source path :: " + srcFile.getPath());

                        srcParser = JavaSourceParser.parse(srcFile);

                        if (srcParser != null) {
                            parserCache.put(cls, srcParser);
                            break;
                        }
                    }

                } catch (Exception e) {
                }
            }

            if (srcParser != null) {
                String classComment = srcParser.getClassComment();
                if (classComment != null) {
                    modelDefinition.setDescription(classComment);
                }
            }


            List<Field> fields = getAllFields(cls);

            for (Field field : fields) {

                if (field.isAnnotationPresent(JsonIgnore.class)) continue;

                if (isEnum) {
                    if (field.getName().equals("name") || field.getName().equals("ordinal") || field.getName().startsWith("$")) {
                        continue;
                    }
                }

                APIField apiField = new APIField();
                apiField.setName(field.getName());

                boolean isOptional = !field.getType().isPrimitive()
                        && !isHasAnnotation(field.getAnnotations(), "javax.validation.constraints.NotNull")
                        && !isHasAnnotation(field.getAnnotations(), "jakarta.validation.constraints.NotNull")
                        && !isHasAnnotation(field.getAnnotations(), "javax.validation.constraints.NotBlank")
                        && !isHasAnnotation(field.getAnnotations(), "jakarta.validation.constraints.NotBlank")
                        && !isHasAnnotation(field.getAnnotations(), "javax.validation.constraints.NotEmpty")
                        && !isHasAnnotation(field.getAnnotations(), "jakarta.validation.constraints.NotEmpty")
                        && !hasSizeMinAnnotation(field.getAnnotations());
                apiField.setOptional(isOptional);

                // 상속 필드인 경우 부모 클래스의 소스에서 코멘트를 조회
                JavaSourceParser fieldParser = null;
                Class<?> declaringClass = field.getDeclaringClass();
                if (parserCache.containsKey(declaringClass)) {
                    fieldParser = parserCache.get(declaringClass);
                } else {
                    // 부모 클래스의 소스 파일을 찾아 파서 생성
                    File parentSrcFile = getClassFile(declaringClass);
                    if (parentSrcFile != null) {
                        try {
                            fieldParser = JavaSourceParser.parse(parentSrcFile);
                        } catch (Exception e) {
                            // 파싱 실패 시 무시
                        }
                    }
                    parserCache.put(declaringClass, fieldParser);
                }

                if (fieldParser != null) {
                    String fieldComment = fieldParser.getFieldComment(field.getName());
                    if (fieldComment != null) {
                        apiField.setDescription(fieldComment);
                    }
                }

                // enum 필드 직접 감지 (getSuperClasses 실패에 의존하지 않도록)
                if (!isEnum && field.getType().isEnum()) {
                    apiField.setType("Enum");
                    apiField.setClassPath(field.getType().getCanonicalName());
                    referenceClassSet.add(field.getType().getCanonicalName());
                    apiFields.add(apiField);
                    continue;
                }

                // Set/Collection 직접 감지 (인터페이스는 getSuperClasses에서 누락될 수 있음)
                if (!isEnum && TypeMapUtils.isCollectionType(field.getType().getCanonicalName())) {
                    apiField.setType("Array");
                    Type genericType = field.getGenericType();
                    if (genericType instanceof ParameterizedType) {
                        Type itemType = ((ParameterizedType) genericType).getActualTypeArguments()[0];
                        String innerTypeName = extractInnerType(itemType.getTypeName());
                        apiField.setClassPath(innerTypeName);
                        if (!TypeMapUtils.isNormalDataType(innerTypeName)) {
                            referenceClassSet.add(innerTypeName);
                        }
                    }
                    apiFields.add(apiField);
                    continue;
                }

                boolean isObject = false;

                try {
                    if (!isEnum) {
                        if (!field.getType().isPrimitive()) {
                            List<Class<?>> superTypes = baseClassUtil.getSuperClasses(field.getType());
                            if (superTypes.size() > 0) {
                                if (!TypeMapUtils.isNormalDataType(field.getType().getCanonicalName())) {
                                    for (Class<?> superType : superTypes) {
                                        if (superType != null && superType.getCanonicalName() != null) {
                                            if (superType.getCanonicalName().indexOf("Object") != -1 || superType.getCanonicalName().indexOf("Collection") != -1 || superType.getCanonicalName().indexOf("List") != -1 || superType.getCanonicalName().indexOf("Enum") != -1) {
                                                isObject = true;
                                                break;
                                            }
                                        }
                                    }

                                    if (!isObject) {
                                        if (field.getType().getCanonicalName().indexOf("Object") != -1 || field.getType().getCanonicalName().indexOf("Collection") != -1 || field.getType().getCanonicalName().indexOf("List") != -1 || field.getType().getCanonicalName().indexOf("Enum") != -1) {
                                            isObject = true;
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                apiField.setClassPath(field.getType().getCanonicalName());
                Type fieldType = null;

                if (!isObject) {
                    apiField.setType(getLastComponent(apiField.getClassPath(), "\\."));
                } else {

                    fieldType = field.getGenericType();
                    if (fieldType instanceof TypeVariable) {
                        // 제네릭 타입 파라미터(T)인 경우 — erasure 타입은 이미 classPath에 설정됨
                        apiField.setType("Object");
                    } else if (TypeMapUtils.isCollectionType(fieldType.getTypeName())) {
                        apiField.setType("Array");

                        if (fieldType instanceof ParameterizedType) {
                            Type genericType = ((ParameterizedType) fieldType).getActualTypeArguments()[0];
                            String innerTypeName = genericType.getTypeName();
                            apiField.setClassPath(extractInnerType(innerTypeName));
                        }
                    } else if (fieldType instanceof Class && ((Class<?>) fieldType).isEnum()) {
                        apiField.setType("Enum");
                    } else {
                        apiField.setType("Object");
                    }
                    String cleanClassPath = extractInnerType(apiField.getClassPath());
                    apiField.setClassPath(cleanClassPath);
                    if (!TypeMapUtils.isNormalDataType(cleanClassPath)) {
                        if (cleanClassPath.equals(s)) {
                            // skip same class
                        } else {
                            referenceClassSet.add(cleanClassPath);
                        }
                    }
                }

                apiFields.add(apiField);
            }

            modelDefinition.setFields(apiFields);

            File dir = new File(docDir, "model");

            if (!dir.exists()) {
                dir.mkdirs();
            }

            File file = new File(dir, s + ".json");

            String apiDocJson = gson.toJson(modelDefinition);

            try {

                writeTo(apiDocJson, file);
                if (!referenceClassSet.isEmpty()) {
                    generationReferenceClassJson(referenceClassSet);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void generateQueryParameterModel(Class<?> clazz, Map<String, APIParameter> parameterMap) throws Exception {
        generateQueryParameterModel(clazz, parameterMap, null);
    }

    private void generateQueryParameterModel(Class<?> clazz, Map<String, APIParameter> parameterMap, Set<String> referenceClassSet) throws Exception {

        Log.i("QUERY PARAM", clazz.getName() + " query parameter parse start ");
        File srcFile = this.baseClassUtil.getSourceFile(clazz);
        // 소스파일이 없는 외부 모듈 클래스 대응
        JavaSourceParser srcParser = srcFile != null ? JavaSourceParser.parse(srcFile) : null;

        // 파서 캐시: 상속 필드의 코멘트 조회를 위해
        Map<Class<?>, JavaSourceParser> parserCache = new HashMap<>();
        if (srcParser != null) {
            parserCache.put(clazz, srcParser);
        }

        List<Field> fields = getAllFields(clazz);

        for (Field field : fields) {

            String parameterType = field.getType().getCanonicalName();
            String tempParameterType;

            if (TypeMapUtils.isNormalDataType(parameterType)) {
                tempParameterType = getLastComponent(parameterType, "\\.");
            } else if (field.getType().isEnum()) {
                tempParameterType = "Enum";
            } else {
                tempParameterType = "Object";
            }

            // 상속 필드인 경우 부모 클래스 소스에서 코멘트 조회
            JavaSourceParser fieldParser;
            Class<?> declaringClass = field.getDeclaringClass();
            if (parserCache.containsKey(declaringClass)) {
                fieldParser = parserCache.get(declaringClass);
            } else {
                File parentSrcFile = getClassFile(declaringClass);
                JavaSourceParser parentParser = null;
                if (parentSrcFile != null) {
                    try {
                        parentParser = JavaSourceParser.parse(parentSrcFile);
                    } catch (Exception e) {
                        // 파싱 실패 시 무시
                    }
                }
                parserCache.put(declaringClass, parentParser);
                fieldParser = parentParser;
            }

            APIParameter parameter = new APIParameter();
            parameter.setClassPath(parameterType);
            parameter.setType(tempParameterType);
            parameter.setEnum(field.getType().isEnum());
            parameter.setDescription(fieldParser != null ? fieldParser.getFieldComment(field.getName()) : null);

            boolean isOptional = !field.getType().isPrimitive()
                        && !isHasAnnotation(field.getAnnotations(), "javax.validation.constraints.NotNull")
                        && !isHasAnnotation(field.getAnnotations(), "jakarta.validation.constraints.NotNull")
                        && !isHasAnnotation(field.getAnnotations(), "javax.validation.constraints.NotBlank")
                        && !isHasAnnotation(field.getAnnotations(), "jakarta.validation.constraints.NotBlank")
                        && !isHasAnnotation(field.getAnnotations(), "javax.validation.constraints.NotEmpty")
                        && !isHasAnnotation(field.getAnnotations(), "jakarta.validation.constraints.NotEmpty")
                        && !hasSizeMinAnnotation(field.getAnnotations());

            parameter.setIsOptional(isOptional);
            parameter.setParameterKind(APIParameterKind.REQUEST_PARAMETER);
            parameter.setName(field.getName());

            // enum 필드는 모델 생성을 위해 referenceClassSet에 추가
            if (field.getType().isEnum() && referenceClassSet != null) {
                referenceClassSet.add(parameterType);
            }

            parameterMap.put(parameter.getName(), parameter);

            Log.i("name : ", parameter.getName() + " type : " + parameter.getType());
        }

        Log.i("QUERY PARAM", clazz.getName() + " query parameter parse end ");
    }

    public String getAnnotationValue(Annotation[] annotations, String clsName) {

        if (annotations != null) {
            for (Annotation annotation : annotations) {
                if (annotation.annotationType().getName().equals(clsName)) {

                    Object val = AnnotationHelper.getValue(annotation);

                    if (val instanceof String[])
                        return ((String[]) val)[0];
                    else
                        return val.toString();
                }
            }
        }

        return "";
    }

    public boolean isHasAnnotation(Annotation[] annotations, String clsName) {

        if (annotations != null) {

            for (Annotation annotation : annotations) {
                if (annotation.annotationType().getName().equals(clsName)) {
                    return true;
                }
            }
        }

        return false;
    }

    public Map<String, Object> getAnnotationValueMap(Annotation[] annotations, String clsName) {
        if (annotations != null) {

            for (Annotation annotation : annotations) {
                if (annotation.annotationType().getName().equals(clsName)) {

                    return AnnotationHelper.getAnnotationAttributes(annotation);
                }
            }
        }


        return null;
    }

    private static final Set<String> GLOBAL_AUTH_HEADERS = new HashSet<>(Arrays.asList(
            "Access-Token", "Authorization", "access-token", "authorization"
    ));

    /**
     * 전역 인증 헤더인지 확인한다 (Access-Token, Authorization 등).
     */
    private boolean isGlobalAuthHeader(String headerName) {
        return headerName != null && GLOBAL_AUTH_HEADERS.contains(headerName);
    }

    /**
     * @Size(min=1) 이상이면 required로 판단한다.
     */
    private boolean hasSizeMinAnnotation(Annotation[] annotations) {
        if (annotations == null) return false;
        for (Annotation annotation : annotations) {
            String name = annotation.annotationType().getName();
            if (name.equals("javax.validation.constraints.Size")
                    || name.equals("jakarta.validation.constraints.Size")) {
                Map<String, Object> attrs = AnnotationHelper.getAnnotationAttributes(annotation);
                if (attrs != null && attrs.get("min") != null) {
                    int min = ((Number) attrs.get("min")).intValue();
                    return min >= 1;
                }
            }
        }
        return false;
    }

    /**
     * JDK, Spring, 프레임워크 등 모델 생성 대상이 아닌 라이브러리 클래스인지 확인한다.
     * 참조 모델 생성 시 basePackage 필터 대신 사용하여, 외부 모듈의 Entity/DTO도
     * 정상적으로 schema가 생성되도록 한다.
     */
    private boolean isFrameworkOrLibraryClass(String className) {
        return className.startsWith("java.")
                || className.startsWith("javax.")
                || className.startsWith("jakarta.")
                || className.startsWith("org.springframework.")
                || className.startsWith("org.hibernate.")
                || className.startsWith("com.fasterxml.")
                || className.startsWith("com.google.")
                || className.startsWith("org.apache.")
                || className.startsWith("one.axim.framework.");
    }

    /**
     * DSL excludePackages / excludeClasses 설정에 의해 제외 대상인지 확인한다.
     */
    private boolean isExcludedClass(Class<?> cls) {
        String className = cls.getName();
        String simpleName = cls.getSimpleName();

        for (String pkg : excludePackages) {
            if (className.startsWith(pkg)) {
                return true;
            }
        }
        for (String name : excludeClasses) {
            if (simpleName.equals(name)) {
                return true;
            }
        }
        return false;
    }

    private APIParameter createQueryParameter(String name, String classPath, String type, String description, String defaultValue) {
        APIParameter param = new APIParameter();
        param.setName(name);
        param.setClassPath(classPath);
        param.setType(type);
        param.setDescription(description);
        param.setIsOptional(true);
        param.setParameterKind(APIParameterKind.REQUEST_PARAMETER);
        if (defaultValue != null) {
            param.setDefaultValue(defaultValue);
        }
        return param;
    }

    /**
     * 클래스 계층을 순회하여 Object 직전까지 모든 선언 필드를 수집한다.
     * 하위 클래스 필드가 먼저 오고, 부모 클래스 필드가 뒤에 추가된다.
     */
    private List<Field> getAllFields(Class<?> cls) {
        List<Field> allFields = new ArrayList<>();
        Class<?> current = cls;
        while (current != null && current != Object.class) {
            Collections.addAll(allFields, current.getDeclaredFields());
            current = current.getSuperclass();
        }
        return allFields;
    }

    /**
     * 제네릭 반환 타입을 재귀적으로 언래핑한다.
     * ResponseEntity<ApiResponse<List<UserDto>>> → List<UserDto> 까지 진입.
     * List/ArrayList/LinkedList를 만나면 멈추고 해당 ParameterizedType 반환.
     * 최종 결과가 Class이면 그대로, TypeVariable이면 그대로 반환.
     */
    private Type unwrapGenericType(Type type) {
        while (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;
            Type rawType = pt.getRawType();
            if (rawType instanceof Class) {
                String rawName = ((Class<?>) rawType).getName();
                // 컬렉션 계열이면 여기서 멈춤 (배열 반환 처리를 위해)
                if (TypeMapUtils.isCollectionType(rawName)) {
                    return type; // Collection<X> 그대로 반환
                }
                // Page/XPage 계열이면 여기서 멈춤 (페이징 반환 처리를 위해)
                if (rawName.equals(SPRING_PAGE) ||
                    rawName.startsWith(XPAGE_PREFIX)) {
                    return type;
                }
            }
            // 래퍼를 벗기고 첫 번째 타입 인자로 진입
            Type[] args = pt.getActualTypeArguments();
            if (args.length == 0) break;
            type = args[0];
        }
        return type;
    }

    /**
     * 제네릭 파라미터가 포함된 타입 문자열에서 내부 타입을 추출한다.
     * "java.util.List<com.example.Dto>" → "com.example.Dto"
     * "<"가 없으면 그대로 반환.
     */
    private String extractInnerType(String typeName) {
        if (typeName == null) return typeName;
        int ltIdx = typeName.indexOf('<');
        if (ltIdx < 0) return typeName;
        int gtIdx = typeName.lastIndexOf('>');
        if (gtIdx < 0) return typeName;
        return typeName.substring(ltIdx + 1, gtIdx);
    }

    /**
     * ClassLoader.loadClass()로 클래스를 로드한다.
     * canonical name (예: com.example.Outer.Inner)으로 실패 시,
     * 오른쪽 '.'부터 '$'로 교체하며 재시도하여 inner class (com.example.Outer$Inner)를 처리한다.
     */
    private Class<?> loadClassWithFallback(ClassLoader classLoader, String className) throws ClassNotFoundException {
        try {
            return classLoader.loadClass(className);
        } catch (ClassNotFoundException e) {
            // inner class fallback: 오른쪽 '.'부터 '$'로 교체하며 재시도
            String candidate = className;
            int lastDot;
            while ((lastDot = candidate.lastIndexOf('.')) > 0) {
                candidate = candidate.substring(0, lastDot) + "$" + candidate.substring(lastDot + 1);
                try {
                    return classLoader.loadClass(candidate);
                } catch (ClassNotFoundException ignored) {
                    // 다음 '.'을 시도
                }
            }
            throw e; // 모든 시도 실패 시 원래 예외 던짐
        }
    }
}
