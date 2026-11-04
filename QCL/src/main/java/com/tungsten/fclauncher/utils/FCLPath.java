package com.tungsten.fclauncher.utils;

import android.content.Context;

import java.io.File;

/**
 * ★ Phase 1.5 替身件：FCL 原 FCLPath 是启动时全量初始化的路径表（依赖外部存储等），
 * fcllibrary 主题层只消费 LT/DK_BACKGROUND_PATH 两个字段，这里按此最小实现：
 * 背景图落在应用私有目录 filesDir/background/，不需要存储权限。
 * 包名保持 com.tungsten.fclauncher.utils 不动（同 FCLApp 理由）。
 */
public class FCLPath {
    public static String LT_BACKGROUND_PATH;
    public static String DK_BACKGROUND_PATH;

    /** QCLApplication.onCreate 调用（先于任何 ThemeEngine.applyBackground） */
    public static void init(Context context) {
        File dir = new File(context.getFilesDir(), "background");
        if (!dir.isDirectory()) {
            dir.mkdirs();
        }
        String base = dir.getAbsolutePath();
        LT_BACKGROUND_PATH = base + "/lt.png";
        DK_BACKGROUND_PATH = base + "/dk.png";
    }
}
