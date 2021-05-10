package android.support.p000v4.app;

import android.content.Context;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.support.annotation.CallSuper;
import android.support.p000v4.app.BackStackRecord;
import android.support.p000v4.app.Fragment;
import android.support.p000v4.app.FragmentManager;
import android.support.p000v4.p002os.BuildCompat;
import android.support.p000v4.util.DebugUtils;
import android.support.p000v4.util.LogWriter;
import android.support.p000v4.view.LayoutInflaterFactory;
import android.support.p000v4.view.ViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.ScaleAnimation;
import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/* renamed from: android.support.v4.app.FragmentManagerImpl */
/* compiled from: FragmentManager */
final class FragmentManagerImpl extends FragmentManager implements LayoutInflaterFactory {
    static final Interpolator ACCELERATE_CUBIC = new AccelerateInterpolator(1.5f);
    static final Interpolator ACCELERATE_QUINT = new AccelerateInterpolator(2.5f);
    static final int ANIM_DUR = 220;
    public static final int ANIM_STYLE_CLOSE_ENTER = 3;
    public static final int ANIM_STYLE_CLOSE_EXIT = 4;
    public static final int ANIM_STYLE_FADE_ENTER = 5;
    public static final int ANIM_STYLE_FADE_EXIT = 6;
    public static final int ANIM_STYLE_OPEN_ENTER = 1;
    public static final int ANIM_STYLE_OPEN_EXIT = 2;
    static boolean DEBUG = false;
    static final Interpolator DECELERATE_CUBIC = new DecelerateInterpolator(1.5f);
    static final Interpolator DECELERATE_QUINT = new DecelerateInterpolator(2.5f);
    static final boolean HONEYCOMB;
    static final String TAG = "FragmentManager";
    static final String TARGET_REQUEST_CODE_STATE_TAG = "android:target_req_state";
    static final String TARGET_STATE_TAG = "android:target_state";
    static final String USER_VISIBLE_HINT_TAG = "android:user_visible_hint";
    static final String VIEW_STATE_TAG = "android:view_state";
    static Field sAnimationListenerField = null;
    ArrayList<Fragment> mActive;
    ArrayList<Fragment> mAdded;
    ArrayList<Integer> mAvailBackStackIndices;
    ArrayList<Integer> mAvailIndices;
    ArrayList<BackStackRecord> mBackStack;
    ArrayList<FragmentManager.OnBackStackChangedListener> mBackStackChangeListeners;
    ArrayList<BackStackRecord> mBackStackIndices;
    FragmentContainer mContainer;
    FragmentController mController;
    ArrayList<Fragment> mCreatedMenus;
    int mCurState = 0;
    boolean mDestroyed;
    Runnable mExecCommit = new Runnable() {
        public void run() {
            FragmentManagerImpl.this.execPendingActions();
        }
    };
    boolean mExecutingActions;
    boolean mHavePendingDeferredStart;
    FragmentHostCallback mHost;
    boolean mNeedMenuInvalidate;
    String mNoTransactionsBecause;
    Fragment mParent;
    ArrayList<Runnable> mPendingActions;
    SparseArray<Parcelable> mStateArray = null;
    Bundle mStateBundle = null;
    boolean mStateSaved;
    Runnable[] mTmpActions;

    FragmentManagerImpl() {
    }

    static {
        boolean z = false;
        if (Build.VERSION.SDK_INT >= 11) {
            z = true;
        }
        HONEYCOMB = z;
    }

    /* renamed from: android.support.v4.app.FragmentManagerImpl$AnimateOnHWLayerIfNeededListener */
    /* compiled from: FragmentManager */
    static class AnimateOnHWLayerIfNeededListener implements Animation.AnimationListener {
        private Animation.AnimationListener mOriginalListener;
        private boolean mShouldRunOnHWLayer;
        View mView;

        public AnimateOnHWLayerIfNeededListener(View v, Animation anim) {
            if (v != null && anim != null) {
                this.mView = v;
            }
        }

        public AnimateOnHWLayerIfNeededListener(View v, Animation anim, Animation.AnimationListener listener) {
            if (v != null && anim != null) {
                this.mOriginalListener = listener;
                this.mView = v;
                this.mShouldRunOnHWLayer = true;
            }
        }

        @CallSuper
        public void onAnimationStart(Animation animation) {
            if (this.mOriginalListener != null) {
                this.mOriginalListener.onAnimationStart(animation);
            }
        }

        @CallSuper
        public void onAnimationEnd(Animation animation) {
            if (this.mView != null && this.mShouldRunOnHWLayer) {
                if (ViewCompat.isAttachedToWindow(this.mView) || BuildCompat.isAtLeastN()) {
                    this.mView.post(new Runnable() {
                        public void run() {
                            ViewCompat.setLayerType(AnimateOnHWLayerIfNeededListener.this.mView, 0, (Paint) null);
                        }
                    });
                } else {
                    ViewCompat.setLayerType(this.mView, 0, (Paint) null);
                }
            }
            if (this.mOriginalListener != null) {
                this.mOriginalListener.onAnimationEnd(animation);
            }
        }

        public void onAnimationRepeat(Animation animation) {
            if (this.mOriginalListener != null) {
                this.mOriginalListener.onAnimationRepeat(animation);
            }
        }
    }

    static boolean modifiesAlpha(Animation anim) {
        if (anim instanceof AlphaAnimation) {
            return true;
        }
        if (anim instanceof AnimationSet) {
            List<Animation> anims = ((AnimationSet) anim).getAnimations();
            for (int i = 0; i < anims.size(); i++) {
                if (anims.get(i) instanceof AlphaAnimation) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean shouldRunOnHWLayer(View v, Animation anim) {
        return Build.VERSION.SDK_INT >= 19 && ViewCompat.getLayerType(v) == 0 && ViewCompat.hasOverlappingRendering(v) && modifiesAlpha(anim);
    }

    private void throwException(RuntimeException ex) {
        Log.e(TAG, ex.getMessage());
        Log.e(TAG, "Activity state:");
        PrintWriter pw = new PrintWriter(new LogWriter(TAG));
        if (this.mHost != null) {
            try {
                this.mHost.onDump("  ", (FileDescriptor) null, pw, new String[0]);
            } catch (Exception e) {
                Log.e(TAG, "Failed dumping state", e);
            }
        } else {
            try {
                dump("  ", (FileDescriptor) null, pw, new String[0]);
            } catch (Exception e2) {
                Log.e(TAG, "Failed dumping state", e2);
            }
        }
        throw ex;
    }

    public FragmentTransaction beginTransaction() {
        return new BackStackRecord(this);
    }

    public boolean executePendingTransactions() {
        return execPendingActions();
    }

    public void popBackStack() {
        enqueueAction(new Runnable() {
            public void run() {
                FragmentManagerImpl.this.popBackStackState(FragmentManagerImpl.this.mHost.getHandler(), (String) null, -1, 0);
            }
        }, false);
    }

    public boolean popBackStackImmediate() {
        checkStateLoss();
        executePendingTransactions();
        return popBackStackState(this.mHost.getHandler(), (String) null, -1, 0);
    }

    public void popBackStack(final String name, final int flags) {
        enqueueAction(new Runnable() {
            public void run() {
                FragmentManagerImpl.this.popBackStackState(FragmentManagerImpl.this.mHost.getHandler(), name, -1, flags);
            }
        }, false);
    }

    public boolean popBackStackImmediate(String name, int flags) {
        checkStateLoss();
        executePendingTransactions();
        return popBackStackState(this.mHost.getHandler(), name, -1, flags);
    }

    public void popBackStack(final int id, final int flags) {
        if (id < 0) {
            throw new IllegalArgumentException("Bad id: " + id);
        }
        enqueueAction(new Runnable() {
            public void run() {
                FragmentManagerImpl.this.popBackStackState(FragmentManagerImpl.this.mHost.getHandler(), (String) null, id, flags);
            }
        }, false);
    }

    public boolean popBackStackImmediate(int id, int flags) {
        checkStateLoss();
        executePendingTransactions();
        if (id >= 0) {
            return popBackStackState(this.mHost.getHandler(), (String) null, id, flags);
        }
        throw new IllegalArgumentException("Bad id: " + id);
    }

    public int getBackStackEntryCount() {
        if (this.mBackStack != null) {
            return this.mBackStack.size();
        }
        return 0;
    }

    public FragmentManager.BackStackEntry getBackStackEntryAt(int index) {
        return this.mBackStack.get(index);
    }

    public void addOnBackStackChangedListener(FragmentManager.OnBackStackChangedListener listener) {
        if (this.mBackStackChangeListeners == null) {
            this.mBackStackChangeListeners = new ArrayList<>();
        }
        this.mBackStackChangeListeners.add(listener);
    }

    public void removeOnBackStackChangedListener(FragmentManager.OnBackStackChangedListener listener) {
        if (this.mBackStackChangeListeners != null) {
            this.mBackStackChangeListeners.remove(listener);
        }
    }

    public void putFragment(Bundle bundle, String key, Fragment fragment) {
        if (fragment.mIndex < 0) {
            throwException(new IllegalStateException("Fragment " + fragment + " is not currently in the FragmentManager"));
        }
        bundle.putInt(key, fragment.mIndex);
    }

    public Fragment getFragment(Bundle bundle, String key) {
        int index = bundle.getInt(key, -1);
        if (index == -1) {
            return null;
        }
        if (index >= this.mActive.size()) {
            throwException(new IllegalStateException("Fragment no longer exists for key " + key + ": index " + index));
        }
        Fragment f = this.mActive.get(index);
        if (f != null) {
            return f;
        }
        throwException(new IllegalStateException("Fragment no longer exists for key " + key + ": index " + index));
        return f;
    }

    public List<Fragment> getFragments() {
        return this.mActive;
    }

    public Fragment.SavedState saveFragmentInstanceState(Fragment fragment) {
        Bundle result;
        if (fragment.mIndex < 0) {
            throwException(new IllegalStateException("Fragment " + fragment + " is not currently in the FragmentManager"));
        }
        if (fragment.mState <= 0 || (result = saveFragmentBasicState(fragment)) == null) {
            return null;
        }
        return new Fragment.SavedState(result);
    }

    public boolean isDestroyed() {
        return this.mDestroyed;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder(128);
        sb.append("FragmentManager{");
        sb.append(Integer.toHexString(System.identityHashCode(this)));
        sb.append(" in ");
        if (this.mParent != null) {
            DebugUtils.buildShortClassTag(this.mParent, sb);
        } else {
            DebugUtils.buildShortClassTag(this.mHost, sb);
        }
        sb.append("}}");
        return sb.toString();
    }

    public void dump(String prefix, FileDescriptor fd, PrintWriter writer, String[] args) {
        int N;
        int N2;
        int N3;
        int N4;
        int N5;
        int N6;
        String innerPrefix = prefix + "    ";
        if (this.mActive != null && (N6 = this.mActive.size()) > 0) {
            writer.print(prefix);
            writer.print("Active Fragments in ");
            writer.print(Integer.toHexString(System.identityHashCode(this)));
            writer.println(":");
            for (int i = 0; i < N6; i++) {
                Fragment f = this.mActive.get(i);
                writer.print(prefix);
                writer.print("  #");
                writer.print(i);
                writer.print(": ");
                writer.println(f);
                if (f != null) {
                    f.dump(innerPrefix, fd, writer, args);
                }
            }
        }
        if (this.mAdded != null && (N5 = this.mAdded.size()) > 0) {
            writer.print(prefix);
            writer.println("Added Fragments:");
            for (int i2 = 0; i2 < N5; i2++) {
                writer.print(prefix);
                writer.print("  #");
                writer.print(i2);
                writer.print(": ");
                writer.println(this.mAdded.get(i2).toString());
            }
        }
        if (this.mCreatedMenus != null && (N4 = this.mCreatedMenus.size()) > 0) {
            writer.print(prefix);
            writer.println("Fragments Created Menus:");
            for (int i3 = 0; i3 < N4; i3++) {
                writer.print(prefix);
                writer.print("  #");
                writer.print(i3);
                writer.print(": ");
                writer.println(this.mCreatedMenus.get(i3).toString());
            }
        }
        if (this.mBackStack != null && (N3 = this.mBackStack.size()) > 0) {
            writer.print(prefix);
            writer.println("Back Stack:");
            for (int i4 = 0; i4 < N3; i4++) {
                BackStackRecord bs = this.mBackStack.get(i4);
                writer.print(prefix);
                writer.print("  #");
                writer.print(i4);
                writer.print(": ");
                writer.println(bs.toString());
                bs.dump(innerPrefix, fd, writer, args);
            }
        }
        synchronized (this) {
            if (this.mBackStackIndices != null && (N2 = this.mBackStackIndices.size()) > 0) {
                writer.print(prefix);
                writer.println("Back Stack Indices:");
                for (int i5 = 0; i5 < N2; i5++) {
                    writer.print(prefix);
                    writer.print("  #");
                    writer.print(i5);
                    writer.print(": ");
                    writer.println(this.mBackStackIndices.get(i5));
                }
            }
            if (this.mAvailBackStackIndices != null && this.mAvailBackStackIndices.size() > 0) {
                writer.print(prefix);
                writer.print("mAvailBackStackIndices: ");
                writer.println(Arrays.toString(this.mAvailBackStackIndices.toArray()));
            }
        }
        if (this.mPendingActions != null && (N = this.mPendingActions.size()) > 0) {
            writer.print(prefix);
            writer.println("Pending Actions:");
            for (int i6 = 0; i6 < N; i6++) {
                writer.print(prefix);
                writer.print("  #");
                writer.print(i6);
                writer.print(": ");
                writer.println(this.mPendingActions.get(i6));
            }
        }
        writer.print(prefix);
        writer.println("FragmentManager misc state:");
        writer.print(prefix);
        writer.print("  mHost=");
        writer.println(this.mHost);
        writer.print(prefix);
        writer.print("  mContainer=");
        writer.println(this.mContainer);
        if (this.mParent != null) {
            writer.print(prefix);
            writer.print("  mParent=");
            writer.println(this.mParent);
        }
        writer.print(prefix);
        writer.print("  mCurState=");
        writer.print(this.mCurState);
        writer.print(" mStateSaved=");
        writer.print(this.mStateSaved);
        writer.print(" mDestroyed=");
        writer.println(this.mDestroyed);
        if (this.mNeedMenuInvalidate) {
            writer.print(prefix);
            writer.print("  mNeedMenuInvalidate=");
            writer.println(this.mNeedMenuInvalidate);
        }
        if (this.mNoTransactionsBecause != null) {
            writer.print(prefix);
            writer.print("  mNoTransactionsBecause=");
            writer.println(this.mNoTransactionsBecause);
        }
        if (this.mAvailIndices != null && this.mAvailIndices.size() > 0) {
            writer.print(prefix);
            writer.print("  mAvailIndices: ");
            writer.println(Arrays.toString(this.mAvailIndices.toArray()));
        }
    }

    static Animation makeOpenCloseAnimation(Context context, float startScale, float endScale, float startAlpha, float endAlpha) {
        AnimationSet set = new AnimationSet(false);
        ScaleAnimation scale = new ScaleAnimation(startScale, endScale, startScale, endScale, 1, 0.5f, 1, 0.5f);
        scale.setInterpolator(DECELERATE_QUINT);
        scale.setDuration(220);
        set.addAnimation(scale);
        AlphaAnimation alpha = new AlphaAnimation(startAlpha, endAlpha);
        alpha.setInterpolator(DECELERATE_CUBIC);
        alpha.setDuration(220);
        set.addAnimation(alpha);
        return set;
    }

    static Animation makeFadeAnimation(Context context, float start, float end) {
        AlphaAnimation anim = new AlphaAnimation(start, end);
        anim.setInterpolator(DECELERATE_CUBIC);
        anim.setDuration(220);
        return anim;
    }

    /* access modifiers changed from: package-private */
    public Animation loadAnimation(Fragment fragment, int transit, boolean enter, int transitionStyle) {
        Animation anim;
        Animation animObj = fragment.onCreateAnimation(transit, enter, fragment.mNextAnim);
        if (animObj != null) {
            return animObj;
        }
        if (fragment.mNextAnim != 0 && (anim = AnimationUtils.loadAnimation(this.mHost.getContext(), fragment.mNextAnim)) != null) {
            return anim;
        }
        if (transit == 0) {
            return null;
        }
        int styleIndex = transitToStyleIndex(transit, enter);
        if (styleIndex < 0) {
            return null;
        }
        switch (styleIndex) {
            case 1:
                return makeOpenCloseAnimation(this.mHost.getContext(), 1.125f, 1.0f, 0.0f, 1.0f);
            case 2:
                return makeOpenCloseAnimation(this.mHost.getContext(), 1.0f, 0.975f, 1.0f, 0.0f);
            case 3:
                return makeOpenCloseAnimation(this.mHost.getContext(), 0.975f, 1.0f, 0.0f, 1.0f);
            case 4:
                return makeOpenCloseAnimation(this.mHost.getContext(), 1.0f, 1.075f, 1.0f, 0.0f);
            case 5:
                return makeFadeAnimation(this.mHost.getContext(), 0.0f, 1.0f);
            case 6:
                return makeFadeAnimation(this.mHost.getContext(), 1.0f, 0.0f);
            default:
                if (transitionStyle == 0 && this.mHost.onHasWindowAnimations()) {
                    transitionStyle = this.mHost.onGetWindowAnimations();
                }
                if (transitionStyle == 0) {
                    return null;
                }
                return null;
        }
    }

    public void performPendingDeferredStart(Fragment f) {
        if (!f.mDeferStart) {
            return;
        }
        if (this.mExecutingActions) {
            this.mHavePendingDeferredStart = true;
            return;
        }
        f.mDeferStart = false;
        moveToState(f, this.mCurState, 0, 0, false);
    }

    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r3v7, resolved type: java.lang.Object} */
    /* JADX DEBUG: Multi-variable search result rejected for TypeSearchVarInfo{r0v0, resolved type: android.view.animation.Animation$AnimationListener} */
    /* JADX WARNING: Multi-variable type inference failed */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void setHWLayerAnimListenerIfAlpha(android.view.View r6, android.view.animation.Animation r7) {
        /*
            r5 = this;
            if (r6 == 0) goto L_0x0004
            if (r7 != 0) goto L_0x0005
        L_0x0004:
            return
        L_0x0005:
            boolean r3 = shouldRunOnHWLayer(r6, r7)
            if (r3 == 0) goto L_0x0004
            r2 = 0
            java.lang.reflect.Field r3 = sAnimationListenerField     // Catch:{ NoSuchFieldException -> 0x0038, IllegalAccessException -> 0x0041 }
            if (r3 != 0) goto L_0x0020
            java.lang.Class<android.view.animation.Animation> r3 = android.view.animation.Animation.class
            java.lang.String r4 = "mListener"
            java.lang.reflect.Field r3 = r3.getDeclaredField(r4)     // Catch:{ NoSuchFieldException -> 0x0038, IllegalAccessException -> 0x0041 }
            sAnimationListenerField = r3     // Catch:{ NoSuchFieldException -> 0x0038, IllegalAccessException -> 0x0041 }
            java.lang.reflect.Field r3 = sAnimationListenerField     // Catch:{ NoSuchFieldException -> 0x0038, IllegalAccessException -> 0x0041 }
            r4 = 1
            r3.setAccessible(r4)     // Catch:{ NoSuchFieldException -> 0x0038, IllegalAccessException -> 0x0041 }
        L_0x0020:
            java.lang.reflect.Field r3 = sAnimationListenerField     // Catch:{ NoSuchFieldException -> 0x0038, IllegalAccessException -> 0x0041 }
            java.lang.Object r3 = r3.get(r7)     // Catch:{ NoSuchFieldException -> 0x0038, IllegalAccessException -> 0x0041 }
            r0 = r3
            android.view.animation.Animation$AnimationListener r0 = (android.view.animation.Animation.AnimationListener) r0     // Catch:{ NoSuchFieldException -> 0x0038, IllegalAccessException -> 0x0041 }
            r2 = r0
        L_0x002a:
            r3 = 2
            r4 = 0
            android.support.p000v4.view.ViewCompat.setLayerType(r6, r3, r4)
            android.support.v4.app.FragmentManagerImpl$AnimateOnHWLayerIfNeededListener r3 = new android.support.v4.app.FragmentManagerImpl$AnimateOnHWLayerIfNeededListener
            r3.<init>(r6, r7, r2)
            r7.setAnimationListener(r3)
            goto L_0x0004
        L_0x0038:
            r1 = move-exception
            java.lang.String r3 = "FragmentManager"
            java.lang.String r4 = "No field with the name mListener is found in Animation class"
            android.util.Log.e(r3, r4, r1)
            goto L_0x002a
        L_0x0041:
            r1 = move-exception
            java.lang.String r3 = "FragmentManager"
            java.lang.String r4 = "Cannot access Animation's mListener field"
            android.util.Log.e(r3, r4, r1)
            goto L_0x002a
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.p000v4.app.FragmentManagerImpl.setHWLayerAnimListenerIfAlpha(android.view.View, android.view.animation.Animation):void");
    }

    /* access modifiers changed from: package-private */
    public boolean isStateAtLeast(int state) {
        return this.mCurState >= state;
    }

    /* access modifiers changed from: package-private */
    /* JADX WARNING: Code restructure failed: missing block: B:128:0x0405, code lost:
        r14 = android.support.p000v4.p002os.EnvironmentCompat.MEDIA_UNKNOWN;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:136:0x0432, code lost:
        if (r19 >= 1) goto L_0x0070;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:138:0x0438, code lost:
        if (r17.mDestroyed == false) goto L_0x044c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:140:0x043e, code lost:
        if (r18.mAnimatingAway == null) goto L_0x044c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:141:0x0440, code lost:
        r15 = r18.mAnimatingAway;
        r18.mAnimatingAway = null;
        r15.clearAnimation();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:143:0x0450, code lost:
        if (r18.mAnimatingAway == null) goto L_0x057c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:144:0x0452, code lost:
        r18.mStateAfterAnimating = r19;
        r19 = 1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:152:0x0485, code lost:
        if (r19 >= 4) goto L_0x04a8;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:154:0x0489, code lost:
        if (DEBUG == false) goto L_0x04a5;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:155:0x048b, code lost:
        android.util.Log.v(TAG, "movefrom STARTED: " + r18);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:156:0x04a5, code lost:
        r18.performStop();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:158:0x04ab, code lost:
        if (r19 >= 3) goto L_0x04ce;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:160:0x04af, code lost:
        if (DEBUG == false) goto L_0x04cb;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:161:0x04b1, code lost:
        android.util.Log.v(TAG, "movefrom STOPPED: " + r18);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:162:0x04cb, code lost:
        r18.performReallyStop();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:164:0x04d1, code lost:
        if (r19 >= 2) goto L_0x042f;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:166:0x04d5, code lost:
        if (DEBUG == false) goto L_0x04f1;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:167:0x04d7, code lost:
        android.util.Log.v(TAG, "movefrom ACTIVITY_CREATED: " + r18);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:169:0x04f5, code lost:
        if (r18.mView == null) goto L_0x050c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:171:0x0501, code lost:
        if (r17.mHost.onShouldSaveFragmentState(r18) == false) goto L_0x050c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:173:0x0507, code lost:
        if (r18.mSavedViewState != null) goto L_0x050c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:174:0x0509, code lost:
        saveFragmentViewState(r18);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:175:0x050c, code lost:
        r18.performDestroyView();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:176:0x0513, code lost:
        if (r18.mView == null) goto L_0x056b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:178:0x0519, code lost:
        if (r18.mContainer == null) goto L_0x056b;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:179:0x051b, code lost:
        r10 = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:180:0x0520, code lost:
        if (r17.mCurState <= 0) goto L_0x0535;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:182:0x0526, code lost:
        if (r17.mDestroyed != false) goto L_0x0535;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:183:0x0528, code lost:
        r10 = loadAnimation(r18, r20, false, r21);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:184:0x0535, code lost:
        if (r10 == null) goto L_0x0560;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:185:0x0537, code lost:
        r13 = r18;
        r18.mAnimatingAway = r18.mView;
        r18.mStateAfterAnimating = r19;
        r10.setAnimationListener(new android.support.p000v4.app.FragmentManagerImpl.C00195(r17, r18.mView, r10));
        r18.mView.startAnimation(r10);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:186:0x0560, code lost:
        r18.mContainer.removeView(r18.mView);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:187:0x056b, code lost:
        r18.mContainer = null;
        r18.mView = null;
        r18.mInnerView = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:189:0x057e, code lost:
        if (DEBUG == false) goto L_0x059a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:190:0x0580, code lost:
        android.util.Log.v(TAG, "movefrom CREATED: " + r18);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:192:0x059e, code lost:
        if (r18.mRetaining != false) goto L_0x05b3;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:193:0x05a0, code lost:
        r18.performDestroy();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:194:0x05a3, code lost:
        r18.performDetach();
     */
    /* JADX WARNING: Code restructure failed: missing block: B:195:0x05a6, code lost:
        if (r22 != false) goto L_0x0070;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:197:0x05ac, code lost:
        if (r18.mRetaining != false) goto L_0x05b9;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:198:0x05ae, code lost:
        makeInactive(r18);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:199:0x05b3, code lost:
        r18.mState = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:200:0x05b9, code lost:
        r18.mHost = null;
        r18.mParentFragment = null;
        r18.mFragmentManager = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:72:0x0223, code lost:
        if (r19 <= 1) goto L_0x0372;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:74:0x0227, code lost:
        if (DEBUG == false) goto L_0x0243;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:75:0x0229, code lost:
        android.util.Log.v(TAG, "moveto ACTIVITY_CREATED: " + r18);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:77:0x0247, code lost:
        if (r18.mFromLayout != false) goto L_0x0355;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:78:0x0249, code lost:
        r11 = null;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:79:0x024e, code lost:
        if (r18.mContainerId == 0) goto L_0x02d9;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:81:0x0255, code lost:
        if (r18.mContainerId != -1) goto L_0x027c;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:82:0x0257, code lost:
        throwException(new java.lang.IllegalArgumentException("Cannot create fragment " + r18 + " for a container view with no id"));
     */
    /* JADX WARNING: Code restructure failed: missing block: B:83:0x027c, code lost:
        r11 = (android.view.ViewGroup) r17.mContainer.onFindViewById(r18.mContainerId);
     */
    /* JADX WARNING: Code restructure failed: missing block: B:84:0x028a, code lost:
        if (r11 != null) goto L_0x02d9;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:86:0x0290, code lost:
        if (r18.mRestored != false) goto L_0x02d9;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:88:?, code lost:
        r14 = r18.getResources().getResourceName(r18.mContainerId);
     */
    /* JADX WARNING: Removed duplicated region for block: B:202:? A[RETURN, SYNTHETIC] */
    /* JADX WARNING: Removed duplicated region for block: B:32:0x0078  */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public void moveToState(android.support.p000v4.app.Fragment r18, int r19, int r20, int r21, boolean r22) {
        /*
            r17 = this;
            r0 = r18
            boolean r4 = r0.mAdded
            if (r4 == 0) goto L_0x000c
            r0 = r18
            boolean r4 = r0.mDetached
            if (r4 == 0) goto L_0x0013
        L_0x000c:
            r4 = 1
            r0 = r19
            if (r0 <= r4) goto L_0x0013
            r19 = 1
        L_0x0013:
            r0 = r18
            boolean r4 = r0.mRemoving
            if (r4 == 0) goto L_0x0027
            r0 = r18
            int r4 = r0.mState
            r0 = r19
            if (r0 <= r4) goto L_0x0027
            r0 = r18
            int r0 = r0.mState
            r19 = r0
        L_0x0027:
            r0 = r18
            boolean r4 = r0.mDeferStart
            if (r4 == 0) goto L_0x003b
            r0 = r18
            int r4 = r0.mState
            r5 = 4
            if (r4 >= r5) goto L_0x003b
            r4 = 3
            r0 = r19
            if (r0 <= r4) goto L_0x003b
            r19 = 3
        L_0x003b:
            r0 = r18
            int r4 = r0.mState
            r0 = r19
            if (r4 >= r0) goto L_0x041e
            r0 = r18
            boolean r4 = r0.mFromLayout
            if (r4 == 0) goto L_0x0050
            r0 = r18
            boolean r4 = r0.mInLayout
            if (r4 != 0) goto L_0x0050
        L_0x004f:
            return
        L_0x0050:
            r0 = r18
            android.view.View r4 = r0.mAnimatingAway
            if (r4 == 0) goto L_0x0069
            r4 = 0
            r0 = r18
            r0.mAnimatingAway = r4
            r0 = r18
            int r6 = r0.mStateAfterAnimating
            r7 = 0
            r8 = 0
            r9 = 1
            r4 = r17
            r5 = r18
            r4.moveToState(r5, r6, r7, r8, r9)
        L_0x0069:
            r0 = r18
            int r4 = r0.mState
            switch(r4) {
                case 0: goto L_0x00b9;
                case 1: goto L_0x0220;
                case 2: goto L_0x0372;
                case 3: goto L_0x037c;
                case 4: goto L_0x03a2;
                default: goto L_0x0070;
            }
        L_0x0070:
            r0 = r18
            int r4 = r0.mState
            r0 = r19
            if (r4 == r0) goto L_0x004f
            java.lang.String r4 = "FragmentManager"
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            java.lang.String r6 = "moveToState: Fragment state for "
            java.lang.StringBuilder r5 = r5.append(r6)
            r0 = r18
            java.lang.StringBuilder r5 = r5.append(r0)
            java.lang.String r6 = " not updated inline; "
            java.lang.StringBuilder r5 = r5.append(r6)
            java.lang.String r6 = "expected state "
            java.lang.StringBuilder r5 = r5.append(r6)
            r0 = r19
            java.lang.StringBuilder r5 = r5.append(r0)
            java.lang.String r6 = " found "
            java.lang.StringBuilder r5 = r5.append(r6)
            r0 = r18
            int r6 = r0.mState
            java.lang.StringBuilder r5 = r5.append(r6)
            java.lang.String r5 = r5.toString()
            android.util.Log.w(r4, r5)
            r0 = r19
            r1 = r18
            r1.mState = r0
            goto L_0x004f
        L_0x00b9:
            boolean r4 = DEBUG
            if (r4 == 0) goto L_0x00d7
            java.lang.String r4 = "FragmentManager"
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            java.lang.String r6 = "moveto CREATED: "
            java.lang.StringBuilder r5 = r5.append(r6)
            r0 = r18
            java.lang.StringBuilder r5 = r5.append(r0)
            java.lang.String r5 = r5.toString()
            android.util.Log.v(r4, r5)
        L_0x00d7:
            r0 = r18
            android.os.Bundle r4 = r0.mSavedFragmentState
            if (r4 == 0) goto L_0x0144
            r0 = r18
            android.os.Bundle r4 = r0.mSavedFragmentState
            r0 = r17
            android.support.v4.app.FragmentHostCallback r5 = r0.mHost
            android.content.Context r5 = r5.getContext()
            java.lang.ClassLoader r5 = r5.getClassLoader()
            r4.setClassLoader(r5)
            r0 = r18
            android.os.Bundle r4 = r0.mSavedFragmentState
            java.lang.String r5 = "android:view_state"
            android.util.SparseArray r4 = r4.getSparseParcelableArray(r5)
            r0 = r18
            r0.mSavedViewState = r4
            r0 = r18
            android.os.Bundle r4 = r0.mSavedFragmentState
            java.lang.String r5 = "android:target_state"
            r0 = r17
            android.support.v4.app.Fragment r4 = r0.getFragment(r4, r5)
            r0 = r18
            r0.mTarget = r4
            r0 = r18
            android.support.v4.app.Fragment r4 = r0.mTarget
            if (r4 == 0) goto L_0x0123
            r0 = r18
            android.os.Bundle r4 = r0.mSavedFragmentState
            java.lang.String r5 = "android:target_req_state"
            r6 = 0
            int r4 = r4.getInt(r5, r6)
            r0 = r18
            r0.mTargetRequestCode = r4
        L_0x0123:
            r0 = r18
            android.os.Bundle r4 = r0.mSavedFragmentState
            java.lang.String r5 = "android:user_visible_hint"
            r6 = 1
            boolean r4 = r4.getBoolean(r5, r6)
            r0 = r18
            r0.mUserVisibleHint = r4
            r0 = r18
            boolean r4 = r0.mUserVisibleHint
            if (r4 != 0) goto L_0x0144
            r4 = 1
            r0 = r18
            r0.mDeferStart = r4
            r4 = 3
            r0 = r19
            if (r0 <= r4) goto L_0x0144
            r19 = 3
        L_0x0144:
            r0 = r17
            android.support.v4.app.FragmentHostCallback r4 = r0.mHost
            r0 = r18
            r0.mHost = r4
            r0 = r17
            android.support.v4.app.Fragment r4 = r0.mParent
            r0 = r18
            r0.mParentFragment = r4
            r0 = r17
            android.support.v4.app.Fragment r4 = r0.mParent
            if (r4 == 0) goto L_0x019d
            r0 = r17
            android.support.v4.app.Fragment r4 = r0.mParent
            android.support.v4.app.FragmentManagerImpl r4 = r4.mChildFragmentManager
        L_0x0160:
            r0 = r18
            r0.mFragmentManager = r4
            r4 = 0
            r0 = r18
            r0.mCalled = r4
            r0 = r17
            android.support.v4.app.FragmentHostCallback r4 = r0.mHost
            android.content.Context r4 = r4.getContext()
            r0 = r18
            r0.onAttach((android.content.Context) r4)
            r0 = r18
            boolean r4 = r0.mCalled
            if (r4 != 0) goto L_0x01a6
            android.support.v4.app.SuperNotCalledException r4 = new android.support.v4.app.SuperNotCalledException
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            java.lang.String r6 = "Fragment "
            java.lang.StringBuilder r5 = r5.append(r6)
            r0 = r18
            java.lang.StringBuilder r5 = r5.append(r0)
            java.lang.String r6 = " did not call through to super.onAttach()"
            java.lang.StringBuilder r5 = r5.append(r6)
            java.lang.String r5 = r5.toString()
            r4.<init>(r5)
            throw r4
        L_0x019d:
            r0 = r17
            android.support.v4.app.FragmentHostCallback r4 = r0.mHost
            android.support.v4.app.FragmentManagerImpl r4 = r4.getFragmentManagerImpl()
            goto L_0x0160
        L_0x01a6:
            r0 = r18
            android.support.v4.app.Fragment r4 = r0.mParentFragment
            if (r4 != 0) goto L_0x03d4
            r0 = r17
            android.support.v4.app.FragmentHostCallback r4 = r0.mHost
            r0 = r18
            r4.onAttachFragment(r0)
        L_0x01b5:
            r0 = r18
            boolean r4 = r0.mRetaining
            if (r4 != 0) goto L_0x03df
            r0 = r18
            android.os.Bundle r4 = r0.mSavedFragmentState
            r0 = r18
            r0.performCreate(r4)
        L_0x01c4:
            r4 = 0
            r0 = r18
            r0.mRetaining = r4
            r0 = r18
            boolean r4 = r0.mFromLayout
            if (r4 == 0) goto L_0x0220
            r0 = r18
            android.os.Bundle r4 = r0.mSavedFragmentState
            r0 = r18
            android.view.LayoutInflater r4 = r0.getLayoutInflater(r4)
            r5 = 0
            r0 = r18
            android.os.Bundle r6 = r0.mSavedFragmentState
            r0 = r18
            android.view.View r4 = r0.performCreateView(r4, r5, r6)
            r0 = r18
            r0.mView = r4
            r0 = r18
            android.view.View r4 = r0.mView
            if (r4 == 0) goto L_0x03fd
            r0 = r18
            android.view.View r4 = r0.mView
            r0 = r18
            r0.mInnerView = r4
            int r4 = android.os.Build.VERSION.SDK_INT
            r5 = 11
            if (r4 < r5) goto L_0x03ef
            r0 = r18
            android.view.View r4 = r0.mView
            r5 = 0
            android.support.p000v4.view.ViewCompat.setSaveFromParentEnabled(r4, r5)
        L_0x0204:
            r0 = r18
            boolean r4 = r0.mHidden
            if (r4 == 0) goto L_0x0213
            r0 = r18
            android.view.View r4 = r0.mView
            r5 = 8
            r4.setVisibility(r5)
        L_0x0213:
            r0 = r18
            android.view.View r4 = r0.mView
            r0 = r18
            android.os.Bundle r5 = r0.mSavedFragmentState
            r0 = r18
            r0.onViewCreated(r4, r5)
        L_0x0220:
            r4 = 1
            r0 = r19
            if (r0 <= r4) goto L_0x0372
            boolean r4 = DEBUG
            if (r4 == 0) goto L_0x0243
            java.lang.String r4 = "FragmentManager"
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            java.lang.String r6 = "moveto ACTIVITY_CREATED: "
            java.lang.StringBuilder r5 = r5.append(r6)
            r0 = r18
            java.lang.StringBuilder r5 = r5.append(r0)
            java.lang.String r5 = r5.toString()
            android.util.Log.v(r4, r5)
        L_0x0243:
            r0 = r18
            boolean r4 = r0.mFromLayout
            if (r4 != 0) goto L_0x0355
            r11 = 0
            r0 = r18
            int r4 = r0.mContainerId
            if (r4 == 0) goto L_0x02d9
            r0 = r18
            int r4 = r0.mContainerId
            r5 = -1
            if (r4 != r5) goto L_0x027c
            java.lang.IllegalArgumentException r4 = new java.lang.IllegalArgumentException
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            java.lang.String r6 = "Cannot create fragment "
            java.lang.StringBuilder r5 = r5.append(r6)
            r0 = r18
            java.lang.StringBuilder r5 = r5.append(r0)
            java.lang.String r6 = " for a container view with no id"
            java.lang.StringBuilder r5 = r5.append(r6)
            java.lang.String r5 = r5.toString()
            r4.<init>(r5)
            r0 = r17
            r0.throwException(r4)
        L_0x027c:
            r0 = r17
            android.support.v4.app.FragmentContainer r4 = r0.mContainer
            r0 = r18
            int r5 = r0.mContainerId
            android.view.View r11 = r4.onFindViewById(r5)
            android.view.ViewGroup r11 = (android.view.ViewGroup) r11
            if (r11 != 0) goto L_0x02d9
            r0 = r18
            boolean r4 = r0.mRestored
            if (r4 != 0) goto L_0x02d9
            android.content.res.Resources r4 = r18.getResources()     // Catch:{ NotFoundException -> 0x0404 }
            r0 = r18
            int r5 = r0.mContainerId     // Catch:{ NotFoundException -> 0x0404 }
            java.lang.String r14 = r4.getResourceName(r5)     // Catch:{ NotFoundException -> 0x0404 }
        L_0x029e:
            java.lang.IllegalArgumentException r4 = new java.lang.IllegalArgumentException
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            java.lang.String r6 = "No view found for id 0x"
            java.lang.StringBuilder r5 = r5.append(r6)
            r0 = r18
            int r6 = r0.mContainerId
            java.lang.String r6 = java.lang.Integer.toHexString(r6)
            java.lang.StringBuilder r5 = r5.append(r6)
            java.lang.String r6 = " ("
            java.lang.StringBuilder r5 = r5.append(r6)
            java.lang.StringBuilder r5 = r5.append(r14)
            java.lang.String r6 = ") for fragment "
            java.lang.StringBuilder r5 = r5.append(r6)
            r0 = r18
            java.lang.StringBuilder r5 = r5.append(r0)
            java.lang.String r5 = r5.toString()
            r4.<init>(r5)
            r0 = r17
            r0.throwException(r4)
        L_0x02d9:
            r0 = r18
            r0.mContainer = r11
            r0 = r18
            android.os.Bundle r4 = r0.mSavedFragmentState
            r0 = r18
            android.view.LayoutInflater r4 = r0.getLayoutInflater(r4)
            r0 = r18
            android.os.Bundle r5 = r0.mSavedFragmentState
            r0 = r18
            android.view.View r4 = r0.performCreateView(r4, r11, r5)
            r0 = r18
            r0.mView = r4
            r0 = r18
            android.view.View r4 = r0.mView
            if (r4 == 0) goto L_0x0417
            r0 = r18
            android.view.View r4 = r0.mView
            r0 = r18
            r0.mInnerView = r4
            int r4 = android.os.Build.VERSION.SDK_INT
            r5 = 11
            if (r4 < r5) goto L_0x0409
            r0 = r18
            android.view.View r4 = r0.mView
            r5 = 0
            android.support.p000v4.view.ViewCompat.setSaveFromParentEnabled(r4, r5)
        L_0x0311:
            if (r11 == 0) goto L_0x0339
            r4 = 1
            r0 = r17
            r1 = r18
            r2 = r20
            r3 = r21
            android.view.animation.Animation r10 = r0.loadAnimation(r1, r2, r4, r3)
            if (r10 == 0) goto L_0x0332
            r0 = r18
            android.view.View r4 = r0.mView
            r0 = r17
            r0.setHWLayerAnimListenerIfAlpha(r4, r10)
            r0 = r18
            android.view.View r4 = r0.mView
            r4.startAnimation(r10)
        L_0x0332:
            r0 = r18
            android.view.View r4 = r0.mView
            r11.addView(r4)
        L_0x0339:
            r0 = r18
            boolean r4 = r0.mHidden
            if (r4 == 0) goto L_0x0348
            r0 = r18
            android.view.View r4 = r0.mView
            r5 = 8
            r4.setVisibility(r5)
        L_0x0348:
            r0 = r18
            android.view.View r4 = r0.mView
            r0 = r18
            android.os.Bundle r5 = r0.mSavedFragmentState
            r0 = r18
            r0.onViewCreated(r4, r5)
        L_0x0355:
            r0 = r18
            android.os.Bundle r4 = r0.mSavedFragmentState
            r0 = r18
            r0.performActivityCreated(r4)
            r0 = r18
            android.view.View r4 = r0.mView
            if (r4 == 0) goto L_0x036d
            r0 = r18
            android.os.Bundle r4 = r0.mSavedFragmentState
            r0 = r18
            r0.restoreViewState(r4)
        L_0x036d:
            r4 = 0
            r0 = r18
            r0.mSavedFragmentState = r4
        L_0x0372:
            r4 = 2
            r0 = r19
            if (r0 <= r4) goto L_0x037c
            r4 = 3
            r0 = r18
            r0.mState = r4
        L_0x037c:
            r4 = 3
            r0 = r19
            if (r0 <= r4) goto L_0x03a2
            boolean r4 = DEBUG
            if (r4 == 0) goto L_0x039f
            java.lang.String r4 = "FragmentManager"
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            java.lang.String r6 = "moveto STARTED: "
            java.lang.StringBuilder r5 = r5.append(r6)
            r0 = r18
            java.lang.StringBuilder r5 = r5.append(r0)
            java.lang.String r5 = r5.toString()
            android.util.Log.v(r4, r5)
        L_0x039f:
            r18.performStart()
        L_0x03a2:
            r4 = 4
            r0 = r19
            if (r0 <= r4) goto L_0x0070
            boolean r4 = DEBUG
            if (r4 == 0) goto L_0x03c5
            java.lang.String r4 = "FragmentManager"
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            java.lang.String r6 = "moveto RESUMED: "
            java.lang.StringBuilder r5 = r5.append(r6)
            r0 = r18
            java.lang.StringBuilder r5 = r5.append(r0)
            java.lang.String r5 = r5.toString()
            android.util.Log.v(r4, r5)
        L_0x03c5:
            r18.performResume()
            r4 = 0
            r0 = r18
            r0.mSavedFragmentState = r4
            r4 = 0
            r0 = r18
            r0.mSavedViewState = r4
            goto L_0x0070
        L_0x03d4:
            r0 = r18
            android.support.v4.app.Fragment r4 = r0.mParentFragment
            r0 = r18
            r4.onAttachFragment(r0)
            goto L_0x01b5
        L_0x03df:
            r0 = r18
            android.os.Bundle r4 = r0.mSavedFragmentState
            r0 = r18
            r0.restoreChildFragmentState(r4)
            r4 = 1
            r0 = r18
            r0.mState = r4
            goto L_0x01c4
        L_0x03ef:
            r0 = r18
            android.view.View r4 = r0.mView
            android.view.ViewGroup r4 = android.support.p000v4.app.NoSaveStateFrameLayout.wrap(r4)
            r0 = r18
            r0.mView = r4
            goto L_0x0204
        L_0x03fd:
            r4 = 0
            r0 = r18
            r0.mInnerView = r4
            goto L_0x0220
        L_0x0404:
            r12 = move-exception
            java.lang.String r14 = "unknown"
            goto L_0x029e
        L_0x0409:
            r0 = r18
            android.view.View r4 = r0.mView
            android.view.ViewGroup r4 = android.support.p000v4.app.NoSaveStateFrameLayout.wrap(r4)
            r0 = r18
            r0.mView = r4
            goto L_0x0311
        L_0x0417:
            r4 = 0
            r0 = r18
            r0.mInnerView = r4
            goto L_0x0355
        L_0x041e:
            r0 = r18
            int r4 = r0.mState
            r0 = r19
            if (r4 <= r0) goto L_0x0070
            r0 = r18
            int r4 = r0.mState
            switch(r4) {
                case 1: goto L_0x042f;
                case 2: goto L_0x04ce;
                case 3: goto L_0x04a8;
                case 4: goto L_0x0482;
                case 5: goto L_0x045c;
                default: goto L_0x042d;
            }
        L_0x042d:
            goto L_0x0070
        L_0x042f:
            r4 = 1
            r0 = r19
            if (r0 >= r4) goto L_0x0070
            r0 = r17
            boolean r4 = r0.mDestroyed
            if (r4 == 0) goto L_0x044c
            r0 = r18
            android.view.View r4 = r0.mAnimatingAway
            if (r4 == 0) goto L_0x044c
            r0 = r18
            android.view.View r15 = r0.mAnimatingAway
            r4 = 0
            r0 = r18
            r0.mAnimatingAway = r4
            r15.clearAnimation()
        L_0x044c:
            r0 = r18
            android.view.View r4 = r0.mAnimatingAway
            if (r4 == 0) goto L_0x057c
            r0 = r19
            r1 = r18
            r1.mStateAfterAnimating = r0
            r19 = 1
            goto L_0x0070
        L_0x045c:
            r4 = 5
            r0 = r19
            if (r0 >= r4) goto L_0x0482
            boolean r4 = DEBUG
            if (r4 == 0) goto L_0x047f
            java.lang.String r4 = "FragmentManager"
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            java.lang.String r6 = "movefrom RESUMED: "
            java.lang.StringBuilder r5 = r5.append(r6)
            r0 = r18
            java.lang.StringBuilder r5 = r5.append(r0)
            java.lang.String r5 = r5.toString()
            android.util.Log.v(r4, r5)
        L_0x047f:
            r18.performPause()
        L_0x0482:
            r4 = 4
            r0 = r19
            if (r0 >= r4) goto L_0x04a8
            boolean r4 = DEBUG
            if (r4 == 0) goto L_0x04a5
            java.lang.String r4 = "FragmentManager"
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            java.lang.String r6 = "movefrom STARTED: "
            java.lang.StringBuilder r5 = r5.append(r6)
            r0 = r18
            java.lang.StringBuilder r5 = r5.append(r0)
            java.lang.String r5 = r5.toString()
            android.util.Log.v(r4, r5)
        L_0x04a5:
            r18.performStop()
        L_0x04a8:
            r4 = 3
            r0 = r19
            if (r0 >= r4) goto L_0x04ce
            boolean r4 = DEBUG
            if (r4 == 0) goto L_0x04cb
            java.lang.String r4 = "FragmentManager"
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            java.lang.String r6 = "movefrom STOPPED: "
            java.lang.StringBuilder r5 = r5.append(r6)
            r0 = r18
            java.lang.StringBuilder r5 = r5.append(r0)
            java.lang.String r5 = r5.toString()
            android.util.Log.v(r4, r5)
        L_0x04cb:
            r18.performReallyStop()
        L_0x04ce:
            r4 = 2
            r0 = r19
            if (r0 >= r4) goto L_0x042f
            boolean r4 = DEBUG
            if (r4 == 0) goto L_0x04f1
            java.lang.String r4 = "FragmentManager"
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            java.lang.String r6 = "movefrom ACTIVITY_CREATED: "
            java.lang.StringBuilder r5 = r5.append(r6)
            r0 = r18
            java.lang.StringBuilder r5 = r5.append(r0)
            java.lang.String r5 = r5.toString()
            android.util.Log.v(r4, r5)
        L_0x04f1:
            r0 = r18
            android.view.View r4 = r0.mView
            if (r4 == 0) goto L_0x050c
            r0 = r17
            android.support.v4.app.FragmentHostCallback r4 = r0.mHost
            r0 = r18
            boolean r4 = r4.onShouldSaveFragmentState(r0)
            if (r4 == 0) goto L_0x050c
            r0 = r18
            android.util.SparseArray<android.os.Parcelable> r4 = r0.mSavedViewState
            if (r4 != 0) goto L_0x050c
            r17.saveFragmentViewState(r18)
        L_0x050c:
            r18.performDestroyView()
            r0 = r18
            android.view.View r4 = r0.mView
            if (r4 == 0) goto L_0x056b
            r0 = r18
            android.view.ViewGroup r4 = r0.mContainer
            if (r4 == 0) goto L_0x056b
            r10 = 0
            r0 = r17
            int r4 = r0.mCurState
            if (r4 <= 0) goto L_0x0535
            r0 = r17
            boolean r4 = r0.mDestroyed
            if (r4 != 0) goto L_0x0535
            r4 = 0
            r0 = r17
            r1 = r18
            r2 = r20
            r3 = r21
            android.view.animation.Animation r10 = r0.loadAnimation(r1, r2, r4, r3)
        L_0x0535:
            if (r10 == 0) goto L_0x0560
            r13 = r18
            r0 = r18
            android.view.View r4 = r0.mView
            r0 = r18
            r0.mAnimatingAway = r4
            r0 = r19
            r1 = r18
            r1.mStateAfterAnimating = r0
            r0 = r18
            android.view.View r0 = r0.mView
            r16 = r0
            android.support.v4.app.FragmentManagerImpl$5 r4 = new android.support.v4.app.FragmentManagerImpl$5
            r0 = r17
            r1 = r16
            r4.<init>(r1, r10, r13)
            r10.setAnimationListener(r4)
            r0 = r18
            android.view.View r4 = r0.mView
            r4.startAnimation(r10)
        L_0x0560:
            r0 = r18
            android.view.ViewGroup r4 = r0.mContainer
            r0 = r18
            android.view.View r5 = r0.mView
            r4.removeView(r5)
        L_0x056b:
            r4 = 0
            r0 = r18
            r0.mContainer = r4
            r4 = 0
            r0 = r18
            r0.mView = r4
            r4 = 0
            r0 = r18
            r0.mInnerView = r4
            goto L_0x042f
        L_0x057c:
            boolean r4 = DEBUG
            if (r4 == 0) goto L_0x059a
            java.lang.String r4 = "FragmentManager"
            java.lang.StringBuilder r5 = new java.lang.StringBuilder
            r5.<init>()
            java.lang.String r6 = "movefrom CREATED: "
            java.lang.StringBuilder r5 = r5.append(r6)
            r0 = r18
            java.lang.StringBuilder r5 = r5.append(r0)
            java.lang.String r5 = r5.toString()
            android.util.Log.v(r4, r5)
        L_0x059a:
            r0 = r18
            boolean r4 = r0.mRetaining
            if (r4 != 0) goto L_0x05b3
            r18.performDestroy()
        L_0x05a3:
            r18.performDetach()
            if (r22 != 0) goto L_0x0070
            r0 = r18
            boolean r4 = r0.mRetaining
            if (r4 != 0) goto L_0x05b9
            r17.makeInactive(r18)
            goto L_0x0070
        L_0x05b3:
            r4 = 0
            r0 = r18
            r0.mState = r4
            goto L_0x05a3
        L_0x05b9:
            r4 = 0
            r0 = r18
            r0.mHost = r4
            r4 = 0
            r0 = r18
            r0.mParentFragment = r4
            r4 = 0
            r0 = r18
            r0.mFragmentManager = r4
            goto L_0x0070
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.p000v4.app.FragmentManagerImpl.moveToState(android.support.v4.app.Fragment, int, int, int, boolean):void");
    }

    /* access modifiers changed from: package-private */
    public void moveToState(Fragment f) {
        moveToState(f, this.mCurState, 0, 0, false);
    }

    /* access modifiers changed from: package-private */
    public void moveToState(int newState, boolean always) {
        moveToState(newState, 0, 0, always);
    }

    /* access modifiers changed from: package-private */
    public void moveToState(int newState, int transit, int transitStyle, boolean always) {
        if (this.mHost == null && newState != 0) {
            throw new IllegalStateException("No host");
        } else if (always || this.mCurState != newState) {
            this.mCurState = newState;
            if (this.mActive != null) {
                boolean loadersRunning = false;
                for (int i = 0; i < this.mActive.size(); i++) {
                    Fragment f = this.mActive.get(i);
                    if (f != null) {
                        moveToState(f, newState, transit, transitStyle, false);
                        if (f.mLoaderManager != null) {
                            loadersRunning |= f.mLoaderManager.hasRunningLoaders();
                        }
                    }
                }
                if (!loadersRunning) {
                    startPendingDeferredFragments();
                }
                if (this.mNeedMenuInvalidate && this.mHost != null && this.mCurState == 5) {
                    this.mHost.onSupportInvalidateOptionsMenu();
                    this.mNeedMenuInvalidate = false;
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void startPendingDeferredFragments() {
        if (this.mActive != null) {
            for (int i = 0; i < this.mActive.size(); i++) {
                Fragment f = this.mActive.get(i);
                if (f != null) {
                    performPendingDeferredStart(f);
                }
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void makeActive(Fragment f) {
        if (f.mIndex < 0) {
            if (this.mAvailIndices == null || this.mAvailIndices.size() <= 0) {
                if (this.mActive == null) {
                    this.mActive = new ArrayList<>();
                }
                f.setIndex(this.mActive.size(), this.mParent);
                this.mActive.add(f);
            } else {
                f.setIndex(this.mAvailIndices.remove(this.mAvailIndices.size() - 1).intValue(), this.mParent);
                this.mActive.set(f.mIndex, f);
            }
            if (DEBUG) {
                Log.v(TAG, "Allocated fragment index " + f);
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void makeInactive(Fragment f) {
        if (f.mIndex >= 0) {
            if (DEBUG) {
                Log.v(TAG, "Freeing fragment index " + f);
            }
            this.mActive.set(f.mIndex, (Object) null);
            if (this.mAvailIndices == null) {
                this.mAvailIndices = new ArrayList<>();
            }
            this.mAvailIndices.add(Integer.valueOf(f.mIndex));
            this.mHost.inactivateFragment(f.mWho);
            f.initState();
        }
    }

    public void addFragment(Fragment fragment, boolean moveToStateNow) {
        if (this.mAdded == null) {
            this.mAdded = new ArrayList<>();
        }
        if (DEBUG) {
            Log.v(TAG, "add: " + fragment);
        }
        makeActive(fragment);
        if (fragment.mDetached) {
            return;
        }
        if (this.mAdded.contains(fragment)) {
            throw new IllegalStateException("Fragment already added: " + fragment);
        }
        this.mAdded.add(fragment);
        fragment.mAdded = true;
        fragment.mRemoving = false;
        if (fragment.mHasMenu && fragment.mMenuVisible) {
            this.mNeedMenuInvalidate = true;
        }
        if (moveToStateNow) {
            moveToState(fragment);
        }
    }

    public void removeFragment(Fragment fragment, int transition, int transitionStyle) {
        boolean inactive;
        int i;
        if (DEBUG) {
            Log.v(TAG, "remove: " + fragment + " nesting=" + fragment.mBackStackNesting);
        }
        if (!fragment.isInBackStack()) {
            inactive = true;
        } else {
            inactive = false;
        }
        if (!fragment.mDetached || inactive) {
            if (this.mAdded != null) {
                this.mAdded.remove(fragment);
            }
            if (fragment.mHasMenu && fragment.mMenuVisible) {
                this.mNeedMenuInvalidate = true;
            }
            fragment.mAdded = false;
            fragment.mRemoving = true;
            if (inactive) {
                i = 0;
            } else {
                i = 1;
            }
            moveToState(fragment, i, transition, transitionStyle, false);
        }
    }

    public void hideFragment(Fragment fragment, int transition, int transitionStyle) {
        if (DEBUG) {
            Log.v(TAG, "hide: " + fragment);
        }
        if (!fragment.mHidden) {
            fragment.mHidden = true;
            if (fragment.mView != null) {
                Animation anim = loadAnimation(fragment, transition, false, transitionStyle);
                if (anim != null) {
                    setHWLayerAnimListenerIfAlpha(fragment.mView, anim);
                    fragment.mView.startAnimation(anim);
                }
                fragment.mView.setVisibility(8);
            }
            if (fragment.mAdded && fragment.mHasMenu && fragment.mMenuVisible) {
                this.mNeedMenuInvalidate = true;
            }
            fragment.onHiddenChanged(true);
        }
    }

    public void showFragment(Fragment fragment, int transition, int transitionStyle) {
        if (DEBUG) {
            Log.v(TAG, "show: " + fragment);
        }
        if (fragment.mHidden) {
            fragment.mHidden = false;
            if (fragment.mView != null) {
                Animation anim = loadAnimation(fragment, transition, true, transitionStyle);
                if (anim != null) {
                    setHWLayerAnimListenerIfAlpha(fragment.mView, anim);
                    fragment.mView.startAnimation(anim);
                }
                fragment.mView.setVisibility(0);
            }
            if (fragment.mAdded && fragment.mHasMenu && fragment.mMenuVisible) {
                this.mNeedMenuInvalidate = true;
            }
            fragment.onHiddenChanged(false);
        }
    }

    public void detachFragment(Fragment fragment, int transition, int transitionStyle) {
        if (DEBUG) {
            Log.v(TAG, "detach: " + fragment);
        }
        if (!fragment.mDetached) {
            fragment.mDetached = true;
            if (fragment.mAdded) {
                if (this.mAdded != null) {
                    if (DEBUG) {
                        Log.v(TAG, "remove from detach: " + fragment);
                    }
                    this.mAdded.remove(fragment);
                }
                if (fragment.mHasMenu && fragment.mMenuVisible) {
                    this.mNeedMenuInvalidate = true;
                }
                fragment.mAdded = false;
                moveToState(fragment, 1, transition, transitionStyle, false);
            }
        }
    }

    public void attachFragment(Fragment fragment, int transition, int transitionStyle) {
        if (DEBUG) {
            Log.v(TAG, "attach: " + fragment);
        }
        if (fragment.mDetached) {
            fragment.mDetached = false;
            if (!fragment.mAdded) {
                if (this.mAdded == null) {
                    this.mAdded = new ArrayList<>();
                }
                if (this.mAdded.contains(fragment)) {
                    throw new IllegalStateException("Fragment already added: " + fragment);
                }
                if (DEBUG) {
                    Log.v(TAG, "add from attach: " + fragment);
                }
                this.mAdded.add(fragment);
                fragment.mAdded = true;
                if (fragment.mHasMenu && fragment.mMenuVisible) {
                    this.mNeedMenuInvalidate = true;
                }
                moveToState(fragment, this.mCurState, transition, transitionStyle, false);
            }
        }
    }

    public Fragment findFragmentById(int id) {
        if (this.mAdded != null) {
            for (int i = this.mAdded.size() - 1; i >= 0; i--) {
                Fragment f = this.mAdded.get(i);
                if (f != null && f.mFragmentId == id) {
                    return f;
                }
            }
        }
        if (this.mActive != null) {
            for (int i2 = this.mActive.size() - 1; i2 >= 0; i2--) {
                Fragment f2 = this.mActive.get(i2);
                if (f2 != null && f2.mFragmentId == id) {
                    return f2;
                }
            }
        }
        return null;
    }

    public Fragment findFragmentByTag(String tag) {
        if (!(this.mAdded == null || tag == null)) {
            for (int i = this.mAdded.size() - 1; i >= 0; i--) {
                Fragment f = this.mAdded.get(i);
                if (f != null && tag.equals(f.mTag)) {
                    return f;
                }
            }
        }
        if (!(this.mActive == null || tag == null)) {
            for (int i2 = this.mActive.size() - 1; i2 >= 0; i2--) {
                Fragment f2 = this.mActive.get(i2);
                if (f2 != null && tag.equals(f2.mTag)) {
                    return f2;
                }
            }
        }
        return null;
    }

    public Fragment findFragmentByWho(String who) {
        Fragment f;
        if (!(this.mActive == null || who == null)) {
            for (int i = this.mActive.size() - 1; i >= 0; i--) {
                Fragment f2 = this.mActive.get(i);
                if (f2 != null && (f = f2.findFragmentByWho(who)) != null) {
                    return f;
                }
            }
        }
        return null;
    }

    private void checkStateLoss() {
        if (this.mStateSaved) {
            throw new IllegalStateException("Can not perform this action after onSaveInstanceState");
        } else if (this.mNoTransactionsBecause != null) {
            throw new IllegalStateException("Can not perform this action inside of " + this.mNoTransactionsBecause);
        }
    }

    public void enqueueAction(Runnable action, boolean allowStateLoss) {
        if (!allowStateLoss) {
            checkStateLoss();
        }
        synchronized (this) {
            if (this.mDestroyed || this.mHost == null) {
                throw new IllegalStateException("Activity has been destroyed");
            }
            if (this.mPendingActions == null) {
                this.mPendingActions = new ArrayList<>();
            }
            this.mPendingActions.add(action);
            if (this.mPendingActions.size() == 1) {
                this.mHost.getHandler().removeCallbacks(this.mExecCommit);
                this.mHost.getHandler().post(this.mExecCommit);
            }
        }
    }

    public int allocBackStackIndex(BackStackRecord bse) {
        synchronized (this) {
            if (this.mAvailBackStackIndices == null || this.mAvailBackStackIndices.size() <= 0) {
                if (this.mBackStackIndices == null) {
                    this.mBackStackIndices = new ArrayList<>();
                }
                int index = this.mBackStackIndices.size();
                if (DEBUG) {
                    Log.v(TAG, "Setting back stack index " + index + " to " + bse);
                }
                this.mBackStackIndices.add(bse);
                return index;
            }
            int index2 = this.mAvailBackStackIndices.remove(this.mAvailBackStackIndices.size() - 1).intValue();
            if (DEBUG) {
                Log.v(TAG, "Adding back stack index " + index2 + " with " + bse);
            }
            this.mBackStackIndices.set(index2, bse);
            return index2;
        }
    }

    public void setBackStackIndex(int index, BackStackRecord bse) {
        synchronized (this) {
            if (this.mBackStackIndices == null) {
                this.mBackStackIndices = new ArrayList<>();
            }
            int N = this.mBackStackIndices.size();
            if (index < N) {
                if (DEBUG) {
                    Log.v(TAG, "Setting back stack index " + index + " to " + bse);
                }
                this.mBackStackIndices.set(index, bse);
            } else {
                while (N < index) {
                    this.mBackStackIndices.add((Object) null);
                    if (this.mAvailBackStackIndices == null) {
                        this.mAvailBackStackIndices = new ArrayList<>();
                    }
                    if (DEBUG) {
                        Log.v(TAG, "Adding available back stack index " + N);
                    }
                    this.mAvailBackStackIndices.add(Integer.valueOf(N));
                    N++;
                }
                if (DEBUG) {
                    Log.v(TAG, "Adding back stack index " + index + " with " + bse);
                }
                this.mBackStackIndices.add(bse);
            }
        }
    }

    public void freeBackStackIndex(int index) {
        synchronized (this) {
            this.mBackStackIndices.set(index, (Object) null);
            if (this.mAvailBackStackIndices == null) {
                this.mAvailBackStackIndices = new ArrayList<>();
            }
            if (DEBUG) {
                Log.v(TAG, "Freeing back stack index " + index);
            }
            this.mAvailBackStackIndices.add(Integer.valueOf(index));
        }
    }

    public void execSingleAction(Runnable action, boolean allowStateLoss) {
        if (this.mExecutingActions) {
            throw new IllegalStateException("FragmentManager is already executing transactions");
        } else if (Looper.myLooper() != this.mHost.getHandler().getLooper()) {
            throw new IllegalStateException("Must be called from main thread of fragment host");
        } else {
            if (!allowStateLoss) {
                checkStateLoss();
            }
            this.mExecutingActions = true;
            action.run();
            this.mExecutingActions = false;
            doPendingDeferredStart();
        }
    }

    /* JADX WARNING: Code restructure failed: missing block: B:26:0x0062, code lost:
        r5.mExecutingActions = true;
        r1 = 0;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:27:0x0066, code lost:
        if (r1 >= r2) goto L_0x007a;
     */
    /* JADX WARNING: Code restructure failed: missing block: B:28:0x0068, code lost:
        r5.mTmpActions[r1].run();
        r5.mTmpActions[r1] = null;
        r1 = r1 + 1;
     */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public boolean execPendingActions() {
        /*
            r5 = this;
            boolean r3 = r5.mExecutingActions
            if (r3 == 0) goto L_0x000c
            java.lang.IllegalStateException r3 = new java.lang.IllegalStateException
            java.lang.String r4 = "FragmentManager is already executing transactions"
            r3.<init>(r4)
            throw r3
        L_0x000c:
            android.os.Looper r3 = android.os.Looper.myLooper()
            android.support.v4.app.FragmentHostCallback r4 = r5.mHost
            android.os.Handler r4 = r4.getHandler()
            android.os.Looper r4 = r4.getLooper()
            if (r3 == r4) goto L_0x0024
            java.lang.IllegalStateException r3 = new java.lang.IllegalStateException
            java.lang.String r4 = "Must be called from main thread of fragment host"
            r3.<init>(r4)
            throw r3
        L_0x0024:
            r0 = 0
        L_0x0025:
            monitor-enter(r5)
            java.util.ArrayList<java.lang.Runnable> r3 = r5.mPendingActions     // Catch:{ all -> 0x0077 }
            if (r3 == 0) goto L_0x0032
            java.util.ArrayList<java.lang.Runnable> r3 = r5.mPendingActions     // Catch:{ all -> 0x0077 }
            int r3 = r3.size()     // Catch:{ all -> 0x0077 }
            if (r3 != 0) goto L_0x0037
        L_0x0032:
            monitor-exit(r5)     // Catch:{ all -> 0x0077 }
            r5.doPendingDeferredStart()
            return r0
        L_0x0037:
            java.util.ArrayList<java.lang.Runnable> r3 = r5.mPendingActions     // Catch:{ all -> 0x0077 }
            int r2 = r3.size()     // Catch:{ all -> 0x0077 }
            java.lang.Runnable[] r3 = r5.mTmpActions     // Catch:{ all -> 0x0077 }
            if (r3 == 0) goto L_0x0046
            java.lang.Runnable[] r3 = r5.mTmpActions     // Catch:{ all -> 0x0077 }
            int r3 = r3.length     // Catch:{ all -> 0x0077 }
            if (r3 >= r2) goto L_0x004a
        L_0x0046:
            java.lang.Runnable[] r3 = new java.lang.Runnable[r2]     // Catch:{ all -> 0x0077 }
            r5.mTmpActions = r3     // Catch:{ all -> 0x0077 }
        L_0x004a:
            java.util.ArrayList<java.lang.Runnable> r3 = r5.mPendingActions     // Catch:{ all -> 0x0077 }
            java.lang.Runnable[] r4 = r5.mTmpActions     // Catch:{ all -> 0x0077 }
            r3.toArray(r4)     // Catch:{ all -> 0x0077 }
            java.util.ArrayList<java.lang.Runnable> r3 = r5.mPendingActions     // Catch:{ all -> 0x0077 }
            r3.clear()     // Catch:{ all -> 0x0077 }
            android.support.v4.app.FragmentHostCallback r3 = r5.mHost     // Catch:{ all -> 0x0077 }
            android.os.Handler r3 = r3.getHandler()     // Catch:{ all -> 0x0077 }
            java.lang.Runnable r4 = r5.mExecCommit     // Catch:{ all -> 0x0077 }
            r3.removeCallbacks(r4)     // Catch:{ all -> 0x0077 }
            monitor-exit(r5)     // Catch:{ all -> 0x0077 }
            r3 = 1
            r5.mExecutingActions = r3
            r1 = 0
        L_0x0066:
            if (r1 >= r2) goto L_0x007a
            java.lang.Runnable[] r3 = r5.mTmpActions
            r3 = r3[r1]
            r3.run()
            java.lang.Runnable[] r3 = r5.mTmpActions
            r4 = 0
            r3[r1] = r4
            int r1 = r1 + 1
            goto L_0x0066
        L_0x0077:
            r3 = move-exception
            monitor-exit(r5)     // Catch:{ all -> 0x0077 }
            throw r3
        L_0x007a:
            r3 = 0
            r5.mExecutingActions = r3
            r0 = 1
            goto L_0x0025
        */
        throw new UnsupportedOperationException("Method not decompiled: android.support.p000v4.app.FragmentManagerImpl.execPendingActions():boolean");
    }

    /* access modifiers changed from: package-private */
    public void doPendingDeferredStart() {
        if (this.mHavePendingDeferredStart) {
            boolean loadersRunning = false;
            for (int i = 0; i < this.mActive.size(); i++) {
                Fragment f = this.mActive.get(i);
                if (!(f == null || f.mLoaderManager == null)) {
                    loadersRunning |= f.mLoaderManager.hasRunningLoaders();
                }
            }
            if (!loadersRunning) {
                this.mHavePendingDeferredStart = false;
                startPendingDeferredFragments();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void reportBackStackChanged() {
        if (this.mBackStackChangeListeners != null) {
            for (int i = 0; i < this.mBackStackChangeListeners.size(); i++) {
                this.mBackStackChangeListeners.get(i).onBackStackChanged();
            }
        }
    }

    /* access modifiers changed from: package-private */
    public void addBackStackState(BackStackRecord state) {
        if (this.mBackStack == null) {
            this.mBackStack = new ArrayList<>();
        }
        this.mBackStack.add(state);
        reportBackStackChanged();
    }

    /* access modifiers changed from: package-private */
    public boolean popBackStackState(Handler handler, String name, int id, int flags) {
        if (this.mBackStack == null) {
            return false;
        }
        if (name == null && id < 0 && (flags & 1) == 0) {
            int last = this.mBackStack.size() - 1;
            if (last < 0) {
                return false;
            }
            BackStackRecord bss = this.mBackStack.remove(last);
            SparseArray<Fragment> firstOutFragments = new SparseArray<>();
            SparseArray<Fragment> lastInFragments = new SparseArray<>();
            if (this.mCurState >= 1) {
                bss.calculateBackFragments(firstOutFragments, lastInFragments);
            }
            bss.popFromBackStack(true, (BackStackRecord.TransitionState) null, firstOutFragments, lastInFragments);
            reportBackStackChanged();
        } else {
            int index = -1;
            if (name != null || id >= 0) {
                int index2 = this.mBackStack.size() - 1;
                while (index >= 0) {
                    BackStackRecord bss2 = this.mBackStack.get(index);
                    if ((name != null && name.equals(bss2.getName())) || (id >= 0 && id == bss2.mIndex)) {
                        break;
                    }
                    index2 = index - 1;
                }
                if (index < 0) {
                    return false;
                }
                if ((flags & 1) != 0) {
                    index--;
                    while (index >= 0) {
                        BackStackRecord bss3 = this.mBackStack.get(index);
                        if ((name == null || !name.equals(bss3.getName())) && (id < 0 || id != bss3.mIndex)) {
                            break;
                        }
                        index--;
                    }
                }
            }
            if (index == this.mBackStack.size() - 1) {
                return false;
            }
            ArrayList<BackStackRecord> states = new ArrayList<>();
            for (int i = this.mBackStack.size() - 1; i > index; i--) {
                states.add(this.mBackStack.remove(i));
            }
            int LAST = states.size() - 1;
            SparseArray<Fragment> firstOutFragments2 = new SparseArray<>();
            SparseArray<Fragment> lastInFragments2 = new SparseArray<>();
            if (this.mCurState >= 1) {
                for (int i2 = 0; i2 <= LAST; i2++) {
                    states.get(i2).calculateBackFragments(firstOutFragments2, lastInFragments2);
                }
            }
            BackStackRecord.TransitionState state = null;
            int i3 = 0;
            while (i3 <= LAST) {
                if (DEBUG) {
                    Log.v(TAG, "Popping back stack state: " + states.get(i3));
                }
                state = states.get(i3).popFromBackStack(i3 == LAST, state, firstOutFragments2, lastInFragments2);
                i3++;
            }
            reportBackStackChanged();
        }
        return true;
    }

    /* access modifiers changed from: package-private */
    public FragmentManagerNonConfig retainNonConfig() {
        FragmentManagerNonConfig child;
        ArrayList<Fragment> fragments = null;
        ArrayList<FragmentManagerNonConfig> childFragments = null;
        if (this.mActive != null) {
            for (int i = 0; i < this.mActive.size(); i++) {
                Fragment f = this.mActive.get(i);
                if (f != null) {
                    if (f.mRetainInstance) {
                        if (fragments == null) {
                            fragments = new ArrayList<>();
                        }
                        fragments.add(f);
                        f.mRetaining = true;
                        f.mTargetIndex = f.mTarget != null ? f.mTarget.mIndex : -1;
                        if (DEBUG) {
                            Log.v(TAG, "retainNonConfig: keeping retained " + f);
                        }
                    }
                    boolean addedChild = false;
                    if (!(f.mChildFragmentManager == null || (child = f.mChildFragmentManager.retainNonConfig()) == null)) {
                        if (childFragments == null) {
                            childFragments = new ArrayList<>();
                            for (int j = 0; j < i; j++) {
                                childFragments.add((Object) null);
                            }
                        }
                        childFragments.add(child);
                        addedChild = true;
                    }
                    if (childFragments != null && !addedChild) {
                        childFragments.add((Object) null);
                    }
                }
            }
        }
        if (fragments == null && childFragments == null) {
            return null;
        }
        return new FragmentManagerNonConfig(fragments, childFragments);
    }

    /* access modifiers changed from: package-private */
    public void saveFragmentViewState(Fragment f) {
        if (f.mInnerView != null) {
            if (this.mStateArray == null) {
                this.mStateArray = new SparseArray<>();
            } else {
                this.mStateArray.clear();
            }
            f.mInnerView.saveHierarchyState(this.mStateArray);
            if (this.mStateArray.size() > 0) {
                f.mSavedViewState = this.mStateArray;
                this.mStateArray = null;
            }
        }
    }

    /* access modifiers changed from: package-private */
    public Bundle saveFragmentBasicState(Fragment f) {
        Bundle result = null;
        if (this.mStateBundle == null) {
            this.mStateBundle = new Bundle();
        }
        f.performSaveInstanceState(this.mStateBundle);
        if (!this.mStateBundle.isEmpty()) {
            result = this.mStateBundle;
            this.mStateBundle = null;
        }
        if (f.mView != null) {
            saveFragmentViewState(f);
        }
        if (f.mSavedViewState != null) {
            if (result == null) {
                result = new Bundle();
            }
            result.putSparseParcelableArray(VIEW_STATE_TAG, f.mSavedViewState);
        }
        if (!f.mUserVisibleHint) {
            if (result == null) {
                result = new Bundle();
            }
            result.putBoolean(USER_VISIBLE_HINT_TAG, f.mUserVisibleHint);
        }
        return result;
    }

    /* access modifiers changed from: package-private */
    public Parcelable saveAllState() {
        int N;
        int N2;
        execPendingActions();
        if (HONEYCOMB) {
            this.mStateSaved = true;
        }
        if (this.mActive == null || this.mActive.size() <= 0) {
            return null;
        }
        int N3 = this.mActive.size();
        FragmentState[] active = new FragmentState[N3];
        boolean haveFragments = false;
        for (int i = 0; i < N3; i++) {
            Fragment f = this.mActive.get(i);
            if (f != null) {
                if (f.mIndex < 0) {
                    throwException(new IllegalStateException("Failure saving state: active " + f + " has cleared index: " + f.mIndex));
                }
                haveFragments = true;
                FragmentState fs = new FragmentState(f);
                active[i] = fs;
                if (f.mState <= 0 || fs.mSavedFragmentState != null) {
                    fs.mSavedFragmentState = f.mSavedFragmentState;
                } else {
                    fs.mSavedFragmentState = saveFragmentBasicState(f);
                    if (f.mTarget != null) {
                        if (f.mTarget.mIndex < 0) {
                            throwException(new IllegalStateException("Failure saving state: " + f + " has target not in fragment manager: " + f.mTarget));
                        }
                        if (fs.mSavedFragmentState == null) {
                            fs.mSavedFragmentState = new Bundle();
                        }
                        putFragment(fs.mSavedFragmentState, TARGET_STATE_TAG, f.mTarget);
                        if (f.mTargetRequestCode != 0) {
                            fs.mSavedFragmentState.putInt(TARGET_REQUEST_CODE_STATE_TAG, f.mTargetRequestCode);
                        }
                    }
                }
                if (DEBUG) {
                    Log.v(TAG, "Saved state of " + f + ": " + fs.mSavedFragmentState);
                }
            }
        }
        if (haveFragments) {
            int[] added = null;
            BackStackState[] backStack = null;
            if (this.mAdded != null && (N2 = this.mAdded.size()) > 0) {
                added = new int[N2];
                for (int i2 = 0; i2 < N2; i2++) {
                    added[i2] = this.mAdded.get(i2).mIndex;
                    if (added[i2] < 0) {
                        throwException(new IllegalStateException("Failure saving state: active " + this.mAdded.get(i2) + " has cleared index: " + added[i2]));
                    }
                    if (DEBUG) {
                        Log.v(TAG, "saveAllState: adding fragment #" + i2 + ": " + this.mAdded.get(i2));
                    }
                }
            }
            if (this.mBackStack != null && (N = this.mBackStack.size()) > 0) {
                backStack = new BackStackState[N];
                for (int i3 = 0; i3 < N; i3++) {
                    backStack[i3] = new BackStackState(this.mBackStack.get(i3));
                    if (DEBUG) {
                        Log.v(TAG, "saveAllState: adding back stack #" + i3 + ": " + this.mBackStack.get(i3));
                    }
                }
            }
            FragmentManagerState fms = new FragmentManagerState();
            fms.mActive = active;
            fms.mAdded = added;
            fms.mBackStack = backStack;
            return fms;
        } else if (!DEBUG) {
            return null;
        } else {
            Log.v(TAG, "saveAllState: no fragments!");
            return null;
        }
    }

    /* access modifiers changed from: package-private */
    public void restoreAllState(Parcelable state, FragmentManagerNonConfig nonConfig) {
        if (state != null) {
            FragmentManagerState fms = (FragmentManagerState) state;
            if (fms.mActive != null) {
                List<FragmentManagerNonConfig> childNonConfigs = null;
                if (nonConfig != null) {
                    List<Fragment> nonConfigFragments = nonConfig.getFragments();
                    childNonConfigs = nonConfig.getChildNonConfigs();
                    int count = nonConfigFragments != null ? nonConfigFragments.size() : 0;
                    for (int i = 0; i < count; i++) {
                        Fragment f = nonConfigFragments.get(i);
                        if (DEBUG) {
                            Log.v(TAG, "restoreAllState: re-attaching retained " + f);
                        }
                        FragmentState fs = fms.mActive[f.mIndex];
                        fs.mInstance = f;
                        f.mSavedViewState = null;
                        f.mBackStackNesting = 0;
                        f.mInLayout = false;
                        f.mAdded = false;
                        f.mTarget = null;
                        if (fs.mSavedFragmentState != null) {
                            fs.mSavedFragmentState.setClassLoader(this.mHost.getContext().getClassLoader());
                            f.mSavedViewState = fs.mSavedFragmentState.getSparseParcelableArray(VIEW_STATE_TAG);
                            f.mSavedFragmentState = fs.mSavedFragmentState;
                        }
                    }
                }
                this.mActive = new ArrayList<>(fms.mActive.length);
                if (this.mAvailIndices != null) {
                    this.mAvailIndices.clear();
                }
                for (int i2 = 0; i2 < fms.mActive.length; i2++) {
                    FragmentState fs2 = fms.mActive[i2];
                    if (fs2 != null) {
                        FragmentManagerNonConfig childNonConfig = null;
                        if (childNonConfigs != null && i2 < childNonConfigs.size()) {
                            childNonConfig = childNonConfigs.get(i2);
                        }
                        Fragment f2 = fs2.instantiate(this.mHost, this.mParent, childNonConfig);
                        if (DEBUG) {
                            Log.v(TAG, "restoreAllState: active #" + i2 + ": " + f2);
                        }
                        this.mActive.add(f2);
                        fs2.mInstance = null;
                    } else {
                        this.mActive.add((Object) null);
                        if (this.mAvailIndices == null) {
                            this.mAvailIndices = new ArrayList<>();
                        }
                        if (DEBUG) {
                            Log.v(TAG, "restoreAllState: avail #" + i2);
                        }
                        this.mAvailIndices.add(Integer.valueOf(i2));
                    }
                }
                if (nonConfig != null) {
                    List<Fragment> nonConfigFragments2 = nonConfig.getFragments();
                    int count2 = nonConfigFragments2 != null ? nonConfigFragments2.size() : 0;
                    for (int i3 = 0; i3 < count2; i3++) {
                        Fragment f3 = nonConfigFragments2.get(i3);
                        if (f3.mTargetIndex >= 0) {
                            if (f3.mTargetIndex < this.mActive.size()) {
                                f3.mTarget = this.mActive.get(f3.mTargetIndex);
                            } else {
                                Log.w(TAG, "Re-attaching retained fragment " + f3 + " target no longer exists: " + f3.mTargetIndex);
                                f3.mTarget = null;
                            }
                        }
                    }
                }
                if (fms.mAdded != null) {
                    this.mAdded = new ArrayList<>(fms.mAdded.length);
                    for (int i4 = 0; i4 < fms.mAdded.length; i4++) {
                        Fragment f4 = this.mActive.get(fms.mAdded[i4]);
                        if (f4 == null) {
                            throwException(new IllegalStateException("No instantiated fragment for index #" + fms.mAdded[i4]));
                        }
                        f4.mAdded = true;
                        if (DEBUG) {
                            Log.v(TAG, "restoreAllState: added #" + i4 + ": " + f4);
                        }
                        if (this.mAdded.contains(f4)) {
                            throw new IllegalStateException("Already added!");
                        }
                        this.mAdded.add(f4);
                    }
                } else {
                    this.mAdded = null;
                }
                if (fms.mBackStack != null) {
                    this.mBackStack = new ArrayList<>(fms.mBackStack.length);
                    for (int i5 = 0; i5 < fms.mBackStack.length; i5++) {
                        BackStackRecord bse = fms.mBackStack[i5].instantiate(this);
                        if (DEBUG) {
                            Log.v(TAG, "restoreAllState: back stack #" + i5 + " (index " + bse.mIndex + "): " + bse);
                            bse.dump("  ", new PrintWriter(new LogWriter(TAG)), false);
                        }
                        this.mBackStack.add(bse);
                        if (bse.mIndex >= 0) {
                            setBackStackIndex(bse.mIndex, bse);
                        }
                    }
                    return;
                }
                this.mBackStack = null;
            }
        }
    }

    public void attachController(FragmentHostCallback host, FragmentContainer container, Fragment parent) {
        if (this.mHost != null) {
            throw new IllegalStateException("Already attached");
        }
        this.mHost = host;
        this.mContainer = container;
        this.mParent = parent;
    }

    public void noteStateNotSaved() {
        this.mStateSaved = false;
    }

    public void dispatchCreate() {
        this.mStateSaved = false;
        moveToState(1, false);
    }

    public void dispatchActivityCreated() {
        this.mStateSaved = false;
        moveToState(2, false);
    }

    public void dispatchStart() {
        this.mStateSaved = false;
        moveToState(4, false);
    }

    public void dispatchResume() {
        this.mStateSaved = false;
        moveToState(5, false);
    }

    public void dispatchPause() {
        moveToState(4, false);
    }

    public void dispatchStop() {
        this.mStateSaved = true;
        moveToState(3, false);
    }

    public void dispatchReallyStop() {
        moveToState(2, false);
    }

    public void dispatchDestroyView() {
        moveToState(1, false);
    }

    public void dispatchDestroy() {
        this.mDestroyed = true;
        execPendingActions();
        moveToState(0, false);
        this.mHost = null;
        this.mContainer = null;
        this.mParent = null;
    }

    public void dispatchMultiWindowModeChanged(boolean isInMultiWindowMode) {
        if (this.mAdded != null) {
            for (int i = this.mAdded.size() - 1; i >= 0; i--) {
                Fragment f = this.mAdded.get(i);
                if (f != null) {
                    f.performMultiWindowModeChanged(isInMultiWindowMode);
                }
            }
        }
    }

    public void dispatchPictureInPictureModeChanged(boolean isInPictureInPictureMode) {
        if (this.mAdded != null) {
            for (int i = this.mAdded.size() - 1; i >= 0; i--) {
                Fragment f = this.mAdded.get(i);
                if (f != null) {
                    f.performPictureInPictureModeChanged(isInPictureInPictureMode);
                }
            }
        }
    }

    public void dispatchConfigurationChanged(Configuration newConfig) {
        if (this.mAdded != null) {
            for (int i = 0; i < this.mAdded.size(); i++) {
                Fragment f = this.mAdded.get(i);
                if (f != null) {
                    f.performConfigurationChanged(newConfig);
                }
            }
        }
    }

    public void dispatchLowMemory() {
        if (this.mAdded != null) {
            for (int i = 0; i < this.mAdded.size(); i++) {
                Fragment f = this.mAdded.get(i);
                if (f != null) {
                    f.performLowMemory();
                }
            }
        }
    }

    public boolean dispatchCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        boolean show = false;
        ArrayList<Fragment> newMenus = null;
        if (this.mAdded != null) {
            for (int i = 0; i < this.mAdded.size(); i++) {
                Fragment f = this.mAdded.get(i);
                if (f != null && f.performCreateOptionsMenu(menu, inflater)) {
                    show = true;
                    if (newMenus == null) {
                        newMenus = new ArrayList<>();
                    }
                    newMenus.add(f);
                }
            }
        }
        if (this.mCreatedMenus != null) {
            for (int i2 = 0; i2 < this.mCreatedMenus.size(); i2++) {
                Fragment f2 = this.mCreatedMenus.get(i2);
                if (newMenus == null || !newMenus.contains(f2)) {
                    f2.onDestroyOptionsMenu();
                }
            }
        }
        this.mCreatedMenus = newMenus;
        return show;
    }

    public boolean dispatchPrepareOptionsMenu(Menu menu) {
        boolean show = false;
        if (this.mAdded != null) {
            for (int i = 0; i < this.mAdded.size(); i++) {
                Fragment f = this.mAdded.get(i);
                if (f != null && f.performPrepareOptionsMenu(menu)) {
                    show = true;
                }
            }
        }
        return show;
    }

    public boolean dispatchOptionsItemSelected(MenuItem item) {
        if (this.mAdded != null) {
            for (int i = 0; i < this.mAdded.size(); i++) {
                Fragment f = this.mAdded.get(i);
                if (f != null && f.performOptionsItemSelected(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean dispatchContextItemSelected(MenuItem item) {
        if (this.mAdded != null) {
            for (int i = 0; i < this.mAdded.size(); i++) {
                Fragment f = this.mAdded.get(i);
                if (f != null && f.performContextItemSelected(item)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void dispatchOptionsMenuClosed(Menu menu) {
        if (this.mAdded != null) {
            for (int i = 0; i < this.mAdded.size(); i++) {
                Fragment f = this.mAdded.get(i);
                if (f != null) {
                    f.performOptionsMenuClosed(menu);
                }
            }
        }
    }

    public static int reverseTransit(int transit) {
        switch (transit) {
            case FragmentTransaction.TRANSIT_FRAGMENT_OPEN:
                return 8194;
            case FragmentTransaction.TRANSIT_FRAGMENT_FADE:
                return FragmentTransaction.TRANSIT_FRAGMENT_FADE;
            case 8194:
                return FragmentTransaction.TRANSIT_FRAGMENT_OPEN;
            default:
                return 0;
        }
    }

    public static int transitToStyleIndex(int transit, boolean enter) {
        switch (transit) {
            case FragmentTransaction.TRANSIT_FRAGMENT_OPEN:
                return enter ? 1 : 2;
            case FragmentTransaction.TRANSIT_FRAGMENT_FADE:
                return enter ? 5 : 6;
            case 8194:
                return enter ? 3 : 4;
            default:
                return -1;
        }
    }

    public View onCreateView(View parent, String name, Context context, AttributeSet attrs) {
        int containerId;
        Fragment fragment;
        int i;
        if (!"fragment".equals(name)) {
            return null;
        }
        String fname = attrs.getAttributeValue((String) null, "class");
        TypedArray a = context.obtainStyledAttributes(attrs, FragmentTag.Fragment);
        if (fname == null) {
            fname = a.getString(0);
        }
        int id = a.getResourceId(1, -1);
        String tag = a.getString(2);
        a.recycle();
        if (!Fragment.isSupportFragmentClass(this.mHost.getContext(), fname)) {
            return null;
        }
        if (parent != null) {
            containerId = parent.getId();
        } else {
            containerId = 0;
        }
        if (containerId == -1 && id == -1 && tag == null) {
            throw new IllegalArgumentException(attrs.getPositionDescription() + ": Must specify unique android:id, android:tag, or have a parent with an id for " + fname);
        }
        if (id != -1) {
            fragment = findFragmentById(id);
        } else {
            fragment = null;
        }
        if (fragment == null && tag != null) {
            fragment = findFragmentByTag(tag);
        }
        if (fragment == null && containerId != -1) {
            fragment = findFragmentById(containerId);
        }
        if (DEBUG) {
            Log.v(TAG, "onCreateView: id=0x" + Integer.toHexString(id) + " fname=" + fname + " existing=" + fragment);
        }
        if (fragment == null) {
            fragment = Fragment.instantiate(context, fname);
            fragment.mFromLayout = true;
            if (id != 0) {
                i = id;
            } else {
                i = containerId;
            }
            fragment.mFragmentId = i;
            fragment.mContainerId = containerId;
            fragment.mTag = tag;
            fragment.mInLayout = true;
            fragment.mFragmentManager = this;
            fragment.mHost = this.mHost;
            fragment.onInflate(this.mHost.getContext(), attrs, fragment.mSavedFragmentState);
            addFragment(fragment, true);
        } else if (fragment.mInLayout) {
            throw new IllegalArgumentException(attrs.getPositionDescription() + ": Duplicate id 0x" + Integer.toHexString(id) + ", tag " + tag + ", or parent id 0x" + Integer.toHexString(containerId) + " with another fragment for " + fname);
        } else {
            fragment.mInLayout = true;
            fragment.mHost = this.mHost;
            if (!fragment.mRetaining) {
                fragment.onInflate(this.mHost.getContext(), attrs, fragment.mSavedFragmentState);
            }
        }
        if (this.mCurState >= 1 || !fragment.mFromLayout) {
            moveToState(fragment);
        } else {
            moveToState(fragment, 1, 0, 0, false);
        }
        if (fragment.mView == null) {
            throw new IllegalStateException("Fragment " + fname + " did not create a view.");
        }
        if (id != 0) {
            fragment.mView.setId(id);
        }
        if (fragment.mView.getTag() == null) {
            fragment.mView.setTag(tag);
        }
        return fragment.mView;
    }

    /* access modifiers changed from: package-private */
    public LayoutInflaterFactory getLayoutInflaterFactory() {
        return this;
    }

    /* renamed from: android.support.v4.app.FragmentManagerImpl$FragmentTag */
    /* compiled from: FragmentManager */
    static class FragmentTag {
        public static final int[] Fragment = {16842755, 16842960, 16842961};
        public static final int Fragment_id = 1;
        public static final int Fragment_name = 0;
        public static final int Fragment_tag = 2;

        FragmentTag() {
        }
    }
}
