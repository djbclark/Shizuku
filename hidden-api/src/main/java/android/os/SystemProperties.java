package android.os;

public class SystemProperties {
    public static String get(String ignoredKey) {
        throw new RuntimeException();
    }
    public static int getInt(String ignoredKey, int ignoredDef) {
        throw new RuntimeException();
    }
}
