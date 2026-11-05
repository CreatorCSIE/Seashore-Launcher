package com.mio.util

import android.app.Activity
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Build
import android.util.DisplayMetrics
import com.tungsten.fcllibrary.component.theme.ThemeEngine

object DisplayUtil {
    @JvmField
    var screenWidth = -1
    @JvmField
    var screenHeight = -1

    lateinit var currentDisplayMetrics: DisplayMetrics

    @JvmField
    var notchSize = -1

    @JvmStatic
    fun getDisplayMetrics(activity: Activity): DisplayMetrics {
        var displayMetrics = DisplayMetrics()
        if (activity.isInMultiWindowMode || activity.isInPictureInPictureMode) {
            displayMetrics = activity.resources.displayMetrics
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // ★ 批 2 微调：上游 FCL 用 Kotlin 2.x 宽容了 display 可空性，
                //   我方钉版 Kotlin 1.8.22 要求显式安全调用（运行时 Activity 总有 display，语义不变）
                activity.display?.getRealMetrics(displayMetrics)
            } else {
                activity.windowManager.getDefaultDisplay().getRealMetrics(displayMetrics)
            }
            if (!ThemeEngine.getInstance().getTheme().fullscreen) {
                if (activity.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) displayMetrics.heightPixels -= notchSize
                else displayMetrics.widthPixels -= notchSize
            }
        }
        return displayMetrics
    }

    @JvmStatic
    fun refreshDisplayMetrics(activity: Activity) {
        currentDisplayMetrics = getDisplayMetrics(activity)
    }

    @JvmStatic
    fun updateWindowSize(activity: Activity) {
        if (notchSize == -1) computeNotchSize(activity)
        refreshDisplayMetrics(activity)
    }

    private fun computeNotchSize(activity: Activity) {
        notchSize = 0
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        runCatching {
            val cutout: Rect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                activity.windowManager.currentWindowMetrics.windowInsets
                    .displayCutout?.boundingRects?.get(0)
            } else {
                activity.window.decorView.rootWindowInsets.displayCutout
                    ?.boundingRects?.get(0)
            } ?: return
            val orientation: Int = activity.resources.configuration.orientation
            notchSize = when (orientation) {
                Configuration.ORIENTATION_PORTRAIT -> cutout.height()
                Configuration.ORIENTATION_LANDSCAPE -> cutout.width()
                else -> cutout.width().coerceAtMost(cutout.height())
            }
        }
    }
}
