package android.support.p000v4.view.accessibility;

import android.view.accessibility.AccessibilityWindowInfo;

/* renamed from: android.support.v4.view.accessibility.AccessibilityWindowInfoCompatApi24 */
class AccessibilityWindowInfoCompatApi24 {
    AccessibilityWindowInfoCompatApi24() {
    }

    public static CharSequence getTitle(Object info) {
        return ((AccessibilityWindowInfo) info).getTitle();
    }

    public static Object getAnchor(Object info) {
        return ((AccessibilityWindowInfo) info).getAnchor();
    }
}
