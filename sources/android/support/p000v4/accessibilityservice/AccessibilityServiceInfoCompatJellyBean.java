package android.support.p000v4.accessibilityservice;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.pm.PackageManager;

/* renamed from: android.support.v4.accessibilityservice.AccessibilityServiceInfoCompatJellyBean */
class AccessibilityServiceInfoCompatJellyBean {
    AccessibilityServiceInfoCompatJellyBean() {
    }

    public static String loadDescription(AccessibilityServiceInfo info, PackageManager pm) {
        return info.loadDescription(pm);
    }
}
