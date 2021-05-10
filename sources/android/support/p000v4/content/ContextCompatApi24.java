package android.support.p000v4.content;

import android.content.Context;
import java.io.File;

/* renamed from: android.support.v4.content.ContextCompatApi24 */
class ContextCompatApi24 {
    ContextCompatApi24() {
    }

    public static File getDataDir(Context context) {
        return context.getDataDir();
    }

    public static Context createDeviceProtectedStorageContext(Context context) {
        return context.createDeviceProtectedStorageContext();
    }

    public static boolean isDeviceProtectedStorage(Context context) {
        return context.isDeviceProtectedStorage();
    }
}
