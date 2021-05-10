package android.support.p000v4.view.accessibility;

import android.view.accessibility.AccessibilityEvent;

/* renamed from: android.support.v4.view.accessibility.AccessibilityEventCompatJellyBean */
class AccessibilityEventCompatJellyBean {
    AccessibilityEventCompatJellyBean() {
    }

    public static void setMovementGranularity(AccessibilityEvent event, int granularity) {
        event.setMovementGranularity(granularity);
    }

    public static int getMovementGranularity(AccessibilityEvent event) {
        return event.getMovementGranularity();
    }

    public static void setAction(AccessibilityEvent event, int action) {
        event.setAction(action);
    }

    public static int getAction(AccessibilityEvent event) {
        return event.getAction();
    }
}
