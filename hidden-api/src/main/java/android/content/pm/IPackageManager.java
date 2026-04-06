package android.content.pm;

import android.os.Binder;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

import java.util.List;

public interface IPackageManager extends IInterface {
    List<PackageInfo> getInstalledPackagesAsUser(int flags, int userId)
            throws RemoteException;

    abstract class Stub extends Binder implements IPackageManager {
        public static IPackageManager asInterface(IBinder ignoredObj) {
            throw new RuntimeException();
        }
    }
}
