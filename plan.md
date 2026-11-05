# Seashore-Launcher 施工计划

> 定位与决策依据见 [research/qcl-android-shell.md](research/qcl-android-shell.md)（§0-§16），本文件只记阶段、验收与挂账。

## Phase 0 · 调研（已完成，2026-10-31 收口）

**产出**：调研纪要 16 节 + legacyfix fork 实装轮（`legacyfix-attic`）。

关键结论索引：
- **路线**：C 案 = 继承 QCL + 减法特化 + 独立新项（§5）；开发台 = x86_64 AVD 主力、真机仅发版 sanity（§9）。
- **渲染线**：GL4ES/Krypton 现成够用，QCL 卡死的自研渲染器属被砍现代层（§10）；阴影三角机理锁定 = Tessellator 预三角化 × gl4es inColor 插值（§12-§13；**§35 修订：坏的在法线不在颜色**），修复实装为 fork `-Dlf.gl4esquad`（结构指纹，混淆名免疫），b1.7.3 真机命中（§14）。**§16 定调：Seashore 采映射表方案（表扩双家族），指纹降为兜底；桌面真 GL 无此病，补丁 Android 专属。**
- **渲染线再定调（2026-11-01，§34）**：自研要求降低 → **fork Krypton（NG-GL4ES）出"Seashore 渲染器"**，与 Krypton 并列、修复从 Krypton 出发；天空/classic 色块根因已定罪（`GL_C3F_V3F vert=4` 上游 typo，§32/§33，Krypton 原样带病待直移），阴影三角已闭环（§35：真凶=Java 预三角化零法线重复顶点，C 侧仅 FPE `normalize(0)` 放大，**fork 无此面可修**，落点=映射表/`-Dlf.gl4esquad`）。详见下"渲染器 Fork 线"。
- **legacyfix × Android**：agent 语义存活，但价值收敛为游戏侧补丁 + 网络代理，渲染层零触碰（§14）；QCL 表名式 GL4ES-Fix 域内完备、repak 属域外 miss（§16 口径修正），Seashore 表方案据此成立。
- **启动路径取证**：launchwrapper-1.6 无 blackboard → 钩 VanillaTweaker；QCL 自定义 AlphaVanillaTweaker 使第三方 agent 注入面静默死（"假注入成功"陷阱，§16）→ **拍板：Seashore 不走 launchwrapper，直调主类 + javaagent 直连，启动语义与补丁语义解耦（§16）**。
- **QCL 输入面缺陷**：键盘通道缺失（dexdump 实锤真缺），Seashore 立项需求（§15）。
- **闸门状态**：闸门一（legacyfix 上 Android 存活面）**已闭环**；闸门二（减法依赖图审计）**转入 Phase 2**。

## Phase 1 · 脚手架：搬 QCL 码 + 开发环境

**目标**：得到"可构建、可安装、可跑远古版本、日志链完整"的 Seashore 骨架，行为等价 QCL 1.5.9 现状（2026-11-01 上游同步，判读见卷宗 §36）。

1. **基座落库** ✅（2026-11-01）：以 `reference/Quanta-Craft-Launcher`（分支 1.5.9，`e49f4b3`；1.5.8/1.5.9 增量全为现代侧、远古面零触碰，§36）为源，工程主体迁入 Seashore 仓（6 模块 + tools/appletfix_src/third_party_sources，剔除 QCL 维护脚本/自更新清单/RELEASE_NOTES）；GPL-3.0 合规——保留版权头与 LICENSE，`reference/` 维持 gitignore 不入库。**GL4ES-Fix 表模块随移植保留**（§16 定调：Seashore 采映射表方案，表扩覆盖双家族=官方混淆+repak 名，应用时机改运行时翻 clinit，不再离线改 jar）。
2. **品牌剥离** ✅（2026-11-01，图标除外）：包名整体迁移 `com.qcl.launcher` → `com.seashore.launcher`（目录移动 + 555 文件替换零残留）、applicationId/FileProvider authority/外置存储根（`/QCL` → `/Seashore`）/三语名称串/UpdateChecker 与 FeedbackUI 指向/版本线（0.1.0）全部就位；内部标识（`qcl.cfg` 文件名、`-Dqcl.*` 属性键、`QCLApplication` 类名、`Theme.QCL.*`）不动；图标仍为 QCL 资产待自建（banner 已有）；UI 布局沿用 FCL 血脉不重写。
3. **开发环境** ✅（2026-11-01）：gradle wrapper 7.3.3 锁版本（distributionUrl 切腾讯镜像——services.gradle.org 直连超时实测）；JDK 17（Temurin 17.0.20.1，`e:\4k\tools\jdk17`——机器仅有 8/21/25，Gradle 7.3.3 上限 17）；build-tools 30.0.3 已从 dl.google 下架（404 实锤），改装 34.0.0 并给 6 模块钉 `buildToolsVersion`；`local.properties` 指 `D:\Android\Sdk`；ABI 走基座自带 `-Darch=x86_64`（abiFilters + assets 按架构裁剪一体）；AVD `emulator-5554` 主力测试机；调试日志路径对齐 Pojav log（`files/debug/pojav_latest_log.txt`）。**零 NDK 假设修正**：`PojavLauncher` 模块有无条件 `externalNativeBuild`（Android.mk，声明 r27c=27.3.13750724），构建机必须有 NDK——本机装的是 r30（xhook 的 `PAGE_SIZE/PAGE_MASK` 被新 Bionic 移除，**实测不兼容**）与 r27c（镜像 zip 名 r27c 实 revision=27.2.12479018），故 `PojavLauncher/build.gradle` 加 `-DndkVer` 覆盖口（默认值=上游，cherry-pick 零冲突），构建命令传 `'-DndkVer=27.2.12479018'`（PS5 含点参数须引号——裸参按点切分实锤）。**构建通过（2026-11-02）**：`Seashore-debug-0.1.0-x86_64.apk` 218MB，badging 验 `package=com.seashore.launcher / versionCode 1 / native-code x86_64` ✓。
4. **修复件挂载验证** ✅（2026-11-02，emulator-5554 实测）：骨架上以 `-javaagent:legacyfix` + 预设（`lf.iconnoop` / `lf.deawt.disable` / `lf.assetsDir`）跑 b1.7.3 与 inf-20100618——直连路径天然生效（无 LW）；阴影三角主路径走 Seashore 自家映射表（步骤 1），`lf.gl4esquad` 仅作兜底顺带验证真机命中（§16 顺延账；离线唯一性取证已闭环：两包全类扫描各恰好命中 1）。
   **实跑结果**：b1.7.3 阴影三角消失（`ss_gl4es` + `lf.gl4esquad` 兜底命中）；0.0.14a_08 天空无色块（fork C3F 修复，`dlopen libss_gl4es.so` 成功）；in-20100223 存读档桥正常（Save：命名框→选文件夹→**mclevel 正常落盘**；Load：文件选择器→读档成功）。工单③随本轮落地：`libss_gl4es.so`（arm64-v8a/x86_64）入 jniLibs + `RendererCompat.ALL` 首条目注册 + `defaultRendererId`/`InitializeSetting` 初始值/`resolveAuto` 三处一致改 `ss_gl4es`。

**设备验证判读（2026-11-02）**：
- **信箱桥=内部协议非品牌面**：品牌剥离时把 `QclFileBridge.DIR` 改成 `/Seashore` → 游戏侧预编译 jar（`qcl_saves_*.jar`，MAILBOX 硬编码 `/sdcard/QCL/.qcl_bridge`）双向失联 →「等待玩家选择超时」+ `qclFillSaves` NPE。已回退原值并注释锁死；两侧同迁需等游戏侧 jar 重建（挂账）。
- **CheckJNI 对 debuggable 强制开启**（`dalvik.vm.checkjni=false` 无效）：`System.loadLibrary("bytehook")` 触发 ART abort——上游 release 靠 Java 桥类缺失静默失败，debug 构建不行。`QCLApplication` 加 `Class.forName` 前置探测：类缺失即跳过加载=与 release 等价，类补齐自动恢复。
- **反编译 hashCode switch 不认识新条目**：`JREUtils.getGraphicsLibrary`/`loadGraphicsLibrary` 两处 switch 均无 `ss_gl4es` 分支，漏改会落 default=`libgl4es_114.so`（§31 sizeof 崩溃件）。已各加早退映射；env 别名同理——`ss_gl4es` 复用 `ng_gl4es` 的 Krypton env 分支 + `POJAV_RENDERER` 对齐，防 native 侧收到未知值。
- **UI 重写 qcl.cfg 会丢 `-Dlf.*` 旗标**（本轮实测）：设置界面保存 cfg 时不保留手工注入的 agent 旗标——映射表内化（步骤 4 主路径）前，预设旗标属易碎面。

**验收**：全新 clone → 一条命令构建安装 → 启动 b1.7.3 阴影三角消失、0618 可进游戏；升级/替换 jar 与 cfg 的路径文档化。**余项**：三版本设备验收已过（b1.7.3/0.0.14a_08/in-20100223，0618 归入阴影三角同窗口面）；**全新 clone 一条命令复验已过（2026-11-03）**：`build.ps1` 入库（JDK17 探测/SDK 探测+local.properties 自愈/NDK+build-tools 预检/含点参数引号封装/产物定位报告），clone 至 `E:\4k\tmp\ssclone` 一条 `.\build.ps1` 出 `Seashore-debug-0.1.0-x86_64.apk` 220.6MB（BUILD SUCCESSFUL 2m41s），APK 内 `lib/x86_64/libss_gl4es.so` 6.65MB=去品牌件 ✓，clone 工作区除脚本外零未预期缺件 ✓。脚本踩坑三则已固化：**PS5 无 BOM UTF-8 .ps1 按 GBK 解析**（中文串炸语法→存 UTF-8 BOM）、**`$home` 是只读自动变量**（函数参数改名）、**`java -version` 走 stderr**（`ErrorActionPreference=Stop` 下须临时降级）。剩：路径文档化待补。

**迁移挂账（2026-11-01 落库轮记）**：① `ResConfHack.jar` 需按 `tools/cacio_patch/README.md` ASM 流程重建——补丁类 L335 硬编码 `LevelFileChooserActivity` 旧包名，改名后 indev/infdev 存读档的内置文件浏览器回落系统选择器（catch 分支 fail-open，良性但需补）；② 图标/启动页仍 QCL 资产；③ `assets/announcement.json` 仍是 QCL 公告内容（展示面待换）；④ donate UI 指向未动；⑤ release 签名缺 `qcl.keystore`（Seashore 自建 keystore 随发版轮）；⑥ `ModLoaderVersions` 的加载器元数据端点暂借 QCL 仓（数据通道非品牌，Phase 2 复核）。

## Phase 1.5 · UI 替代调研——FCL 新 UI（2026-11-03 调研落笔，未开工）

**关键纠偏**：FCL 的"新 UI"**不是 Compose**——全仓 viewBinding + XML + 自定义 View（Material 风），Kotlin 2.4.10/AGP 8.13.2/minSdk 26，新代码 Kotlin（协程+StateFlow+Room+ViewModel）与存量 Java 混存。"替代=上 Compose"的前提不成立，替代实质是**换一套更现代的 XML/自定义 View 组件体系**。

**FCL 现状（2026-11）**：仓库=`FCL-Team/FoldCraftLauncher`（不存在 FCL-2/FCL），极活跃（1.3.4.4 发于 11-02，发版节奏几天一版）；许可=纯 GPL-3.0 标准全文（无 Section 7 附加条款）；内嵌 Terracotta=AGPL-3.0（我仓已含同模块，义务面相同）；后端=Amethyst-Android（Pojav 主仓 2025-09 已归档）+ gl4es fork（NG-GL4ES/Krypton，**与我 ss_gl4es 同源**）+ lwjglx + SFPEW——与 QCL 同一技术血统，架构对话成本低。

**血脉**：QCL 现 UI 本就借旧版 FCL（卷宗 §1.1.1）——换新 UI 实质="上游重新同步"，冲突最小路径；公开"FCL UI 移植/换壳"先例未检索到（367 fork 多为二改打包）。

**耦合面**：包层=`com.tungsten.fcl`（页面，ViewModel 直接 import `fclcore.download.*`/`DownloadProviders`，MainActivity.kt 单文件 51KB）/ **`fcllibrary`（组件库 ~50+ 文件：FCLButton/FCLDialog/ThemeEngine/文件浏览器，基本可独立复用）** / `fclcore`（HMCL 移植的下载/版本/账户）/ `fclauncher`（启动/JNI）。页面层与 FCL 数据层强耦合，组件层可拆。

**我方基数**：QCL 模块 526 Java 文件 ~9.4 万行（含逻辑）、140 layout、Kotlin 仅 8 文件；PojavLauncher 模块 29 文件（唯一布局 `activity_pojav.xml`=游戏画面）。**UI 替换必须保留的边界**：PojavMinecraftActivity（Surface/AWT/渲染）、RendererCompat/InitializeSetting/JREUtils 渲染器链、QclFileBridge 信箱桥、FilePicker/Terracotta、TouchInjector 触控层。

**三路径评估**：
- **(a) 移植 FCL 新 UI 进 QCL 基座（推荐）**：两段走——①引 `fcllibrary` 组件层+ThemeEngine 换视觉语言（按钮/对话框/文件浏览器，改动局部、可逐屏替换）；②主页面（版本选择/设置/下载）按 FCL 新 UI 重绘，需适配层把 FCL 页面的 fclcore 类型依赖映射到我 LaunchVersion/GameLaunchSetting。量级：中大（数百文件），GPL 衍生义务已满足（整仓已 GPL）。
- **(b) 以 FCL 为壳、移植 Seashore 特性**：javaagent 注入与渲染器插件口都好挂，但"纯远古减法"与 FCL 全能定位相悖（QCL 经验：提上游必被拒），且要自维护 fork 追"几天一版"上游——量大且持续，**不推荐**。
- **(c) 仅借鉴设计语言自研重建**：零 GPL 纠缠，等于重写 140 layout，量大，**仅作 fallback**。

**风险**：①上游高频=漂移，移植须钉某一 tag 分批搬，不追主线；②商标（FCL 名/图标）不可带走，品牌面继续 Seashore；③minSdk 对齐（FCL=26 vs 我现值）；④ThemeEngine 与现有 QCL 样式资源冲突面；⑤远古实跑力是我方独有强项（FCL 无 legacyfix 机制），UI 替换不得动步骤 4 映射表内化路径。

**决策点（已拍板 2026-11-04）**：主路径=**(a)**；起步=**①组件层**；钉版=**FCL 1.3.4.4**（参考源 `E:\4k\reference\FCL-1.3.4.4`，tarball 解包非 git）。

### 首批执行计划（2026-11-04 开工，依赖面已逐文件核清）

**落点与包名**：全部搬入 `QCL/src/main/java/`，**包名保持 `com.tungsten.*` 不动**（与 `qcl.cfg` 同理=内部标识，cherry-pick 上游零冲突）；仅两类 import 重写：`com.tungsten.fcl.R`→`com.seashore.launcher.R`、`com.tungsten.fcl.databinding.X`→`com.seashore.launcher.databinding.X`（viewBinding 生成类随我模块包名走）。

**① 底座（依赖闭包，逐文件验证过）**：
- `fclcore/fakefx/**` 整树（~150 文件，实测零外部 `com.tungsten` import=纯叶子）；
- `fclcore/task`：Task/Schedulers/TaskExecutor/AsyncTaskExecutor/TaskListener/TaskEvent/CompletableFutureTask/TaskCompletableFuture（**跳过** FetchTask/FileDownloadTask/GetTask/DownloadException=拖 HttpRequest）；
- `fclcore/event`：Event/EventBus/EventManager（跳过具体事件类）；
- `fclcore/util`：Lang/Logging/InvocationDispatcher/ReflectionHelper/ToStringBuilder/SimpleMultimap/Pair + `function/Exceptional*` 7 件 + `io/IOUtils` + `platform/OperatingSystem`（纯 JDK）；
- `com/mio/util/ImageUtil.kt`（ThemeEngine 消费；loadInto 用 Glide）。

**② 主题层**：`fcllibrary/component/theme` 三件套（ThemeEngine/ThemeData/ThemePreference）。替身件：`com.tungsten.fcl.FCLApp`（静态 getAppContext/getActivity，QCLApplication 挂 ActivityLifecycleCallbacks 喂当前 Activity）+ `com.tungsten.fclauncher.utils.FCLPath`（stub，仅需 LT/DK_BACKGROUND_PATH→filesDir/background/lt.png|dk.png）。注意：ThemeEngine.isNightMode 读 SP("launcher").themeMode（QCL 现用 uiMode 键）——首批保持源码原样，接线留页面批。

**③ 控件批**：`component/view` 全部 21 件（含 color 子包 ColorPickerView/AlphaPatternDrawable）+ `component/dialog` 6 件 + `component/{FCLAdapter,ResultListener,FCLCheckBoxTreeAdapter,FCLCheckBoxTreeItem}` + `anim/DynamicIslandAnim` + `fcllibrary/util/{ConvertUtils,LocaleUtils}` + `ui/ProgressDialog`。**跳过**：FCLActivity/FCLFragment/ui 子包（页面框架，批 2）、browser 子包（文件浏览器，批 3）、crash 子包（QCL 已有 customactivityoncrash）、LogSharingUtils（拖 HttpRequest）。

**④ 资源批**：attrs.xml 的 16 个 styleable（FCLButton/FCLCheckBox/FCLCheckedTextView/FCLEditText/FCLImageButton/FCLConstraintLayout/FCLImageView/FCLLinearLayout/FCLNumberSeekBar/FCLPreciseSeekBar/FCLRadioButton/FCLSpinner/FCLTabLayout/FCLTextView/FCLView/ColorPickerView）；drawable：fcl_button、ic_baseline_add_24、ic_baseline_remove_24、ic_baseline_arrow_drop_down_24、bg_number_seekbar_track、background_light、background_dark + dialog 布局引用面；layout：item_spinner_dropdown + dialog_edit/dialog_color_picker/dialog_full_edit/dialog_full_image；style：FCLSpinnerPopupAnimation。资源名加 `fcl_` 前缀或直接同名视 QCL 冲突面定（QCL 现有资源先查再放）。

**⑤ 构建配置**（版本钉=Kotlin 1.8.22/AGP 7.2.2 兼容面，勿照抄 FCL 组合）：
- 根 build.gradle：classpath `org.jetbrains.kotlin:kotlin-serialization:1.8.22`；
- QCL/build.gradle：`buildFeatures { viewBinding true }`；依赖 `kotlinx-coroutines-android:1.7.1`、`androidx.datastore:datastore:1.0.0`、`kotlinx-serialization-json:1.5.2`、`com.github.bumptech.glide:glide:4.16.0`、`org.glavo:chardet:2.5.0`（IOUtils 的 charset 检测；Logging 只用其 DEFAULT_BUFFER_SIZE 常量）；**viewpager2 批 1 不加**（仅 FCLMultiPageUI 消费=批 2）。仓库走 aliyun central 镜像可解。

**验收**：`.\build.ps1` 出 APK；首批不动任何现有页面（纯增量入库）。**chardet 若镜像解析失败**→降级桩化 IOUtils 的 UniversalDetector 两方法。

### 试点批（2026-11-05，实验室页换装=FCL 组件层首消费）

**接线三处（终态）**：① `MainActivity.onCreate` 在 `setContentView` 前调 `ThemeEngine.setupThemeEngine(this)`（等价 FCL 的 FCLActivity 首行约定，必须前置——FCL 控件 inflate 时同步取色）；② `ui_lab.xml` 整页换 `FCLTextView`（auto_text_tint/use_theme_color）+ `FCLLinearLayout` + `FCLImageView`（图标 background→src + auto_src_tint），结构/ID 零变动；③ 页面底色**不接** FCL 主题背景——分层判读：页面底色归 QCL（`launcher_ui_background` 全透明透出 DynamicBackground 壁纸，卡片 `qcl_button_gray` 默认态本就透明、仅按压显淡白，此为 QCL 原始设计），组件取色归 ThemeEngine，各管一层；FCL 背景图留给批 2 的 FCLActivity 统一决策。曾试过卡片 tint=ltColor + 根背景接主题图，观感事故（ltColor 与浅蓝背景图撞色抹平层次、撤主题图后透出夜间壁纸发虚），已回退。

**验证（AVD 全过）**：主界面零回归；亮色=默认蓝灰、暗色（SP `launcher.themeMode=2`）=dk 背景+白字、**换主题色（橙色 theme.json）=整页卡片/副标题/图标即时变色**——组件层"一色表驱动一页"的价值实锤；卡片点击→对话框正常。

**教训两则（页面批适用）**：
- `LinearLayoutCompat` **不是** `LinearLayout` 子类（直接继承 ViewGroup）——QCL 反编译代码按 `LinearLayout` 强转 findViewById 处，换 FCL 容器即 ClassCastException（本轮装机首崩即此）。页面批移植时同类强转是主要回归面，改按 `View` 持有。
- ThemeEngine 的 DataStore 实际落盘 = **`filesDir/datastore/theme.json`**（androidx `dataStore` 委托的 produceFile=File(dataDir,"datastore/"+fileName)，**非** `data/datastore/` 也非 dataDir 根）；锁文件读后即删（瞬态，不可作为访问痕迹）；kotlinx JSON 解析失败静默回退默认值（catch SerializationException）。手工造主题文件验证时以此路径为准。

**观感判读**：默认主题色 `#7797CF` 蓝灰与 QCL 原灰白系接近，亮色下观感等价（=替换安全的旁证）；差异在换色/暗色路径才显现。真实闭环待**设置页接 ThemeEngine.applyAndSave**（现 QCL 设置不产 FCL 主题键）。

**余项**：FCLAlertDialog 试点（dialog_alert 已在库，找一处简单 AlertDialog.Builder 替换）；批 2（FCLActivity/FCLFragment 页面框架）/批 3（文件浏览器）照旧。

## Phase 2 · 减法特化（大概，需具体调研后细化）

**目标**：砍现代版本支持，收敛 Pre-Classic → b1.8/1.5.2，与 Attic 桌面仓同一非目标。

初判砍面（待审计证据化）：
- 渲染器选择层的现代项（**2026-11-01 拍板**：只留 Holy-GL4ES + Krypton + Seashore-Renderer 三条目、默认 fork；砍 Zink/VirGL/MobileGlues，见"渲染器 Fork 线"）；
- 现代版启动链：1.6+ 版本解析（新 version.json 格式/现代 libraries 规则）、Forge/Fabric/OptiFine 安装面、现代登录链（保留离线 + 远古正版校验，MS 登录是否留 = 调研点）；
- 数据层：现代版本表与下载源（Betacraft 端点为主）；
- 壳内杂项：boatLauncher 等未启用分支。

**前置调研（= 闸门二）**：
- [ ] 现代/远古在启动主链的**分支点依赖图**（静态 grep + 运行时验证双轨）；
- [ ] 每个子系统砍除面的证据清单（删了断什么、不删的维护成本）；
- [ ] 数据层 258 远古版表与现代表分离面；
- [ ] GPL 衍生声明在瘦身后的合规复核。

**纪律**：Phase 1 未验收不开刀；审计文档先行，砍除按 PR 小步走。

## 后续线（未排期，挂账）

- **账号线·MS 登录 client-id 已备案（2026-10-31，同日换发：旧 id `ae2f1dd0-…` 有问题作废）**：`77b8ff69-e48f-4a72-ae7d-f6fb8490b477`（Seashore 自建 Azure 应用注册）。性质 = 公开构件（Pojav/FCL/HMCL 同款各自内置，非机密）；预期形态 = 公共客户端 device-code / MSAL 流，**不配 client secret**。OAuth 授权 URL 里出现的就是它，后续实现时以本条为准，防错拿别家 id（FCL/Pojav 的 id 混用会踩 Azure 重定向白名单）。Phase 2 L34"MS 登录是否留"调研点就此备好前提件。
- 键盘输入通道立项（§15：捕获层 / 系统键分流 / IME 字符输入）。**Phase 3 愿望单**（非 Phase 2 特化主线；如需提前可在 2~3 间补 2.x）。
  - **lwjglx diff 改判（2026-11-05，以 Amethyst 为准）**：把 `reference/Amethyst-Android`（活体后端谱系，LGPLv3）+ `reference/PojavLauncher`（归档上游）+ `reference/lwjglx`（独立切片，README 自标已过时）与本项目对齐后实锤——LWJGL2 兼容层 = 预编译 `lwjgl-lwjglx.jar`，本项目落 `QCL/src/main/assets/app_runtime/lwjgl333|lwjgl341/jars/`。3.4.1 线 `org/lwjgl/input|opengl` 类清单与 Amethyst **逐条同尺寸**（Keyboard 11116B、Mouse 9493B、InfdevMouse 1000B、EventQueue 1716B…），整包 SHA1 差异只在 glfw 绑定/manifest/时间戳等外围条目。归档 Pojav 是 224KB 旧构建，本项目已对齐 Amethyst 线、不比基准旧/薄。
  - **结论**：键盘缺口**不在 lwjglx 兼容层**（`org.lwjgl.input.Keyboard` 完整实现 + `InfdevMouse` + `EventQueue` 均在包内，且与 Amethyst 同版）→ **换/升 lwjglx 无意义**。§15 的"真缺"缺在**派发链**：① 下端 native——GLFW/SDL 的 Android 按键事件是否喂进 `Keyboard` 的 `EventQueue`（`GLFWInputImplementation`/native glfw 回调）；② 上端 Activity——`PojavMinecraftActivity` 是否把系统 `onKeyDown/onKeyUp` 派发到 GLFW。工单据此**改查派发链，不动兼容层**。
- legacyfix fork 发版轮：diff 清点出清测试脚手架（`lf.direct` 已拆）、s3 文件级 handler、a1.1.1 版本门、`lf.qcl` 预设、§14 实现轮总账补记。
- **阴影三角补丁内化（`-Dlf.gl4esquad` 实现轮；落点 Phase 2 或并入 legacyfix fork 发版轮，2026-11-01 设计定稿）**：
  - **挂载**：`-javaagent:<数据目录>/legacyfix.jar -Dlf.gl4esquad`，与 `lf.iconnoop`/`lf.deawt.disable`/`lf.assetsDir` 同预设面；通道前提已验=Pojav `jre_launcher` 透传 JVM 参数 + QCL cacio-agent 先例（§4）、闸门一 agent 语义存活（§14）、直调主类绕 launchwrapper（§16，避 AlphaVanillaTweaker"假注入成功"陷阱）。
  - **机理**（GL4ES-Fix 三行语义 fork 化，MIT→GPL 方向合法）：premain 注册 `ClassFileTransformer` → 目标类名解析=**映射表主路**（键=版本家族名，值=Tessellator 混淆类名；**表扩双家族**=官方混淆名+repak 名，纠 QCL 表名式对 repak 翻车 §14/§16）+**结构指纹兜底**（`<clinit>` `PUTSTATIC <field>:Z`+类形匹配；离线唯一性已闭环=两包全类扫描各恰好命中 1）→ ASM 拦 `<clinit>` 在 `PUTSTATIC` 前插 `ICONST_0` → `convertQuadsToTriangles` 初值翻 false → 游戏发 GL_QUADS（§35）。
  - **版本门（数据驱动，对齐 Attic 戒律）**：仅坏窗口 `inf_20100611~b1.7.3` 生效；in-20100125 起 Tessellator 重写 C4UB、字段不存在，不挂。
  - **失败语义=fail-open**（纠 GL4ES-Fix 坑：premain 查表 miss 抛 RuntimeException → JVM `abort()` 无 hs_err，§12 首败复盘）：按 legacyfix `Patcher` 同族纪律（§4），miss 只记一行 Error、照常启动。
  - **两步走**：验证轮=直接挂上游 `GL4ESFix_v1.0-javaagent.jar` + `-Dgl4esfix.mcVersion=b1.7.x`（家族名键，勿传精确版本）在 Seashore 骨架复现 E2 痊愈；发版形态=内化进 `legacyfix-attic` fork（§11 单 fork 双平台，桌面/Android 同一构件）。
  - **边界**：与渲染器无关（§35 数据面），归主仓/legacyfix fork，**不进 Seashore-Renderer 工单**；渲染器选择坑（§32）属壳层 UX，与本补丁正交。
- E5 窗口层对照、classic 旋转 resize 实测。
- **渲染器 Fork 线（已迁出独立仓，2026-11-01）**：工作位 = `e:\4k\github-projects\Seashore-Renderer`（remote `CreatorCSIE/Seashore-Renderer`，fork NG-GL4ES 0.4.5，submodule 齐），**计划整迁其 plan.md**（三面账/工单序/纪律/接口账以该文件为准），主仓不再挂渲染器侧工单。根因结论留在卷宗 §31-§35：C3F 色块 typo=独立仓直移；阴影三角=**主仓映射表**（`-Dlf.gl4esquad`，§16/§35——游戏侧数据面，主仓仍管此面，与渲染器无关）；崩溃 sizeof=上游已修白捡。主仓余下接口 = Phase 1 jniLibs 内置 + 渲染器条目注册 + **默认=fork（Seashore-Renderer，2026-11-01 拍板）**（规避 §32 切版本回落坑）。渲染器选项收敛为三：**Holy-GL4ES + Krypton + Seashore-Renderer（默认）**，Zink/VirGL/MobileGlues 随 Phase 2 砍除。VirGL/Zink"mesa 在场未接管"悬案留卷宗挂账。
