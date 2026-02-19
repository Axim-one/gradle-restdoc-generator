package one.axim.gradle.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;

public class SuperRequestMappingUtil {

    private static final Set<String> MAPPING_ANNOTATIONS = Set.of(
            "org.springframework.web.bind.annotation.GetMapping",
            "org.springframework.web.bind.annotation.PostMapping",
            "org.springframework.web.bind.annotation.PutMapping",
            "org.springframework.web.bind.annotation.DeleteMapping",
            "org.springframework.web.bind.annotation.RequestMapping",
            "org.springframework.web.bind.annotation.PatchMapping"
    );

    private static Annotation findMappingAnnotation(Method method) {
        for (Annotation annotation : method.getAnnotations()) {
            if (MAPPING_ANNOTATIONS.contains(annotation.annotationType().getName())) {
                return annotation;
            }
        }
        return null;
    }

    public static boolean isRequestMappingNull(Method method) {
        return findMappingAnnotation(method) != null;
    }

    public static String value(Method method) {
        Annotation annotation = findMappingAnnotation(method);
        if (annotation != null) {
            Object val = AnnotationHelper.getValue(annotation);
            if (val instanceof String[])
                return ((String[]) val)[0];
            else if (val != null)
                return val.toString();
        }
        return null;
    }

    public static String name(Method method) {
        Annotation annotation = findMappingAnnotation(method);
        if (annotation != null) {
            Object val = AnnotationHelper.getValue(annotation, "name");
            if (val != null)
                return val.toString();
        }
        return "";
    }

    public static String methodName(Method method) {
        Annotation annotation = findMappingAnnotation(method);
        if (annotation == null) return "";

        String name = annotation.annotationType().getName();
        return switch (name) {
            case "org.springframework.web.bind.annotation.GetMapping" -> "GET";
            case "org.springframework.web.bind.annotation.PostMapping" -> "POST";
            case "org.springframework.web.bind.annotation.PutMapping" -> "PUT";
            case "org.springframework.web.bind.annotation.DeleteMapping" -> "DELETE";
            case "org.springframework.web.bind.annotation.PatchMapping" -> "PATCH";
            default -> "";
        };
    }
}
