/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  android.annotation.SuppressLint
 *  android.app.Activity
 *  android.content.Context
 *  android.content.Intent
 *  android.graphics.Bitmap
 *  android.graphics.BitmapFactory
 *  android.graphics.BitmapFactory$Options
 *  android.graphics.Color
 *  android.graphics.drawable.BitmapDrawable
 *  android.graphics.drawable.Drawable
 *  android.net.Uri
 *  android.os.Build$VERSION
 *  android.os.Environment
 *  android.os.Handler
 *  android.os.Message
 *  android.text.Editable
 *  android.text.TextWatcher
 *  android.view.View
 *  android.view.View$OnClickListener
 *  android.view.ViewGroup
 *  android.widget.CompoundButton
 *  android.widget.CompoundButton$OnCheckedChangeListener
 *  android.widget.EditText
 *  android.widget.ImageButton
 *  android.widget.LinearLayout
 *  android.widget.RadioButton
 *  android.widget.TextView
 *  androidx.annotation.NonNull
 *  androidx.appcompat.widget.SwitchCompat
 *  com.tungsten.filepicker.Constants$SELECTION_MODES
 *  com.tungsten.filepicker.FileChooser
 */
package com.seashore.launcher.launcher.uis.universal.setting.right.launcher;

import com.seashore.launcher.utils.QclColors;
import com.seashore.launcher.skin.gltf.SkinRenderer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
// ★★★★★ 2026-10-16 字体颜色自定义调节用到 Button
import android.widget.Button;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import com.seashore.launcher.launcher.MainActivity;
import com.seashore.launcher.launcher.dialogs.tools.ColorSelectorDialog;
import com.seashore.launcher.launcher.uis.tools.BaseUI;
import com.seashore.launcher.manifest.AppManifest;
import com.seashore.launcher.utils.animation.CustomAnimationUtils;
import com.seashore.launcher.utils.file.UriUtils;
import com.seashore.launcher.utils.gson.GsonUtils;
import com.tungsten.filepicker.Constants;
import com.tungsten.filepicker.FileChooser;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.seashore.launcher.R;
public class ExteriorSettingUI
extends BaseUI
implements View.OnClickListener,
CompoundButton.OnCheckedChangeListener {
    private static final int PICK_BACKGROUND_REQUEST = 4000;
    public LinearLayout exteriorSettingUI;
    // ★ Phase 1.5 批 4：「QCL 主题」下拉框（selectTheme）、自定义主题色块
    //   （customThemeBlock）、重置主题色按钮（resetThemeColor）及其预览控件
    //   整体退役 —— 主题色唯一入口 = 下方 FCL 主题组（ThemeEngine）。

    // ★★★★★ 2026-10-16 新增：字体颜色自定义调节（外观页）
    private LinearLayout selectFontColor;
    private View fontColorView;
    private TextView fontColorText;
    private Button resetFontColor;
    private LinearLayout selectPanelColor;
    private View panelColorView;
    private TextView panelColorText;
    /** ★ 1.5.2：重置**面板（背景板）色**的按钮。 */
    private TextView resetPanelColor;
    /** ★ 1.5.0 新增：「自定义面板色」色块（点进去开调色板）。 */
    private LinearLayout customPanelBlock;
    private View panelColorBlockView;
    private TextView panelColorBlockText;
    private SwitchCompat fullscreenSwitch;
    private SwitchCompat showAccountModelSwitch;
    private SwitchCompat transBgSwitch;
    /** ★ 1.5.0：3D 人物动作下拉（站立/走路/跑步/旋转/挥手） */
    private Spinner accountModelAnimSpinner;
    /** ★ 1.5.0：动作那一整行（开关关闭时整行置灰不可选） */
    private LinearLayout accountModelAnimRow;
    private LinearLayout fullscreenSetting;
    private RadioButton defaultRadio;
    private RadioButton classicRadio;
    /** ★ 1.5.0：白天背景（固定显示白天图） */
    private RadioButton dayRadio;
    /** ★ 1.5.0：排版选择（旧排版 / 新排版） */
    private RadioButton layoutOldRadio;
    private RadioButton layoutNewRadio;
    private RadioButton customRadio;
    private RadioButton onlineRadio;
    private EditText editBgPath;
    private EditText editBgUrl;
    private ImageButton selectBgPath;
    @SuppressLint(value={"HandlerLeak"})
    public final Handler handler = new Handler(){

        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
        }
    };

    public ExteriorSettingUI(Context context, MainActivity activity) {
        super(context, activity);
    }

    @Override
    @SuppressLint(value={"UseCompatLoadingForDrawables"})
    public void onCreate() {
        super.onCreate();
        this.exteriorSettingUI = (LinearLayout)this.activity.findViewById(R.id.ui_setting_exterior);
        this.selectPanelColor = (LinearLayout)this.activity.findViewById(R.id.select_panel_color);
        this.panelColorView = this.activity.findViewById(R.id.panel_color_view);
        this.panelColorText = (TextView)this.activity.findViewById(R.id.panel_color_text);
        // ★ 1.5.2：面板（背景板）色的重置按钮
        this.resetPanelColor = (TextView)this.activity.findViewById(R.id.reset_panel_color);
        if (this.resetPanelColor != null) {
            this.resetPanelColor.setOnClickListener((View.OnClickListener)this);
        }
        // ★ 1.5.0：自定义面板色块（★ 批 4：自定义主题色块已退役）
        this.customPanelBlock = (LinearLayout)this.activity.findViewById(R.id.custom_panel_block);
        this.panelColorBlockView = this.activity.findViewById(R.id.panel_color_block_view);
        this.panelColorBlockText = (TextView)this.activity.findViewById(R.id.panel_color_block_text);
        // ★★★★★ 2026-10-16 新增：**字体颜色自定义调节**的三个控件
        //   （用户要求「在外观那里加一个字体颜色自定义调节功能」）
        this.selectFontColor = (LinearLayout)this.activity.findViewById(R.id.select_font_color);
        this.fontColorView = this.activity.findViewById(R.id.font_color_view);
        this.fontColorText = (TextView)this.activity.findViewById(R.id.font_color_text);
        this.resetFontColor = (Button)this.activity.findViewById(R.id.reset_font_color);
        if (this.selectFontColor != null) {
            this.selectFontColor.setOnClickListener((View.OnClickListener)this);
        }
        if (this.resetFontColor != null) {
            this.resetFontColor.setOnClickListener((View.OnClickListener)this);
        }
        refreshFontColorUi();
        // ★ 1.5.2：启动等待界面图片 —— 6 格绑定 + 点击单独换图
        int[] slotIds = new int[]{
                R.id.wait_bg_0, R.id.wait_bg_1, R.id.wait_bg_2,
                R.id.wait_bg_3, R.id.wait_bg_4, R.id.wait_bg_5};
        for (int si = 0; si < WAIT_BG_SLOTS; si++) {
            this.waitBgSlots[si] = (android.widget.ImageView)
                    this.activity.findViewById(slotIds[si]);
            final int slot = si;
            if (this.waitBgSlots[si] != null) {
                this.waitBgSlots[si].setOnClickListener(v -> pickWaitBg(slot));
            }
        }
        this.resetWaitBg = (TextView) this.activity.findViewById(R.id.reset_wait_bg);
        if (this.resetWaitBg != null) {
            this.resetWaitBg.setOnClickListener((View.OnClickListener) this);
        }
        refreshWaitBgSlots();
        this.fullscreenSwitch = (SwitchCompat)this.activity.findViewById(R.id.switch_full_screen);
        this.showAccountModelSwitch = (SwitchCompat)this.activity.findViewById(R.id.switch_show_account_model);
        this.transBgSwitch = (SwitchCompat)this.activity.findViewById(R.id.switch_transparent_bg);
        // ★ 1.5.0：3D 人物动作（站立/走路/跑步/旋转/挥手）
        this.accountModelAnimSpinner = (Spinner)this.activity.findViewById(R.id.spinner_account_model_anim);
        this.accountModelAnimRow = (LinearLayout)this.activity.findViewById(R.id.account_model_anim_row);
        this.fullscreenSetting = (LinearLayout)this.activity.findViewById(R.id.fullscreen_layout);
        this.defaultRadio = (RadioButton)this.activity.findViewById(R.id.select_bg_default);
        this.classicRadio = (RadioButton)this.activity.findViewById(R.id.select_bg_classic);
        this.dayRadio = (RadioButton)this.activity.findViewById(R.id.select_bg_day);
        this.customRadio = (RadioButton)this.activity.findViewById(R.id.select_bg_custom);
        this.onlineRadio = (RadioButton)this.activity.findViewById(R.id.select_bg_online);
        // ★★★ 1.5.0：排版（旧排版 / 新排版）。这里就先把选中状态定好 —— 此刻**监听还没注册**
        //   （注册在下面 onCreate 末尾），所以不会触发 onCheckedChanged、不会误弹「重启后生效」。
        this.layoutOldRadio = (RadioButton)this.activity.findViewById(R.id.select_layout_old);
        this.layoutNewRadio = (RadioButton)this.activity.findViewById(R.id.select_layout_new);
        boolean useNewLayout = this.activity.launcherSetting.useNewLayout();
        if (this.layoutNewRadio != null) {
            this.layoutNewRadio.setChecked(useNewLayout);
        }
        if (this.layoutOldRadio != null) {
            this.layoutOldRadio.setChecked(!useNewLayout);
        }
        this.editBgPath = (EditText)this.activity.findViewById(R.id.edit_bg_path);
        this.editBgUrl = (EditText)this.activity.findViewById(R.id.edit_bg_url);
        this.selectBgPath = (ImageButton)this.activity.findViewById(R.id.select_bg_path);
        // ★ 1.3.4：type 0（自动切换/动态背景）改由「网络」选项承载；type 1（经典图片）成为「默认」
        // ★ 1.5.0：type 1 = 「默认」= 【按现实时间自动切昼夜】（老配置存量值就是 1，自动继承，无需迁移）
        //           新增 type 4 =「黑夜」（固定黑夜图）、type 5 =「白天」（固定白天图）
        if (this.activity.launcherSetting.launcherBackground.type == 0) {
            // 动态背景（自动切换）→ 选中「网络」
            this.onlineRadio.setChecked(true);
            this.editBgPath.setEnabled(false);
            // ★ 1.5.2：URL 可编辑 —— 玩家可填自己的图；留空则仍用内置 6 张。
            //   只改「能不能编辑」，不碰任何启动/跳转流程。
            this.editBgUrl.setEnabled(false);
            this.selectBgPath.setEnabled(false);
        } else if (this.activity.launcherSetting.launcherBackground.type == 1) {
            // ★ 1.5.0：默认 → 按现实时间自动切昼夜
            this.defaultRadio.setChecked(true);
            this.editBgPath.setEnabled(false);
            this.editBgUrl.setEnabled(false);
            this.selectBgPath.setEnabled(false);
            com.seashore.launcher.launcher.uis.main.DynamicBackground
                    .applyAutoDayNight(this.activity, this.activity.launcherLayout);
        } else if (this.activity.launcherSetting.launcherBackground.type == 4) {
            // ★ 1.5.0：黑夜（固定）
            this.classicRadio.setChecked(true);
            this.editBgPath.setEnabled(false);
            this.editBgUrl.setEnabled(false);
            this.selectBgPath.setEnabled(false);
            this.activity.launcherLayout.setBackground(this.context.getDrawable(R.drawable.ic_background_classic));
        } else if (this.activity.launcherSetting.launcherBackground.type == 5) {
            // ★ 1.5.0：白天（固定）
            this.dayRadio.setChecked(true);
            this.editBgPath.setEnabled(false);
            this.editBgUrl.setEnabled(false);
            this.selectBgPath.setEnabled(false);
            this.activity.launcherLayout.setBackground(this.context.getDrawable(R.drawable.qcl_bg_day));
        } else if (this.activity.launcherSetting.launcherBackground.type == 2) {
            this.customRadio.setChecked(true);
            this.editBgPath.setEnabled(true);
            this.editBgUrl.setEnabled(false);
            this.selectBgPath.setEnabled(true);
            if (new File(this.activity.launcherSetting.launcherBackground.path).exists() && ExteriorSettingUI.isImageFile(this.activity.launcherSetting.launcherBackground.path)) {
                // ★★★★★ 1.5.8（照 FCL 1.3.4.3 的「自持位图 + 防回收」思路）：
                //
                //   ① **降采样**：原来 decodeFile 不做采样，手机相册里 4000x3000 的图
                //      会整张解成 48 MB 的 ARGB_8888，低内存设备上直接 OOM 或返回 null。
                //      现在按屏幕尺寸算 inSampleSize（只解到够铺满屏幕的大小）。
                //   ② **null 兜底**：decodeFile 返回 null 时，原代码
                //      `new BitmapDrawable(bitmap)` 会在构造里 bitmap.getDensity() 处 NPE
                //      （文件损坏/内存不足都会走到这里）。现在退回经典背景。
                //   ③ **换背景前回收旧位图**：BitmapDrawable 自己不会 recycle 位图，
                //      反复换背景会一直累积。这里把旧的那张回收掉。
                Bitmap qclNewBg = decodeSampledBackground(
                        (String) this.activity.launcherSetting.launcherBackground.path);
                if (qclNewBg != null) {
                    recycleOldBackground(this.activity.launcherLayout);
                    this.activity.launcherLayout.setBackground((Drawable) new BitmapDrawable(
                            this.activity.getResources(), qclNewBg));
                } else {
                    // 解码失败（损坏 / 内存不足）→ 退回经典背景，绝不留 null
                    android.util.Log.w("jrelog", "[背景] 自定义背景解码失败，退回经典:" 
                            + this.activity.launcherSetting.launcherBackground.path);
                    this.activity.launcherLayout.setBackground(
                            this.context.getDrawable(R.drawable.ic_background_classic));
                }
            } else {
                // ★ 1.5.0：自定义路径无效时的占位图 → 换成经典（黑夜）那张
                this.activity.launcherLayout.setBackground(this.context.getDrawable(R.drawable.ic_background_classic));
            }
        } else {
            // ★ 1.3.4：type 3（旧「单张网络图」）与 type 0（动态背景）统一按「网络 = 自动切换」处理
            //   旧用户升级后：选中「网络」，由 DynamicBackground 接管轮播
            this.onlineRadio.setChecked(true);
            this.editBgPath.setEnabled(false);
            this.editBgUrl.setEnabled(false);
            this.selectBgPath.setEnabled(false);
            if (this.activity.launcherSetting.launcherBackground.type == 3) {
                this.activity.launcherSetting.launcherBackground.type = 0;
                this.activity.refreshDynamicBackground();
            }
        }
        this.editBgPath.setText((CharSequence)this.activity.launcherSetting.launcherBackground.path);
        this.editBgUrl.setText((CharSequence)this.activity.launcherSetting.launcherBackground.url);
        this.selectPanelColor.setOnClickListener((View.OnClickListener)this);
        // ★ 1.5.0：自定义面板色块 → 开调色板（★ 批 4：主题色块已退役）
        if (this.customPanelBlock != null) {
            this.customPanelBlock.setOnClickListener((View.OnClickListener)this);
        }
        this.fullscreenSwitch.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        if (this.showAccountModelSwitch != null) {
            this.showAccountModelSwitch.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        }
        if (this.transBgSwitch != null) {
            this.transBgSwitch.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        }
        this.defaultRadio.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.classicRadio.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        if (this.dayRadio != null) {
            this.dayRadio.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        }
        this.customRadio.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        this.onlineRadio.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        if (this.layoutOldRadio != null) {
            this.layoutOldRadio.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        }
        if (this.layoutNewRadio != null) {
            this.layoutNewRadio.setOnCheckedChangeListener((CompoundButton.OnCheckedChangeListener)this);
        }
        // ★ 1.5.0：动作下拉的监听**最后**注册（回填在前面已完成，避免一进页面就回调）
        this.registerAccountModelAnimListener();
        this.selectBgPath.setOnClickListener((View.OnClickListener)this);
        this.editBgPath.addTextChangedListener(new TextWatcher(){

            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void afterTextChanged(Editable editable) {
                ExteriorSettingUI.this.activity.launcherSetting.launcherBackground.path = ExteriorSettingUI.this.editBgPath.getText().toString();
                GsonUtils.saveLauncherSetting(ExteriorSettingUI.this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
                if (new File(ExteriorSettingUI.this.editBgPath.getText().toString()).exists() && ExteriorSettingUI.isImageFile(ExteriorSettingUI.this.editBgPath.getText().toString())) {
                    Bitmap bitmap = BitmapFactory.decodeFile((String)ExteriorSettingUI.this.editBgPath.getText().toString());
                    ExteriorSettingUI.this.activity.launcherLayout.setBackground((Drawable)new BitmapDrawable(bitmap));
                } else {
                    // ★ 1.5.0：占位图 → 经典（黑夜）那张
                    ExteriorSettingUI.this.activity.launcherLayout.setBackground(ExteriorSettingUI.this.context.getDrawable(R.drawable.ic_background_classic));
                }
            }
        });
        this.editBgUrl.addTextChangedListener(new TextWatcher(){

            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            public void afterTextChanged(Editable editable) {
                ExteriorSettingUI.this.activity.launcherSetting.launcherBackground.url = ExteriorSettingUI.this.editBgUrl.getText().toString();
                GsonUtils.saveLauncherSetting(ExteriorSettingUI.this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
                new Thread(() -> {
                    try {
                        URL url = new URL(ExteriorSettingUI.this.editBgUrl.getText().toString());
                        HttpURLConnection httpURLConnection = (HttpURLConnection)url.openConnection();
                        httpURLConnection.setDoInput(true);
                        httpURLConnection.connect();
                        InputStream inputStream = httpURLConnection.getInputStream();
                        Bitmap bitmap = BitmapFactory.decodeStream((InputStream)inputStream);
                        ExteriorSettingUI.this.handler.post(() -> ExteriorSettingUI.this.activity.launcherLayout.setBackground((Drawable)new BitmapDrawable(bitmap)));
                    }
                    catch (IOException e) {
                        // ★ 1.5.0：URL 拉取失败时的兜底 → 经典（黑夜）那张
                        ExteriorSettingUI.this.handler.post(() -> ExteriorSettingUI.this.activity.launcherLayout.setBackground(ExteriorSettingUI.this.context.getDrawable(R.drawable.ic_background_classic)));
                        e.printStackTrace();
                    }
                }).start();
            }
        });
        this.fullscreenSwitch.setChecked(this.activity.launcherSetting.fullscreen);
        if (this.showAccountModelSwitch != null) {
            this.showAccountModelSwitch.setChecked(this.activity.launcherSetting.showAccountModel);
        }
        // ★★★ 1.5.0：3D 人物动作 —— 回填选中项 + 按「显示人物模型」开关联动可用性。
        //   ★ 用户要求：开关**关闭时动作选择不可用**，开启后**可自由选择**。
        //   ★ 回填必须放在 setOnItemSelectedListener **之前**，否则一进页面就触发一次
        //     onItemSelected、把当前值又写回去（虽然值相同，但会多做一次无谓的磁盘写入）。
        //     ⇒ 所以这里只回填，监听在下面 registerAnimListener() 里注册。
        if (this.accountModelAnimSpinner != null) {
            int anim = this.activity.launcherSetting.accountModelAnim;
            // ★ 1.5.0：老 MinecraftSkinRenderer（连同 GameCharacter）已删除，
            //   这里只用字面量做范围校验（下拉仍是 0~5：待机/走/跑/转/挥手/点头）。
            if (anim < 0 || anim > 5) {
                anim = 0;
            }
            this.accountModelAnimSpinner.setSelection(anim);
        }
        this.applyAccountModelAnimEnabled(this.activity.launcherSetting.showAccountModel);
        // ★★★ 1.5.0：草方块开关移除后，「背景透明度」的同步必须独立出来 ——
        //   它原来被错误地塞在 grassUiSwitch 的分支里（草方块开关为 null 就永远不会同步），
        //   所以删草方块时必须把这段搬出来，否则这个开关会一直显示错的状态。
        if (this.transBgSwitch != null) {
            this.transBgSwitch.setChecked(this.activity.launcherSetting.transparentBackground);
        }
        // ★ 1.5.0：排版单选回填。用 useNewLayout()（老配置没存过这个键时算「新排版」），
        //   与 onCreate 里的初值口径一致；setChecked 与当前设置一致 → 不会触发保存/Toast。
        boolean layoutIsNew = this.activity.launcherSetting.useNewLayout();
        if (this.layoutNewRadio != null) {
            this.layoutNewRadio.setChecked(layoutIsNew);
        }
        if (this.layoutOldRadio != null) {
            this.layoutOldRadio.setChecked(!layoutIsNew);
        }
        this.refreshColorEditable();
        // ★ Phase 1.5 批 2：FCL 主题区接线（外观设置页 ui_setting_exterior.xml 新增组）
        this.setupFclThemeSection();
        if (Build.VERSION.SDK_INT < 28) {
            this.fullscreenSetting.setVisibility(8);
        }
    }

    // ===== ★ Phase 1.5 批 2：FCL ThemeEngine 主题区 =====
    // 形态照搬 FCL LauncherSettingPage 主题区（模式三选 + 主/次色×亮/暗 4 取色行）。
    // 语义映射（★ 批 4 收口）：themeMode 0=自动=FCL 原件跟随系统 / 1=强制亮 /
    // 2=强制暗，存 SP("launcher")。原批 2 的「现实时间」覆写语义已退役。
    private Spinner fclThemeModeSpinner;
    private LinearLayout[] fclColorBlocks;
    private View[] fclColorViews;
    private TextView[] fclColorTexts;

    private void setupFclThemeSection() {
        this.fclThemeModeSpinner = (Spinner) this.activity.findViewById(R.id.fcl_spinner_theme_mode);
        int[] blockIds = new int[]{R.id.fcl_select_color, R.id.fcl_select_color2, R.id.fcl_select_color_dark, R.id.fcl_select_color2_dark};
        int[] viewIds = new int[]{R.id.fcl_color_view, R.id.fcl_color2_view, R.id.fcl_color_dark_view, R.id.fcl_color2_dark_view};
        int[] textIds = new int[]{R.id.fcl_color_text, R.id.fcl_color2_text, R.id.fcl_color_dark_text, R.id.fcl_color2_dark_text};
        int[] resetIds = new int[]{R.id.fcl_reset_color, R.id.fcl_reset_color2, R.id.fcl_reset_color_dark, R.id.fcl_reset_color2_dark};
        this.fclColorBlocks = new LinearLayout[4];
        this.fclColorViews = new View[4];
        this.fclColorTexts = new TextView[4];
        for (int i = 0; i < 4; ++i) {
            this.fclColorBlocks[i] = (LinearLayout) this.activity.findViewById(blockIds[i]);
            this.fclColorViews[i] = this.activity.findViewById(viewIds[i]);
            this.fclColorTexts[i] = (TextView) this.activity.findViewById(textIds[i]);
            final int idx = i;
            this.fclColorBlocks[i].setOnClickListener(v -> this.showFclColorPicker(idx));
            ((TextView) this.activity.findViewById(resetIds[i])).setOnClickListener(v -> {
                // 重置=写回 ThemePreference 默认值（0xFF7797CF/0xFF000000/0xFF4E77C1/0xFFFFFFFF）
                this.applyAndSaveFclColor(idx, this.fclColorDefault(idx));
                this.refreshFclThemeBlocks();
            });
        }
        int themeMode = this.context.getSharedPreferences("launcher", Context.MODE_PRIVATE).getInt("themeMode", 0);
        this.fclThemeModeSpinner.setSelection(themeMode);
        this.fclThemeModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                ExteriorSettingUI.this.applyFclThemeMode(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        this.refreshFclThemeBlocks();
    }

    private void applyFclThemeMode(int themeMode) {
        this.context.getSharedPreferences("launcher", Context.MODE_PRIVATE).edit().putInt("themeMode", themeMode).apply();
        // ★ Phase 1.5 批 4：0 回归 FCL 原件语义 —— 跟随系统深色模式
        //   （系统级日落/日出定时切换，FCL/QCL 都自然跟着变）；
        //   原「现实时间钉亮暗」（applyRealTimeNightMode）已整体退役。
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                themeMode == 0 ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
                        : themeMode == 1 ? androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO
                                : androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        // 刷新已挂载的 FCL 控件取色（configChanges 含 uiMode，亮暗切换不重建 Activity）
        com.tungsten.fcllibrary.component.theme.ThemeEngine.getInstance().refreshTheme();
        // ★ 批 4：原「缝针」pushFclColorToQclTheme 已删 —— QCL 原生 UI 跟色
        //   统一由 ThemeEngine 刷新链尾钩子（ThemeRefreshUtils.ensureGlobalRecolorHook）
        //   完成；launcherTheme 存档不再被写（色源只有 ThemeEngine 一家）。
    }

    private int fclColorRaw(int idx) {
        com.tungsten.fcllibrary.component.theme.ThemeData t =
                com.tungsten.fcllibrary.component.theme.ThemeEngine.getInstance().getTheme();
        switch (idx) {
            case 0: return t._getColor();
            case 1: return t._getColor2();
            case 2: return t.getColorDark();
            default: return t.getColor2Dark();
        }
    }

    private int fclColorDefault(int idx) {
        switch (idx) {
            case 0: return 0xFF7797CF;
            case 1: return 0xFF000000;
            case 2: return 0xFF4E77C1;
            default: return 0xFFFFFFFF;
        }
    }

    private void applyFclColor(int idx, int color) {
        com.tungsten.fcllibrary.component.theme.ThemeEngine e =
                com.tungsten.fcllibrary.component.theme.ThemeEngine.getInstance();
        switch (idx) {
            case 0: e.applyColor(color); break;
            case 1: e.applyColor2(color); break;
            case 2: e.applyColorDark(color); break;
            default: e.applyColor2Dark(color); break;
        }
    }

    private void applyAndSaveFclColor(int idx, int color) {
        com.tungsten.fcllibrary.component.theme.ThemeEngine e =
                com.tungsten.fcllibrary.component.theme.ThemeEngine.getInstance();
        switch (idx) {
            case 0: e.applyAndSave(this.context, color); break;
            case 1: e.applyAndSave2(this.context, color); break;
            case 2: e.applyAndSaveDark(this.context, color); break;
            default: e.applyAndSave2Dark(this.context, color); break;
        }
        // ★ 批 4：原「缝针」pushFclColorToQclTheme 已删 —— applyAndSave* 内部
        //   会走 updateTheme→notifyThemeChanged→刷新链尾钩子，QCL 原生 UI 自动跟色
        //   （拖动即时预览同链路，比原来只在确认时推一次更跟手）。
    }

    private void showFclColorPicker(final int idx) {
        final int initColor = this.fclColorRaw(idx);
        new com.tungsten.fcllibrary.component.dialog.FCLColorPickerDialog(this.context, initColor,
                new com.tungsten.fcllibrary.component.dialog.FCLColorPickerDialog.Listener() {
                    @Override
                    public void onColorChanged(int newColor) {
                        // 拖动即时预览（不持久化）
                        ExteriorSettingUI.this.applyFclColor(idx, newColor);
                    }

                    @Override
                    public void onPositive(int destColor) {
                        ExteriorSettingUI.this.applyAndSaveFclColor(idx, destColor);
                        ExteriorSettingUI.this.refreshFclThemeBlocks();
                    }

                    @Override
                    public void onNegative(int color) {
                        // 取消=回退预览到初始色
                        ExteriorSettingUI.this.applyFclColor(idx, initColor);
                    }
                }).show();
    }

    private void refreshFclThemeBlocks() {
        for (int i = 0; i < 4; ++i) {
            int color = this.fclColorRaw(i);
            this.fclColorViews[i].setBackgroundColor(color);
            this.fclColorTexts[i].setText(String.format("#%06X", color & 0xFFFFFF));
        }
    }

    /**
     * ★ 1.5.0：草方块 UI 下线后，主题色 / 面板色**永远可编辑**。
     *   原来这里按 {@code uiTheme == 1} 把两块颜色选择器禁用并调成半透明；
     *   现在固定为「启用 + 不透明」。
     */
    private void refreshColorEditable() {
        // ★ 批 4：selectTheme/customThemeBlock/colorView/colorText 已退役，只剩面板色
        this.setEnabledRecursive((View)this.selectPanelColor, true);
        if (this.customPanelBlock != null) {
            this.setEnabledRecursive((View)this.customPanelBlock, true);
        }
        if (this.panelColorView != null) {
            this.panelColorView.setAlpha(1.0f);
        }
        if (this.panelColorText != null) {
            this.panelColorText.setAlpha(1.0f);
        }
    }

    private void setEnabledRecursive(View v, boolean enabled) {
        if (v == null) {
            return;
        }
        v.setEnabled(enabled);
        v.setClickable(enabled);
        if (v instanceof ViewGroup) {
            ViewGroup g = (ViewGroup)v;
            for (int i = 0; i < g.getChildCount(); ++i) {
                g.getChildAt(i).setEnabled(enabled);
            }
        }
    }

    @Override
    @SuppressLint(value={"UseCompatLoadingForDrawables"})
    public void onStart() {
        super.onStart();
        // ★ 批 2：重进页面时同步 FCL 主题区色块（可能已在实验室页/其他会话改过）
        if (this.fclColorBlocks != null) {
            this.refreshFclThemeBlocks();
        }
        CustomAnimationUtils.showViewFromLeft((View)this.exteriorSettingUI, this.activity, this.context, false);
        if (this.activity.isLoaded) {
            this.activity.uiManager.settingUI.startExteriorSettingUI.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_white));
        }
        // ★ 批 4：主题色预览（colorView/colorText）随 QCL 主题行一起退役，
        //   FCL 主题区的色块由 refreshFclThemeBlocks() 负责同步。
        int pc = ExteriorSettingUI.getPanelColor(this.context, this.activity.launcherSetting.panelColor);
        this.panelColorView.setBackgroundColor(pc);
        this.panelColorText.setText((CharSequence)(QclColors.format(pc)));
        // ★ 1.5.0：下拉框下面那个"面板色自定义色块"也要显示当前面板色
        this.applyPanelColorToBlock(pc);
    }

    @Override
    @SuppressLint(value={"UseCompatLoadingForDrawables"})
    public void onStop() {
        super.onStop();
        CustomAnimationUtils.hideViewToLeft((View)this.exteriorSettingUI, this.activity, this.context, false);
        if (this.activity.isLoaded) {
            this.activity.uiManager.settingUI.startExteriorSettingUI.setBackground(this.context.getResources().getDrawable(R.drawable.launcher_button_parent));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 4000 && data != null && resultCode == -1) {
            Uri uri = data.getData();
            this.editBgPath.setText((CharSequence)UriUtils.getRealPathFromUri_AboveApi19(this.context, uri));
            return;
        }
        // ★ 1.5.2：启动等待界面的 6 格图片（requestCode = WAIT_BG_BASE + 槽位下标）
        if (requestCode >= WAIT_BG_BASE && requestCode < WAIT_BG_BASE + WAIT_BG_SLOTS
                && resultCode == -1 && data != null) {
            int slot = requestCode - WAIT_BG_BASE;
            String path = data.getStringExtra("path");
            if (path == null && data.getData() != null) {
                path = UriUtils.getRealPathFromUri_AboveApi19(this.context, data.getData());
            }
            if (path != null && !path.isEmpty() && slot >= 0 && slot < WAIT_BG_SLOTS) {
                String[] paths = ensureWaitBgArray();
                paths[slot] = path;
                this.activity.launcherSetting.waitBgPaths = paths;
                GsonUtils.saveLauncherSetting(this.activity.launcherSetting,
                        AppManifest.SETTING_DIR + "/launcher_setting.json");
                refreshWaitBgSlots();
            }
        }
    }

    // ======================================================================
    // ★★★★★ 1.5.2 新增：**启动等待界面图片（6 格）**
    //   用户要求：外观里点自定义 → 展示 6 张图 → 点想换的那张单独换。
    //   自适应由布局保证（等分宽 + 固定 46dp 高 + centerCrop），这里只做降采样解码，
    //   保证**大图也不会把预览撑爆或 OOM**。
    // ======================================================================

    private static final int WAIT_BG_SLOTS = 6;
    private static final int WAIT_BG_BASE = 0x5B00;   // "WB"

    private final android.widget.ImageView[] waitBgSlots = new android.widget.ImageView[WAIT_BG_SLOTS];
    /** ★ 1.5.2：等待界面图片的「恢复默认」按钮。 */
    private TextView resetWaitBg;

    /** 点某一格 → 打开文件选择器只换这一张。 */
    private void pickWaitBg(int slot) {
        try {
            android.content.Intent intent = new android.content.Intent(
                    this.context, com.tungsten.filepicker.FileChooser.class);
            intent.putExtra("SELECTION_MODE", 0);              // 单选
            intent.putExtra("ALLOWED_FILE_EXTENSIONS", "png;jpg;jpeg;webp");
            intent.putExtra("INITIAL_DIRECTORY",
                    android.os.Environment.getExternalStorageDirectory().getAbsolutePath());
            this.activity.startActivityForResult(intent, WAIT_BG_BASE + slot);
        } catch (Throwable t) {
            Toast.makeText(this.context, "打开文件选择器失败：" + t.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    /** 保证 waitBgPaths 长度是 6。 */
    private String[] ensureWaitBgArray() {
        String[] old = this.activity.launcherSetting.waitBgPaths;
        if (old != null && old.length == WAIT_BG_SLOTS) {
            return old;
        }
        String[] arr = new String[WAIT_BG_SLOTS];
        if (old != null) {
            for (int i = 0; i < Math.min(old.length, WAIT_BG_SLOTS); i++) {
                arr[i] = old[i];
            }
        }
        this.activity.launcherSetting.waitBgPaths = arr;
        return arr;
    }

    /** 6 格预览：有自定义图就显示（**降采样**），没有就显示内置图。 */
    private void refreshWaitBgSlots() {
        String[] paths = ensureWaitBgArray();
        // ★ 1.5.2：同时把这 6 格同步给轮播引擎（DynamicBackground.customFrames），
        //   这样**启动等待界面**（LayoutPanel）用的就是你在这里选的图。
        //   ★ 固定构建 6 个元素（没选的用内置图填充），保证槽位与 6 格严格一一对应，
        //     不会因为只选了 2 张就变成 2 格循环。
        java.util.List<android.graphics.drawable.Drawable> frames = new java.util.ArrayList<>();
        for (int i = 0; i < WAIT_BG_SLOTS; i++) {
            android.widget.ImageView iv = this.waitBgSlots[i];
            try {
                String p = paths[i];
                android.graphics.Bitmap bmp = null;
                if (p != null && !p.isEmpty() && new java.io.File(p).isFile()) {
                    bmp = decodeSampled(p, 160);
                }
                if (bmp != null) {
                    if (iv != null) {
                        iv.setImageBitmap(bmp);
                    }
                    frames.add(new android.graphics.drawable.BitmapDrawable(
                            this.context.getResources(), bmp));
                } else {
                    if (iv != null) {
                        iv.setImageResource(
                                com.seashore.launcher.launcher.uis.main.DynamicBackground.RES_IDS[i]);
                    }
                    android.graphics.drawable.Drawable d =
                            androidx.core.content.ContextCompat.getDrawable(this.context,
                                    com.seashore.launcher.launcher.uis.main.DynamicBackground.RES_IDS[i]);
                    if (d != null) {
                        frames.add(d);
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        try {
            com.seashore.launcher.launcher.uis.main.DynamicBackground.setCustomFrames(frames);
        } catch (Throwable ignored) {
        }
    }

    /** 降采样解码（maxSide 是目标最长边像素；预览只有 46dp，160 足够）。 */
    private static android.graphics.Bitmap decodeSampled(String path, int maxSide) {
        try {
            android.graphics.BitmapFactory.Options o = new android.graphics.BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            android.graphics.BitmapFactory.decodeFile(path, o);
            if (o.outWidth <= 0 || o.outHeight <= 0) {
                return null;
            }
            int sample = 1;
            int longest = Math.max(o.outWidth, o.outHeight);
            while (longest / (sample * 2) >= maxSide) {
                sample *= 2;
            }
            android.graphics.BitmapFactory.Options d = new android.graphics.BitmapFactory.Options();
            d.inSampleSize = sample;
            d.inPreferredConfig = android.graphics.Bitmap.Config.RGB_565;
            return android.graphics.BitmapFactory.decodeFile(path, d);
        } catch (Throwable t) {
            return null;
        }
    }

    public static int getPanelColor(Context context, String color2) {
        if (color2 == null || color2.equals("DEFAULT") || color2.isEmpty()) {
            return context.getResources().getColor(R.color.qcl_panel_gray_alt);
        }
        try {
            return QclColors.parseSafe(color2, 0xFFFFFFFF);
        }
        catch (Throwable t) {
            return context.getResources().getColor(R.color.qcl_panel_gray_alt);
        }
    }

    public static void applyPanelTint(Context context, View root, int color2) {
        Drawable bg;
        if (root == null) {
            return;
        }
        if (root instanceof ViewGroup) {
            ViewGroup g = (ViewGroup)root;
            for (int i = 0; i < g.getChildCount(); ++i) {
                ExteriorSettingUI.applyPanelTint(context, g.getChildAt(i), color2);
            }
        }
        if ((bg = root.getBackground()) == null || bg.getConstantState() == null) {
            return;
        }
        for (int id2 : new int[]{R.drawable.launcher_view_white, R.drawable.launcher_view_light_gray}) {
            Drawable ref = context.getResources().getDrawable(id2);
            if (ref == null || ref.getConstantState() == null || !ref.getConstantState().equals(bg.getConstantState())) continue;
            bg.mutate().setTint(color2);
            break;
        }
    }

    // ★ Phase 1.5 批 4：getThemeColor / parseThemeColorSafe 已退役 ——
    //   原 17 处读色点统一改走 ThemeRefreshUtils.currentThemeColor(Context)
    //   （色源 = FCL ThemeEngine，自带亮暗档自适应与非法值兜底）。

    public static boolean isImageFile(String filePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile((String)filePath, (BitmapFactory.Options)options);
        return options.outWidth != -1;
    }

    // ★ Phase 1.5 批 4：setupThemeColorSpinner / applyThemeColorToViews 已退役 ——
    //   「QCL 主题」下拉行整体删除，主题色唯一入口 = FCL 主题组（setupFclThemeSection）。

    /** 面板色变化后，刷到下方那个自定义面板色块。 */
    private void applyPanelColorToBlock(int color) {
        if (this.panelColorBlockView != null) {
            this.panelColorBlockView.setBackgroundColor(color);
        }
        if (this.panelColorBlockText != null) {
            this.panelColorBlockText.setText(QclColors.format(color));
        }
    }

    public void onClick(View v) {
        ColorSelectorDialog dialog;
        // ★ Phase 1.5 批 4：customThemeBlock（自定义主题色块）整条分支已退役
        //   —— 主题色入口只剩 FCL 主题组；调色板即时预览走 ThemeEngine 刷新链。
        // ★★★★★ 2026-10-16 新增：**字体颜色自定义调节**
        //   （用户要求「在外观那里加一个字体颜色自定义调节功能」）。
        //
        //   点色块 → 打开调色板；选定后：
        //     ① 写入 launcherSetting.fontColor 并落盘；
        //     ② 立刻对当前界面应用一次（用户当场看到效果）；
        //     ③ 刷新本页的色块预览与十六进制文字。
        //   点「跟随主题」按钮 → 清空设置，回到日间黑字 / 夜间白字。
        if (v == this.selectFontColor) {
            dialog = new ColorSelectorDialog(this.context, true,
                    com.seashore.launcher.utils.ui.FontColorUtils.parse(this.activity.launcherSetting.fontColor));
            dialog.setColorSelectorDialogListener(new ColorSelectorDialog.ColorSelectorDialogListener() {

                @Override
                @SuppressLint(value={"SetTextI18n"})
                public void onColorSelected(int color2) {
                    ExteriorSettingUI.this.applyFontColor(color2);
                }

                @Override
                @SuppressLint(value={"SetTextI18n"})
                public void onPositive(int destColor) {
                    ExteriorSettingUI.this.applyFontColor(destColor);
                }

                @Override
                @SuppressLint(value={"SetTextI18n"})
                public void onNegative(int initColor) {
                    // 取消：把界面恢复成"当前已保存的设置"，不要留下半路预览
                    ExteriorSettingUI.this.applyFontColor(
                            com.seashore.launcher.utils.ui.FontColorUtils.parse(
                                    ExteriorSettingUI.this.activity.launcherSetting.fontColor));
                }
            });
            dialog.show();
        }
        // ★ 1.5.2：等待界面 6 格图片 → 恢复默认（全部清空，回内置图）
        if (v == this.resetWaitBg) {
            this.activity.launcherSetting.waitBgPaths = new String[WAIT_BG_SLOTS];
            GsonUtils.saveLauncherSetting(this.activity.launcherSetting,
                    AppManifest.SETTING_DIR + "/launcher_setting.json");
            refreshWaitBgSlots();
            Toast.makeText(this.context, "等待界面图片已恢复默认", Toast.LENGTH_SHORT).show();
        }
        // ★ Phase 1.5 批 4：resetThemeColor（重置主题色）分支已退役 ——
        //   主题色重置由 FCL 主题组每行的重置按钮（fclColorDefault）负责。
        if (v == this.resetFontColor) {
            this.activity.launcherSetting.fontColor = null;   // null = 跟随主题
            GsonUtils.saveLauncherSetting(this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
            // 重建当前界面即可恢复成主题默认文字色
            this.refreshFontColorUi();
            try {
                this.activity.recreate();
            } catch (Throwable ignored) {
            }
        }
        // ★ 1.5.0：面板色的两个入口都走这里 —— 右侧小按钮 + 下拉框下方的第二个自定义色块。
        if (v == this.resetPanelColor) {
            // ★★★★★ 1.5.2 新增：**重置面板（背景板）色**。
            //   用户要求：「背景板颜色你也没加重置按钮」。
            //   做法：panelColor 置 null（= 跟随昼夜默认 qcl_panel_gray），
            //   然后立刻把默认色刷回预览块与整个界面（与手动选色走同一套 applyPanelTint）。
            try {
                this.activity.launcherSetting.panelColor = null;
                GsonUtils.saveLauncherSetting(this.activity.launcherSetting,
                        AppManifest.SETTING_DIR + "/launcher_setting.json");
                int def = ExteriorSettingUI.getPanelColor(this.context, null);
                if (this.panelColorView != null) {
                    this.panelColorView.setBackgroundColor(def);
                }
                if (this.panelColorText != null) {
                    this.panelColorText.setText(QclColors.format(def));
                }
                this.applyPanelColorToBlock(def);
                ExteriorSettingUI.applyPanelTint(this.context, this.exteriorSettingUI, def);
                ExteriorSettingUI.applyPanelTint(this.context,
                        this.activity.getWindow().getDecorView(), def);
                Toast.makeText(this.context, "面板色已恢复默认", Toast.LENGTH_SHORT).show();
            } catch (Throwable ignored) {
            }
        }
        if (v == this.selectPanelColor || v == this.customPanelBlock) {
            dialog = new ColorSelectorDialog(this.context, true, ExteriorSettingUI.getPanelColor(this.context, this.activity.launcherSetting.panelColor));
            dialog.setColorSelectorDialogListener(new ColorSelectorDialog.ColorSelectorDialogListener(){

                @Override
                @SuppressLint(value={"SetTextI18n"})
                public void onColorSelected(int color2) {
                    ExteriorSettingUI.this.panelColorView.setBackgroundColor(color2);
                    ExteriorSettingUI.this.panelColorText.setText((CharSequence)(QclColors.format(color2)));
                    ExteriorSettingUI.this.applyPanelColorToBlock(color2);
                }

                @Override
                @SuppressLint(value={"SetTextI18n"})
                public void onPositive(int destColor) {
                    ExteriorSettingUI.this.activity.launcherSetting.panelColor = QclColors.format(destColor);
                    GsonUtils.saveLauncherSetting(ExteriorSettingUI.this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
                    ExteriorSettingUI.this.panelColorView.setBackgroundColor(destColor);
                    ExteriorSettingUI.this.panelColorText.setText((CharSequence)(QclColors.format(destColor)));
                    ExteriorSettingUI.this.applyPanelColorToBlock(destColor);
                    ExteriorSettingUI.applyPanelTint(ExteriorSettingUI.this.context, (View)ExteriorSettingUI.this.exteriorSettingUI, destColor);
                    ExteriorSettingUI.applyPanelTint(ExteriorSettingUI.this.context, ExteriorSettingUI.this.activity.getWindow().getDecorView(), destColor);
                }

                @Override
                @SuppressLint(value={"SetTextI18n"})
                public void onNegative(int initColor) {
                    ExteriorSettingUI.this.panelColorView.setBackgroundColor(initColor);
                    ExteriorSettingUI.this.panelColorText.setText((CharSequence)(QclColors.format(initColor)));
                    ExteriorSettingUI.this.applyPanelColorToBlock(initColor);
                }
            });
            dialog.show();
        }
        if (v == this.selectBgPath) {
            Intent intent = new Intent(this.context, FileChooser.class);
            intent.putExtra("SELECTION_MODE", Constants.SELECTION_MODES.SINGLE_SELECTION.ordinal());
            intent.putExtra("ALLOWED_FILE_EXTENSIONS", "png;jpg");
            intent.putExtra("INITIAL_DIRECTORY", new File(Environment.getExternalStorageDirectory().getAbsolutePath()).getAbsolutePath());
            this.activity.startActivityForResult(intent, 4000);
        }
    }

    @SuppressLint(value={"UseCompatLoadingForDrawables"})
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        if (buttonView == this.fullscreenSwitch) {
            this.activity.launcherSetting.fullscreen = isChecked;
            GsonUtils.saveLauncherSetting(this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
            if (Build.VERSION.SDK_INT >= 28) {
                this.activity.getWindow().getAttributes().layoutInDisplayCutoutMode = isChecked ? 1 : 2;
            }
            this.activity.getWindow().setFlags(256, 256);
        }
        // ★ 1.5.0：草方块 UI 开关已移除（那套主题一堆 bug，用户要求下线）。
        //   原来这里会写 launcherSetting.uiTheme 并立刻 QclThemeUtils.apply(...)。
        // 1.3.7: 主界面是否显示账号人物（默认开）
        if (this.showAccountModelSwitch != null && buttonView == this.showAccountModelSwitch) {
            this.activity.launcherSetting.showAccountModel = isChecked;
            GsonUtils.saveLauncherSetting(this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
            // ★ 1.5.0：开关一变，动作选择行的可用性要跟着变（关=置灰不可选，开=可选）
            this.applyAccountModelAnimEnabled(isChecked);
        }
        // ★ 1.2.3：透明界面背景（默认开；关掉恢复原来的灰色面板）
        // ★★★★★ 2026-10-16 **修「夜间模式还有白色背景 → 白字看不见」**。
        //
        //   用户实测：「现在夜间模式有些背景还是全是白的，导致玩家根本看不见白色的字」。
        //
        //   ★ 根因：这里**硬编码**了 `#C8EDEDED`。而 `#C8EDEDED` 正是
        //     `values/colors.xml` 里 `qcl_panel_gray` 的**日间值** ——
        //     夜间本该由 `values-night/colors.xml` 翻成深色 `#E61A1F26`。
        //     但 `Color.parseColor` 是**运行时硬编码**，不走资源限定符 ⇒ 夜间也是近白底；
        //     而 `colorPureBlack`（946 个布局的正文色）在夜间已翻成 **#FFFFFF 白**
        //     ⇒ **白底 + 白字 = 完全看不见**。
        //     （`values/colors.xml` 的注释就写着这条铁律：
        //       「浅色态：面板 #C8EDEDED ↔ 文字 #000000；夜间态：面板→深色 ↔ 文字→白色；
        //         只翻一个就会出现『白底白字』→ 玩家根本看不见」。）
        //
        //   ★ 修法：改用资源 `R.color.qcl_panel_gray` ——
        //     `values/` 是 `#C8EDEDED`（日间浅灰，视觉与原来完全一致），
        //     `values-night/` 是 `#E61A1F26`（夜间深色），**自动昼夜翻转**。
        if (this.transBgSwitch != null && buttonView == this.transBgSwitch) {
            this.activity.launcherSetting.transparentBackground = isChecked;
            GsonUtils.saveLauncherSetting(this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
            View container = this.activity.findViewById(R.id.main_ui_container);
            if (container != null) {
                container.setBackgroundColor(isChecked
                        ? android.graphics.Color.TRANSPARENT
                        : this.context.getResources().getColor(R.color.qcl_panel_gray));
            }
        }
        // ★★★ 1.5.0：排版切换（旧排版 / 新排版）。
        //   ★ 只有「值真的变了」才写入 + 弹提示 —— 否则每次进外观页回填 setChecked
        //     都会触发一次 onCheckedChanged，一进页面就弹「重启后生效」，很烦。
        //   ★ 必须重启才生效：MainActivity.inflateMainUi() 只在初始化时 inflate **一套**
        //     （两套布局 id 相同，同时挂进视图树会 findViewById 串页），运行时换不了。
        if (buttonView == this.layoutNewRadio && isChecked) {
            this.layoutOldRadio.setChecked(false);
            if (!this.activity.launcherSetting.useNewLayout()) {
                this.activity.launcherSetting.uiStyle = 1;
                this.activity.launcherSetting.uiStyleChosen = true;
                GsonUtils.saveLauncherSetting(this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
                // ★★★★★ 2026-10-16 修「切排版必须玩家手动重启」：
                //   用户要求「切换旧排版和新排版必须重启，玩家必须手动重启，
                //   改成自动重启一下启动器」。
                //   原来只弹一句「重启后生效」，现在**保存后自动重建 Activity**，
                //   玩家不用自己动手（重建 = 重新 inflate 对应的那套布局）。
                this.restartLauncherForLayoutChange();
            }
        }
        if (buttonView == this.layoutOldRadio && isChecked) {
            this.layoutNewRadio.setChecked(false);
            if (this.activity.launcherSetting.useNewLayout()) {
                this.activity.launcherSetting.uiStyle = 0;
                this.activity.launcherSetting.uiStyleChosen = true;
                GsonUtils.saveLauncherSetting(this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
                this.restartLauncherForLayoutChange();
            }
        }
        if (buttonView == this.defaultRadio && isChecked) {
            this.classicRadio.setChecked(false);
            if (this.dayRadio != null) {
                this.dayRadio.setChecked(false);
            }
            this.customRadio.setChecked(false);
            this.onlineRadio.setChecked(false);
            this.editBgPath.setEnabled(false);
            this.editBgUrl.setEnabled(false);
            this.selectBgPath.setEnabled(false);
            // ★ 1.5.0：「默认」= 按现实时间自动切昼夜（type 1）
            this.activity.launcherSetting.launcherBackground.type = 1;
            this.activity.refreshDynamicBackground();
            com.seashore.launcher.launcher.uis.main.DynamicBackground
                    .applyAutoDayNight(this.activity, this.activity.launcherLayout);
        }
        if (buttonView == this.classicRadio && isChecked) {
            this.defaultRadio.setChecked(false);
            if (this.dayRadio != null) {
                this.dayRadio.setChecked(false);
            }
            this.customRadio.setChecked(false);
            this.onlineRadio.setChecked(false);
            this.editBgPath.setEnabled(false);
            this.editBgUrl.setEnabled(false);
            this.selectBgPath.setEnabled(false);
            // ★ 1.5.0：「黑夜」= 固定黑夜图（type 4）
            this.activity.launcherSetting.launcherBackground.type = 4;
            this.activity.refreshDynamicBackground();
            this.activity.launcherLayout.setBackground(this.context.getDrawable(R.drawable.ic_background_classic));
        }
        if (this.dayRadio != null && buttonView == this.dayRadio && isChecked) {
            this.defaultRadio.setChecked(false);
            this.classicRadio.setChecked(false);
            this.customRadio.setChecked(false);
            this.onlineRadio.setChecked(false);
            this.editBgPath.setEnabled(false);
            this.editBgUrl.setEnabled(false);
            this.selectBgPath.setEnabled(false);
            // ★ 1.5.0：「白天」= 固定白天图（type 5）
            this.activity.launcherSetting.launcherBackground.type = 5;
            this.activity.refreshDynamicBackground();
            this.activity.launcherLayout.setBackground(this.context.getDrawable(R.drawable.qcl_bg_day));
        }
        if (buttonView == this.customRadio && isChecked) {
            this.defaultRadio.setChecked(false);
            this.classicRadio.setChecked(false);
            if (this.dayRadio != null) {
                this.dayRadio.setChecked(false);
            }
            this.onlineRadio.setChecked(false);
            this.editBgPath.setEnabled(true);
            this.editBgUrl.setEnabled(false);
            this.selectBgPath.setEnabled(true);
            this.activity.launcherSetting.launcherBackground.type = 2;
            this.activity.refreshDynamicBackground();
            this.activity.launcherSetting.launcherBackground.path = this.editBgPath.getText().toString();
            if (new File(this.editBgPath.getText().toString()).exists() && ExteriorSettingUI.isImageFile(this.editBgPath.getText().toString())) {
                Bitmap bitmap = BitmapFactory.decodeFile((String)this.editBgPath.getText().toString());
                this.activity.launcherLayout.setBackground((Drawable)new BitmapDrawable(bitmap));
            } else {
                // ★ 1.5.0：点「自定义」但路径还无效时的占位 → 经典（黑夜）那张
                this.activity.launcherLayout.setBackground(this.context.getDrawable(R.drawable.ic_background_classic));
            }
        }
        if (buttonView == this.onlineRadio && isChecked) {
            this.defaultRadio.setChecked(false);
            this.classicRadio.setChecked(false);
            if (this.dayRadio != null) {
                this.dayRadio.setChecked(false);
            }
            this.customRadio.setChecked(false);
            this.editBgPath.setEnabled(false);
            // ★★★★★ 1.5.2：启用 URL —— 「网络」模式终于能换玩家自己的图了。
            //
            //   ★ 「网络」的真实含义（读 DynamicBackground 源码确认）：
            //     轮播内置的 6 张图（qcl_bg_1~6），每 10 秒一张；
            //     并用**网络时间**（HTTP 响应头 Date）判断昼夜 —— 这才是"网络"一词的由来，
            //     它**不是**"从网络拉图"。所以原来 URL 框被禁用后，玩家根本没法换图。
            //
            //   ★ 只改背景绘制，**不碰** dismiss / startActivity / 启动检查任何流程。
            this.editBgUrl.setEnabled(false);
            this.selectBgPath.setEnabled(false);
            this.activity.launcherSetting.launcherBackground.type = 0;
            // 把当前输入的 URL 记下来（多行/逗号分隔都支持），再重启轮播
            this.activity.launcherSetting.launcherBackground.url =
                    this.editBgUrl.getText() == null ? null : this.editBgUrl.getText().toString();
            this.activity.refreshDynamicBackground();
        }
        GsonUtils.saveLauncherSetting(this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
    }

    /**
     * ★★★ 1.5.0：3D 人物动作下拉的可用性（用户：「关闭时选择不可用，开启后可自由选择」）。
     *
     * @param enabled 「显示人物模型」开关是否打开
     */
    private void applyAccountModelAnimEnabled(boolean enabled) {
        if (this.accountModelAnimSpinner != null) {
            this.accountModelAnimSpinner.setEnabled(enabled);
            // 置灰时用半透明表达「不可选」，比只 setEnabled 更直观
            this.accountModelAnimSpinner.setAlpha(enabled ? 1.0f : 0.4f);
        }
        if (this.accountModelAnimRow != null) {
            this.accountModelAnimRow.setAlpha(enabled ? 1.0f : 0.4f);
        }
    }

    /**
     * ★★★ 1.5.0：注册动作下拉的选中监听。
     *
     * <p>★ 必须**晚于** setSelection() 调用（回填在 onCreate 里做），
     * 否则一进页面就会回调一次 onItemSelected、白白写一次磁盘。
     */
    private void registerAccountModelAnimListener() {
        if (this.accountModelAnimSpinner == null) {
            return;
        }
        this.accountModelAnimSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                // ★ 1.5.0：主界面人物已换成 glTF 骨骼动画，选项是 4 个**模型内真实存在的 clip**
                //   （idle + idle_sub_1/2/3），不再是老的程序化模拟动作。
                //   这里的 position 就是 clip 序号 0~3，MainUI 会据此调 playAnimation()。
                if (position < 0 || position > 3) {
                    return;
                }
                // ★ 值没变就不写盘（进页面回填时可能触发一次）
                if (ExteriorSettingUI.this.activity.launcherSetting.accountModelAnim == position) {
                    return;
                }
                ExteriorSettingUI.this.activity.launcherSetting.accountModelAnim = position;
                GsonUtils.saveLauncherSetting(ExteriorSettingUI.this.activity.launcherSetting, AppManifest.SETTING_DIR + "/launcher_setting.json");
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    /**
     * ★★★★★ 2026-10-16 新增：切换排版后**自动重启启动器**。
     *
     * <p>用户要求：「切换那个旧排版和新排版必须要重启，玩家必须手动重启，
     * 改成自动重启一下启动器」。
     *
     * <p>★ 为什么必须重建：{@code MainActivity.inflateMainUi()} 只在初始化时
     * inflate **一套**布局（{@code ui_main.xml} 旧排版 / {@code ui_main_new.xml} 新排版）。
     * 两套布局的 id 集合完全相同，同时挂进视图树会让 {@code findViewById} 抓错页
     * ⇒ 运行时换不了，只能重建 Activity。
     *
     * <p>★ 实现要点：
     * <ul>
     *   <li>先用 Toast 告诉用户"正在重启"，否则界面突然消失会以为崩了；</li>
     *   <li>延迟 600ms 再重建，让 Toast 与开关动画先画完；</li>
     *   <li>在**主线程**调用 {@code recreate()}。</li>
     * </ul>
     */
    private void restartLauncherForLayoutChange() {
        try {
            Toast.makeText(this.context, "正在切换到新排版，启动器即将自动重启…",
                    Toast.LENGTH_SHORT).show();
        } catch (Throwable ignored) {
        }
        try {
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                    new Runnable() {
                        @Override
                        public void run() {
                            try {
                                ExteriorSettingUI.this.activity.recreate();
                            } catch (Throwable ignored) {
                            }
                        }
                    }, 600L);
        } catch (Throwable t) {
            // 兜底：拿不到 Handler 时立刻重建
            try {
                this.activity.recreate();
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * ★★★★★ 2026-10-16 新增：应用「自定义字体颜色」。
     *
     * <p>用户要求「在外观那里加一个字体颜色自定义调节功能」。
     *
     * @param color 选中的颜色（含 alpha）。写盘后**立刻**对当前界面生效，
     *              这样用户不用退出设置页就能看到效果。
     */
    private void applyFontColor(int color) {
        try {
            this.activity.launcherSetting.fontColor = QclColors.format(color);
            GsonUtils.saveLauncherSetting(this.activity.launcherSetting,
                    AppManifest.SETTING_DIR + "/launcher_setting.json");
            com.seashore.launcher.utils.ui.FontColorUtils.apply(this.activity, color);
            this.refreshFontColorUi();
        } catch (Throwable ignored) {
        }
    }

    /** 刷新字体颜色那一行的预览（色块颜色 + 十六进制文字）。 */
    private void refreshFontColorUi() {
        try {
            int color = com.seashore.launcher.utils.ui.FontColorUtils.parse(
                    this.activity.launcherSetting.fontColor);
            boolean followTheme = (color == 0);
            if (followTheme) {
                // 跟随主题：用当前主题的正文色做预览（日间黑 / 夜间白）
                color = this.context.getResources().getColor(R.color.colorPureBlack);
            }
            if (this.fontColorView != null) {
                android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
                gd.setShape(android.graphics.drawable.GradientDrawable.RECTANGLE);
                gd.setCornerRadius(4f);
                gd.setColor(color);
                gd.setStroke(2, 0x66FFFFFF);
                this.fontColorView.setBackground(gd);
            }
            if (this.fontColorText != null) {
                this.fontColorText.setText(followTheme
                        ? this.context.getString(R.string.exterior_setting_ui_font_color_reset)
                        : QclColors.format(color));
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * ★ 1.5.8：按屏幕尺寸降采样解码背景图（照 FCL 的「不整张大图进内存」）。
     *
     * <p>为什么必须做：手机相册的照片常见 4000x3000，整张解成 ARGB_8888 约 48 MB；
     * 低内存设备上极易 OOM，或 decodeFile 直接返回 null
     * （原代码随后 new BitmapDrawable(null) 会 NPE）。
     *
     * <p>算法：先只读边界（inJustDecodeBounds），按目标宽高算 2 的幂次 inSampleSize，
     * 再真正解码 —— 安卓官方推荐的省内存写法。
     *
     * @return 解码后的 Bitmap；失败返回 null（调用方负责兜底）
     */
    private static Bitmap decodeSampledBackground(String path) {
        try {
            android.graphics.BitmapFactory.Options probe =
                    new android.graphics.BitmapFactory.Options();
            probe.inJustDecodeBounds = true;
            android.graphics.BitmapFactory.decodeFile(path, probe);
            if (probe.outWidth <= 0 || probe.outHeight <= 0) {
                return null;
            }
            int targetW = Math.max(probe.outWidth / 2, 720);
            int targetH = Math.max(probe.outHeight / 2, 1280);
            int sample = 1;
            while ((probe.outWidth / sample) > targetW * 2
                    || (probe.outHeight / sample) > targetH * 2) {
                sample *= 2;
            }
            android.graphics.BitmapFactory.Options opts =
                    new android.graphics.BitmapFactory.Options();
            opts.inSampleSize = Math.max(1, sample);
            opts.inPreferredConfig = android.graphics.Bitmap.Config.ARGB_8888;
            return android.graphics.BitmapFactory.decodeFile(path, opts);
        } catch (Throwable t) {
            return null;
        }
    }

    /**
     * ★ 1.5.8：换背景前把旧的 BitmapDrawable 持有的位图回收。
     *
     * <p>BitmapDrawable 不会替我们 recycle 位图 —— 反复「选图 → 换背景」
     * 会让旧位图一直留在堆里（每张几十 MB）。
     *
     * <p>安全前提：只回收我们自己 new 出来的 BitmapDrawable
     * （getDrawable() 返回的 selector/vector 不是 BitmapDrawable，不会命中），
     * 且只在视图未挂载时回收，避免「正在绘制时被回收」崩溃。
     */
    private static void recycleOldBackground(android.view.View view) {
        try {
            if (view == null) {
                return;
            }
            android.graphics.drawable.Drawable old = view.getBackground();
            if (old instanceof BitmapDrawable) {
                Bitmap bmp = ((BitmapDrawable) old).getBitmap();
                if (bmp != null && !bmp.isRecycled() && !view.isAttachedToWindow()) {
                    bmp.recycle();
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
