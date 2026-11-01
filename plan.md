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
4. **修复件挂载验证**：骨架上以 `-javaagent:legacyfix` + 预设（`lf.iconnoop` / `lf.deawt.disable` / `lf.assetsDir`）跑 b1.7.3 与 inf-20100618——直连路径天然生效（无 LW）；阴影三角主路径走 Seashore 自家映射表（步骤 1），`lf.gl4esquad` 仅作兜底顺带验证真机命中（§16 顺延账；离线唯一性取证已闭环：两包全类扫描各恰好命中 1）。

**验收**：全新 clone → 一条命令构建安装 → 启动 b1.7.3 阴影三角消失、0618 可进游戏；升级/替换 jar 与 cfg 的路径文档化。

**迁移挂账（2026-11-01 落库轮记）**：① `ResConfHack.jar` 需按 `tools/cacio_patch/README.md` ASM 流程重建——补丁类 L335 硬编码 `LevelFileChooserActivity` 旧包名，改名后 indev/infdev 存读档的内置文件浏览器回落系统选择器（catch 分支 fail-open，良性但需补）；② 图标/启动页仍 QCL 资产；③ `assets/announcement.json` 仍是 QCL 公告内容（展示面待换）；④ donate UI 指向未动；⑤ release 签名缺 `qcl.keystore`（Seashore 自建 keystore 随发版轮）；⑥ `ModLoaderVersions` 的加载器元数据端点暂借 QCL 仓（数据通道非品牌，Phase 2 复核）。

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
- 键盘输入通道立项（§15：捕获层 / 系统键分流 / IME 字符输入）。
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
