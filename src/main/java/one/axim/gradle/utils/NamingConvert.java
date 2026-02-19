package one.axim.gradle.utils;

/**
 * Created by dudgh on 2017. 5. 27..
 */
public class NamingConvert {

    public static String toUnderScoreName(String str) {

        return str.replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase();
    }

    //    public static String toCamelCase(String s){
    //        String[] parts = s.split("_");
    //        String camelCaseString = "";
    //        for (String part : parts){
    //            camelCaseString = camelCaseString + toProperCase(part);
    //        }
    //        return camelCaseString;
    //    }

    public static String toCamelCase(String value) {

        StringBuilder sb = new StringBuilder(value);

        for (int i = 0; i < sb.length(); i++) {
            if (sb.charAt(i) == '_') {
                sb.deleteCharAt(i);
                sb.replace(i, i + 1, String.valueOf(Character.toUpperCase(sb.charAt(i))));
            }
        }

        return sb.toString();
    }

    public static String toCamelCaseByClassName(String s) {

        String[] parts = s.split("_");
        String camelCaseString = "";
        for (String part : parts) {
            camelCaseString = camelCaseString + toProperCase(part);
        }
        return camelCaseString;
    }

    static String toProperCase(String s) {

        return s.substring(0, 1).toUpperCase() +
                s.substring(1).toLowerCase();
    }
}