/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.app.Activity
 *  android.content.Context
 *  android.content.res.Resources
 *  android.graphics.Bitmap
 *  android.graphics.Bitmap$Config
 *  android.graphics.BitmapFactory
 *  android.graphics.BitmapFactory$Options
 *  android.graphics.drawable.BitmapDrawable
 *  android.graphics.drawable.ColorDrawable
 *  android.graphics.drawable.Drawable
 *  android.graphics.drawable.TransitionDrawable
 *  android.os.Handler
 *  android.os.Looper
 *  android.view.View
 *  androidx.core.content.ContextCompat
 */
package com.seashore.launcher.launcher.uis.main;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import androidx.core.content.ContextCompat;

import com.seashore.launcher.R;
public class DynamicBackground {
    private static final long INTERVAL_MS = 10000L;
    private static final int FADE_MS = 600;
    public static final int[] RES_IDS = new int[]{R.drawable.qcl_bg_1, R.drawable.qcl_bg_2, R.drawable.qcl_bg_3, R.drawable.qcl_bg_4, R.drawable.qcl_bg_5, R.drawable.qcl_bg_6};

    /** ★ 1.5.0：白天背景图（玩家提供的 26.3 截图）。 */
    public static final int RES_DAY = R.drawable.qcl_bg_day;
    /** ★ 1.5.0：黑夜背景图（玩家提供的 26.3 截图，沿用原名 ic_background_classic）。 */
    public static final int RES_NIGHT = R.drawable.ic_background_classic;

    // ★ Phase 1.5 批 4：原 DAY_START_HOUR/DAY_END_HOUR「按现实时间分昼夜」已退役 ——
    //   昼夜判定改走 ThemeEngine.isNightMode（跟随系统深色模式 / FCL 强制亮暗）。
    private final Activity activity;
    private final View target;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Drawable[] cache = new Drawable[RES_IDS.length];
    private int index = 0;
    private boolean running = false;

    // ★★★★★ 1.5.2 新增：**自定义背景图**（「网络」模式下玩家自己指定的图）。
    //
    //   用户需求：「网络自动切换背景图，没有自定义的更改选项，
    //             只有普通的背景图是有自定义更改」。
    //
    //   ★ 范围严格限定：**只影响背景怎么画**，不碰任何启动/关闭/跳转流程。
    //     外部把 URL 下载解码成 Drawable 后塞进来，轮播就改用它们；
    //     塞空 = 回到内置 6 张（下载失败也走这条路降级）。
    private static volatile java.util.List<Drawable> customFrames = null;

    /** 由外部调用：设置「网络」模式要轮播的自定义图（传 null/空 = 恢复内置 6 张）。 */
    public static void setCustomFrames(java.util.List<Drawable> frames) {
        customFrames = (frames == null || frames.isEmpty()) ? null : frames;
    }

    /** 当前是否在用自定义图。 */
    public static boolean hasCustomFrames() {
        return customFrames != null;
    }

    /**
     * ★ 1.5.2：给**游戏内等待界面**（{@code LayoutPanel}）取用自定义帧。
     *
     * <p>等待界面的那 6 格轮播原本永远只放内置 `qcl_bg_1~6`；现在只要玩家在
     * 外观 → 背景 → 网络里填了图，这里就返回他的图，**两边一致**。
     *
     * @return 该下标的自定义帧；没有自定义图时返回 null（调用方回退内置图）
     */
    public static android.graphics.drawable.Drawable getCustomFrame(int index) {
        java.util.List<android.graphics.drawable.Drawable> f = customFrames;
        if (f == null || f.isEmpty()) {
            return null;
        }
        return f.get(Math.abs(index) % f.size());
    }

    /** 自定义帧数量（0 = 没有自定义图）。 */
    public static int getCustomFrameCount() {
        java.util.List<android.graphics.drawable.Drawable> f = customFrames;
        return f == null ? 0 : f.size();
    }

    /** 当前轮播总帧数（自定义优先）。 */
    private static int frameCount() {
        java.util.List<Drawable> f = customFrames;
        return (f == null) ? RES_IDS.length : f.size();
    }

    private final Runnable tick = new Runnable(){

        @Override
        public void run() {
            if (!DynamicBackground.this.running) {
                return;
            }
            DynamicBackground.this.index =
                    (DynamicBackground.this.index + 1) % Math.max(1, frameCount());
            DynamicBackground.this.apply(DynamicBackground.this.index, true);
            DynamicBackground.this.handler.postDelayed((Runnable)this, 10000L);
        }
    };

    public DynamicBackground(Activity activity, View target) {
        this.activity = activity;
        this.target = target;
    }

    public void start() {
        if (this.running) {
            return;
        }
        this.running = true;
        this.apply(this.index, false);
        this.handler.postDelayed(this.tick, 10000L);
    }

    public void stop() {
        this.running = false;
        this.handler.removeCallbacks(this.tick);
    }

    /**
     * ★ 1.2.9：从后台回到前台后**保证轮播还在跑**。
     *
     * 现象：切到后台再回来，背景图就不动了（卡在一张上）。
     * 可能的原因有几种（Handler 回调被系统清掉、背景被别处换成了静态图、
     * 或者中途被 stop 过），这里不猜具体是哪一种 —— 回到前台就无条件把下一轮挂上，
     * 并且发现背景已经不是轮播的图时补一张回来。
     */
    public void ensureRunning() {
        if (!this.running) {
            this.start();
            return;
        }
        this.handler.removeCallbacks(this.tick);
        this.handler.postDelayed(this.tick, INTERVAL_MS);
        Drawable cur = this.target.getBackground();
        // 背景被主题/别处换成静态图了（或压根没有）→ 把当前这张补回来
        if (cur == null || !(cur instanceof TransitionDrawable)) {
            this.apply(this.index, false);
        }
    }

    private void apply(int i, boolean animate) {
        Drawable next = this.get(i);
        if (next == null) {
            return;
        }
        if (!animate || this.target.getBackground() == null) {
            this.target.setBackground(next);
            return;
        }
        Drawable current = this.target.getBackground();
        TransitionDrawable td = new TransitionDrawable(new Drawable[]{current, next});
        td.setCrossFadeEnabled(true);
        this.target.setBackground((Drawable)td);
        td.startTransition(600);
    }

    private Drawable get(int i) {
        if (this.cache[i] != null) {
            return this.cache[i];
        }
        // ★ 1.5.2：有自定义图时优先用它（内置 6 张完全不参与）
        java.util.List<Drawable> custom = customFrames;
        if (custom != null && !custom.isEmpty()) {
            Drawable d = custom.get(i % custom.size());
            if (d != null) {
                return d;
            }
        }
        int safe = (i < 0 || i >= this.cache.length) ? 0 : i;
        if (this.cache[safe] != null) {
            return this.cache[safe];
        }
        try {
            Bitmap bmp = this.decodeScaled(RES_IDS[safe]);
            if (bmp == null) {
                return null;
            }
            Bitmap processed = this.dim(bmp);
            if (processed != bmp) {
                bmp.recycle();
            }
            this.cache[safe] = new BitmapDrawable(this.activity.getResources(), processed);
        }
        catch (Throwable t) {
            this.cache[safe] = new ColorDrawable(-1184275);
        }
        return this.cache[safe];
    }

    private Bitmap decodeScaled(int resId) {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeResource((Resources)this.activity.getResources(), (int)resId, (BitmapFactory.Options)o);
        int w = o.outWidth;
        int h = o.outHeight;
        if (w <= 0 || h <= 0) {
            return null;
        }
        int reqW = Math.max(1080, this.activity.getResources().getDisplayMetrics().widthPixels * 3 / 2);
        int sample = 1;
        while (w / sample > reqW * 2) {
            sample *= 2;
        }
        BitmapFactory.Options d = new BitmapFactory.Options();
        d.inSampleSize = sample;
        d.inPreferredConfig = Bitmap.Config.RGB_565;
        try {
            return BitmapFactory.decodeResource((Resources)this.activity.getResources(), (int)resId, (BitmapFactory.Options)d);
        }
        catch (OutOfMemoryError e) {
            return null;
        }
    }

    private Bitmap dim(Bitmap src) {
        return src;
    }

    public static void applySingle(Activity activity, View view, int resId) {
        try {
            Drawable d = ContextCompat.getDrawable((Context)activity, (int)resId);
            if (d != null) {
                view.setBackground(d);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    // ★ Phase 1.5 批 4：「现实时间取时面」整体退役 ——
    //   currentHourBlocking / cachedHour / FETCHING / currentHour / refreshHourAsync /
    //   fetchNetHour / isDaytimeNow / isSystemNightMode / applyRealTimeNightMode 全删。
    //   昼夜判定改走 ThemeEngine.isNightMode（themeMode=0 跟随系统深色模式，
    //   系统级按日落/日出定时切换；1/2 强制亮/暗）。网络取时不再有任何调用方。

    /**
     * ★ Phase 1.5 批 4：昼夜判定改走 {@code ThemeEngine.isNightMode} ——
     *   themeMode=0 跟随系统深色模式（系统级按日落/日出定时切换），
     *   1/2 强制亮/暗。与 values-night 主题配色**同一判据**，壁纸和配色永远一致。
     *
     * <p>原「现实时间取时面」（网络取时 + 本地时钟 + 8 小时时钟偏差校准）
     * 整套机制随 applyRealTimeNightMode 一起退役。
     */
    public static int autoResId(Context ctx) {
        return com.tungsten.fcllibrary.component.theme.ThemeEngine.isNightMode(ctx)
                ? RES_NIGHT : RES_DAY;
    }

    /**
     * ★ 1.5.0：把昼夜背景应用到给定视图。
     *
     * <p>★ Phase 1.5 批 4：判定改走 {@link #autoResId(Context)}（ThemeEngine 亮暗），
     *   原「网络时间异步校正」块整体退役 —— 判据是同步的系统状态，无需校准。
     */
    public static void applyAutoDayNight(final Activity activity, final View view) {
        applySingle(activity, view, autoResId(activity));
    }
}

