# Seashore-Launcher — QCL/Android 壳调研纪要

> 2026-10-28 自 Attic-Minecraft-Launcher 会话探讨落档（plan.md L339「Android 第三壳调研」条的展开，该条本体尚未改写，等崩溃界面落地闸门）。
> 取证面：QCL 源码稀疏检出（分支 1.5.7）于 `e:\4k\github-projects\Attic-Minecraft-Launcher\reference\Quanta-Craft-Launcher`（blob:none + sparse，只含源码目录，JRE/so 资产未拉）；legacyfix 源码于同项目 `reference\legacyfix`（v3 @ 6ed9e82）。

## 0. 结论一句话

**C 案定方向**：继承 QCL 基座 + 减法特化（砍现代版本支持，同桌面端非目标）+ 独立新项目（即 Seashore-Launcher，与 Attic 桌面仓分仓）；UI 布局沿用 FCL 血脉（GPL-3.0 合法，可微调）；品牌身份（名/图标）自建。

## 1. QCL 画像（Quanta-Craft-Launcher，ALLEN201123/Quanta-Craft-Launcher）

- Android MC Java 版启动器，GPL-3.0，维护者 Rod123456（bilibili UID 550905358），单人活跃开发（1.5.x 系）。
- 定位**全能**（现代版至 26.x/Java 25 + 远古特化）；1.1.1 起启动/渲染/输入/运行时链路参照 FCL 自研（UI 亦借 FCL 微调）；1.1.0 前基于 HMCL-PE（已移除）。
- 远古面：唯一支持下载全部 258 枚考古归档版本、唯一全远古中文补丁（逐版 FontRenderer 混淆名表）。
- 执行栈：Pojav 后端 + SDL3 原生窗口 + CaciCavallo（AWT→Android 原生窗口后端，jre8/jre17 两套）+ 捆绑 OpenJDK 移植（jre8/17/21/25，`app_runtime/java/`，与 FCL 同目录构件逐字节一致）+ lwjglx（Pojav 定制 LWJGL2：`GLFWInputImplementation`，源码级 stub `Display.setParent`）。
- 渲染器：全部第三方（默认 Krypton；Holy-GL4ES/Zink/VirGL/MobileGlues），自研渲染器进行中未发布——**为现代版需求（GL 3.x/4.6、光影）服务**。
- plan.md L339 旧括号「libjpe/Redcore 路线」= 讹误，公开检索零命中（GitHub API `redcore+jvm` 0 条、`libjpe` 仅 libjpeg 系模糊）；实际管线词 = Pojav 系捆绑 OpenJDK + SDL3/cacio + 第三方 GL 转译。libcore/ART 路线不可行（无 AWT，跑不了桌面 MC）。

## 2. 数据层取证（"通道复用"实锤）

- `QCL/src/main/assets/legacy_version_archive.json`：自述 = Betacraft 档案快照（files.betacraft.uk，2026-09-13 抓取），258 条，与 Attic 桌面端同源同端点。字段：`id/otherName/releaseTime/compileTime/sortTime/category/officialId/infoUrl/jarUrl`。
  - 可借鉴：**`otherName` 别名映射**（pc-132011=rd-132211，Attic 版本表无此层）；**`sortTime=compileTime`**（与 Attic agentMount"编译日期升序"同一课）。
- `QCL/src/main/assets/legacy_launch_template.json`：无官方元数据版本的骨 JSON（镜像 b1.7.3：launchwrapper 主类 + LWJGL 2.9.0 + pre-1.6 assetIndex + jre-legacy），`__ID__/__TIME__/__TYPE__/__TWEAK__/__LWVER__/__LWSHA1__` 占位符由 `LegacyArchiveInstallTask` 填充。与 Attic"Mojang 重发布 JSON 回填库池"同思路、不同形态（模板 vs 数据驱动）。
- 启动语义分岔：**QCL 完全不用 legacyfix**。applet 时代版本走 launchwrapper Tweaker（infdev/alpha→`AlphaVanillaTweaker`，主类 `net.minecraft.client.MinecraftApplet`；classic 的 `com.mojang.minecraft.MinecraftApplet` 只有 launchwrapper 1.6 才回退命中——`LegacyArchiveInstallTask.java` L418-476 注释有踩坑记录）。他们自解了 applet 容器/中文字体/FileDialog 同一套问题。
- 汉化资产（`tools/legacy_cn/`：`legacy_keys*.tsv`/`en2zh3.tsv`/`mkasset.py`/`extract_keys.py` + `assets/cn_*` 逐版包）：核心是**数据表**（逐版混淆名 + 翻译表），补丁发生在游戏 jar 字节码层、与宿主栈无关 → 可反哺桌面端（落点首选 = legacyfix fork 的 opt-in 补丁 `-Dlf.cn`，默认关）。landmine 他们已踩过（同名不同物、跳过名单必须带版本）——数据表形态进 Attic 数据驱动纪律无摩擦。工作量 = 258 版整套移植，进愿望池。

## 3. AWT 平台税与二层补丁镜像（cacio_patch）

- `tools/cacio_patch/README.md`：indev/infdev 存读档用 AWT `FileDialog`，cacio `CacioFileDialogPeer` 无 null 保护 → NPE 对话框建不起来。修法 = ASM 方法级补丁打进 `ResConfHack.jar`，靠 `-Xbootclasspath/p` 排 cacio-shared 之前覆盖。
  - 与 Attic `legacyfixPatch.ts` 同族的**二进制层二层补丁**，同样脆弱；判据 marker 文件迭代 V1→V4（教训"只看 jar 在不在 → 老安装一律跳过解压"= Attic rootPopulated 快照修复同族坑）。
  - 推论：**fork 补丁进源码、二层 patch 退役**的纪律在 Android 侧同样成立。
- `appletfix_src/qcl/MouseProbe.java`：lwjglx Mouse 内部状态反射探针（"验证完成后删除本类与触发点"）——诊断先行文化一致。
- 崩溃界面参照 `QCL/src/main/java/com/qcl/launcher/launcher/CrashReportActivity.java`：纯代码构建；左=完整日志（等宽 10sp、横+纵滚动、可选中、权重 1.6），右=标题+提示+琥珀色摘要（缺省回落日志首几行）+三钮（分享/退出/返回启动器）；深色 2B2B2B/1E1E1E 圆角 10。接口两 extra：`crash_text`/`crash_summary`——与 Attic 崩溃分类器 v0 产出形状天然对齐。

## 4. legacyfix × Android：不是"不行"，是三层分裂 + 实验门槛极低

- **失败语义兜底（取证）**：`reference/legacyfix/common/src/main/java/uk/betacraft/legacyfix/patch/Patcher.java` L53-86——逐补丁 try/catch，`PatchException` 只记一行 Error 继续，仅 `required=true` 才 `System.exit(-1)`；DeAwtPatch required=false。**最坏情况 = 全部补丁 skip + 日志，游戏照常启动**。
- **机制层可行**：QCL 已在用 `-javaagent`（`cacio-agent.jar`，jre17 套）；Pojav `jre_launcher.c` 透传 JVM 参数；javassist 纯 Java 零 natives。
- 三层分裂：
  1. **直接可用层**：GameArgs URL 重定向（12 条；c0.24 s3 死链问题 Android 同样存在，legacyfix 反能替他们解）、ProxyPatch 代理/皮肤、`-Dlf.*` 设置面、全部**游戏侧补丁**（目标是同一份游戏 jar 字节码，平台无关）。
  2. **冗余/冲突层**：DeAwtPatch + LWJGL 侧补丁——lwjglx 已源码级 stub `Display.setParent`，"强制游戏用 LWJGL 窗口"在 Android 即现状；javassist 锚点不匹配 → fail-open skip，无害但无用。
  3. **待实机验证层**：VisualizerPatch 等碰 AWT peer 的（cacio 能建窗，FileDialog NPE 说明 peer 细节有坑）。

## 5. C 案论证（为何"继承 QCL + 减法特化 + 独立新项"干净）

1. **rebase 矛盾消失**：QCL 定位全能，永不发纯远古构建，减法提上游必被拒——分叉是产品定义使然，非违纪。
2. **渲染器悬崖被砍没**：纯远古渲染需求只剩 GL 1.x/2.x 固定管线→GLES，GL4ES/Krypton 现成够用；QCL 卡死的自研渲染器恰属被砍的现代层。
3. **减法面有界（实测）**：`download/` 11 个加载器子包中现代专属（fabric/quilt/neoforge/forge/optifine/liteloader/modloader/cleanroom/babric）约 34 文件 ≈ launcher 包（354 文件）10%；再砍 jre17/21/25、lwjgl341/333 资产、现代渲染器集。远古链在 QCL 本就是分模块（`LegacyArchiveInstallTask`/`LegacyVersionArchive`/`UnlistedVersions`），非主干截肢。Pojav 核心 + cacio + jre8 + lwjglx 正是远古所需，全保留。

## 6. 硬成本（记账）

- **Android 平台税全自持**：JRE 资产更新、NDK/so、Android target API 逐年抬、商店合规——solo 第三壳，重于 Electron/Tauri 一个量级。
- **纪律 = cherry-pick 非 rebase**：新仓保留 QCL pull-only remote，按模块簇摘取（远古模块/cn_* 汉化/cacio patch 系仍在活跃演进），不整体同步。
- **GPL 义务**：QCL→FCL→Pojav→cacio 全 GPL-3.0 血脉，代码/UI 布局继承零风险（LICENSE + THIRD_PARTY_NOTICES 原样带走）；**名字/图标=商标面**，Seashore 新身份自建。

## 7. 立项前两闸门

1. **闸门一（-javaagent 晒状态表）**：`-javaagent:legacyfix.jar` 塞进 QCL/FCL 启动参数，跑 b1.7.3/infdev，读 `Patches:` 状态表——验证修复语义在 Android 的存活面（上轮"无人走过的路"修正为：路在，只是没人晒过状态表）。执行平台 = x86_64 AVD（见 §9）。
2. **闸门二（减法可行性审计）**：确认现代/远古在启动主链的分支点（`LegacyArchiveInstallTask` 与 FCL 主干耦合深度），把"砍 10%"从文件数证据升级为依赖图证据。

## 8. 与 Attic 桌面仓的通道关系

- 跨仓复用 = **数据/通道**（非代码非 UI）：Betacraft 端点 + 版本表 + agentMount 决策表 + legacyfix fork 补丁矩阵 + c0.24 死链声音物化技巧（`resources/` 预置，平台无关）。
- Attic 侧待办联动：plan.md L339 重写为 C 案 + 两闸门 + cherry-pick 纪律 + 「libjpe/Redcore」括号修正——**仍等启动器层崩溃界面落地闸门**，与 legacyfix fork 定稿（`Attic…/_scratch/legacyfix-fork.draft.md`）同批入稿。

## 9. 开发/测试平台决策（2026-10-29 拍板）

**x86_64 AVD = 主力开发台；真机降级为发版 sanity（optional，非前置门槛）。**

- **速度事实**：x86_64 AVD 走硬件虚拟化（近原生 CPU）；PC 上 ARM64 系统镜像无官方支持、纯软件转译不可用——模拟器阵营只有 x86_64 一个现实选项。
- **可观测性事实**（破除"实机不好抓测试"）：本项目调试面 = 日志+构件，不是断点；游戏跑在捆绑 JRE 的 fork 进程里本就无 JDWP 可打，模拟器在可观测性上零额外优势。真机取证全套：`adb logcat`（`Patches:` 表/stdout/stderr，可流式落盘）、`adb pull` + debuggable `run-as`（数据根/level.dat/崩溃 dump）、`screencap`/`screenrecord`（渲染结果）、WiFi adb 免插线。
- **实证流程**：不预设"模拟器图形路径不可信"——远古版渲染极轻（GL 1.x、854×480、无光影），`-gpu host` 跑 b1.7.3/infdev 可能直接能玩。先实测；AVD 塌了或发版前才上 ARM 真机（任意 arm64 旧手机/电视盒）。
- **构建面**：`ndk.abiFilters 'x86_64'` + 只捆 `bin-x86_64` JRE → 最小 APK。QCL 仓自带 `jniLibs/x86_64/`（21 so）与 `bin-x86_64.tar.xz`，fork 期不碰 C 源码 = **零 NDK**。
- **装机要求**：Android Studio + SDK + x86_64 system image ≈ 15-20GB；Windows 走 WHPX（Hyper-V 平台，老 HAXM 已弃用）；AVD 内存 4GB+；桌面侧 Gradle 需 JDK 17+。
- **产品 ABI 策略（留 plan.md 拍板，与测试平台解耦）**：arm64-v8a 为主、armeabi-v7a 视社区存量、x86/x86_64 best-effort 或不 ship（Pojav 系 x86 支持历来最弱且上游已停更）。

## 10. 渲染器线（2026-10-29 新立）：先修 GL4ES，自研作后续

**立场更新**：§初版"不自研渲染器"改为**修复优先、自研作后续**——等 QCL 自研（未发布、且其"不以 MobileGlues 为模仿对象"的立场拉长周期）不如自己动手：修 GL4ES 的过程即自研的最佳预演，修成则"自研第一件事"消失。

**QCL 表述的问题（README L76-82/L142-145 原文取证）**：远古版（Classic 等）天空/场景被大块红紫青色块覆盖；归因"gl4es/Zink 共用同一颜色处理链路，**首帧图形状态建立后被固化**（窗口/表面建立 + 首次着色器编译段）"；换任何第三方渲染器同结果、FCL 同；QCL 定性为"所有走第三方转译路线的天花板"。

**三点判读**：
1. **色块颜色=指纹**：红/紫/青=通道序类错误（RGB/BGR 互换、格式错位），非光照/雾类状态错。头号嫌疑=gl4es 首帧读取的 **EGLConfig/表面像素格式被烘焙进着色器链路**。此类 bug 属"现有 GL4ES 可修"，不属"必须重写渲染器"。
2. **窗口层嫌疑**："窗口/表面建立"正是 QCL 1.1.1 自研部分（窗口/Surface/EGL/帧节奏）。若其窗口层 EGLConfig 选择与 gl4es 格式假设不匹配，修复面在**启动器层表面创建参数**而非渲染器 C 代码——比修 gl4es 更便宜。
3. **"换渲染器同结果"待独立复核**：QCL 只点名 gl4es/Zink，未提 VirGL/OSMesa 软渲染（独立链路）。三渲染器对照实验直接缩小根因域：VirGL 正确→锁 gl4es/Zink 格式路径；VirGL 也错→根因在上游（游戏侧状态/窗口层），修复面完全不同。

**支配性论证**：修成→天花板论崩塌、上游反哺全生态（gl4es=MIT（立项核实），Krypton/Holy-GL4ES 各为独立 fork）、legacyfix fork 同款 PR 先行纪律；修不成→对"自研是否可行/坑在哪"有一手判断，不是白工。

**执行面**：本线=唯一碰 NDK 的线（编译 gl4es/Krypton），与壳线零 NDK 隔离；复现台=x86_64 AVD 合法（QCL 自述设备无关，非驱动怪癖，§9 真机=发版 sanity 仍成立）。

**第一步（复现+三组对照+取证）**：
- [ ] AVD 上同一份 Classic jar（c0.30 survival）分别走 Holy-GL4ES / Krypton / VirGL 启动，截图对照。
- [ ] gl4es verbose 日志 + 启动时 EGLConfig 属性 dump（`eglGetConfigAttrib` 面）。
- [ ] 判读：格式错位 vs 状态固化 vs 窗口层污染，三选一锁域后进修复设计。
