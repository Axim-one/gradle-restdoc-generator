package one.axim.gradle;

import one.axim.gradle.data.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * spec-bundle.json의 각 API에 requestSample/responseSample JSON 예시를 자동 생성한다.
 *
 * <p>값 결정 우선순위 (4단계 폴백 체인):
 * <ol>
 *   <li>{@code @XSample("value")} 어노테이션</li>
 *   <li>description 내 괄호 힌트 (예: "체인 타입 (BSC, ETHEREUM)")</li>
 *   <li>필드명 패턴 매칭 (*Address, *Code, *Id 등)</li>
 *   <li>classPath 기반 타입 기본값 (Long→1, Boolean→true 등)</li>
 * </ol>
 *
 * @since 2.1.5
 */
public class SampleGenerator {

    private static final String XSAMPLE_ANNOTATION = "one.axim.gradle.annotation.XSample";
    private static final Pattern DESC_PAREN_PATTERN = Pattern.compile("\\(([^)]+)\\)");
    private static final int MAX_DEPTH = 2;

    private static final ObjectMapper prettyMapper = new ObjectMapper() {{
        enable(SerializationFeature.INDENT_OUTPUT);
    }};

    private final Map<String, APIModelDefinition> models;
    private final ClassLoader classLoader;

    public SampleGenerator(Map<String, APIModelDefinition> models, ClassLoader classLoader) {
        this.models = models;
        this.classLoader = classLoader;
    }

    /**
     * API 엔트리에 requestSample, responseSample을 생성하여 설정한다.
     */
    public void generateSamples(APIDefinition api) {
        api.setRequestSample(buildRequestSample(api));
        api.setResponseSample(buildResponseSample(api));
    }

    // --- Request Sample ---

    private String buildRequestSample(APIDefinition api) {
        String method = api.getMethod();
        if (method == null) return null;
        if (!method.equalsIgnoreCase("POST") && !method.equalsIgnoreCase("PUT") && !method.equalsIgnoreCase("PATCH")) {
            return null;
        }

        if (api.getParameters() == null) return null;

        for (APIParameter param : api.getParameters()) {
            if (param.getParameterKind() == APIParameterKind.REQUEST_BODY) {
                String classPath = param.getClassPath();
                APIModelDefinition model = models.get(classPath);
                if (model == null) return null;

                Map<String, Object> obj = buildObjectFromModel(model, 0, new HashSet<>());
                return toJsonString(obj);
            }
        }
        return null;
    }

    // --- Response Sample ---

    private String buildResponseSample(APIDefinition api) {
        String returnClass = api.getReturnClass();
        if (returnClass == null || "void".equalsIgnoreCase(returnClass)) return null;

        APIModelDefinition model = models.get(returnClass);

        if (model != null) {
            Map<String, Object> obj = buildObjectFromModel(model, 0, new HashSet<>());

            String pagingType = api.getEffectivePagingType();
            if (pagingType != null) {
                return toJsonString(wrapWithPaging(obj, pagingType));
            } else if (api.isArrayReturn()) {
                return toJsonString(Collections.singletonList(obj));
            } else {
                return toJsonString(obj);
            }
        }

        // 원시 타입 반환 (List<String>, Set<Long> 등 포함)
        Object primitiveValue = getTypeDefault(returnClass, 0);
        if (primitiveValue != null) {
            if (api.isArrayReturn()) {
                return toJsonString(Collections.singletonList(primitiveValue));
            }
            return toJsonString(primitiveValue);
        }

        return null;
    }

    // --- Object Builder ---

    private Map<String, Object> buildObjectFromModel(APIModelDefinition model, int depth, Set<String> visited) {
        if (depth > MAX_DEPTH || visited.contains(model.getName())) {
            return Collections.emptyMap();
        }
        visited.add(model.getName());

        Map<String, Object> obj = new LinkedHashMap<>();

        if (model.getFields() == null) return obj;

        // @XSample 리플렉션을 위한 Java 클래스 로드
        Class<?> javaClass = tryLoadClass(model.getName());

        for (APIField field : model.getFields()) {
            // Enum 모델의 경우 필드가 enum 상수 — sample에서는 제외
            if ("Enum".equals(model.getType())) continue;

            Object value = resolveFieldValue(field, javaClass, depth, visited);
            if (value != null) {
                obj.put(field.getName(), value);
            }
        }

        visited.remove(model.getName());
        return obj;
    }

    // --- 4단계 폴백 체인 ---

    private Object resolveFieldValue(APIField field, Class<?> javaClass, int depth, Set<String> visited) {
        // 우선순위 1: @XSample 어노테이션
        if (javaClass != null) {
            String sampleValue = getXSampleValue(javaClass, field.getName());
            if (sampleValue != null) {
                return parseXSampleValue(sampleValue, field.getClassPath());
            }
        }

        // 우선순위 2: description 괄호 힌트
        String descValue = extractFromDescription(field.getDescription());
        if (descValue != null) return descValue;

        // 우선순위 3: 필드명 패턴
        Object patternValue = matchFieldNamePattern(field.getName(), field.getClassPath());
        if (patternValue != null) return patternValue;

        // 우선순위 4: 타입 기본값
        return resolveTypeDefault(field, depth, visited);
    }

    // --- 우선순위 1: @XSample ---

    private String getXSampleValue(Class<?> clazz, String fieldName) {
        try {
            Field javaField = findDeclaredField(clazz, fieldName);
            if (javaField == null) return null;

            for (Annotation ann : javaField.getDeclaredAnnotations()) {
                if (ann.annotationType().getName().equals(XSAMPLE_ANNOTATION)) {
                    try {
                        return (String) ann.annotationType().getMethod("value").invoke(ann);
                    } catch (Exception e) {
                        return null;
                    }
                }
            }
        } catch (Exception e) {
            // 리플렉션 실패 시 폴백
        }
        return null;
    }

    private Field findDeclaredField(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }

    private Object parseXSampleValue(String sampleValue, String classPath) {
        if (classPath == null) return sampleValue;
        switch (classPath) {
            case "java.lang.Long":
            case "long":
                try { return Long.parseLong(sampleValue); } catch (NumberFormatException e) { return sampleValue; }
            case "java.lang.Integer":
            case "int":
                try { return Integer.parseInt(sampleValue); } catch (NumberFormatException e) { return sampleValue; }
            case "java.lang.Boolean":
            case "boolean":
                return Boolean.parseBoolean(sampleValue);
            case "java.lang.Double":
            case "double":
            case "java.lang.Float":
            case "float":
                try { return Double.parseDouble(sampleValue); } catch (NumberFormatException e) { return sampleValue; }
            default:
                return sampleValue; // String, BigDecimal, Enum, DateTime → 문자열 그대로
        }
    }

    // --- 우선순위 2: description 괄호 힌트 ---

    private String extractFromDescription(String description) {
        if (description == null || description.isEmpty()) return null;
        Matcher matcher = DESC_PAREN_PATTERN.matcher(description);
        if (matcher.find()) {
            String content = matcher.group(1);
            String[] values = content.split(",");
            if (values.length > 0) {
                return values[0].trim();
            }
        }
        return null;
    }

    // --- 우선순위 3: 필드명 패턴 ---

    private Object matchFieldNamePattern(String fieldName, String classPath) {
        if (fieldName == null) return null;
        String lower = fieldName.toLowerCase();

        if (lower.endsWith("address")) return "0x1234567890abcdef1234567890abcdef12345678";
        if (lower.endsWith("hash")) return "0xabcdef1234567890abcdef1234567890abcdef1234567890abcdef1234567890";
        if (lower.endsWith("code")) return "CODE-001";
        if (lower.endsWith("id") && !lower.equals("id")) return "user-001";
        if (lower.equals("id")) return 1L;
        if (lower.endsWith("url") || lower.endsWith("link")) return "https://example.com/callback";
        if (lower.endsWith("name")) return "샘플 이름";
        if (lower.equals("memo") || lower.equals("description") || lower.equals("remark")) return "메모";
        if (lower.endsWith("email")) return "user@example.com";
        if (lower.endsWith("phone")) return "010-1234-5678";

        return null;
    }

    // --- 우선순위 4: 타입 기본값 ---

    private Object resolveTypeDefault(APIField field, int depth, Set<String> visited) {
        String classPath = field.getClassPath();
        String type = field.getType();

        // Enum 필드 — 모델에서 첫 번째 값 사용
        if ("Enum".equals(type) || field.isOptional() == false && classPath != null) {
            APIModelDefinition enumModel = models.get(classPath);
            if (enumModel != null && "Enum".equals(enumModel.getType()) && enumModel.getFields() != null && !enumModel.getFields().isEmpty()) {
                return enumModel.getFields().get(0).getName();
            }
        }

        // Array (List, Set)
        if ("Array".equals(type)) {
            return Collections.emptyList();
        }

        // 중첩 Object
        if ("Object".equals(type)) {
            APIModelDefinition nestedModel = models.get(classPath);
            if (nestedModel != null) {
                return buildObjectFromModel(nestedModel, depth + 1, new HashSet<>(visited));
            }
            return Collections.emptyMap();
        }

        return getTypeDefault(classPath, depth);
    }

    private Object getTypeDefault(String classPath, int depth) {
        if (classPath == null) return "sample";
        switch (classPath) {
            case "java.lang.String":
            case "String":
                return "sample";
            case "java.lang.Long":
            case "long":
                return 1L;
            case "java.lang.Integer":
            case "int":
                return 1;
            case "java.lang.Short":
            case "short":
            case "java.lang.Byte":
            case "byte":
                return 1;
            case "java.lang.Character":
            case "char":
                return "A";
            case "java.lang.Boolean":
            case "boolean":
                return true;
            case "java.lang.Number":
            case "java.lang.Double":
            case "double":
            case "java.lang.Float":
            case "float":
                return 0.0;
            case "java.math.BigDecimal":
                return "100.00";
            case "java.time.LocalDateTime":
                return "2026-01-15 14:30:00";
            case "java.time.LocalDate":
                return "2026-01-15";
            case "java.time.LocalTime":
                return "14:30:00";
            case "java.util.Date":
            case "java.time.Instant":
            case "java.time.ZonedDateTime":
            case "java.time.OffsetDateTime":
                return "2026-01-15T14:30:00";
            default:
                return "sample";
        }
    }

    // --- Paging Wrapper ---

    private Map<String, Object> wrapWithPaging(Map<String, Object> content, String pagingType) {
        Map<String, Object> wrapper = new LinkedHashMap<>();
        if (PagingType.XPAGE.equals(pagingType)) {
            wrapper.put("pageRows", Collections.singletonList(content));
            wrapper.put("page", 1);
            wrapper.put("size", 20);
            wrapper.put("offset", 0);
            wrapper.put("totalCount", 1);
            wrapper.put("hasNext", false);
            wrapper.put("sort", null);
            wrapper.put("orders", Collections.emptyList());
        } else if (PagingType.SPRING.equals(pagingType)) {
            wrapper.put("content", Collections.singletonList(content));
            wrapper.put("totalElements", 1);
            wrapper.put("totalPages", 1);
            wrapper.put("size", 20);
            wrapper.put("number", 0);
            wrapper.put("numberOfElements", 1);
            wrapper.put("first", true);
            wrapper.put("last", true);
            wrapper.put("empty", false);
        }
        return wrapper;
    }

    // --- Helpers ---

    private Class<?> tryLoadClass(String className) {
        if (className == null || classLoader == null) return null;
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    private String toJsonString(Object obj) {
        try {
            return prettyMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return null;
        }
    }
}
