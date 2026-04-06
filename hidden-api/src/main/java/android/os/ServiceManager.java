package android.os;

public class ServiceManager {
    public static IBinder getService(String ignoredName) {
        throw new RuntimeException();
    }
}