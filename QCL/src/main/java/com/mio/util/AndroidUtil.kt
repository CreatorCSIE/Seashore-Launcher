package com.mio.util

import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2

// ★ Phase 1.5 批 2 替身件：FCL 原 AndroidUtil.kt 是 347+ 行杂烩（保活锁/剪贴板/权限等），
//   fcllibrary 页面框架只消费 disableMouseWheelScroll 一个扩展函数，按 FCLApp/FCLPath
//   替身惯例最小抽取。生成类名 AndroidUtilKt 与上游一致（FCLMultiPageUI 按此静态调用）。
//   后续批次若需其他函数，从 reference/FCL-1.3.4.4 对应位置增补。

fun ViewPager2.disableMouseWheelScroll() {
    for (i in 0 until childCount) {
        val child = getChildAt(i)
        if (child is RecyclerView) {
            child.setOnGenericMotionListener { _, event ->
                event.action == MotionEvent.ACTION_SCROLL
            }
        }
    }
}
