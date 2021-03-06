package android.support.p003v7.app;

import android.content.Context;
import android.support.p003v7.app.AppCompatDelegateImplV23;
import android.support.p003v7.app.AppCompatDelegateImplV9;
import android.view.KeyboardShortcutGroup;
import android.view.Menu;
import android.view.Window;
import java.util.List;

/* renamed from: android.support.v7.app.AppCompatDelegateImplN */
class AppCompatDelegateImplN extends AppCompatDelegateImplV23 {
    AppCompatDelegateImplN(Context context, Window window, AppCompatCallback callback) {
        super(context, window, callback);
    }

    /* access modifiers changed from: package-private */
    public Window.Callback wrapWindowCallback(Window.Callback callback) {
        return new AppCompatWindowCallbackN(callback);
    }

    /* renamed from: android.support.v7.app.AppCompatDelegateImplN$AppCompatWindowCallbackN */
    class AppCompatWindowCallbackN extends AppCompatDelegateImplV23.AppCompatWindowCallbackV23 {
        AppCompatWindowCallbackN(Window.Callback callback) {
            super(callback);
        }

        public void onProvideKeyboardShortcuts(List<KeyboardShortcutGroup> data, Menu menu, int deviceId) {
            AppCompatDelegateImplV9.PanelFeatureState panel = AppCompatDelegateImplN.this.getPanelState(0, true);
            if (panel == null || panel.menu == null) {
                super.onProvideKeyboardShortcuts(data, menu, deviceId);
            } else {
                super.onProvideKeyboardShortcuts(data, panel.menu, deviceId);
            }
        }
    }
}
