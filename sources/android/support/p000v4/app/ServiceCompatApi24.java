package android.support.p000v4.app;

import android.app.Service;

/* renamed from: android.support.v4.app.ServiceCompatApi24 */
class ServiceCompatApi24 {
    ServiceCompatApi24() {
    }

    public static void stopForeground(Service service, int flags) {
        service.stopForeground(flags);
    }
}
