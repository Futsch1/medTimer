package android.util;

/**
 * @noinspection ALL
 */
public class Log {
    public static boolean enable = true;

    public static int d(String tag, String msg) {
        if (enable)
            System.out.println("DEBUG: " + tag + ": " + msg);
        return 0;
    }

    public static int i(String tag, String msg) {
        if (enable)
            System.out.println("INFO: " + tag + ": " + msg);
        return 0;
    }

    public static int w(String tag, String msg) {
        if (enable)
            System.out.println("WARN: " + tag + ": " + msg);
        return 0;
    }

    public static int e(String tag, String msg) {
        if (enable)
            System.out.println("ERROR: " + tag + ": " + msg);
        return 0;
    }

    // add other methods if required...
}
