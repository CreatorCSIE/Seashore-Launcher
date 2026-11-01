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
  2. **冗余/冲突层**：DeAwtPatch + LWJGL 侧补丁——lwjglx 已源码级 stub `Display.setParent`，"强制游戏用 LWJGL 窗口"在 Android 即现状；javassist 锚点不匹配 → fail-open skip，无害但无用。（**E3 修正**：DeAwtPatch 锚点实际匹配、Applied 且致害——游戏被塞进左下角小窗，须显式 `-Dlf.deawt.disable` 才恢复，不是 fail-open，见 §14）
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

**实验矩阵（2026-10-31 重排，E1/E2 已完成见 §12）**：
- [x] E1 复现：QCL b1.7.3 在 AVD 上复现手臂/持物伪影（Krypton + Holy-GL4ES 双渲染器对照）。
- [x] E2 修复验证：GL4ES-Fix javaagent 版实证痊愈（详见 §12）。
- [x] E3 闸门一：legacyfix 上游 26.10 `-javaagent` 上 QCL b1.7.3 两轮实测，判读见 §14。**阴影三角不修**（legacyfix 不碰渲染层）。fork dev-20261030 轮推迟——与上游同码（渲染面 0 命中），等 `-Dlf.gl4esquad` 落地后合并为一次验证。
- [x] E4 classic 色块：c0.0.23a_01 × gl4es 系两列（Krypton/Holy-GL4ES）**同现象**，判读见 §13。Zink/VirGL 两列环境性不可用（缺 Vulkan/virtio-gpu，见 §12 附注），软渲染列留待真机或 `-qemu -enable-vulkan` 重试。
- [ ] E5 非 FCL 血脉窗口层对照（排除 QCL 自研窗口层嫌疑）。
- [ ] gl4es verbose 日志 + 启动时 EGLConfig 属性 dump（`eglGetConfigAttrib` 面）。
- [ ] 判读：格式错位 vs 状态固化 vs 窗口层污染，三选一锁域后进修复设计。

## 11. legacyfix 单 fork 双平台决策（2026-10-30 拍板）

- **不分第二个 fork**：`CreatorCSIE/legacyfix-attic` 即桌面+Android 双平台特化宿主（那边已 clone 并编出 `build/libs/legacyfix-dev-20261030.jar` + 三 addon dev 包）。
- **特化 = 运行时设置面，非构建期分叉**：平台差异全部收进 `-Dlf.*` 开关（如 `-Dlf.gl4esquad`、`-Dlf.cn`），同一构件两壳共用；避免"每平台一个 jar"的重蹈覆辙（对齐 Attic 数据驱动纪律）。
- **cacio FileDialog NPE 可源码化**：QCL 的 ASM+`-Xbootclasspath/p` hack（§3）本质是 legacyfix 同族补丁的部署形态变体——fork 侧可把它做成 opt-in 补丁进源码，二层 patch jar 退役（§3 推论的首个落点候选）。

## 12. 阴影三角闭环实证：机理 + E1/E2 结果 + 附加工单（2026-10-31）

**现象（E1 复现）**：QCL 1.5.7 / AVD(API 34 x86_64) / b1.7.3，第一人称裸臂出现对角黑色阴影三角（暗棕楔形+亮残条）；手持沙块时黑面更重。Krypton Wrapper 与 Holy-GL4ES **双渲染器同在**（日志属性绑定名差异证明切换生效）→ 定罪 gl4es 家族通病，Krypton fork 无罪。

**机理（全链条）**：
1. vanilla b1.7.3 `Tessellator` 自带静态 boolean（混淆字段 `b:Z`，即 TRIANGLE_MODE）=true → Java 侧**预三角化**，quad 拆成 6 顶点发 `GL_TRIANGLES`；
2. gl4es 的 `inColor` 属性处理在**重复顶点**上插值破裂 → 对角黑三角；（**2026-11-01 修订**：坏的不在颜色、在法线——真凶=Java 预三角化产生零法线重复顶点，C 侧放大=FPE `normalize(0)`，全链见 §35）
3. 桌面 GL 十五年无恙：驱动对退化插值的处理容错 + 面角亮度本就相近，误差不可见；
4. **修复 = 翻转该字段为 false** → 游戏发 4 顶点 `GL_QUADS`，gl4es 自己拆 → 干净。上游 GL4ES-Fix（itselix99，Babric mod）的 javaagent 分支即此一行翻转的无 mod 加载器版：ASM 拦截 `<clinit>` 中 `PUTSTATIC name="b" desc="Z"` 改推 `ICONST_0`。

**E2 实证（PASS）**：
- 首败复盘：加 `-javaagent:... -Dgl4esfix.mcVersion=b1.7.3` 后游戏进程 SIGABRT。根因=**agent 版本表键是版本家族名**（`b1.7.x`→混淆类 `nw`、`b1.6.x`→`ns`…），传精确版本 `b1.7.3` 查表 miss → premain 抛 RuntimeException → JVM 对 javaagent 失败直接 `abort()`（无 hs_err_pid）。**/sdcard FUSE 路径假设排除**——agent 已正常加载并跑到版本检查。
- 修正 `b1.7.x` + 渲染器切回 `ng_gl4es` 后：**裸臂痊愈 ✓ 持沙块痊愈 ✓**（截图双证）。
- **判定：`-Dlf.gl4esquad` 单点 fork 补丁立项成立**——持物与手臂同走 Tessellator 通道，无需拆两枚补丁；"持物更重"只是角间色差更大更显性，非第二根因。

**实验坑记录（复用价值）**：
- QCL 游戏 stdout 落盘 = `/sdcard/Android/data/com.qcl.launcher/files/debug/pojav_latest_log.txt`（含完整 JVM 参数表+env 表，取证首选）；
- 版本级配置 = `<版本目录>/qcl.cfg`（Gson，`\u003d` 转义），可 adb pull→改→push，**改前必 `am force-stop`** 防内存对象回写覆盖；
- GL4ES-Fix javaagent 版 premain 无成功日志（静默生效），以渲染结果为准。

**附加工单（E2 副产物）**：
1. **Seashore 硬化工单**：Zink/VirGL 在 AVD 上 SIGSEGV `egl_make_current+180`（libpojavexec.so，null display）= 缺 Vulkan/virtio-gpu 的环境问题——继承 Pojav 血脉即继承此坑，启动器侧必须把 EGL init 失败做成体面报错（对接 §3 CrashReportActivity 形态），不许 native 裸崩。
2. legacyfix fork 待办新增补丁：`-Dlf.gl4esquad`（Tessellator TRIANGLE_MODE 翻转，逐版混淆名表可直接抄 GL4ES-Fix 的 29 键家族表）。

## 13. E4 实证：classic 色块与阴影三角同族，修复面锁定 gl4es C 层（2026-10-31）

**样本**：c0.0.23a_01（AVD 实测可用最古版）× Krypton / Holy-GL4ES 双列，均无 agent 干净运行。

**现象**：两列**同现象**——天空半屏品红/半屏正常，对角分界+白边；生成界面青/淡黄/粉巨三角+黑色对角线（与 §12 手臂阴影三角同一指纹，被全屏级巨 quad 放大）；**草/土/沙/水/云纹理全部正常**。切换有效性由日志属性名差异背书（Krypton=`inEllipseOffset/inEllipseRadii` 自研日月补丁 shader；Holy=`coverage/geomSubset`——两家不同）。

**三点排除**（推翻/修正 §10 判读）：
1. **通道序/EGLConfig 像素格式假设证伪**：若是表面格式或 RGB/BGR 错位，纹理路径必一起坏；实际坏的只有**纯顶点色图元**（天空巨 quad、loading 渐变、手臂），纹理图元全好。
2. **"状态固化"（QCL 归因）不成立**：色块形态是逐图元的插值错误（对角分界=quad 拆分线），不是首帧全局状态。
3. **Krypton 专属 shader 层嫌疑排除**：Holy 同坏 → 定罪 **gl4es 共同祖先代码**（顶点色 `inColor` 插值/默认值处理），修一处全家族受益。

**E2 修法不可复用的实锤**：c0.0.23a_01 jar 体检 = 368 类、**无 Tessellator**（2009 年纯立即模式 `glBegin/glVertex`）——预三角化字段不存在，Java 层无翻转点。

**统一假设（修正版）**：gl4es 顶点色处理缺陷有两种显形——
- b1.7.3+（beta 起有 Tessellator）：预三角化 6 顶点流 → 小面积黑三角，**Java 层可绕**（`-Dlf.gl4esquad`，E2 已证）；
- classic（立即模式巨 quad）：gl4es 自己拆 quad 也炸 → **绕无可绕，修复面=gl4es C 层**（正好落在 §10"渲染器线=唯一碰 NDK 的线"射程内）。

**"天花板论"判定**：继续不成立——根因是 gl4es 家族一个具体缺陷，不是"所有第三方转译路线"的结构性极限。但 classic 面修复从"可能零成本 Java 补丁"上修为"C 层修复"，工作量重估进渲染器线立项书。

**下一步**：
- [x] gl4es 源码取证（Krypton 分支 NG-GL4ES 已核，2026-10-31）：**"inColor 插值 × 重复顶点"机理字面义证伪**，着色器路径为朴素逐顶点直通，分叉点收敛到游戏侧顶点流/颜色数组对齐——全量判读见 §17。
- [x] 动态取证第 3 级（`-Dlf.gl4estrace` 补丁 + AVD 双态对照，2026-10-31）：**"平行缓冲错位"假设证伪**——颜色数组两态均未启用、几何流两态同构；唯一差异 = 致病态 Java 端预三角化（mode=4, 6/面）vs 修复态 GL_QUADS 直通（mode=7, 4/面）——**注意：本节双态跑在 b1.7.3，取证对象是阴影三角线**（用户指正），全量判读见 §18。
- [x] classic 色块线第 3 级（c0.0.23a_01，2026-10-31）：游戏侧 21 处 glColor*/clearColor 调用点**静态枚举全清白**（无一处发品红/红紫青），且运行时 LWJGL 实为 lwjglx shim（立即模式方法全 native，Java 钩天花板已到）→ **色块实锤下游 gl4es 伪造**，下一手 = C 层 4b——全量见 §19。
- [ ] EGLConfig dump 降级为次要取证（格式假设已死，仅留窗口层嫌疑 E5）。
- [ ] 软渲染列（VirGL/OSMesa）对照留真机或 `-qemu -enable-vulkan` 后补。

## 14. E3 实证：legacyfix 上游 26.10 @ b1.7.3——agent 全链路可用，但渲染层零触碰（2026-10-31）

**实验面**：AVD b1.7.3 × ng_gl4es，纯 `-javaagent`（无 Tweaker），两轮。第一轮裸配，第二轮 `-Dlf.deawt.disable=true -Dlf.assetsDir=/storage/emulated/0/QCL/.minecraft/assets`。

**核心结论：阴影三角不修（实机阴性）**。legacyfix 全仓 grep `gl4es|quad|tessellat|inColor|shadow` = 0 命中——它的补丁面是窗口/网络/启动器/版本杂症，**根本不碰渲染层**。§12 `-Dlf.gl4esquad` fork 补丁仍是唯一路径，且 E3 证明它不能指望上游顺带解决，只能我们自己写。

**机制判读**：
1. **纯 `-javaagent` 全生效**：`premain` → `LaunchWrapperInjector.inject` 成功（"Injected LaunchWrapper transformer"），patch 经 LaunchWrapper transformer 链注入，与 Tweaker 路径语义等价。§4 担心的"只打日志不 patch"证伪。12 patch 全评估：11 Applied，`classic-resize` 被版本检测正确 Unapplicable。**（2026-10-31 修正：此结论仅在壳用默认 VanillaTweaker 时成立——b1.7.3 恰属此类；QCL 自定义 AlphaVanillaTweaker 的 0618 上钩子全miss、补丁静默死，见 §16。）**
2. **冲突层实锤**：`deawt`（"Forces the game to use LWJGL's windowing system"）把 applet 强转 LWJGL/DeAWT 独立窗口 → 游戏缩在左下角小窗；`-Dlf.deawt.disable=true` 后全屏缩放完全恢复。`mouse` 是其配套（"required for deAWT"），禁用 deawt 后无害但无意义。§4"fail-open skip"假设推翻（已在 §4 就地修正）。
3. **禁用机制**：`-Dlf.<id>.disable`（`Patch.shouldApply`：default-on 补丁 setting 存在即禁，值任意）；`game-dir`/`java-modules` required 不可禁。QCL 场景推荐最小集：`deawt`、`mouse` 禁用 + `assetsDir` 必配。
4. **proxy 层缺口**：`lf.assetsDir` 不配则 downloadAssets 写相对路径炸（第一轮 3 个 newsound 失败）；配了之后 proxy 自己的预下载安静，**但游戏运行时 newsound 直连死 s3 不被拦截** → 404 风暴 + 静音。皮肤路径确认被拦截（`SkinHandler.connect` 进栈，`getFixedSkin` NPE 另案）。`ResourceIndexHandler` 只服务 index（XML/TXT 生成），无文件级 serving handler → fork 待办：给 `s3.amazonaws.com/MinecraftResources/**` 加文件级 handler（从 assetsDir 供文件）。
5. **版本门记账**：`a1.1.1`（SeecretSaturdayPatch，目标 Alpha v1.1.1）在 b1.7.3 上错误 Applied，暂无副作用，fork 侧建议补版本门。

**三层分裂修订**：待验证层（渲染）确认**空**——legacyfix 在 QCL 上的价值收敛为：游戏侧补丁（indev-sound/texture-pack-folder/screenshot/game-dir）+ 网络代理（需补文件级 handler）；渲染修复零贡献，全靠 fork 新补丁。

**下一步**：
- [ ] fork 落地 `-Dlf.gl4esquad`（Tessellator 静态 boolean 翻转，E2/GL4ES-Fix 语义已证）→ 之后 fork dev 轮与上游轮合并验证。
- [ ] fork 补 s3 文件级 handler + a1.1.1 版本门 + QCL 场景默认禁 deawt/mouse（可做成 `lf.qcl` 预设）。

## 15. QCL 输入面缺陷：键盘通道缺失（2026-10-31 记录，Seashore 立项需求）

**缺陷**：QCL 只保留了触控输入通道，**不支持物理/蓝牙键盘信号输入**。平板接键盘（或蓝牙键盘）打不进游戏——聊天栏、F3、快捷键、ESC 全依赖此通道，legacy 版本在平板形态因此不可完整游玩。这是 QCL 壳的固有缺口，与 legacyfix/渲染层无关。

**Seashore 需求**：自建键盘输入链路，目标形态 = 平板外接键盘/蓝牙键盘即插即用。

**技术路径**（按输入消费者分两种状态）：
1. **deawt-off（QCL/Seashore 预设态，主路径）**：游戏读的是 Pojav 系 LWJGL2 `Keyboard` 实现，其事件队列由壳层喂入（`PojavAWT.dispatchEvent` 或等价机制）。Seashore 需在 Activity/SurfaceView 捕获 `onKeyDown/onKeyUp`，做 **Android keyCode → AWT VK/LWJGL scancode 映射**后注入该队列。
2. **deawt-on（Android 不推荐）**：游戏直接挂 AWT Canvas `KeyListener`，键盘事件走 AWT 分发——但 deawt 在 Android 引发缩窗（§14），不取。

**复用点**：Pojav 系虚拟触控按钮本质就是"按钮 → 模拟按键事件"，映射表已存在；物理键盘路径只需把捕获源从按钮换成 `KeyEvent`，**共用同一张 keyCode→scancode 表和同一个注入队列**，新增代码集中在捕获层与修饰键状态同步（Shift/Ctrl/Alt 的 Android modifier → AWT mask）。

**待办（Seashore 侧）**：
- [ ] 键盘捕获层：`dispatchKeyEvent` 拦截 → 区分游戏键/系统键（返回键、音量不进游戏队列）。
- [ ] 修饰键与 IME：物理键盘的字符输入（聊天栏）需走 `onKeyPreIme`/Text 输入路径，非纯 scancode。
- [ ] 与 QCL 预设联动：`lf.qcl` 预设（§14）默认 deawt/mouse 禁用后，输入面完全由壳层负责——键盘通道是预设成立的前置条件之一。

## 16. 0618 启动路径取证 + Seashore 架构决策：与 launchwrapper 解耦（2026-10-31）

**现象**：0618 × fork jar 两轮全空——gl4esquad 无 flipped、iconnoop 无效（setIcon NPE 仍在），agent 日志停在 "Injected LaunchWrapper transformer" 后再无任何 LF 痕迹。

**根因链**（三段取证）：
1. launchwrapper-1.6 的 `Launch` 类**没有 blackboard 字段**（javap 实锤）→ `LaunchWrapperInjector` 走 `patchOldLw` 分支：钩 `VanillaTweaker.injectIntoClassLoader` 注入 LFTweaker。
2. b1.7.3 json 无 tweakClass → LW 1.6 用 `DEFAULT_TWEAK=VanillaTweaker` → 钩子命中 → 补丁链活（§14"全生效"结论在 b1.7.3 成立）。
3. 0618 json 显式 `--tweakClass net.minecraft.launchwrapper.AlphaVanillaTweaker`（QCL 自定义类，绕开 VanillaTweaker）→ 钩子 miss → LFTweaker/LFTransformer 从未注册；而 `inject()` 照样返回 true → `premain` 提前 return → 直连 `inst.addTransformer` 也没注册 → **补丁全家静默死**。"假注入成功"陷阱。

**顺带纠错（点/斜杠"坑"实为误诊，已平反）**：曾判定 Gl4esQuadPatch 第 4 道门存在 `clas.getName()`（点分）vs 常量池类名（斜杠）比较错——**错**。javassist `ConstPool.getFieldrefClassName()` 内部经 `Utils.toClassName` 归一为**点分**，与 `CtClass.getName()` 同形；原代码一直是对的，误导来自 javap 注释显示斜杠形式。该"修复"反而破坏多包名匹配，已回退。0618 两轮不命中的唯一原因就是上面的 LW 钩子 miss（补丁根本没执行过）。在跑 jar 的 `net.minecraft.client.a.d` javap 核验 clinit：`iconst_1; putstatic b:Z`（自引用）四门全过；QCL 捆绑 GL4ES-Fix（表名式）确认没动 repak jar（版本目录有 `*.jar.orig` 原地改写备份——b1.7.3 的 `.orig` 证明表名式在官方混淆族上改写成功——但 repak 类未被覆盖）。**（2026-10-31 口径修正：TRIANGLE_MODE 型 flag 仅存在于表覆盖的版本集内，QCL 表在其保障域（官方混淆族 × 这些版本）已完备——repak miss 属"域外"而非"表残缺"。结构指纹的价值相应收敛为：家族免疫（Seashore 走 Betacraft 通道 = repak 家族，同版本同 flag 同病，表式必 miss）+ 免维护（新混淆源零表行）。）**

**指纹唯一性取证（整包全类扫描，2026-10-31）**：把四道门镜像成扫描器跑在两个真机 jar 全类集上——b1.7.3（官方混淆）：748 类、195 过 `(III)` 预筛、**恰好命中 1**（`nw`）；0618（repak 多包名）：341 类、84 预筛、**恰好命中 1**（`net.minecraft.client.a.d`）。误杀面另有一层结构性兜底：transformer 命中即 `applied=true`，**单会话至多翻一个类**。收紧候选实测：候选门"flag 被类内 GETSTATIC 读回"两真阳均满足（可作第五门）；候选门"(III) 方法体内读 flag"两真阳均**不**满足（不可用，会杀真阳）。

**方案定调修正（2026-10-31，用户拍板倾向）**：**映射表已实装于 fork**——`Gl4esQuadTablePatch`（`-Dlf.gl4esquadtable`，opt-in）：版本 id（`lf.version`，GameArgs 捕获 `--version` 或显式 `-Dlf.version=`）→ Tessellator 类名，确定性匹配零误杀；表命中但 clinit 模式不符时**拒绝瞎猜**（报错不动类，指纹补丁兜底）。**全表 30 组 1:1 移植自 GL4ES-Fix v1.0.1 javaagent**（`reference/GL4ES-Fix` 同作者其elix99 项目，QCL 内置表即其衍生）：官方混淆名 nw/ns/na/lj/kv/jy/jg/jf（b1.7→b1.0.x 通配）+ a 系 20 组精确/区间键 + infdev 三区间（末组 `inf_20100625-inf_20100611 → net.minecraft.client.a.d` 即 repak 名，与在机 0618 jar 实测一致——**作者表本就双家族**）；字段名全族统一 `"b"`（其 ASM visitor 硬编码 PUTSTATIC b:Z，与我们 nw.b/a.d.b 双实证吻合）。键匹配三态：精确→`.x` 通配→闭区间字典序（区间按特异性排序，infdev 窄区间先于 `a1.0.3-inf_20100630` 宽区间）；版本 id 归一 `-`→`_` 以吃 QCL 的 `inf-` 命名。域内完备事实确认后，**Seashore 采映射表方案**——封闭版本集下表是确定性匹配（零误杀）、随 QCL 移植免费获得、不绑 agent。成立条件：①表**扩覆盖双家族**（官方混淆 + BetaCraft repak 名，每版本按 jar 来源配行，封闭集手工可完成——QCL-0618 的 miss 是表行缺失非机制不行）；②应用时机从"离线改 jar（`.orig`）"改为运行时按表翻 clinit，不背哈希完整性债。**桌面侧澄清**：阴影三角是 gl4es 转译层特有的病（inColor 插值 × 重复顶点），桌面真 GL 下 `GL_QUADS` 原生正确、预三角化无害——**桌面 Attic 线无此补丁需求**，gl4esquad 是 Android 专属（opt-in 关闭即零痕迹，留在 fork 不碍桌面）。结构指纹的剩余定位：Seashore 表方案的兜底（表 miss 时的 B 计划）+ fork 已实装零成本，不再是"两线分治"的桌面支柱。

**E5 真机渲染器对照（Redmi 23090RA98C / Mali-G610，非 root，2026-10-31）**：0618 裸跑（无 legacyfix）四渲染器过一遍，adb 拉日志判"真挂载"（dlopen .so + GL vendor 串）防假对照——
- **GL4ES**：真跑，**唯一进游戏**（`libng_gl4es.so` QCL 自带、首帧正常；setIcon NPE 被捕获属已知非致命）。**（标签修正 2026-10-31：`libng_gl4es.so` 出处查实 = `BZLZHH/NG-GL4ES`，自称 **"Krypton Wrapper"**（gl4es + Pojav gl4es-114-extra 双源 fork，Release 0.4.5）——即这轮实际是 **Krypton 在跑**，非裸 gl4es；目录里并存的 `libgl4es_114.so` 才是传统 gl4es-114 路径。QCL 的"GL4ES"选项默认吃 NG 版。）
- **MobileGlues**：真挂载（`picked=mg`、env 四连注入、`libmobileglues.so` dlopen 成功、customGLVersion=4.0.0）但**黑屏**——MG 是桌面 GL 3.2+/4.0→ES 的 ANGLE 式转译，0618 的 GL 1.x 固定管线函数 not available 刷屏。**真结论，非假对照**。
- **VirGL/Zink**：**假对照，判无效**——两轮 GL vendor 均为 `ARM/Mali`（系统裸 ES），mesa 栈未实际接管。**（勘误 2026-10-31：先前"QCL 未打包 mesa、生态不存在"结论作废——设备 `/data/app/.../lib/arm64/` 目录实查 `libEGL_mesa.so`、`libvirgl_test_server.so`、`libOSMesa_8/81`、`libglapi.so`、`libvulkan_freedreno.so` 全在，且同时存在 `libgl4es_114.so`（旧版）与 `libng_gl4es.so`（ng 版，实际被 dlopen 的就是它）。真实失败机制待定：mesa .so 在场但初始化未接管（疑点：mesa 的 Vulkan winsys 在该 Mali blob/驱动组合上失败后静默回落，或 picked=zink 的 `env=[]` 空注入本就没走 mesa 路径）——需下次启动带 mesa 调试 env（`MESA_DEBUG=1`/`LIBGL_DEBUG=verbose`）重取证。挂账由"销账"改回**"开放：mesa 在场但未接管，根因待查"**。）
- **判定**：真跑过的两家里 **gl4es 是 legacy 版本（≤b1.7.3/infdev）当前唯一可用转译层**（MG 真挂载仍黑=固定管线不支持），Phase 0 "GL4ES/Krypton 现成够用"获实机背书；Seashore 渲染层维持 gl4es 路线。VirGL/Zink 因"mesa 在场未接管"根因未明，挂账保持开放（低优先，取证靠调试 env 下轮补）。classic 系色块问题（若后续要查）对照面 = gl4es vs Krypton（+ 待根因明朗后的真 VirGL/Zink）。

**fork 落地**：新增 opt-in `-Dlf.direct=true`——跳过 LW/FML/Fabric 注入，强制直连 Instrumentation 路径（直连 transformer 覆盖所有 classloader）。**（2026-10-31 发版纪律拍板：`lf.direct` 定性为 launchwrapper 取证/测试链专用脚手架，已从 fork 移除、不进发版；设备 cfg 同步清除。发版保留的真修复只有一项：premain 逐类 redefine + `catch (Throwable)` 加固（gl4esquad 维持原实现——点/斜杠"修正"经取证系误诊，已回退）。QCL-0618 在发版 jar 下回到"补丁静默死"状态——可接受，QCL 只是试验田；Seashore 无 LW 直调主类，天然落直连路径，不需要 direct。）**

**direct 路径首跑（0618，2026-10-31）**：注入面确认打通——补丁首次打出完整评估清单（gl4esquad/iconnoop 等 13 Applied），但随即崩：premain 的 CtTransformer 批量 `redefineClasses` 在 QCL Display shim 上抛**裸 `VerifyError`**（javassist 产出的字节码过不了校验器），而原代码 `catch (Exception)` 接不住 Error → premain 中断 → `processing of -javaagent failed` → signal 6 整 JVM 被杀。b1.7.3 走 LFTweaker 路径从不执行这段，故从未暴露。**已加固**：逐类 redefine + `catch (Throwable)`，单类失败只降级（iconnoop 失效回到非致命 NPE），不再拖死 agent 与 JVM；新 jar 已推设备。

**状态**：direct 测试链已拆除，0618 复测随之关闭（发版 jar 在 AlphaVanillaTweaker 壳下补丁仍静默死，属 QCL 侧固有限制，不再追）。gl4esquad 对 repak 名的真机命中验证顺延至 Seashore 直连路径落地后（离线判据已闭环：在跑 jar 四门全过 + 整包唯一性各恰好命中 1）。

**Seashore 架构决策（拍板）**：**不走 launchwrapper**。"现代启动器启动老版本"的 tweakClass/LW 链与 legacyfix 补丁面是两件事——QCL 正是被绑死的教训（换自定义 Tweaker 就把第三方 agent 的注入面整条带崩）。Seashore 形态：
- 壳层自拼 classpath（游戏 jar + LWJGL2 shim + libraries），**直调版本主类**，不经 Launch/tweaker。
- `-javaagent:legacyfix` + `lf.qcl` 式预设。classpath 无 Launch 时 `inject()` 自然 NotFoundException → 自动落直连路径，连 `lf.direct` 都不需要（显式加仅作防御）。
- 收益：补丁覆盖与壳的 Tweaker 选型彻底解耦，"假注入成功"类坑结构性消失；启动语义归壳、补丁语义归 agent。
- 边界：beta+ 时代若要支持 Forge/FML 需另引 LW 兼容层——独立工单，不入首发。

**待办**：
- [ ] fork 发版轮：清点 diff，确保只带真修复（iconnoop、DisplayLists native 守卫、premain 加固；gl4esquad 维持原实现），测试脚手架（lf.direct 等）全部出清；§14 遗留的 s3 文件级 handler、a1.1.1 版本门、`lf.qcl` 预设同轮评估。候选第五门（flag 类内 GETSTATIC 读回）是否入指纹，发版轮定夺。
- [ ] §14 补记 fork 实现轮总账（gl4esquad 真机命中 nw、IconNoopPatch、javassist isEmpty 坑、点/斜杠误诊与平反、premain Error 逃逸坑、repak 家族差异证据、指纹唯一性取证）——发版轮一并落。

## 17. NG-GL4ES 源码取证：阴影三角"inColor 插值 × 重复顶点"机理**字面义证伪**，分叉点收敛到顶点流/颜色数组对齐（2026-10-31，classic 色块取证阶梯第 2 级）

**取证面**：`reference/NG-GL4ES`（BZLZHH/NG-GL4ES，README 自证 = **Krypton Wrapper**，gl4es + Pojav gl4es-114-extra 双源 fork，Release 0.4.5；即 §16 E5 真机唯一进游戏的那条渲染器）。行号以下均指该 fork `src/gl/` 下文件。

**证伪链（四步，全部源码实锤）**：
1. **`inColor` 不存在**：全仓 0 命中。顶点色属性实名 `_gl4es_Color`（`shaderconv.c` L23 内建映射表 `gl_Color→_gl4es_Color, vec4, lowp`；主 FPE 着色器同族命名）。§12/§16"桌面侧澄清"里"inColor 插值"措辞需按实名修订。
2. **颜色路径 = 朴素逐顶点直通，无重复顶点特殊处理**：顶点侧 `Color = gl_Color`（`fpe_shader.c` L395），varying `Color` 声明 L223/L911（custom shader 包装路径为 `varying lowp vec4 Color` + `attribute lowp vec4 _gl4es_Color`，L1502-1505/L1519）；片元侧 `vec4 fColor = Color`（L1011）→ texenv 合成 → `gl_FragColor`（L1464）。全程标准 smooth 插值，**无 flat/provoking-vertex 逻辑、无 per-quad 状态、无重复顶点分支**——"在重复顶点上插值破裂"在着色器层没有落点。
3. **自相矛盾实锤：gl4es 自己的 quad 三角化与 vanilla Tessellator 预三角化完全同构**。三处转换全部 `(v0,v1,v2)+(v0,v2,v3)`、同复制对角顶点、同绕序：`drawing.c` L349-379（glDrawElements 直转）、L771-799（glDrawArrays 静态索引表）、`list.c` L225-238/L241-262（renderlist 转换与合并）。**E2 修复态（游戏发 GL_QUADS）走的正是这条复制顶点路径**——若"复制顶点致插值破裂"成立，修复态也该炸；不炸 → 假设死。
4. **次要嫌疑逐一排除**：color 数组 `size!=4` 拦截仅 ES1.1 生效（`drawing.c` L293-295，Android ES2/3 不触发）；色数组禁用兜底 = per-draw 常量 `vavalue`（`fpe.c` L1546 默认 `(0,0,0,1)` 黑）——产生不了对角楔形；3→4 分量填充 filler=1.0 正确（`array.c` L258-262）；quad 大数组分片（>4×8000，`drawing.c` L721）与 BATCH 阈值（L734-740）只改调度不改数据。

**修正后的分叉点定位（第 3 级阶梯对象）**：修复态与致病态在 gl4es 侧收敛为同一三角流 → 差异只能在**游戏发出的顶点流本身**：
- **b1.7.3 面**：Tessellator 预三角化时位置/color/UV **平行缓冲是否错位复制**（如亮度 buffer 未随对角重复顶点复制 → 颜色数组与位置数组索引错位 → 对角楔形）。下一步 = 反编译 b1.7.3 Tessellator `addVertex`/`draw` 的 TRIANGLE_MODE 分支，核对 colorBuffer 写入次数 vs rawBuffer。
- **classic 面（E4 品红半屏）**：无 Tessellator，颜色走 gl4es **立即模式 list 记录链**（`glColor4f`→vavalue→`glVertex` 逐顶点记录，`listrl.c`/`list.c`）——品红=(R,0,B) 形态指向**颜色分量进数组时错位/缺项**，而非插值。§13"修复面=gl4es C 层"结论暂维持（classic 绕无可绕的事实未变），但根因假设从"shader 插值"改锁"立即模式颜色记录"。
- **公分母**：两条显形路径共享"颜色数据进数组的对齐/复制"，不再共享"着色器"。

**对既有账目的影响**：
- `-Dlf.gl4esquad` 的**实证疗效不受影响**（E2 双证痊愈是现象级事实），但其立项说明书里的机理表述须按本节改写——翻 TRIANGLE_MODE 的真收益可能是"让游戏端平行缓冲保持 4 顶点/面的一致性"，而非"避开 gl4es 的坏插值"。
- fork 发版轮（§16 待办）补记时引用本节；渲染器线立项书（gl4es C 层修复）在拿到第 3 级证据前**暂缓开工**，避免修错层。

## 18. 动态取证（方案 A 落地）：平行缓冲错位假设**证伪**，两态唯一可观测量差异 = 游戏端"预三角化 vs GL_QUADS 直通"（2026-10-31，**阴影三角线**第 3 级）

> **定位修正（用户指正）**：本节双态对照跑在 b1.7.3 上，取证对象是**阴影三角线**（gl4esquad 两态 = 病/修）；classic 色块线（E4 品红/红紫青大块）的致病版本是 c0.0.23a_01，b1.7.3 测不到它。classic 线的动态/静态取证见 §19。

**工具链**：legacyfix-attic fork 新增 opt-in 取证补丁 `-Dlf.gl4estrace`（`Gl4esTracePatch` + `Gl4esTraceHook`，标注发版前出清）。钩 `GL11` 全部 typed-buffer 指针/绘制重载（6+7+7+1+3+1+1 个），每次 draw 输出一行：arrays/elements、mode/count、col/vtx/tex 指针参数 + 按 stride 切分的逐顶点内容（前 16 顶点）。工程坑位两枚：① 设备侧裁剪版 javassist 的源编译器把限定类名解析成嵌套形式（`uk$betacraft...`），任何 pool classpath 安排都救不回（`pool.get` 能解析、`MemberResolver` 仍失败）→ 改为向 GL11 注入**反射 dispatcher**（`Class.forName` 字符串定位 hook，仅引用 java.lang 类型），跨版本稳定；② 启动器 stdout 按 ~2048 字符截断日志行 → dump 改紧凑逐顶点格式。`glEnableClientState` 在该 LWJGL nightly 里 GL11 无重载可钩（0 instrumented），enable 位不可观测，改用"color 缓冲内容 + 画面是否着色"间接判定。

**致病态捕获**（trace on、gl4esquad off、ng_gl4es、b1.7.3 进世界）：地形 display-list 大 draw = `arrays mode=4(GL_TRIANGLES) count=4680`（780 面 ×6），逐顶点流严格 `(v0,v1,v2)+(v0,v2,v3)`，pos/tex 复制一致；交错 stride 32（pos 0-11 / tex 12-19 / color 20-23），**color 字段 = 首面 255,255,255,255、其余全 0,0,0,0**。GUI draw 则 `col=unset`。

**修复态捕获**（gl4esquad=true）：同一批地形 = `arrays mode=7(GL_QUADS) count=3120`（780 面 ×4），几何坐标与致病态逐面一致；**color 字段模式与致病态完全相同**（首面白、余零）。

**判读（第 3 级结论）**：
1. §17"colorBuffer 随对角重复顶点错位复制"**证伪**：两态 color 缓冲内容模式相同，且若 color 数组真被启用，全零段会把地形画成纯黑——画面有色 → **地形颜色数组根本未被使用**（面亮度走逐面 `glColor4f`/display-list 顶点捕获烘焙，不进平行缓冲）。"错位"没有载体。
2. 游戏发出的**几何流两态同构且正确**（面数、坐标、UV 全对齐），§17 修正假设中"顶点流分叉"同样落空——分叉的只是**表示形式**：致病态 = Java 端已预三角化（mode=4, 6/面），修复态 = GL_QUADS 直通（mode=7, 4/面）。
3. 因此色块的产生点被压缩到 gl4es C 层**对 TRIANGLES 与 QUADS 两种输入的逐面着色/顶点捕获差异**（quad 路径有 §17 核过的转换与配套状态，triangle 路径按普通流处理）——与 §12-13 原始直觉重新合流，但这次是动态实锤而非机理推断。

**下一步（第 4 级候选）**：a) 定位致病态 Java 端预三角化的出处（legacyfix 哪个默认补丁在 b1.7.3 里把 Tessellator 翻成 TRIANGLE_MODE——它才是"病态"的直接生产者）；b) gl4es C 层对比 quad 直通 vs 三角输入的顶点捕获/面着色路径（`list.c` 顶点记录 × `fpe.c` vavalue/法线），找"同一几何、两种表示、不同显色"的落点。

**现场账**：设备 cfg 现为**修复态**（gl4esquad=true + trace 全开 + maxverts=10000/dump=256/max=3000）；jar=`/sdcard/Download/legacyfix-dev-20261031.jar`；取证补丁与 dispatcher 方案在 fork 工作区，未提交，发版轮出清。

## 19. classic 色块线第 3 级（c0.0.23a_01）：游戏侧颜色流**静态枚举全清白**，色块实锤下游 gl4es 伪造；Java 钩天花板 = lwjglx shim 立即模式全 native（2026-10-31）

**版本轴修正（用户指正）**：classic 色块（E4 品红/红紫青大块）的致病版本是 c0.0.23a_01；§18 的 b1.7.3 双态属阴影三角线。本节才是 classic 线的第 3 级。

**渲染形态扫描（静态，javap）**：c0.0.23a_01 = **纯立即模式 + display list**（glBegin/glEnd、glVertex3f/2f、glColor4f/3f、glNewList/glEndList/glCallList、glInterleavedArrays；无 glVertexPointer 热路径，drawArrays 仅 1 处）。§18 版 trace（指针 + 数组 draw + color==null 过滤）对它**一行都抓不到**——已把 trace 扩到立即模式（begin/end 逐图元聚合行、clearColor、newList/callList、interleaved）。

**运行时 LWJGL 真相**：QCL 实际挂的是 **lwjgl333 shim**（`/data/user/0/com.qcl.launcher/files/lwjgl333/jars/lwjgl-3.3.3-merged-modules.jar`，json 里写的 2.9.0 不作数）。javap 实锤：**立即模式方法全是 `public static native`**（glBegin/glEnd/glVertex3f/glColor3f/4f/glNewList/glEndList/glCallList），javassist 无法给 native 方法插码 → **Java 侧 GL11 钩对 classic 到顶**，动态只剩非 native 面（glClearColor/glEnable/glDisable/指针族/drawArrays）可观测。

**direct 路径两枚 bug（已修，jar 18:31 版）**：① dispatcher 字段声明误塞进 `CtMethod.make` 源码（它只解析单个成员）→ 编译失败 `lf$c not found` → 改 `CtField.make` 单独加字段；② direct 路径 redefine 循环用 `Class.forName` 预加载 GL11，而 dispatcher 加字段构成 schema 变更 → `redefineClasses` 抛 `UnsupportedOperationException` → **GL11 整份转换被弃**（连可钩的非 native 方法也静默零输出）→ 改为只对 `getAllLoadedClasses` 已加载者尝试 redefine，未加载类交给 addTransformer 在自然加载时转换（加字段对新建类合法）。

**颜色静态枚举（第 3 级核心证据）**：全部 18 个引用 GL11 的游戏类、21 处 glColor*/glClearColor 调用点逐一提取入参构造序列——**全为灰阶/低饱和常量**：白 (1,1,1,1)、0.8/0.6/0.5/0.4/0.2 灰、黑+alpha (0,0,0,0.4/0.7)、sin 动画的水/云色 (0.2·sin·0.4+0.5 类，灰蓝域)、方块面色 = 色×getBrightness()、/255 计算值；天空 clearColor = (0, 场, 场, 场) 日/夜插值。**没有一处调用点发送品红 (1,0,1)/红/紫/青**。

**判读**：
1. §10 色块指纹（通道序类错误）**实锤在下游**：游戏输入色流干净，伪造点在 gl4es 对立即模式顶点色的捕获/插值/FPE 着色器生成链——与 §17 的 inColor 朴素直通结论拼合后，唯一未验证环节 = gl4es C 层 list.c/fpe.c。
2. classic 线**不再追加 Java 侧动态取证**（天花板已至；ASM 调用点改写可绕 native 限制，但静态枚举已把"游戏发什么色"钉死，动态全量流的边际价值低）。
3. 下一手 = **4b：gl4es C 层取证**（NG-GL4ES `list.c` 顶点记录 × `fpe.c` vavalue→inColor 生成，找合法灰阶输入变品红输出的落点）。

**确认跑（18:31 修复版 jar，重启 c0.0.23a_01，2026-10-31 18:36）**：direct 修复生效——`LFTRACE ready` 出现、无 redefinition failed；非 native 面全活：**C=6**（`clearColor=0.883,0.941,1.0,0.0` 淡蓝天空，动态实锤输入色干净）；**A=19994**（glInterleavedArrays 每帧高频，fmt=0x2a2a/0x2a24 + DirectFloatBuffer 32/48 floats——预算被吃光，P/L 因 native 恒 0）。清屏色动态 + 21 调用点静态 = classic 线第 3 级双证闭合。

**LWJGL3 native 钩的出路（回答"3.3.3 没办法处理吗"）**：GL11 的立即模式方法靠 RegisterNatives 绑定，剥 native 标志塞方法体 = 绑定崩，javassist 路线是死路。可行路按成本排序：① **ASM 调用点改写**（动游戏 18 个类，在每个 INVOKESTATIC GL11.glXxx 前 DUP 参数 + 调 trace——绕开绑定问题，纯 Java，legacyfix 已有 byte 级 Transformer API）；② **C 层 LD_PRELOAD**（`setprop wrap.com.qcl.launcher` + trace so，但 LWJGL3 走 eglGetProcAddress 取址可能绕过符号插桩，且首次碰 NDK）；③ **gl4es 带 trace 重编**（= 4b 本尊）。判定：①技术可行但静态枚举已钉死"游戏发什么色"，全量动态流边际价值低——**不建议做**，直接进 4b。

**现场账**：c0.0.23a_01 cfg = 取证态（javaagent + `lf.direct=true` + gl4estrace max=20000/dump=8 + 五枚 disable：deawt/classic-resize/classic-performance/bitdepth/lwjgl-genlists + lf.version + assetsDir）；jar=dev-20261031（18:31 修复版，确认跑已完成）；AVD 操作归用户，我只推送。

## 20. classic 色块线 4b（NG-GL4ES C 层静态取证）：立即模式颜色链**设计正确，三嫌疑全案**——伪造点被挤出快照链，剩余未验证环节 = list→VBO 上传/绑定（2026-10-31）

**取证面**：`reference/NG-GL4ES/src/gl/` 立即模式顶点色全链——`gl4eswraps.c`（入口转发）→ `gl4es.c`（编译态分发）→ `listrl.c`（快照/数组）→ `list.c`（分配/recycle/extend）→ `listdraw.c`（消费绑定）。

**全链核验（逐环实锤，行号均指该 fork）**：
1. **入口转发干净**：`glColor3f→glColor4f(r,g,b,1)`（gl4eswraps.c L450）；`glVertex2f/3f→glVertex4f(x,y,0,1)/(x,y,z,1)`（L608-619）——classic 的 2f 高度图地形全汇到 rlVertex4f，无独立窄路径。
2. **编译态分发完备**（gl4es.c L438-470）：STAGE_DRAW→`rlColor4f`；compiling/stage<DRAW→`lastColors`+`lastColorsSet`；pending+POSTDRAW→`post_colors`。三分支末尾统一更新 `glstate->color`。post_colors 在 recycle（list.c L883-896）与 extend（L590-591）两处均有转接兜底，无丢色窗口。
3. **嫌疑①（首条 glColor 前的顶点槽=malloc 垃圾）否决**：`alloc_sublist` 确是裸 malloc（list.h L262-263），但 `rlColorCommon` 首建 color 数组时对**已入列的全部 len 个顶点 catch-up 回填** `lastColors`（listrl.c L221-236），此后每顶点由 `rlVertexCommon` 写槽（L22）——`[0,len)` 全覆盖，无未初始化槽。`lastColors` 源头 = alloc_renderlist 快照 `glstate->color`（默认白），rlEnd 反向写回保帧间连续（listrl.c L214-217）。
4. **嫌疑②（GL_BGRA 借 .size 当格式标，drawing.c L127/136/231）降级**：只活在 glColorPointer 客户端数组路径；§19 已钉死 classic = 纯立即模式+display list，不进此门。对本案无关（对别的游戏仍是雷）。
5. **嫌疑③（merger 共享缓冲跨帧错位）否决**：`merger_used` 闩锁保证 merger_master 单所有者（listrl.c L560-571，只有无主列表才拿 use_glstate）；`resize_renderlist` merger 分支 realloc 后**重钉当前列表全部指针**（list.c L773-803），悬垂不成立。stride 全链一致：merger 交错 20 float/80B（begin 时 vert/color/tex/normal/fogcoord_stride 五连设 L563-568；rlVertex4f idx=len*5*4；catch-up stride=5*4；绑定用 color_stride）、非 merger packed（color_stride memset 0 = 紧凑，语义正确）。
6. **消费端**（listdraw.c L551-571）：有数组→COLOR_ARRAY+`glColorPointer(4,FLOAT,color_stride,list->color)`；无数组→关阵列走常量色（post_color 回放 L861）。`final_colors` 合成支路需 color_sum+secondary+ES1+非 merger 四条件，classic 不触发（其 indices 支路确有"未覆盖槽留 malloc 垃圾"的理论洞，域外记账）。

**附带发现（上游工单候选，非本案根因）**：`rlVertexP3uiv/P2uiv/P4uiv` 在 use_glstate 路径 idx=`len*3*4`/`len*2*4`/`len*4*4`，与 merger 布局 stride `len*5*4` 不符（listrl.c L36-74）——packed 整型顶点进 merger 必致属性错位。classic 不用 glVertexPointerv 族，够不着；但这是 gl4es 家族真 bug，PR 面可顺带。

**判读**：§19 说"唯一未验证环节 = list.c/fpe.c"——现在 **list 侧颜色链验清白**。合法灰阶进、品红出的伪造点被挤出快照/数组/绑定三段，剩余候选压缩为两段：
- **A. list→VBO 上传/缓存路径**（listdraw.c L40-42/L125-126/L165-171：work[] real_base/stride 计算 + vao 属性存取）——数组内容对、上传后 GPU 读到的不对，只剩这一跳；
- **B. FPE 属性绑定/着色生成**（fpe.c vao 装配 × fpe_shader.c 光照段——§17 只核了 color 直通，未核 lighting 开启时 `Color×material+light` 合式；classic 0.0.23a 亮度走 Java 侧 glColor 乘法、疑不开 GL_LIGHTING，B 先验概率低于 A）。

**阶梯定级**：4b（静态）到此收敛，**修复落点仍未钉死 → 4c（带 trace 的 gl4es 重编 = Phase1 NDK 线）成为唯一判定点**：在 rlVertexCommon 出口与 VBO 上传口各打一个逐顶点颜色 dump，两态（品红块出现/不出现）对照，A/B 二选一当场见分晓。渲染器线立项书"暂缓开工"闸门维持——C 层修复在 4c 前不许动工，避免修错层。

**对 QCL 落地的含义**：Phase1 覆盖验证（adb root 换 `libng_gl4es.so`）的前置是补丁存在；补丁存在的前置是 4c 定位。顺序锁定 = 4c 定位 → 补丁 → Phase1 AVD 验证 → Phase2 捆 jniLibs → Phase3 上游 PR。QCL 侧 Phase1 的免重签覆盖路径（`/data/app/*/com.qcl.launcher*/lib/x86_64/`）与三阶段方案本身不变。

## 21. 4c 工具链落地：trace 构建基座 = 上游 gl4es v1.1.4（ndk-build，NDK r30 全链打通）（2026-10-31）

**基座选型（关键决策）**：trace 构建**不打 NG-GL4ES**，打上游 ptitSeb/gl4es **v1.1.4 精确 tag**（浅克隆于 `reference/gl4es-1.1.4/`）：
1. NG 的 CMakeLists 拉 glslang+SPIRV-Cross 重型 3rdparty，本地检出为空目录（vendored 无 submodule 内容）→ 现编必挂；上游 gl4es 纯 C 零依赖。
2. **版本轴**：设备 Holy = `libgl4es_114.so`（1.1.4 系）；CHANGELOG 实锤 **1.1.6 有 "Fixed many issues with VAO and VBO"**——恰是 §20 嫌疑 A 的领地，用 1.1.7 编 trace 可能"病已自愈"误判。1.1.2 的 merger 改进/GL_BGRA VA 均在 1.1.4 之前，基线一致。
3. 两渲染器同复现（§13）→ 共同祖先代码取证成立，且 Phase1 覆盖对象可换 **Holy 的 so**（同目录），不必碰 NG。

**工具链账**：NDK r30 = `D:\Android\Sdk\ndk\30.0.16248370`（便携版，不走 PATH，一切用绝对路径）；cmake 3.29.3 + ninja 1.12.1 来自 `D:\mingw64\bin`（宿主无关，MinGW 的 gcc 与 VS BuildTools 均不参与——编译器 = NDK clang）。`ANDROID_HOME` 未设，SDK = `D:\Android\Sdk`。

**构建入口 = ndk-build，非 CMake**：1.1.4 的 CMakeLists **没有 ANDROID 分支**（链接缺 `glx_init/FreeFBVisual/BlitEmulatedPixmap` + `__android_log_vprint`——Android 路走的是 `Android.mk`，Pojav 当年同款）。CMake 路另需 `-include include/android_debug.h -DNOX11 -DNO_GBM` 才能对齐（android_debug.h 声明 log 接口并把全部 printf 重定向 logcat "LIBGL" tag——trace 输出通道天然就位）。本地改动三处（全部只记在 clone 里，不 commit）：
- `Android.mk`：`LOCAL_MODULE := gl4es_114`（产物名=设备 so 名）+ 去 `-DSTATICLIB` + `BUILD_STATIC_LIBRARY→BUILD_SHARED_LIBRARY`
- `Application.mk`（新增）：`APP_ABI=x86_64 / APP_PLATFORM=android-21 / APP_STL=none`

**构建命令**（cwd = `reference/gl4es-1.1.4`）：

```powershell
& 'D:\Android\Sdk\ndk\30.0.16248370\ndk-build.cmd' NDK_PROJECT_PATH=. APP_BUILD_SCRIPT=Android.mk NDK_APPLICATION_MK=Application.mk
```

**冒烟 PASS（未打 trace 补丁）**：`libs/x86_64/libgl4es_114.so` = 1.2MB；SONAME=`libgl4es_114.so`；NEEDED 仅 dl/log/c/m（gl4es 走 dlopen 取 GLES，无 libGLESv2 硬依赖）；`glBegin/glColor4f/glVertex3fv/glDrawArrays` 导出齐。坑一枚记账：1.1.4 `logs.c` 裸用 `__android_log_vprint` 靠 Android.mk 的 `-include android_debug.h` 兜底，NDK r30 clang 已把隐式声明升 error——**CMake 路必挂，ndk-build 路天然活**。

**4c trace 补丁设计（待写）**：三探针 + 文件门控（`access("/sdcard/gl4es_trace")` 开关，adb touch/rm 免重编；预算计数防刷屏）——① `listrl.c` rlVertexCommon 出口：逐顶点 dump 实际写入 `list->color` 的 RGBA（对照 §19 灰阶枚举 = 数组入口真值）；② `listdraw.c` draw_renderlist 绑定段（L551 块）：dump 首顶点色 + color_stride + use_glstate；③ 同文件 VBO 化路径（work[] real_base/stride 计算处）：dump 上传参数。判读 = 三点对账，颜色在哪一跳从灰阶变饱和，A/B 立判。

**trace 补丁已落 + 编译 PASS（2026-10-31 23:10）**：产物 `reference/gl4es-1.1.4/libs/x86_64/libgl4es_114.so`（1.2MB，TRV/TRD/TRB/门控路径/预算标记五字符串实测嵌在二进制里）。改动清单（全在 clone 内，不 commit）：新增 `src/gl/gltrace.c/h`（门控=首调 access 缓存 + 预算 60000 行 + 耗尽打 `TR BUDGET EXHAUSTED`）；`listrl.c` rlVertexCommon color memcpy 后插**探针①** `TRV <list> #<顶点序号> r g b a`；`listdraw.c` draw_renderlist 正常绑定分支插**探针②** `TRD <list> len ilen gs stride c0(4) c1(4)`（c1 偏移=stride/4，merger 路 stride=80B→20 float 正确）；list2VBO 上传循环后插**探针③** `TRB <list> vbo off size stride len c0(4)`（size≠len×stride 即上传参数说谎）；Android.mk 源列表 +gltrace.c。**探针③ 只在 VBO/batch 路激活时出现——TRB 出现与否本身就是信息**（默认立即模式客户数组路不经过 list2VBO）。

**Phase1 AVD 操作步骤（用户操作设备，推送配合）**：
1. `adb root` → 定位 `/data/app/*/com.qcl.launcher*/lib/x86_64/libgl4es_114.so`（先 `ls -Z` 记录原 owner/权限/context）
2. `adb push libgl4es_114.so /data/local/tmp/` → `cp` 覆盖 + 还原 owner/chmod 644（SELinux context 若为 app_lib_file 需 `restorecon`）
3. 门控：`adb shell touch /sdcard/gl4es_trace`；QCL 里选 **Holy(gl4es) 渲染器**启动 c0.0.23a_01（§13 已证 Holy 同复现）
4. 收口：`adb logcat -s LIBGL`（android_debug.h 把全部 printf 重定向到此 tag）落盘对账
5. landmine 备案：游戏进程若因 scoped storage 读不到 `/sdcard/gl4es_trace`（门控静默=零输出），换 app 专属路径 `/sdcard/Android/data/com.qcl.launcher/files/` 重编一版即可（一行改动）

**判读表（三点对账，§20 嫌疑 A/B 二选一）**：
| TRV（入口） | TRD（绑定） | TRB（上传参数） | 结论 |
|---|---|---|---|
| 饱和/垃圾 | — | — | §20 静态结论被推翻，回炉快照链 |
| 灰阶正确 | 饱和 | — | 数组在 rlEnd~draw 之间被改，查 merger 共享/realloc |
| 灰阶正确 | 灰阶正确 | size/stride 不自洽 | **嫌疑 A 实锤**（list→VBO 上传） |
| 灰阶正确 | 灰阶正确 | 自洽或无 TRB | **嫌疑 B 实锤**（FPE 绑定→GLES 转译段，查 fpe shader 颜色 uniform/attribute 接线） |

## 22. 4c 第一轮 trace 实机数据：CPU 数组完全无罪，主嫌疑收敛到 FPE 转译段（疑纹理）（2026-10-31 深夜）

**现场**：trace so 覆盖 Holy 成功（landmine 实录：`restorecon` 把 755 打回 644，dlopen 需 exec 位，chmod 修正；原版备份 `/data/local/tmp/libgl4es_114.orig.so`）。游戏"老崩"（疑纯 1.1.4 缺 Pojav gl4es-114-extra 补丁，另案不追），但 4 个 session 都抓到生成世界色块期数据，预算耗尽 ×4。日志=`build\gl4es-trace-libgl.log`（24 万行，TRV 192000 + TRD 48000 + TRB 0）。

**判读结果（判读表第 4 行命中）**：
- TRV 全量零饱和：只有 `0.2471` 灰 ×96000 + 白 ×96000 两种色（§19 灰阶枚举实锤到运行时）
- TRD c0/c1 全灰（注：第一轮扫描脚本报 72000"饱和"系 PS5 解析缺陷误报，scan2 分 c0/c1 重扫 = 0 饱和为准）
- TRB=0：list2VBO 从未激活 → **嫌疑 A（VBO 上传）当场死**
- 全日志品红族值零命中 → **品红从未进入任何 CPU 数组**（写入/绑定两端皆清白；"rlEnd~draw 被篡改"也死）
- gs=1 全程 merger 路、list 指针十几个共享 merger_master——但两两颜色皆灰，merger 共享对色块无罪（数组层面）

**新头号嫌疑（B 细化）：FPE 转译段的纹理线**。经典地形 = 纹理 quad × 灰调制；**品红 = "缺纹理"经典调试填充色**；灰 × 品红 = 品红块——天然解释"游戏全发灰阶、屏幕却饱和"。候选机制：gl4es FPE 纹理对象/上传失败时采样落调试填充，或 fpe shader 颜色合成接线错。下一步探针 **TRT**：draw_renderlist 纹理绑定段 dump（tex 指针、texture id、target、vbo_tex 激活态）+ 判读前先看 fpe shader 里纹理缺位时的输出路径。判据：TRT 显示纹理 id=0/未就绪 ↔ 色块时刻 = 定罪纹理上传/绑定线；TRT 全正常 = 回 fpe shader 颜色合成。

## 23. 4c 第二轮 trace（TRT 纹理绑定）：绑定线全清白，嫌疑收敛到"纹理内容"（2026-10-31 下午）

**现场**：TRT 探针版 so 覆盖成功（同 landmine 流程：cp+chmod 755，不 restorecon）；TRV 撤除、预算扩 200000。用户跑一局（未崩，日志刷屏致卡顿），收 59569 行 TRT + 59197 行 TRD + 362 行 TRB，预算未耗尽。日志=build\gl4es-trace-libgl.log（18.8MB，round1 已归档 -round1.log）。扫描脚本 build\scan-trt.ps1。

**判读结果（判据"TRT 全正常"命中 → 回 fpe 颜色合成/内容线）**：
- 全部 59569 行只有 u0（MC 单 TMU，符合预期）；**nullBD=0、vld0=0、id0=0、tg-1=0、tcNULL=0、mm_need=0**
- bound id 集合 = {3,4,5,...,22} 十几个真实纹理；尺寸 = 256x256×59197（terrain）+ 16x16×372；全部 valid=1
- set_texture 全程 0（capture 恢复路 L439 从未走，绑定全靠 draw 时 glstate——TRT 采样的正是消费端真值，覆盖面完整）
- act!=bound=37927 属正常重绑节流差值
- **结论：realize_textures 喂给 GLES2 的每一笔绑定都是"有效、完整、非空"纹理对象。绑定线无罪。**

**静态排除（同轮完成）**：
- 全 src 无品红常量；fpe_shader.c 合成段（L968-1060）只有 mul/add/mix/replace，无调试填充 → gl4es 不产品红
- LIBGL_MIPMAP=3（launcher 侧设置，init 日志 "ignore MipMap"）→ [texture_params.c L241-259] mipmap filter 被降级 GL_LINEAR，纹理完备性无 mipmap 坑
- realize_active 只切单元不传数据；gl4es_glTexImage2D 上传是即时的（texture.c L1388/1410）
- init 日志两条环境注记：`glGetError() always return GL_NOERROR`（LIBGL_NOERROR=1）+ BGRA8888 扩展在用

**剩余唯一解释 = 纹理内容本身是品红**（模拟器/emugl 对未初始化或转换坏掉的上传填品红调试色；灰块=正常纹理×灰顶点色，品红块=品红内容×白顶点色）。下一探针 **TRU**：texture.c glTexImage2D 实际 GLES 上传点 dump id/level/fmt/type/w/h/data==NULL/首像素字，判据：terrain 上传若 data!=NULL 且首像素非品红族 → 内容清白，罪在 BGRA 转换/采样层（换 LIBGL_* 开关实验）；若首像素品红族/NULL → 定罪上传转换。

## 24. 4c 第三轮 trace（TRU 上传内容取证）：**定罪完成——色块源头=应用侧上传的"白×alpha0"纹理，gl4es 无罪**（2026-11-01）

**现场**：TRU 探针版覆盖成功；用户跑一局，出色块但没进世界就崩（崩=纯 1.1.4 缺 Pojav 补丁，另案）。日志=build\gl4es-trace-libgl.log（52MB，TRT 165944/TRD 165363/TRU 392/OTHER 751=init banner×4 会话+TRB，**零 gl4es 错误**）。

**TRU 判读（fmt=1908 GL_RGBA, type=1401 UNSIGNED_BYTE，全部 rz=0 px=1 即时直传，无 NULL 上传）**：
- **p0 直方图无品红族**：`00ffffff`×76（=(255,255,255,α=0) 白透明！）、`9x ff 3x 2x` 系列×~200（16×16 动态纹理，蓝=水/流动动画，每 ~70ms 重传）、00000000×12、ff000000、ffffffff
- **id=22（真 terrain）p0=ff4ab474**：bytes=(116,180,74,255) 草绿不透明 → terrain 解码/上传链路正常
- **id=3-8,10-19,25-27 共 17+ 张 256×256 纹理：CPU 侧首 texel 就是白×α=0**（ARGB int 0x00FFFFFF=缺 0xFF alpha 掩码，或白透明占位图）——gl4es 收到时已如此，探针读的是 gles_glTexImage2D 调用点实参，POT RGBA8 直通无转换，**上游=应用/launcher 的 Java 纹理生成路**
- TRT×TRU 交叉定罪：本轮绘制绑定的主力就是这些白/α0 纹理（id=5×31670、id=7×29563、id=4/6/3/9/13/14 各数千~万），真 terrain id=22 仅 15830 笔且集中在更晚时段

**色块机理（全链闭合）**：白×α0 纹理 × 灰光照顶点色（第一轮 TRV 已证全灰）= **灰块**；α=0 混合透出 framebuffer 未初始化内容 = **品红块**（emulator GL 调试填充色，非 gl4es 常量，全库 grep 无品红）。

**修复方向（Phase2 转火应用侧）**：
1. 审计 launcher/游戏 Java 纹理路：谁在传 0x00RRGGBB（ARGB 漏 `|0xFF000000`）或白透明占位图；对照 terrain 路（alpha 正确）找分叉点
2. 设备已恢复：原版 libgl4es_114.so（含 Pojav 补丁）回滚 + 门控关闭，下轮起游戏不再被 trace 拖慢/缺补丁崩
3. gl4es-1.1.4 换回原版后，若色块仍在 → 100% 应用侧实锤；若消失 → 回头补查 gl4es 上传（预期不会）

## 25. Phase2 审计第一轮：会话版本=**Classic c0.0.23a_01**（非 1.5.2！）+ 白/α0 纹理与 gamma=100 新线索（2026-11-01）

**取证材料**：/storage/emulated/0/Android/data/com.qcl.launcher/files/debug/pojav_latest_log.txt（13MB，含 stdout 重定向的 LIBGL trace+Java 日志，已拉本地 build\pojav-full.log）；tombstone_15（build\tombstone15.txt）；TRT×TRD 交叉表（round3 日志）。

**版本与栈（args 实录）**：client.jar=versions/c0.0.23a_01；执行栈=launchwrapper+**AlphaVanillaTweakInjector(LegacyFork)**+`-javaagent:MioLibPatcher.jar`+**caciocavallo**（`cacio.font.fontmanager=sun.awt.X11FontManager`、FreetypeFontScaler、MetalLookAndFeel、managed.screensize=2400x1080、applet 模式 2400x1080）；lwjgl 3.3.3 merged + lwjglx。

**审计结论**：
1. **jar 资产真实**：c0.0.23a_01.jar 与 .jar.orig 的 PNG 列表/大小逐项一致（terrain 18803B 等），patcher 只增 font/glyph_*.png。→ 白/α0 纹理**不是死链物化占位图**。
2. **16×16 动态纹理=水动画**：首像素蓝 (34,55,255,149)、每 ~70ms 重传、随 tick 变暗——Classic water.png 动画路，正常。
3. **白/α0 256×256×17 的身份收敛**：Classic 自身只有 terrain/tallterrain/gui/clouds 四张 256 级纹理且 terrain 解码正常（id=22 绿）→ 17 张空白纹理最可疑来源=**caci 字体图集**（X11FontManager 在 Android 上枚举失败→空白 256×256 字形图集，白×α0）或 **MioLib 覆盖层**。TRT×TRD 交叉：绑这些纹理的绘制 c0=灰0.247/白1.0（加载屏期）→ 屏幕灰块=空白纹理×灰/白顶点色；α=0 混合透出 framebuffer=品红块。
4. **新罪证：gamma=100 直通**：id=22 地形绘制中 1033 笔 **c0=100.0000**（越界！）——Classic GameSettings.gamma 是 0-100 百分比，疑似某补丁/移植路把百分比直接喂 glColor4f 未 /100 → 过曝白块。待定位到 Java 侧具体调用。
5. **崩溃定性（老崩另案）**：tombstone_15=SIGSEGV SEGV_ACCERR，线程 JNISurfaceTextu，栈=gl4es→emulator `GL2Encoder::s_glBufferData`→`GLSharedGroup::updateBufferData`→memcpy 读**悬垂客户端指针**（VBO batch 路 "try to use VBO"）。=游戏/gl4es 向 glBufferData 传已释放 buffer，与色块独立。

**下一步判据实验（不动 gl4es）**：①去掉 `-javaagent:MioLibPatcher.jar` 跑一局对比白/α0 上传是否消失（消失=罪在 patcher/caci 字体路）；②审计 reference/Quanta-Craft-Launcher 里 gamma/glColor4f 调用与 cacio 字体图集创建代码；③崩溃线复查 org.lwjgl.system.allocator=system + LWJGL buffer 生命周期（悬垂 memcpy）。

## 26. 判据实验①前置：MioLibPatcher / qcl_appletfix 两 jar 解包审计——**双双脱罪，白/α0 纹理嫌疑收敛到 caci 字体图集**（2026-11-01）

**来源确认（回应"启动参数里没有 miolibpatcher"）**：用户没配错——`-javaagent` 是 **QCL 每次启动自动追加**的：jar 落在 app 私有目录 `/data/user/0/com.qcl.launcher/files/`，mtime=启动时刻（16:00 与本次启动一致）=重写式部署；QCL 源码 grep "MioLibPatcher" 无匹配（sparse checkout 未含注入模块，但设备侧证据已足）。

**取证方式**：run-as 失败（非 debuggable），`adb root` + `adb pull` 完整拉下（811164B/5679B，build\MioLibPatcher.jar、build\qcl_appletfix.jar），javap 反汇编全部 transformer。

**MioLibPatcher.jar（Premain-Class=com.mio.libpatcher.MainAgent，javassist 内嵌）——18 个 transformer 逐一过堂，全是现代版/MOD 兼容垫片**：
- SmoothFontTransformer：目标 `bre.smoothfont.FontUtils.getMaxFontSizeIndex→return 7`——**唯一碰字体的，但目标是 SmoothFont MOD 类，Classic 会话不加载 → no-op**
- 其余目标=Sodium/Essentialium/VulkanMod/Fabric/Forge/Create/Axiom/Veil/oshi/TTS/RandomPatches——Classic 全不加载
- Classic 会话可能激活的仅两个：`org.lwjgl.system.Library.checkHash→{}`（跳 native 库哈希校验）+ `ALC10.alcGetCurrentContext` 垫片——**均不触纹理/GL 内容**
- **结论：MioLibPatcher 与白/α0 纹理无关；判据实验①（去 -javaagent 对照）价值降级为可选**

**qcl_appletfix.jar=AlphaVanillaTweakInjector 本体（QCL 自编，仅 4 class）**：职责=applet 启动引导（找 MinecraftApplet 候选、读 `qcl.applet.width/height`、注 username/sessionid、Center 布局）——**零 GL/纹理代码，脱罪**。

**收敛后的头号嫌疑= caciocavallo 字体图集**：`cacio.font.fontmanager=sun.awt.X11FontManager` 在 Android 无 fontconfig/X11 字体环境，字形图集生成失败→白×α0 占位内容，与 17 张 256×256 空白纹理吻合。下一步：审计 caci 字体路（glyph 图集尺寸/创建点）+ QCL 里 gamma/glColor4f 调用（§25-4 罪证）。

## 27. Phase2 审计第二轮：**白/α0 纹理身份实锤=QCL 中文补丁注入的 CJK 字形页**+ gamma=100 罪证撤销（caci 假说证伪）（2026-11-01）

**第三个注入者现身（qcl.cfg extraJavaFlags 实录）**：`-javaagent:/sdcard/Download/legacyfix-dev-20261031.jar -Dlf.direct=true -Dlf.gl4estrace=true(max 20000/dump 8) -Dlf.deawt/classic-resize/classic-performance/bitdepth/lwjgl-genlists.disable=true -Dlf.version=c0.0.23a_01`——闸门一实验配置残留仍生效。但拉下 legacyfix-dev jar（633 entries）全量扫描：**零 glyph_/cjk/Font 内容**，其 v3 patch 矩阵（classic/lwjgl/misc/thirdparty 27 类）也不碰纹理→legacyfix 与白纹理无关。

**真凶链（游戏 jar 反汇编，build\c0.0.23a_01.jar=设备 patched 1.15MB）**：
1. patched jar 比 .orig(193KB) 多 223 个 `font/glyph_XX.png`+`glyph_sizes.bin`(65536B) 资产 + 新增类 `com/mojang/minecraft/c/l`（**Compiled from "Font.java"，1.4.4 式 CJK TTF 字体引擎回移植**：`loadCjkPage(page)`→`TextureLoader.a("/font/glyph_%02X.png")` 懒加载 256×256 字形页，`cjkPageTex[256]`）
2. **glyph_00.png 实测=256×256，透明像素=白×α0（0x00FFFFFF）**——与 TRU 定罪的 17 张空白纹理首 texel 完全吻合 → **身份实锤：白/α0 纹理=CJK 字形页**（caci 字体图集假说证伪，AWT 文字不走 GL 路）
3. 纹理上传路（`a/a.class`=TextureLoader）审计**无罪**：ARGB getRGB→字节序 r,g,b,a→GL_RGBA/UNSIGNED_BYTE 忠实转换，无 alpha 丢失
4. 触发面：加载屏/服务器列表等渲染中文文本→按页懒加载（17 页=文本实际触及的 CJK 页数）；字形页为现代管线设计（GL_SRC_COLOR 混合+COMBINE），在 Classic GL_MODULATE+标准混合下白×α0 内容×灰/白顶点色=灰块、α0 透出 framebuffer=品红块——§24 机理至此全链闭合到具体资产
5. **注入者=QCL 中文补丁资产管线**：`git ls-tree` 实锤 `QCL/src/main/assets/cn_b173/font/glyph_*.png` 等按版本预置的 cn_* 中文化资产（含字体回移植），版本安装时合并进 client jar（设备 jar mtime=02:19 安装时刻）

**gamma=100 罪证撤销（§25-4 改判）**：全 jar 扫 glColor 变体调用方（a/d、a/g、c/h、c/m、c/o、c/l）——**所有 glColor4f 实参要么是常量（0.2/0.4/0.5/0.8/1.0），要么经 /255.0f 归一**（c/h 打包 int 色换算全正确），无任何调用点能产出 100.0。TRD 的 c0=100.0=地形 chunk 顶点色数组的 **GL_UNSIGNED_BYTE 光照字节原值**（100/255≈0.39 正常光照），探针读原始值未除 255 的显示口径问题，非罪证。

**修复方向（4c 收口候选，待评审）**：
- A. **绕过**：QCL 设置里关中文化/换无 CJK 回移植的版本装法（若可选）→ 验证色块消失即终审实锤
- B. **资产修复**：把字形页透明像素预乘为黑×α0（0x00000000）或白×α255 重打包——灰块变黑块？需先判混合态：α0 透出仍在→品红仍现，治标
- C. **管线修复**：让回移植字体在 Classic 下按现代混合态渲染（glBlendFunc(GL_SRC_COLOR,GL_ONE)+GL_COMBINE texenv）——语义正确但动注入产物
- D. **砍掉回移植**：Seashore 特化时不注入 cn_* 字体资产（本项目是自家启动器，减法自由）——**最符合立项方向**

## 28. 用户截图新症状：缺通道楔形（青/黄/品红）+ 闸门一残留配置清理（2026-11-01）

**截图证据**（ dirt 加载屏背景正常=右侧泥土纹理完好）：屏幕被三块**从角发射的巨型楔形 quad** 覆盖=**青(0,255,255)/淡黄(255,255,~150)/品红粉(255,~100,255)**+黑楔——恰为 RGB **缺一通道**的互补色族，且几何=小 quad 被放大到全屏尺度。

**归因假说（待判据实验）**：qcl.cfg extraJavaFlags 残留闸门一旗标 `-Dlf.classic-resize.disable=true`（**resize 补丁关死**→游戏窗口尺寸与实际 viewport 失配→文字/渐变 quad 放大成楔形）+ deawt/classic-performance/bitdepth/lwjgl-genlists 全 disable + CJK 引擎的现代混合期望（SRC_COLOR/COMBINE）在 Classic 态下产缺通道色。**4c 现象可能被"实验配置残留"整体放大**。

**处置与改判**：**用户实证：去掉 legacyfix 也是同样效果 → 归因假说证伪**，楔形与 legacyfix/旗标无关，属 QCL 核心栈（applet 2400×1080/caci/gl4es）现象。

**操作教训（本轮踩坑实录）**：曾把 qcl.cfg 里 5 个 `lf.*.disable=true` 当"实验残留"清理 → **游戏卡死在 gl4es 着色器初始化后起不来**——这些 disable 是 §14 闸门一的**有意结论**（渲染层补丁在 Android 不存活），不是残留！已从 build\qcl.cfg.bak 去 BOM 回滚（1203B 原样）。**PS5 坑**：`Out-File -Encoding utf8` 备份带 EF BB BF BOM，回推前必须剥 3 字节，否则 QCL JSON 解析风险。

**当前状态**：qcl.cfg=截图时已知好配置；探针 .so（round3 版）+门控 /sdcard/gl4es_trace 已重上；logcat 采集在跑（build\gl4es-trace-wedge.log）。判据实验：用户复现楔形→抓 glColor 三元组 (0,1,1)/(1,1,0)/(1,0,1) 调用点+绑定时序，定死楔形绘制方（字体路放大 or 背景路坏色）。

## 29. 楔形采集轮判读：排除纹理内容与 glColor 电流，锁定顶点色数组（2026-11-01）

wedge 轮（16:41 会话，TRT=37644/TRD=37592/TRU=51）：
- **缺通道三元组扫描 hits=0**：TRD 的 c0/c1（电流色+数组首元素）无 (0,1,1)/(1,1,0)/(1,0,1) → 楔形色**不是 glColor 电流**
- **TRU 全去重无楔形色内容**：id=20/21=16×16 动画蓝（水/熔岩，p0 逐帧递增）；id=9=不透明白；id=23=全0/24=不透明黑；id=3-8/10-19/25-27=白×α0 256×256（**字形页再现，§27 复现成立**）；id=22=草绿 terrain
- **gl4es glColorMask=GLES2 原生直通**（gl4es.c L1109-1120），非 FPE 模拟 → mask 合成假说排除
- **剩余头号嫌疑=顶点色数组非首元素**：渐变楔形=quad 四顶点各异色，本轮探针只采数组 c1 首元素漏其余
- **下一步**：加 TRW 探针（listdraw 逐笔采色数组全元素+viewport+矩阵顶），重编重部署再跑；数组含缺通道色→Java 侧渐变/文字路喂坏色实锤，数组干净→GL 状态/混合路

## 30. TRW 轮判读：数组彻底干净，定罪下移到 GLES2 绑定层（2026-11-01）

TRW 探针版（1223064B）16:50 部署；第一轮 logcat 后台进程 17:23 意外断流（exit 255，wedge.log 停在 16:41 旧会话）→ 清环形缓冲重启采集到 **build\gl4es-trace-wedge2.log**。17:26 新游戏进程 5462 全程入档：TRW=9781 / TRD=9811 / 预算未耗尽。

- **全日志饱和缺通道色 hits=0**（TRW 前 4 元素 + TRD/TRB 一起扫）：数组里只有白/灰(0.2471)/100×3 三族 → **§29"数组非首元素藏楔形色"假说证伪**
- **楔形仍在屏**（screencap-now.png，17:28 游戏内）：青/黄/品红/黑楔全部从**左上角 (0,0) 发射**，共享顶点；右上露出正常泥土贴图世界。vp=0,0,2400,1080（applet 拉伸态）。HUD 文字 quad 正常渲染在楔形之上
- **判读**：楔形色不在任何 CPU 色数组里 → 产生点在**gl4es 消费端之后**：
  1. **绑定错位假说（新头号）**：FPE 着色器的 color attribute 实际指向了顶点/纹素缓冲（位置值 0..2400 钳到 0/1 = 饱和缺通道楔形，几何上"角发射"= 以原点为共享顶点的错位采样）
  2. **无 color 数组的绘制**（list->color==NULL，走电流色 uniform）→ TRD/TRW 天然不采，楔形可能是这类绘制的 uniform 色本身坏掉
- **下一步（TRB2/ATT 探针）**：在实际 `gles_glDrawArrays/glDrawElements` 落点（listdraw.c 尾部 draw 处）逐笔 dump：ATT_COLOR/ATT_VERTEX 的 real_buffer+real_pointer+stride+clientstate 使能位 + glstate->color 电流色 + list->len/mode。锁定画全屏大三角的那一笔到底把什么数据喂给了 color 属性。

## 31. 崩溃线实锤=上游索引上传 sizeof bug，一行修复+ATT 探针部署（2026-11-01）

楔形复现轮（进程 5462，uptime 149s）17:28:49 SIGSEGV(SEGV_ACCERR)@memcpy←GLSharedGroup::updateBufferData←s_glBufferData←**libgl4es draw_renderlist listdraw.c:802**（llvm-addr2line 符号化，tombstone_18）。崩溃寄存器：拷贝 size rdx=0x8C88=35976=**8994×4**，恰为崩溃前最后一笔 TRD `len=5996 ilen=8994` 的 ilen×4；源指针越界读到未映射页（fault=src+size−0x10，scudo 块头显示源块仅 ~18KB）。

- **根因（上游原生代码，git diff 排除探针引入）**：L802 `gles_glBufferData(GL_ELEMENT_ARRAY_BUFFER, list->ilen*sizeof(GL_UNSIGNED_SHORT), ...)`——`GL_UNSIGNED_SHORT` 是 `#define 0x1403` 整数字面量，`sizeof`=4，索引实为 GLushort(2B) → **EBO 上传尺寸翻倍越界读**。全库仅此一处。桌面/常规 heap 布局下多读 2× 落在邻块内存=静默 UB；AVD 上索引数组恰贴页边界→崩。**"启动不起来"=此 bug 间歇发作**（读越界是否撞未映射页取决于堆布局，故有时能跑有时秒崩）。
- **判读边界**：上传虽越读，但 `glDrawElements(count=list->ilen)` 只用前 ilen 个索引=正确段 → **此 bug 不解释楔形**，只解释崩溃线。楔形线继续走 ATT。
- **修复（工作区，未 commit，遵守纪律）**：L802 改 `list->ilen*sizeof(GLushort)`。
- **ATT 探针（同轮部署，17:37 覆盖 so）**：listActiveVBO 之后、draw 之前，`len<=8` 门控逐笔 dump：ATT_COLOR/ATT_VERTEX 绑定实况（enabled/size/type/stride/real_buffer/real_pointer）+ list->color/vert 指针 + vbo_array + post_color 电流色 + v0/v1 顶点坐标 + c0 首色。预算 200k→500k（上轮 136k 行/149s，防截断）。
- **下一步**：用户重启游戏复现楔形 → 找 v0 覆盖全屏/坐标异常的那笔小绘制，比对 cA 绑定是否指向 vert/tex 缓冲（错位→查 realize_array/绑定链）或 en=0 走 post 电流色（坏→查 glColor/FPE uniform 上传）。

## 32. 天空品红三角线**定罪+修复双确认**：gl4es `glInterleavedArrays(GL_C3F_V3F)` 步长 typo `vert=4`（应 3）（2026-11-01，wedge18p→18w 七轮探针）

症状：世界内天空巨型品红/青/黄/白三角（间歇性）+ 生成世界进度条/GUI 一次性色污染。修复后全部消失（用户目视确认）。

**探针阶梯（so md5 链，全部工作区未 commit）**：
- **18p**（ec1a711d）：TRU 补 `gid=`（游戏 texture id）与 `glname` 对账 + TRVQ 补 `gt=`。结果：映射恒等（22→22、23→23），**§31 前后的"游戏 id↔glname 错位一格"假说证伪**；天空 plane 真绑真采 23。
- **18q**（cbdaa225）：drawing.c 加 `/sdcard/gl4es_skip` 门控跳过 prg=10&&t0=23 draw + texture.c 补 TexSubImage 探针（TRS2）。用户报"伪影消失"——后被证伪为**间歇性巧合**（TRSK 当时未进世界，从未 >0）；TRS2 全程 0 hits（游戏无 TexSubImage 补传）。
- **18r**（a72ae9a3）：`gltrace_texread` 临时 FBO attach 回读驱动纹理 23 内容。结果：**上传时+绘制时全零**（1566 次 draw23 回读全 `px=00000000`，FBO complete）→ 零纹理+MODULATE+alpha test(0>0) 应整片 discard，天空 plane 画不出任何像素 → "品红写入者=天空 plane"归因存疑（每 8 门控归因模糊）。
- **18s**（0b3e07f6）：pxcheck 移除 `count<=64` 限制。结果：品红 TRVQ 仍只挂在 cnt=6 天空 tile 上，大 draw 无 → 嫌疑未转移，需查 shader。
- **18t**：判读补漏——list 路径 skycheck 打的是 `TRQ`（非 TRVQ），此前 grep 漏看。结果：TRQ 品红 **0 hits**，地形（prg=7 t2d=22）无罪实锤。
- **18u**（738041c1）：把 wedge18f 的 shader dump 块抽成 `gltrace_dump_shader`，pxcheck 对 prg=10 触发一次。结果：**prg=10 片元着色器无 sampler2D、无 texture2D()**——texcoord 数组禁用+无 texgen 时 FPE 直接跳过纹理阶段，**片元=顶点色插值直通**。屏幕色=顶点色，垃圾顶点色即屏幕垃圾色。
- **18v**（947db747）：TRVQ 后加 `TRV6`（颜色数组原始 28 float）。结果：缓冲内容 `[c3 p3]` 紧凑 24B 记录（`(0.5,0.8,1.0)(-2048,74,512)…` 四角平铺），**无 padding**。
- **18w**（9dc051bd）：glVertexPointer/glColorPointer/glTexCoordPointer 入口 `TRPC` 打原始实参。结果：天空 plane 声明 `c(sz3,st28,+0)+v(sz4,st28,+12)` ≠ 数据 24B 步长 → **从 v1 起逐顶点错位 4 字节**：c1=(0.8,1.0,-1536)→钳→黄、c2=(1.0,-1536,74)→钳→品红、p1=(512,0.5,0.8)→垃圾坐标巨型三角。对照地形 `t(2,32)+c(3,32)+v(3,32)` 声明=数据 → 正常。

**责任定位**：TRPC 的 28/4 不是游戏直接调用，而是 **gl4es `glInterleavedArrays` 自拆 pointer**（gl4es.c L167-170）：`case GL_C3F_V3F: color=3; vert=4;`。vert=4 → `stride=tex*4+color*4+normal*4+vert*4` 误算 12+16=**28**（规范 C3F+V3F=6 float=24B），且 vertex size=4、偏移 +12，与 TRPC 观测逐位吻合。全 switch 其余格式（T2F_C3F_V3F=32、C4UB_V3F、T2F_V3F 等）均核对无误，**仅此一处 typo**。**上游溯源（已证）**：raw.githubusercontent ptitSeb/gl4es **v1.1.4** 同文件同分支亦为 `vert = 4`（本地 WebFetch 核对 L146-149）→ **真实上游 bug，非本仓库注入**，OpenGL 1.2 附录表 C3F_V3F=6 float/24B，正确值 `vert=3`。

**游戏侧调用点（源码回源，E:\4k\github-projects\minecraft-verification-analysis\src-minecraft）**：`LevelRenderer.renderClouds`（0.0.14a_08 树 L250-263）云层下蓝色垫层 plane：`tesselator.color(0.5F,0.8F,1.0F)` + `vertex(x, depth+10, z)`，±2048 步长 512——与 TRV6 观测色值、y=74、tile 尺寸**字面全等**。Tesselator 数据驱动 mode：`hasColor && !hasTexture → GL11.glInterleavedArrays(GL_C3F_V3F, 0, buf)`，全游戏唯一 color-only 大 plane 调用方。

**版本窗口四树复核（用户报告合龙，雾为共变混杂）**：
- 0.0.13a_03 干净：**无 renderClouds**（grep 0 hits）→ 从不触发 GL_C3F_V3F 路径，与雾模式无关
- 0.0.14a_08 坏：renderClouds 新增 → 踩中
- 20100124-2 坏：Tessellator.java L34 仍调 GL_C3F_V3F
- 20100125 干净：Tessellator 重写 C4UB（IntBuffer 打包色）→ 步长正确
- **修正用户合取假说**："C3F∧LINEAR 雾"应降级为"游戏存在 color-only Tesselator 大面（GL_C3F_V3F）"单条件；实验 1（改 EXP）在本机理下预测**仍坏**（错位发生在顶点数组读取，雾只参与事后混合）。**用户最终裁定（2026-11-01）**：不是雾，就是单纯 C3F——合取假说废弃，与 vert=4 根因自洽。

**修复（工作区，未 commit，遵守纪律）**：gl4es.c L169 `vert = 4` → `vert = 3`。编译部署（so md5 22456e21，x86_64），用户进世界确认：**天空巨型三角全部消失，含生成世界进度条色污染**（进度条文字 quad 同为 color-only Tesselator 路径，一并解释 GUI 一次性伪影）。


**现场状态（AVD 恢复后，2026-11-01 收口）**：gl4es 工作树已**剥离全部探针**回干净态——`git checkout` 11 个改动文件 + 删 gltrace.c/h/Application.mk，重打且仅重打三处：①gl4es.c `GL_C3F_V3F vert=3`（天空线修复）②listdraw.c `ilen*sizeof(GLushort)`（§31 崩溃线修复）③Android.mk 构建脚手架（`gl4es_114` 模块名 + BUILD_SHARED_LIBRARY，上游为静态库 GL，此为部署必需）。最终 diff 仅 3 文件 +5/-7。探针全量存档于 `reference/gl4es-forensics/`（wedge-series.patch 725 行 + gltrace.c/h + Application.mk，随时可恢复取证形态）。设备侧：纯净修复版 so（md5 `ea5ac172`，1220216B）已部署应用 lib 目录；trace 日志 404MB、w18*.sh 判读脚本、截图 png、probe.so、trg18f.txt 等全部删除；`/data/local/tmp` 仅存 `libgl4es_114.orig.so`（QCL 出厂备份，1282432B）作回滚保险。**渲染器选择坑（虚惊一案）**：QCL 切游戏版本后渲染器默认回落到 Krypton，不再走 libgl4es_114.so——验证 gl4es 修复必须手动选 Holy-GL4ES，否则伪影"复发"是假象（Krypton 不含本修复）。另：toybox `grep -iE 'a|b|c'` 正则在 AVD 上会段错误崩掉 grep 自身（tombstone_29=grep，非游戏），判读脚本避免该写法。**纯净版终验（2026-11-01 收口）**：0.0.14a_08（坏窗口首版）+ Holy-GL4ES + `ea5ac172` 进世界——天空干净（蓝底+棋盘云）、树叶/地形全正常、43 帧，用户截图确认。天空线全链闭环。仍未 commit（遵守纪律）。**待办**：①~~楔形线（c0.0.23a_01 classic 会话）与本 typo 是否同族~~ → **已闭环，见 §33**；②~~上游溯源已完成（见上，v1.1.4 同带此 bug）→ 可整理 issue/PR 素材提交 ptitSeb/gl4es（一行 `vert=4→3`，附 renderClouds 复现路径与 TRPC/TRV6 证据）~~ → **用户裁定（2026-11-01）：不向 gl4es 上游交 PR，本账撤销**；修复以我方 fork（Seashore-Renderer）+ QCL issue #5 通报形态落地。

## 33. Classic 色块/楔形线**同族闭环**：与 §32 同一 `GL_C3F_V3F vert=4` typo（2026-11-01）

§13→§31 的 classic 色块线（c0.0.23a_01，楔形自屏幕角发射/色块污染）由 §32 根因**一并解释**，证据链三点回源：

1. **版本在坏窗口内**：0.0.23a_01 ∈ [0.0.14a_08, in-20100124-2]（§32 四树窗口）。
2. **同路径存在**：`Tesselator.java L34` 有 `glInterleavedArrays(GL_C3F_V3F)` 分支；color-only 调用点≥两处——`LevelRenderer.renderClouds L246`（云底 plane `color(0.5F,0.8F,1.0F)`，同 0.0.14a_08）+ `ProgressListener.java L56-72`（生成世界进度界面 `color(4210752/8421504/8454016)` 灰/绿/棕色块——**正是当年 E4"classic 色块"的案发面**）。
3. **旧结论全部对上**：§19"游戏侧颜色流静态枚举全清白、色块实锤下游 gl4es 伪造"=vert=4 用错步长读对了数据；§17"分叉点收敛到顶点流/颜色数组对齐"=4 字节错位；§30"楔形自 (0,0) 发射"=错位读出的垃圾坐标（如 (512,0.5,0.8)）投影到原点附近。

**状态**：纯净版 `ea5ac172`（含 vert=3）对 classic 会话回归，用户 2026-11-01 确认闭环。§13-§31 各轮探针的"伪造点"悬案至此全部归位：渲染面=vert=4 步长 typo，崩溃面=§31 索引 sizeof——两处均已修，同属 gl4es 上游原生 bug。

## 34. 渲染器线方向变更：自研降级 → **Krypton（NG-GL4ES）基座 Fork**（2026-11-01）

**决策（用户拍板）**：§10 的"修复优先、自研作后续"再降级——不自研渲染器，直接 fork **Krypton Wrapper**（aka NG-GL4ES，BZLZHH，本地 `reference/NG-GL4ES`，Release 0.4.5），出货形态=与 Krypton 并列的新渲染器条目，但**修复从 Krypton 代码出发**（非 ptitSeb 主干 1.1.4）。

**Krypton 画像**：gl4es + Pojav `gl4es-114-extra` 双 fork，MIT（自有修改代码亦 MIT，§6 许可纪律零摩擦）；构建=**CMake**（非我们 1.1.4 用的 ndk-build，Android 交叉工具链待验）；捆绑 SPIRV-Cross/glslang（ARB→SPIR-V 高级 shader 面，即其"capability to handle more advanced shaders"卖点）；部分代码来自 MobileGlues。

**三面审计（对 NG-GL4ES 0.4.5 源码逐处核对）**：
1. **天空/色块面（§32/§33 C3F typo）：Krypton 原样带病**——`src/gl/gl4es.c` L220-223 `case GL_C3F_V3F: color=3; vert=4;` 与 1.1.4 逐字一致。**这直接解释 §13/E1"Krypton 与 Holy-GL4ES 同现象"**：两个 fork 继承同一上游 typo，非"共用颜色链路"（QCL README 归因作废，§10 判读 3 再证）。修复=§32 已验证的 `vert=4→3`，可直移。
2. **崩溃面（§31 EBO sizeof）：Krypton 已修**——`src/gl/listdraw.c` L762 `list->ilen*sizeof(GLushort)`（上游后续提交已改对）。fork 白捡，无需动作；旁证 Krypton 有跟上游修。
3. **阴影三角面（§12，0611~b1.7.3）：Krypton 未修**——`end_renderlist` QUADS 分支（list.c L853-858）与 1.1.4 逐字一致（单 quad→TRIANGLE_FAN / 多 quad→quads2triangles）；drawing.c 被重构（scratch 统一索引展开，L347-370），但 QUADS→TRIANGLES 语义未变。与 E1 实证吻合（Krypton 下裸臂阴影同在）。**此面=本 fork 的首个取证+修复任务**。

**GL4ES-Fix 修复法核对（b1.7.3 反混淆树回源，`src-minecraft-beta-1.7.3/net/minecraft/src/Tessellator.java`）**：
- 修复本体=javaagent ASM 在 `<clinit>` 把 `PUTSTATIC b:Z`（`convertQuadsToTriangles = true`，L14）改推 `ICONST_0`——与 §12 判读一致；版本表键=家族名（`b1.7.x`→混淆类 `nw`），精确版本查表 miss 即 premain 抛异常→JVM abort（§12 首败复盘，上游仓库 javaagent 分支三文件已全量核对）。
- 触发面（源码级）：`draw()` L114-118 `if(drawMode==7 && convertQuadsToTriangles) glDrawArrays(GL_TRIANGLES, …)`——游戏侧把 quad 预三角化为 6 顶点（`addVertex` L232-250 在第 4 顶点复制 v0/v2 发 `[v0,v1,v2,v0,v2,v3]`）；翻 false 后改发 `glDrawArrays(GL_QUADS, …)` 4 顶点，由 gl4es 自己展开。
- **"为何禁用这条链路就能修"的 C 侧含义**：两形态几何等价（同为 6 顶点两三角），差别仅在**进入 gl4es 的 draw 形态**——预三角化走 `GL_TRIANGLES` arrays 直绘批（`indices=NULL`、不展开），QUADS 走 gl4es 内部展开批（单 quad→FAN 4 顶点 / 索引展开→TRIANGLES）。故障必在 TRIANGLES-arrays 直绘路径独有的处理段（批合并/FPE 颜色属性侧），而非展开算法本身。（**2026-11-01 证伪**：两路径读穿后严格等价，故障不在 C 的模式处理段，见 §35）
- **C 侧故障点仍未钉死（开放悬案，fork 首任务）**：GL4ES-Fix 作者未解释机理；上游 ptitSeb/gl4es **#429**（b1.7.3+1.12.2 同症状，2023-05 开、open、零评论）亦无人定位。§12"inColor 插值破裂"为占位描述。取证可复用 `reference/gl4es-forensics/` 探针套件，但注意 wedge-series.patch 打在 1.1.4 上，NG-GL4ES 的 drawing.c 已重构，移植需重对锚点。（**2026-11-01 已钉死，本节作废**：真凶=Java 数据层零法线重复顶点，C 侧仅放大，见 §35；fork 工单③随之改写）

**构建面审计（CMakeLists.txt 逐行核对，回答"fork 后编 Android ABI 可行性"）**：
- **一等 Android 支持已在**：`if(ANDROID)` → `-DNOX11` + 排除 `src/glx/glx.c` + 链接 `android`/`log`（L56-58/L140-142/L169-174）；EGL 不禁（运行时 dlopen libEGL/libGLESv2/v3，context 由启动器 jre_launcher 创建）——**与 Pojav gl4es-114-extra 同形态**，Seashore（Pojav 血脉壳）集成零摩擦。Android 特化代码面极薄（全 src 仅 logs.c 的 `__android_log`），无 ANativeWindow 依赖。
- 产物 `libng_gl4es.so` SHARED，LTO+`-O3 -ffunction/data-sections --gc-sections -Wl,--strip-all`（APK 尺寸友好）；`DEFAULT_ES=3`（114-extra 血统，GLES3 后端默认，远古 MC 可运行时 `LIBGL_ES=1/2` 回落；AVD 支持 GLES3.2 无碍）。
- **硬约束**：`CMAKE_CXX_STANDARD 23` + glslang/SPIRV-Cross 静态捆绑（ARB→SPIR-V shader 面）→ **NDK 下限 r26（clang 17+）**，我们的 r30 满足；重依赖×多 ABI 全编不便宜（一次性成本，产物可缓存进 jniLibs，对齐 §9"fork 期零 NDK"纪律）。
- **本地副本不完整**：`reference/NG-GL4ES` 为无 `.git` 的源码拷贝，两 submodule 目录空——fork 工作目录须上游 clone（含 submodule）重建。COMPILE.md 的 Android 节是上游 gl4es 陈旧文档（提 Android.mk，本仓库无此文件），实际构建=NDK 工具链文件 + cmake。
- ABI 矩阵：arm64-v7a/x86 由 gl4es 本体长期支持，glslang 亦支持 32 位 ARM；x86(32) 按 §9 拍板 best-effort 或不 ship。**内置形态**：预编译 so 进 `app/src/main/jniLibs/<abi>/`，渲染器注册在壳层（§32 坑：QCL 切版本回落默认渲染器，Seashore 可把 fork 版设为默认，规避该坑）。

**Fork 工单（按序）**：①fork 工作目录重建（上游 clone+submodule）→ NDK r30 CMake 编 **arm64-v8a + x86_64**（AVD 验证优先）跑通基座 ②直移 `vert=4→3`（已验证，预期天空/classic 色块全消）③阴影三角 C 侧取证定罪（TRIANGLES-arrays vs QUADS 展开两批路径差分）→ 出修复或上游 PR ④扩 armeabi-v7a（x86 视 §9 策略）+ 内置 Seashore jniLibs + 渲染器条目命名待定。~~§32 待办②（上游 PR 素材）优先级不变，可并行~~（**2026-11-01 用户裁定：不交上游 PR，撤账**）。

**工单③状态（2026-11-01）**：已闭环（§35）——差分结论=两路径严格等价、C 无罪，真凶在 Java 数据层。**Krypton fork 不需要修此面**，出货落点改写为壳侧 `-Dlf.gl4esquad`（§12 立项的单点 fork 补丁）。

## 35. 阴影三角 C 侧取证**定罪闭环**：真凶=Java 预三角化零法线重复顶点，C 侧放大=FPE `normalize(0)`（2026-11-01，§34 工单③销案）

**现象**：第一人称裸臂/持物 quad 对角"暗棕楔形+亮残条"，爆发窗口 inf_20100611~b1.7.3（GL4ES-Fix 支持表下界=inf_20100611，逐版本自洽；§12 E1/E2 实证痊愈）。

**定罪链（b1.7.3 反混淆树 × gl4es-1.1.4 C 双侧回源，本轮完成）**：

1. **数据层（真凶，Java 侧）**：`Tessellator.addVertex` L232-249 预三角化复制循环只搬 slot [0-2]xyz/[3-4]tex/[5]color，**从不搬 slot [6] 法线**；且复制顶点占据的缓冲位置（每 quad 第 4、5 槽的 +6）也永远不会被正常路径写入（正常路径只写第 1-3 顶点与第 6 顶点的 +6），`rawBuffer` 一次分配、`reset()` 不清零 → **重复 v0/v2 的法线恒为 (0,0,0)**。
2. **触发面**：手臂与持物都 setNormal → hasNormals=true → 法线数组启用——手臂 `TexturedQuad.draw` L41/43（经 `ModelRenderer.compileDisplayList` 捕获 display list，数据源仍是 Tessellator）、持物面片 `ItemRenderer` L51-124（每面 startDrawingQuads/setNormal/4 顶点/draw）、持方块 `RenderBlocks` L3174+。光照在场：`ItemRenderer` L43 `GL_RESCALE_NORMAL`（该开关仅在光照语义下有意义）。
3. **C 侧放大（非罪魁）**：gl4es 的 TRIANGLES/QUADS 两路径读穿（`glDrawArrays`→batch/mergelist→`end_renderlist`→`draw_renderlist`），**几何严格等价**——同对角线 (0,1,2/0,2,3)、同属性绑定、同 shader，§34 L547"故障必在 TRIANGLES-arrays 独有处理段"证伪。C 侧唯一角色=FPE 光照在 GL_RESCALE_NORMAL/GL_NORMALIZE 下生成 `vec3 normal = normalize(gl_NormalMatrix * gl_Normal)`（`fpe_shader.c` L743-744），`normalize(vec3(0))` 在 shader 中未定义（NaN/零）→ 含零法线顶点的三角形 diffuse 塌缩 → 只剩环境光=暗棕楔形，楔形沿对角线、亮残条居正确法线顶点一侧，与 E1 截图逐像素吻合。
4. **桌面为何无恙**：桌面固定管线对长度 0 的 rescale 是驱动定义的容错行为（良性）；shader 化实现把它显性化。这解释 §12 E1"双渲染器同在"=**shader 化光照家族的共性放大，非任何 fork 的代码责任**——§34 审计 3"Krypton 未修"应读作"Krypton 无病可修"。
5. **修复法重述**：GL4ES-Fix 翻 `convertQuadsToTriangles`=false → 游戏发 GL_QUADS 4 顶点（法线全对），gl4es/Krypton 自行展开索引 → 破损数据根本不产生。**"禁用这条链路"=禁用产生坏数据的链路，属数据侧修复**；babric/ML mod 版同语义。

**旧结论修订**：§12 机理第 2 步"inColor 插值破裂"作废（颜色复制完整、坏在法线；§17 已证属性实名 `_gl4es_Color` 朴素直通，与本节自洽）。

**出货两选项**：
- **A 数据侧（推荐）**：壳侧 legacyfix fork 单点补丁 `-Dlf.gl4esquad`（§12 已立项、E2 已实证），对 0611~b1.7.3 窗口版本翻字段。与渲染器无关，Krypton/Holy/未来自研全受益。
- **B C 侧防御（可选增强）**：FPE `normalize` 前加零向量保护——可兜住一切发零法线的应用（不止 MC），但属超出 GL spec 未定义区的行为固化，进上游 PR 前需单独评估。

**上游 PR 素材（§32 待办①）增补**：#429（b1.7.3+1.12.2 同症状、open、零评论）可直接以本节证据链回帖定位：Java 侧数据 bug + shader `normalize(0)` 未定义行为，非 gl4es 展开/合并/颜色链路问题。（**2026-11-01 用户裁定：不向 gl4es 上游交 PR/回帖，本段仅作素材存档**；本案修复面本就在 Java 侧，出货走选项 A `-Dlf.gl4esquad`，与上游无关。）

## 36. QCL 上游同步：1.5.7 → 1.5.9 判读（2026-11-01，Phase 1 基座落库前置）

reference 克隆已切至 `1.5.9`（`e49f4b3`，1.5.8/1.5.9 同提交，tag v1.5.8/v1.5.9）。增量 6 commits、+2256/−45（28 文件），**全部现代侧，远古面零触碰**——基座落库源版本改钉 1.5.9。

**1.5.8 核心（Minecraft 26.3 启动线，非我方域）**：真根因 = 26.3 默认图形 API 仍 OpenGL，GL 路径过 `libmobileglues.so` 的 `glGetString` 对部分查询返回 NULL、调用方 `strlen(NULL)` → 原生 SIGSEGV（tombstone 铁证）；26.4 官方改 Default≈Prefer Vulkan 故不崩。修法 = 26.3+ 追加官方参数 `--graphicsBackend vulkan` + 自动写 MG 配置 `enableNoError=2`（键名坑：非 ignoreError）。附带：MobileGlues 内置（aar 7.8MB 四 ABI）、JNA 5.19.1 natives 解压链、首帧视角甩动修（cursor NaN 哨兵）、安装器 JVM 起不来 catch Throwable、fml.toml earlyWindowControl、背景图采样解码。

**1.5.9**：公告「展开」按钮修复 + 补建公告 id=109。

**对我方的四点判读**：
1. **渲染器库解析顺序 = Phase 1 fork so 集成通道实锤**：1.5.8 起"内置（APK 自带）→ 外部插件 APK → `renderer/<id>/` 手放目录"三级解析（`RendererPicker`/`RendererCompat.isBuiltinLibPresent`），Seashore-Renderer 产物走 jniLibs 内置即一等公民；条目注册面 = `RendererCompat.ALL`（id/glName/displayName/supportRange/recommended 结构），我方三条目收敛（Holy-GL4ES + Krypton + Seashore-Renderer 默认）在此裁剪。
2. **「查看上次启动日志」+ logcat 原生崩溃 tag 补白**（1.5.8）与 §3 崩溃界面参照同族——基座自带，Phase 1 白捡。
3. **README"天花板"表述未动**：1.5.8/1.5.9 只增渲染器表与 26.3 根因段，远古色块的"第三方共有天花板"归因原样——issue #5 修订稿的更新建议依然成立，且他们 26.3 一案的取证文风（真根因=组合而非自家 bug）与我方两案同构。
4. **cherry-pick 纪律再验**：1.5.8 改动密集在现代启动链（PojavLauncher/CheckJavaTask/JREUtils），恰属 Phase 2 砍面——摘取时按模块簇走，勿被"升级冲动"整分支同步。
