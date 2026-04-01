package one.axim.gradle.generator.utils;

import one.axim.gradle.generator.LanguageType;

import java.util.HashMap;

public class TypeMapUtils {

    private static final HashMap<String, String> POSTMAN_TYPE_MAP = new HashMap<String, String>();

    static {

        POSTMAN_TYPE_MAP.put("String", "string");
        POSTMAN_TYPE_MAP.put("BigDecimal", "number");
        POSTMAN_TYPE_MAP.put("Boolean", "boolean");
        POSTMAN_TYPE_MAP.put("boolean", "boolean");
        POSTMAN_TYPE_MAP.put("Integer", "number");
        POSTMAN_TYPE_MAP.put("Short", "number");
        POSTMAN_TYPE_MAP.put("Long", "number");
        POSTMAN_TYPE_MAP.put("Float", "number");
        POSTMAN_TYPE_MAP.put("Double", "number");
        POSTMAN_TYPE_MAP.put("byte[]", "number");
        POSTMAN_TYPE_MAP.put("Date", "string");
        POSTMAN_TYPE_MAP.put("Time", "string");
        POSTMAN_TYPE_MAP.put("int", "number");
        POSTMAN_TYPE_MAP.put("long", "number");
        POSTMAN_TYPE_MAP.put("float", "number");
        POSTMAN_TYPE_MAP.put("double", "number");
        POSTMAN_TYPE_MAP.put("short", "number");
        POSTMAN_TYPE_MAP.put("Array", "Array");
    }

    public static boolean isNormalDataType(String type) {
        return type.startsWith("java.math.") || type.startsWith("java.lang.")
                || type.startsWith("java.time.")
                || (type.startsWith("java.util") && !isCollectionType(type))
                || type.indexOf(".") == -1 || type.indexOf("ResponseEntity") != -1;
    }

    /**
     * List, Set, Collection 등 컬렉션 타입인지 확인한다.
     * 이들은 OpenAPI에서 array로 매핑되어야 하므로 "normal data type"이 아니다.
     */
    public static boolean isCollectionType(String type) {
        return type.startsWith("java.util.List")
                || type.startsWith("java.util.ArrayList")
                || type.startsWith("java.util.LinkedList")
                || type.startsWith("java.util.Set")
                || type.startsWith("java.util.HashSet")
                || type.startsWith("java.util.LinkedHashSet")
                || type.startsWith("java.util.TreeSet")
                || type.startsWith("java.util.Collection");
    }

    public static String GetTypeByLanuage(String type, LanguageType languageType) {

        switch (languageType) {
            case JAVA -> {

                return type;
            }
            case POSTMAN -> {

                return POSTMAN_TYPE_MAP.get(type);
            }
            default -> {

                return null;
            }
        }
    }
}
