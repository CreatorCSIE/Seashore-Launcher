package com.seashore.launcher.launcher.uis.lab;

import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import com.seashore.launcher.R;
import com.seashore.launcher.launcher.MainActivity;
import com.seashore.launcher.launcher.dialogs.lab.LabColorTextDialog;
import com.seashore.launcher.launcher.dialogs.lab.LabDatapackDialog;
import com.seashore.launcher.launcher.dialogs.lab.LabRecipeDialog;
import com.seashore.launcher.launcher.dialogs.lab.LabSchematicDownloadDialog;
import com.seashore.launcher.launcher.dialogs.lab.LabSchematicStudioDialog;
import com.seashore.launcher.launcher.dialogs.lab.LabSeedDialog;
import com.seashore.launcher.launcher.uis.tools.BaseUI;
import com.seashore.launcher.utils.animation.CustomAnimationUtils;

/**
 * 实验室页面：集中放置若干实验性小工具，点击卡片弹出对应弹窗。
 */
public class LabUI extends BaseUI implements View.OnClickListener {
    public LinearLayout labUI;
    // ★ 试点批：卡片容器换成 FCLLinearLayout——它继承 LinearLayoutCompat
    //   （is-a ViewGroup，**不是** android.widget.LinearLayout），只能按 View 持有。
    //   本页对卡片仅做点击监听，View 类型足够。
    private View cardColorText;
    private View cardSeed;
    private View cardRecipe;
    private View cardDatapack;
    private View cardSchematicStudio;
    private View cardSchematicDownload;

    public LabUI(Context context, MainActivity mainActivity) {
        super(context, mainActivity);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.labUI = (LinearLayout) this.activity.findViewById(R.id.ui_lab);
        this.cardColorText = this.activity.findViewById(R.id.lab_card_color_text);
        this.cardSeed = this.activity.findViewById(R.id.lab_card_seed);
        this.cardRecipe = this.activity.findViewById(R.id.lab_card_recipe);
        this.cardDatapack = this.activity.findViewById(R.id.lab_card_datapack);
        this.cardSchematicStudio = this.activity.findViewById(R.id.lab_card_schematic_studio);
        this.cardSchematicDownload = this.activity.findViewById(R.id.lab_card_schematic_download);
        this.cardColorText.setOnClickListener(this);
        this.cardSeed.setOnClickListener(this);
        this.cardRecipe.setOnClickListener(this);
        this.cardDatapack.setOnClickListener(this);
        this.cardSchematicStudio.setOnClickListener(this);
        this.cardSchematicDownload.setOnClickListener(this);
        // ★ 试点批判读（2026-11-05）：页面根背景**不接** FCL 主题背景图——
        //   FCL 默认 background_light 浅蓝与 ltColor（#7797CF 提亮）几乎同色，
        //   卡片层次被抹平（观感事故）。QCL 自有 launcher_ui_background 已有
        //   values-night 亮暗两套，页面底色归 QCL、组件取色归 ThemeEngine，
        //   各管一层。FCL 背景图留给批 2 的 FCLActivity 页面框架统一决策。
    }

    @Override
    public void onStart() {
        super.onStart();
        this.activity.showBarTitle(this.context.getResources().getString(R.string.lab_ui_title), canGoBackToLast(), true);
        CustomAnimationUtils.showViewFromLeft(this.labUI, this.activity, this.context, true);
    }

    @Override
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft(this.labUI, this.activity, this.context, true);
    }

    @Override
    public void onClick(View view) {
        if (view == this.cardColorText) {
            new LabColorTextDialog(this.activity).show();
        } else if (view == this.cardSeed) {
            new LabSeedDialog(this.activity).show();
        } else if (view == this.cardRecipe) {
            new LabRecipeDialog(this.activity).show();
        } else if (view == this.cardDatapack) {
            new LabDatapackDialog(this.activity).show();
        } else if (view == this.cardSchematicStudio) {
            new LabSchematicStudioDialog(this.activity).show();
        } else if (view == this.cardSchematicDownload) {
            new LabSchematicDownloadDialog(this.activity).show();
        }
    }
}