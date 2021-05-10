package android.support.p000v4.print;

import android.content.Context;
import android.print.PrintAttributes;

/* renamed from: android.support.v4.print.PrintHelperApi23 */
class PrintHelperApi23 extends PrintHelperApi20 {
    /* access modifiers changed from: protected */
    public PrintAttributes.Builder copyAttributes(PrintAttributes other) {
        PrintAttributes.Builder b = super.copyAttributes(other);
        if (other.getDuplexMode() != 0) {
            b.setDuplexMode(other.getDuplexMode());
        }
        return b;
    }

    PrintHelperApi23(Context context) {
        super(context);
        this.mIsMinMarginsHandlingCorrect = false;
    }
}
