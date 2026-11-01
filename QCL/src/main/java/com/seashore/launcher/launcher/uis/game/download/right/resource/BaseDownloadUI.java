package com.seashore.launcher.launcher.uis.game.download.right.resource;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import com.seashore.launcher.launcher.MainActivity;
import com.seashore.launcher.launcher.mod.RemoteMod;
import com.seashore.launcher.launcher.mod.RemoteModRepository;
import com.seashore.launcher.launcher.uis.tools.BaseUI;
import com.seashore.launcher.utils.animation.CustomAnimationUtils;
import com.seashore.launcher.utils.string.ModTranslations;

import com.seashore.launcher.R;
/* loaded from: classes2.dex */
public class BaseDownloadUI extends BaseUI {
    public LinearLayout baseDownloadUI;
    public RemoteMod bean;
    public boolean isFirst;
    public ModTranslations.Mod modTranslation;
    public RemoteModRepository repository;
    public int resourceType;

    public BaseDownloadUI(Context context, MainActivity mainActivity, RemoteModRepository remoteModRepository, RemoteMod remoteMod, int i) {
        super(context, mainActivity);
        ModTranslations modTranslations;
        this.isFirst = true;
        this.repository = remoteModRepository;
        this.bean = remoteMod;
        if (i == 0) {
            modTranslations = ModTranslations.MOD;
        } else if (i == 1) {
            modTranslations = ModTranslations.MODPACK;
        } else {
            modTranslations = ModTranslations.EMPTY;
        }
        this.modTranslation = modTranslations.getModByCurseForgeId(remoteMod.getSlug());
        this.resourceType = i;
        onCreate();
    }

    @Override // com.seashore.launcher.launcher.uis.tools.BaseUI, com.seashore.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onCreate() {
        super.onCreate();
        this.baseDownloadUI = (LinearLayout) LayoutInflater.from(this.context).inflate(R.layout.ui_download_resource, (ViewGroup) null);
    }

    @Override // com.seashore.launcher.launcher.uis.tools.BaseUI, com.seashore.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStart() {
        super.onStart();
        this.activity.uiContainer.addView(this.baseDownloadUI);
        ViewGroup.LayoutParams layoutParams = this.baseDownloadUI.getLayoutParams();
        layoutParams.width = -1;
        layoutParams.height = -1;
        this.baseDownloadUI.setLayoutParams(layoutParams);
        this.activity.showBarTitle(this.bean.getTitle(), canGoBackToLast(), false);
        CustomAnimationUtils.showViewFromLeft(this.baseDownloadUI, this.activity, this.context, true);
    }

    @Override // com.seashore.launcher.launcher.uis.tools.BaseUI, com.seashore.launcher.launcher.uis.tools.UILifecycleCallbacks
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.baseDownloadUI, this.activity, this.context, true);
        this.activity.uiContainer.removeView(this.baseDownloadUI);
    }

    public <T> T findViewById(int i) {
        return (T) this.baseDownloadUI.findViewById(i);
    }
}
