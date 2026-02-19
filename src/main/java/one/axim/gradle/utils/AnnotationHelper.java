package one.axim.gradle.utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

public class AnnotationHelper {

    public static Object getValue(Annotation annotation) {
        return getValue(annotation, "value");
    }

    public static Object getValue(Annotation annotation, String attributeName) {
        try {
            Method method = annotation.annotationType().getMethod(attributeName);
            return method.invoke(annotation);
        } catch (Exception e) {
            return null;
        }
    }

    public static Map<String, Object> getAnnotationAttributes(Annotation annotation) {
        Map<String, Object> attributes = new HashMap<>();
        for (Method method : annotation.annotationType().getDeclaredMethods()) {
            if (method.getParameterCount() == 0 && method.getDeclaringClass() != Annotation.class) {
                try {
                    attributes.put(method.getName(), method.invoke(annotation));
                } catch (Exception e) {
                    // skip
                }
            }
        }
        return attributes;
    }
}
