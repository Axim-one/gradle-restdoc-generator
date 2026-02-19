package one.axim.gradle.utils;

public class Log {

    private static boolean debug;

    public static boolean isDebug() {
        return debug;
    }

    public static void setDebug(boolean debug) {
        Log.debug = debug;
    }

    /**
     * Debug log
     */
    public static void d(String tag, String msg) {
        if (debug)
            println(Priority.DEBUG, tag, msg);
    }

    /**
     * Info Log
     */
    public static void i(String tag, String msg) {
        println(Priority.INFO, tag, msg);
    }

    /**
     * Warn Log
     */
    public static void w(String tag, String msg) {
        println(Priority.WARN, tag, msg);
    }

    /**
     * Error Log
     */
    public static void e(String tag, String msg) {
        println(Priority.ERROR, tag, msg);
    }

    public static void e(String tag, Throwable tr) {
        println(Priority.ERROR, tag, tr.getMessage());
    }

    private static void println(Priority priority, String tag, String msg) {
        System.out.format("[%s] [%s] %s%n", priority.toString(), tag, msg);
    }

    enum Priority {
        DEBUG, INFO, WARN, ERROR
    }
}
