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
2. gl4es 的 `inColor` 属性处理在**重复顶点**上插值破裂 → 对角黑三角；
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
- [ ] gl4es 源码取证：立即模式→shader 转换中顶点色属性的插值/默认值路径（Krypton/Holy 共同祖先 = lordgrey/gl4es 上游）。
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

**顺带修复**：Gl4esQuadPatch 第 4 道门把 `clas.getName()`（点分）直接 equals 常量池 fieldref 类名（斜杠）——单段混淆名（nw）恰好成立，多包 repak 名（net.minecraft.client.a.d）永不成立，已改 `replace('.','/')`。在跑 jar 的 `net.minecraft.client.a.d` javap 核验 clinit：`iconst_1; putstatic b:Z`（自引用）四门全过；QCL 捆绑 GL4ES-Fix（表名式）确认没动 repak jar（版本目录有 `*.jar.orig` 原地改写备份，但该类未被覆盖，§14"表名翻车"结论维持）。

**fork 落地**：新增 opt-in `-Dlf.direct=true`——跳过 LW/FML/Fabric 注入，强制直连 Instrumentation 路径（直连 transformer 覆盖所有 classloader）。**（2026-10-31 发版纪律拍板：`lf.direct` 定性为 launchwrapper 取证/测试链专用脚手架，已从 fork 移除、不进发版；设备 cfg 同步清除。发版保留的真修复只有两个：Gl4esQuadPatch 点/斜杠比较修正、premain 逐类 redefine + `catch (Throwable)` 加固。QCL-0618 在发版 jar 下回到"补丁静默死"状态——可接受，QCL 只是试验田；Seashore 无 LW 直调主类，天然落直连路径，不需要 direct。）**

**direct 路径首跑（0618，2026-10-31）**：注入面确认打通——补丁首次打出完整评估清单（gl4esquad/iconnoop 等 13 Applied），但随即崩：premain 的 CtTransformer 批量 `redefineClasses` 在 QCL Display shim 上抛**裸 `VerifyError`**（javassist 产出的字节码过不了校验器），而原代码 `catch (Exception)` 接不住 Error → premain 中断 → `processing of -javaagent failed` → signal 6 整 JVM 被杀。b1.7.3 走 LFTweaker 路径从不执行这段，故从未暴露。**已加固**：逐类 redefine + `catch (Throwable)`，单类失败只降级（iconnoop 失效回到非致命 NPE），不再拖死 agent 与 JVM；新 jar 已推设备。

**状态**：direct 测试链已拆除，0618 复测随之关闭（发版 jar 在 AlphaVanillaTweaker 壳下补丁仍静默死，属 QCL 侧固有限制，不再追）。点/斜杠修正的真机命中验证顺延至 Seashore 直连路径落地后（离线判据已闭环：在跑 jar 四门全过）。

**Seashore 架构决策（拍板）**：**不走 launchwrapper**。"现代启动器启动老版本"的 tweakClass/LW 链与 legacyfix 补丁面是两件事——QCL 正是被绑死的教训（换自定义 Tweaker 就把第三方 agent 的注入面整条带崩）。Seashore 形态：
- 壳层自拼 classpath（游戏 jar + LWJGL2 shim + libraries），**直调版本主类**，不经 Launch/tweaker。
- `-javaagent:legacyfix` + `lf.qcl` 式预设。classpath 无 Launch 时 `inject()` 自然 NotFoundException → 自动落直连路径，连 `lf.direct` 都不需要（显式加仅作防御）。
- 收益：补丁覆盖与壳的 Tweaker 选型彻底解耦，"假注入成功"类坑结构性消失；启动语义归壳、补丁语义归 agent。
- 边界：beta+ 时代若要支持 Forge/FML 需另引 LW 兼容层——独立工单，不入首发。

**待办**：
- [ ] fork 发版轮：清点 diff，确保只带真修复（gl4esquad 含点/斜杠修正、iconnoop、DisplayLists native 守卫、premain 加固），测试脚手架（lf.direct 等）全部出清；§14 遗留的 s3 文件级 handler、a1.1.1 版本门、`lf.qcl` 预设同轮评估。
- [ ] §14 补记 fork 实现轮总账（gl4esquad 真机命中 nw、IconNoopPatch、javassist isEmpty 坑、点/斜杠坑、premain Error 逃逸坑、repak 家族差异证据）——发版轮一并落。
