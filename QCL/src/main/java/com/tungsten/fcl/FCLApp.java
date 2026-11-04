package com.tungsten.fcl;

import android.app.Activity;
import android.content.Context;

import java.lang.ref.WeakReference;

/**
 * ★ Phase 1.5 替身件：FCL 原 FCLApp 是 Application（FCL 自己的启动类），
 * Seashore 的真实 Application 是 QCLApplication——这里只保留 fcllibrary
 * 消费的两个静态门面（getAppContext/getActivity），由 QCLApplication 接线喂值。
 * 包名保持 com.tungsten.fcl 不动：移植文件 import 零改动、cherry-pick 上游零冲突。
 */
public final class FCLApp {
    private static Context appContext;
    private static WeakReference<Activity> currentActivity = new WeakReference<>(null);

    private FCLApp() {
    }

    /** QCLApplication.onCreate 调用 */
    public static void init(Context context) {
        appContext = context.getApplicationContext();
    }

    /** QCLApplication 的 ActivityLifecycleCallbacks 接线调用 */
    public static void setCurrentActivity(Activity activity) {
        currentActivity = new WeakReference<>(activity);
    }

    public static Context getAppContext() {
        return appContext;
    }

    public static Activity getActivity() {
        return currentActivity.get();
    }
}
