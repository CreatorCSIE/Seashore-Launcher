# Seashore-Launcher 施工计划

> 定位与决策依据见 [research/qcl-android-shell.md](research/qcl-android-shell.md)（§0-§16），本文件只记阶段、验收与挂账。

## Phase 0 · 调研（已完成，2026-10-31 收口）

**产出**：调研纪要 16 节 + legacyfix fork 实装轮（`legacyfix-attic`）。

关键结论索引：
- **路线**：C 案 = 继承 QCL + 减法特化 + 独立新项（§5）；开发台 = x86_64 AVD 主力、真机仅发版 sanity（§9）。
- **渲染线**：GL4ES/Krypton 现成够用，QCL 卡死的自研渲染器属被砍现代层（§10）；阴影三角机理锁定 = Tessellator 预三角化 × gl4es inColor 插值（§12-§13），修复实装为 fork `-Dlf.gl4esquad`（结构指纹，混淆名免疫），b1.7.3 真机命中（§14）。
- **legacyfix × Android**：agent 语义存活，但价值收敛为游戏侧补丁 + 网络代理，渲染层零触碰（§14）；QCL 捆绑 GL4ES-Fix 表名式对 repak jar 翻车（§14/§16）。
- **启动路径取证**：launchwrapper-1.6 无 blackboard → 钩 VanillaTweaker；QCL 自定义 AlphaVanillaTweaker 使第三方 agent 注入面静默死（"假注入成功"陷阱，§16）→ **拍板：Seashore 不走 launchwrapper，直调主类 + javaagent 直连，启动语义与补丁语义解耦（§16）**。
- **QCL 输入面缺陷**：键盘通道缺失（dexdump 实锤真缺），Seashore 立项需求（§15）。
- **闸门状态**：闸门一（legacyfix 上 Android 存活面）**已闭环**；闸门二（减法依赖图审计）**转入 Phase 2**。

## Phase 1 · 脚手架：搬 QCL 码 + 开发环境

**目标**：得到"可构建、可安装、可跑远古版本、日志链完整"的 Seashore 骨架，行为等价 QCL 1.5.7 现状。

1. **基座落库**：以 `reference/Quanta-Craft-Launcher`（分支 1.5.7）为源，工程主体迁入 Seashore 仓；GPL-3.0 合规——保留版权头与 LICENSE，`reference/` 维持 gitignore 不入库。
2. **品牌剥离**：包名/`applicationId`（`com.qcl.launcher` → Seashore 命名空间）、FileProvider authority、外置存储路径常量（`QCL/.minecraft` → `Seashore/.minecraft`）、名字与图标自建（banner 已有）；UI 布局沿用 FCL 血脉不重写。
3. **开发环境**：Android Studio + gradle wrapper 锁版本；`abiFilters 'x86_64'` 零 NDK；AVD `emulator-5554` 为主力测试机（adb：`D:\Android\Sdk\platform-tools\adb.exe`）；调试日志路径对齐 Pojav log（`files/debug/pojav_latest_log.txt`）。
4. **修复件挂载验证**：骨架上以 `-javaagent:legacyfix` + 预设（`lf.gl4esquad` / `lf.iconnoop` / `lf.deawt.disable` / `lf.assetsDir`）跑 b1.7.3 与 inf-20100618——直连路径天然生效（无 LW），顺带补上点/斜杠修正的真机命中证据（§16 顺延账）。

**验收**：全新 clone → 一条命令构建安装 → 启动 b1.7.3 阴影三角消失、0618 可进游戏；升级/替换 jar 与 cfg 的路径文档化。

## Phase 2 · 减法特化（大概，需具体调研后细化）

**目标**：砍现代版本支持，收敛 Pre-Classic → b1.8/1.5.2，与 Attic 桌面仓同一非目标。

初判砍面（待审计证据化）：
- 渲染器选择层的现代项（Vulkan/Zink/自研 GL 层；GL4ES/Krypton 保留为唯一路径）；
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

- 键盘输入通道立项（§15：捕获层 / 系统键分流 / IME 字符输入）。
- legacyfix fork 发版轮：diff 清点出清测试脚手架（`lf.direct` 已拆）、s3 文件级 handler、a1.1.1 版本门、`lf.qcl` 预设、§14 实现轮总账补记。
- E5 窗口层对照、classic 旋转 resize 实测、gl4es 源码取证。
