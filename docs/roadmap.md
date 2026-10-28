# 路线图

本文档只记录产品方向、推进顺序和开放边界，不维护逐轮实现记录或验收细节。
当前产品目标见 `docs/goal.md`，运行时状态见
`docs/flow-over-ipc-runtime.md`，可执行验收契约见
`docs/acceptance-tools.md`。

## 当前产品方向

当前目标是在 macOS 上完成 Vaadin Flow 产品功能 parity：应用继续使用标准 Flow
Java 编程模型、标准 production bundle 和官方 Flow client/server；native host 负责
窗口、生命周期与原生能力，opaque IPC 只承载通用 request、push 和
command/result，不解释组件或 UIDL 语义。

长期产品 completion gate 聚焦三条主线；当前阶段 A 只做矩阵与基线提案，不执行
下列所有实现任务：

1. **Flow 能力面**：补齐代表性组件族、数据与资源行为、真实输入、焦点、无障碍和
   状态同步的共享能力矩阵。
2. **运行时连续性与耐久性**：闭合 push、重连、multi-window、sidecar replacement
   以及长时间和并发传输的产品边界。
3. **macOS 原生产品能力**：完成权限模型、sandbox-safe 文件授权和原生能力生命周期。

开发体验、分发与更新、Windows/Linux host 具有长期价值，但不属于当前 macOS
产品功能 completion gate。

## 已具备的基础

### Framework 与 Native Host

状态：核心架构已实现。

- 已提供公开 Java 桌面应用入口、capability API、阻塞式 sidecar launcher 和统一 IPC
  envelope。
- macOS host 由 AppKit/WKWebView 实现，拥有窗口、bundle manifest、sidecar staging
  和 application/window ready handshake。
- Pure Flow 是默认主线；Spring Boot 保持为显式 compatibility adapter。
- 已具备有界 sidecar supervision、ready deadline、replacement、失败恢复和可观察退出
  状态。
- 恢复中的 JVM 超过 ready deadline 且不响应 SIGTERM 时，增加绑定精确 Process/PID/
  generation 的一秒 grace 后 SIGKILL；仅实际退出回调推进下一次恢复。focused 三代真实
  WKWebView 验收覆盖阻塞 shutdown hook、原窗口/WebView/scope、新文档与恢复后的
  Java listener/UIDL/push。现有主动 planned replacement 入口仅允许 development
  webapp，标准 production bundle 会拒绝；不能将 dev-vite 的替换证据计作产品入口。
  产品运行时主动替换的触发、授权、候选校验与回退契约仍需定义并实现。
  底层 recovery/replacement 退休已共享按 Process/PID/generation 绑定的一秒 TERM→KILL
  deadline，首次请求拥有原因与期限，实际退出或 host shutdown 取消旧 deadline。
  合作/忽略 TERM、提前退出/换代、混合请求和 shutdown 竞争有真实专用子进程覆盖；
  KILL 失败或首次请求两秒后仍未收到实际退出回调，报告 fatal failure 并保留归属交给
  host shutdown；回调前禁止新代启动。真实子进程加回调调度替身覆盖超时和迟到退出结算，
  尚非 OS 信号错误、JVM/controller/WKWebView 产品替换证据；产品替换整体结算仍待闭合。
  强制终止不保证应用清理或事务完成。
- 普通 Quit／最后窗口关闭在资源 drain 后，先异步等待 shutdown/EOF、TERM、KILL 与
  实际 JVM exit，再同意 AppKit 退出；发送信号不等同于退出。失败取消当次 Quit，保留
  已隔离的精确进程归属，后续 Quit 只重试终止，不恢复 Flow。focused 双模式门禁在
  相同公开关闭路径覆盖合作及阻塞 hook、准备阶段 Java/UIDL/已消费 push、可信最终
  点击回执与外部 host/JVM 消失确认，清理不能补救 PASS。该门禁为空资源 drain；
  timeout/retry/late callback 为调度替身覆盖，非真实 OS signal-error 注入。
  host 主线程的其它卡死、应用事务完成仍不在此证据内。
- host→sidecar stdin 使用逐 generation 绑定的有界非阻塞 FIFO，写满 pipe 不再占住
  AppKit 或进程归属锁；字节/帧上限、无写进展 deadline 和写错误使通道失败并进入
  fatal retirement，不能丢帧后继续或把旧字节重放给新 JVM。独立 focused gate 覆盖
  真实 Flow 准备状态、精确 JVM 暂停、实际 pipe 背压、主循环响应、产品 Cmd-Q 和
  TERM/KILL 后的真实退出；外部确认先于清理。压力请求为 browser-programmatic，
  准备与菜单输入为 generated AppKit；整 JVM 暂停不等同于 reader-only 故障隔离。
  已观察到进程退出后的迟到写错误交给实际退出处理；该调度竞态有替身回归，不承诺
  所有 OS 退出/写错误竞态均自动恢复，也不保证 Java 事务或其它 host 线程的响应。
- fatal runtime shutdown 复用资源 drain 与实际进程退出确认后保留失败状态 `1`；
  终止失败时保持隔离与精确进程归属，后续 Quit 只重试退出且不改为成功状态。
  首次 ready 超时的合作/阻塞 JVM 门禁在任何兜底清理前独立确认 host/JVM 均消失。
  此分支按契约不开窗口，是 native lifecycle 故障证据，不代表 Flow UI 或全部 fatal
  原因均已验证。另有真实窗口 focused gate：外部原生面板选取后保留空闲只读 grant，
  测试应用发送 unsupported-version frame，验证授权失效、描述符关闭及 scope stop、
  drain 完成、合作 JVM 实际退出和 fatal 状态记录，再独立确认 host/JVM 消失。
  准备点击为 generated AppKit，面板为 Computer Use；不代表 App Sandbox、in-flight
  I/O、强制子进程终止或全部 fatal 原因。
- native command/result 已绑定 host-local 单调 sidecar generation；initial、recovery 与
  replacement 尚未 `ready` 时拒绝命令，旧 generation 的异步 callback 与 AppKit panel
  会在进程确认退出后失效、关闭并阻止结果写入新 JVM。该边界已覆盖真实
  `NSOpenPanel`、意外退出、保留窗口/WebView、command id/scope 重用和新代取消结果。
- Production host 的 trace、redaction 和 compaction 保持 transport-oriented，不包含
  组件、fixture 或验收语义。
- 已通过真实双窗口验证同一个 `window.setTitle` 命令按 manifest 窗口 scope 分别允许和
  拒绝；失败结果保留完整 host 权限 payload，full trace 可动态关联 command/result 与
  window/session/UI scope，同时拒绝路径不产生原生标题副作用。
- CLI 开发命令已显式选择 `allow-all-dev`，`package` 与 macOS product builder 缺省已选择
  `manifest-enforced`；应用缺少源 `desktop-capabilities.json` 时会生成空授权 manifest，
  product 显式 manifest 则先经过严格 JSON parse 并生成一次仅供 staging 使用的 immutable
  canonical snapshot，后续校验与 staging 只使用该 snapshot。invalid JSON、XML plist、
  缺失或不支持的 mode，以及两种 permissive mode 都会在破坏性 staging 前拒绝；duplicate
  key 收敛为唯一 runtime-effective 值，例如前 `allow-all-dev`、后 `manifest-enforced`
  最终只 stage enforced mode。开发 staging 仍接受显式 `allow-all-dev` /
  `trace-only-dry-run`。CLI contract smoke 验证完整 build-time/canonical 契约；focused
  runner 先做两次 permissive builder 负探针，再对真实 product bundle 做四类
  parser-invalid post-build 启动负探针；这些负探针均不启动窗口。恢复原 manifest 后再复用
  default-deny 真实 WKWebView、可信 AppKit click、Java/UIDL gate 作为正向对照。
- owner window 关闭的原生能力生命周期已闭合一个真实 `NSOpenPanel` slice：host 先使
  operation 失效并释放 panel，再向 Java 返回可关联的
  `owner-window-closed` typed result，公开 `openFileResult` 将其归一为 `INVALIDATED`，
  随后释放窗口 context；Java 同步完成 UI detach、
  `SessionDestroy` 和 session close。保留的 main window、WebView 与同一 sidecar 继续可用。
- `DesktopFileDialog.openFileResult` 已提供稳定的
  `INVALIDATED`、`DENIED`、`CANCELED`、`SELECTED`、`ERROR` 结果及明确优先级，同时保留
  原始 command result；旧 `openFile` 继续以 `Optional<Path>` 兼容。真实
  `NSOpenPanel` focused gate 已闭合正常取消、并发操作错误、独立 secondary 权限拒绝、
  main-scoped background push/ack 和无额外 panel 副作用。

### Flow over IPC

状态：主链已稳定并具有广泛验证，但共享能力矩阵尚未完全闭合。

- 标准 Vaadin production webapp、官方 Flow client/server 和 opaque request/push
  bridge 已构成唯一 pure Flow browser runtime。
- 已覆盖 JavaScript 调用、ClientCallable、Java listener、UIDL/state patch、资源加载、
  push/reconnect/resync 和 sidecar replacement 等通用链路。
- 已验证输入、选择、校验、popup、menu、overlay、Grid/lazy data、renderer、editor、
  drag-and-drop 等代表性组件行为；这些组件只作为共享能力 probe，不是 production
  适配单位。
- 已验证德语到美语 locale 切换下的精确 decimal 输入、scale 与 Java listener/UIDL
  保持，以及独立德语 Binder 表单的必填、malformed null→null 仅校验转换、自定义
  拒绝与合法恢复、最后合法 bean 保留和五次 Tab 提交的独立 UIDL 往返；仍不代表
  更广 locale、bound-form 或跨窗口表单验证。host 的标准 App/Edit 菜单通过原生
  responder chain 支持编辑命令，已覆盖
  Cmd+A 全选替换，以及单窗口和两个保留 WebView 的单字符原生撤销/重做、各自三次
  客户端 Java/UIDL 往返与独立历史，以及单窗口纯文本 Cut/Paste、原生剪贴板 epoch
  和有界核验恢复。另有一次同 ID 关闭/重开的编辑归属验证：旧副窗口退休、新实例
  空撤销的有界无变化、新历史正控及保留主窗口历史均独立关联。跨窗口/非文本
  剪贴板、复杂编辑历史、native sheet 和反复 owner churn 仍需独立验证。一次 settled
  edit 后的意外 sidecar 恢复已验证保留原生对象/scope、实际新文档、空撤销无变化及
  新历史正控；不保留旧 Java 值/历史，不代表在途编辑或反复恢复。
- 已验证 Download/Upload、multipart、取消与重试、并发 request 隔离、bounded queue
  和 multi-window 传输等代表性资源与耐久性行为。
- secondary window 的 session scope 由 host 生成 opaque UUID，独立于公开 window id；
  sidecar 对同一窗口使用有序 handler lane，不同窗口可并行推进，关闭 tombstone 不会被
  其他窗口的长任务阻塞。reader 会把完整的 retired scope 保留至进程退出，在 handler
  查找和 capability correlation 前静默丢弃其 late frame；同公开 id 的新 UUID 仍在旧代
  teardown 后正常进入原 window lane。首次 scope context 创建采用 acquire/publish 事务；
  close 前已入队但在 tombstone 后才触发创建的旧代请求会完整回滚 Vaadin session、push、
  native registration 和 owner maps，不会阻塞新代。
- Flow listener 或 `UI.access` 可显式捕获 immutable `DesktopCapabilityContext` 并传给
  任意 executor；capability 调用持续绑定捕获时的 opaque owner scope，不暴露 UI、session
  或 scope identity。owner 关闭或同名窗口以新 UUID 重开后，旧 context fail closed，
  不会转投 main 或新窗口。真实 A→B→C 两次连续同名重开已验证三个不同 UUID 与三个不同
  原生 window/WebView、A/B 旧 context 在 C fresh context 成功前后均稳定失效、
  scope-exact push/ack 及同 sidecar/main 连续性。互补的 production-host gate 以
  test-support fake sidecar 将 A/B 旧 command 和 HTTP/push frame 实际送入生产
  `main.swift` handler，验证两个 process-lifetime tombstone 的 typed 拒绝与丢弃，以及 C
  fresh UUID 的 live command 和 browser response-credit 连续性；真实 Flow gate 继续承担
  Java listener、UIDL 和可见状态证据。该契约不承诺 ThreadLocal 或 executor 的隐式自动
  传播，也不把两次连续重开外推为无限周期或 recovery/replacement 保证。
- main 已关闭但 secondary 保留的窗口拓扑可跨 sidecar recovery 延续：新 sidecar
  lifecycle-ready 后重放 main 的标准 `windowClosed`，不重建 main；保留 secondary 的
  原生 window、WKWebView 和 opaque scope，并按其原 route 重载和建立新 Flow state。
- CLI 已支持标准 Vaadin frontend 构建、Vite/HMR workflow，以及保留 native window
  的受控 sidecar replacement。

具体已验证分支、focused 限制和执行命令统一维护在 runtime 与 acceptance 文档中，
不在 roadmap 重复展开。

## 阶段 A 交付与基线决策

唯一状态清单是 [runtime 能力矩阵](flow-over-ipc-runtime.md#product-capability-matrix)。
矩阵共 28 项共享契约，包含四类原始范围；源码与工具定位不等于全面验证。
当前提案尚待确认，既有详细缺口列表仍保留，但不再把每个“更广/更多/更长”的扩展
自动追加为本轮完成门槛。

需要用户确认的决策：

1. 接受矩阵中的量化基线建议，还是调整三窗口、十次 churn、1,000 项数据、256 MiB
   传输和 30 分钟耐久性范围。这些是建议验收配置，不是已有支持承诺。
2. 是否同意延期 N06 持久书签/覆盖写事务，以及 R05 透明跨进程传输恢复、丰富剪贴板、
   更多 locale/RTL 和任意组合深度。它们仍属长期目标，不因本轮盘点被取消。
3. 是否接受 R06 产品 planned replacement、实际 VoiceOver 和 Finder 边界为首个
   基线必选。推荐保留；若要延期，必须明确缩小“首个基线”的声明，不称全面 parity。
4. 确认可供系统交互验收的 macOS/硬件环境和操作窗口；涉及权限、剪贴板或外部文件的
   具体操作另行获取授权。人工等待项与可自动推进项分开排期。

## 基线获批后的建议顺序

阶段 A 到交付矩阵为止。以下是阶段 B 的实施建议，不是自动执行指令：

1. **证据与输入前提（D01）**：定位 active/key 激活失败，区分系统拒绝、外部输入
   前提和 host bug；保留失败 trace，建立不覆盖上一轮证据的运行收集方式。
2. **生命周期主链（R03/R04 → R06）**：先通过最新代码的三代恢复回归，再定义并实现
   产品 replacement 触发、授权、候选/rollback 与状态丢失契约；复用退出监督而非
   绕过 development-only guard。R06 完成后复验 N04/N05/N07 在途授权失效。
3. **共享 Flow 行为（F01–F08、R01/R02、D02）**：重用已有代表性样本；优先补
   真正缺失的 unknown-size selected mutation、限定 churn 和当前基线回归，避免
   重复扩展组件排列组合。
4. **真实系统边界（D03–D05、N03–N05）**：安排物理/IME、VoiceOver、Finder、
   通知横幅与 Sandbox 操作；记录外部来源和用户观察，不用合成事件冒充。
5. **并发和耐久性（R05）**：在基础功能与 owner 隔离通过后执行量化负载；补齐
   progress/cancel/retry/resume 的逐操作支持表，排除误称跨 JVM continuation。
6. **冻结候选验收**：同一源码 revision/bundle 身份下运行必选回归和证据审查，
   全量非窗口 smoke 加真实门禁；任何范围内失败阻止基线完成。

## 长期能力扩展清单

### 1. 闭合 Flow 共享能力矩阵

优先补齐仍只有 focused 证据或尚缺代表性覆盖的通用行为：

- 已验证 German-to-US decimal separator 切换及德语必填/malformed/custom Binder
  拒绝恢复之外的 locale、数字/日期时间格式、双向文本、国际化输入和更广 bound-form
  组合；
- lazy/virtualized 数据在已验证 unknown-size append、未选尾部 shrink + `refreshAll`
  reset，以及 exact-count 显式取消选择后删除和相邻项重选之外的 unknown-size
  selected-item deletion、多项事务、任意重复 reset 序列、reorder、fresh-object identity、
  改变排序键/过滤归属的 mutation 和更广分页组合下的状态保持；
- 更广的 picker、menu、overlay、Grid 和 collection-value 交互组合；
- 原生 Edit 命令在单字符撤销/重做与单窗口纯文本 Cut/Paste 之外的编辑历史、
  自动文本替换、跨窗口/非文本剪贴板、native sheet，以及一次 settled 同 ID
  close/reopen 和一次 settled-edit 意外 sidecar 恢复之外的反复 owner churn、在途
  编辑、planned replacement 和多次失败组合；
- 物理输入硬件、touch 和 assistive technology 与现有可信 AppKit 输入证据之间的产品
  边界；
- browser drag-and-drop 与外部 Finder file drop 的明确职责分界；外部文件的独立
  manifest、标准 multipart/Java 摘要及外部操作证据验收面已准备，但真实 Finder
  跨应用拖放仍待通过，不能从同应用自动化或 DOM `isTrusted` 推断来源。

发现失败时继续先分类为 framework 通用缺口、上游/应用语义、验收缺口或 native-host
产品边界；只有通用产品能力缺口才修改 production code。

### 2. 提升运行时耐久性和规模

- 扩展 generated/lazy frontend graph 和复杂资源依赖图覆盖。
- 扩展大传输、并发 fan-out、multi-window 和长时间运行规模。
- 在已验证的 secondary owner close、同公开 ID A→B→C 两次连续重开与
  main-closed/单 secondary recovery 基础上，扩展更高窗口 fan-out、更长时间或更多次
  close/reopen、并发关闭和 recovery/replacement 组合。
- 明确 sidecar replacement 期间不可恢复 request、可重试操作与未来可恢复 continuation
  的边界。
- 扩展 Range、multipart 和 process-external transfer state 等尚未产品化的传输能力。

### 3. 完成 macOS 原生能力产品边界

- 在已闭合的 `window.setTitle` 窗口级 allow/deny、correlation、packaged 源 manifest
  缺省 deny-all，以及显式 permissive manifest package 拒绝策略基础上，补齐 clipboard、
  dialog、notification、window 和 file capability 的统一权限与审计模型。
- 保持 staged manifest resource 缺失时 host startup fatal；product source 的 JSON/mode
  错误已在 build time 拒绝。构建后被引入 staged bundle 的 parser-invalid JSON、schema、
  permission mode 或 window entry 也在 `NSApplication`、sidecar 和窗口创建前以稳定诊断
  退出；host 仅解析一次并复用 immutable snapshot。该 focused 证据不外推为对仍然合法的
  manifest 替换提供签名级防篡改保证。
- FileDialog 的权限拒绝、用户取消和操作错误，以及 Clipboard 的无文本、空文本、权限
  拒绝和操作错误公开 typed outcome 区分已闭合；继续扩展 macOS OS authorization、
  非文本 clipboard 格式和并发 pasteboard 竞争状态覆盖。
- Notification 已提供区分 manifest `DENIED`、OS `OS_DENIED`、
  `AUTHORIZATION_REQUIRED`、`SCHEDULED`、`INVALIDATED` 和 `ERROR` 的公开 typed
  outcome，以及稳定 native error code 和内容/identifier/error trace 脱敏。
  授权/调度 API 错误另保留 allowlisted native domain token 与整数 code，供公开 raw
  result 和 full/compact trace 安全诊断；不按错误文本或诊断码推断用户拒绝或可重试性。
  授权 API 返回错误后只追加一次只读 OS 状态检查；仅新观察为 denied 时按
  `OS_DENIED` 结算，否则保留原 `ERROR`。该异步回调沿用 operation token 失效检查，
  不重试授权或发起调度；状态快照不代表永久授权状态或具体操作者。
  acceptance overlay 的无弹窗策略已有 focused 真实窗口证据：三个 generated AppKit
  点击分别得到空标题错误、需要授权和独立副窗口 manifest 拒绝，公开后台 capability
  调用与 native result/push/可见状态完整关联，OS 状态前后仍未决定。这不代表默认
  production host 的真实授权选择或物理输入证据。
  独立真实允许/拒绝分支也已通过 focused gate：外部操作者选择系统权限，OS 从未决定
  变为 authorized/denied，公开结果为 `SCHEDULED`/`OS_DENIED`，三阶段 scoped
  command/result、Java listener、push 和可见状态一致；Flow 按钮仍是 generated AppKit 输入。
  `SCHEDULED` 仅表示系统接受请求，不是 banner 或送达证据。独立 focused 前台送达
  门禁保留 production nil delegate：同一新 app 先完成公开授权准备调用，再发已授权
  probe，公开结果 ID 的 SHA-256 与真实 Notification Center 留存记录精确匹配；
  dispatch 到查询区间的 active/key 与同步 resign 计数证明未离开前台。两次调用均
  关联 generated AppKit、native command/result、Java listener 和 push；不外推物理
  输入、声音或可靠送达保证，外部横幅可见性仍未确认。测试只对精确匹配的 ID 请求
  best-effort 移除，不提供公开取消保证。继续补齐前台可见
  presentation 和已提交系统请求与 owner/generation 失效竞争的
  产品验证。迟到 add callback 已按原 identifier 做第二次 best-effort 移除并丢弃结果，
  不修改新操作；该分支目前只有确定性 helper 验证，不是实际 OS 移除或原子撤销证据。
  失效结果不能被当作通知未发生或可安全重试的保证。
- path-free 只读 selected-file grant 的有界读取、显式 release、释放后拒绝访问、精确
  owner/scope 与 owner close 回收已闭合 focused 真实窗口证据；公开单次读取上限为
  64 KiB，基础门禁读取 19 字节并验证内容，token/path/filename/content 的 trace 脱敏也有
  正反例覆盖。面板选取与确认来自外部原生 UI，WKWebView 点击来自 generated AppKit
  events，不能外推为物理输入或 Finder 来源验证。成功 security-scope acquisition 与
  release/owner-close 时对应的 stop 路径已覆盖。
  独立 typed grant 范围门禁覆盖真实 65,573 字节非均匀文件：负偏移、零长度、超过
  64 KiB 的请求均返回 `INVALID_RANGE`，随后同一授权完成 64 KiB 整块、短尾块、
  文件末尾及越界 EOF、回退 seek 和释放；未覆盖文件外部变更或并发 read。
  sidecar 异常退出的 focused gate 已验证旧 generation grant 撤销、关闭及 scope stop，
  原双窗口/WKWebView/scope 保留，新 JVM 状态重建后重新选择的独立 grant 可读、可释放并
  拒绝释放后访问；这不是旧 grant 跨进程存活或恢复证据。
  正常 AppKit Quit 的 focused gate 已验证 owner 双窗口仍打开时，grant 撤销、句柄关闭和
  scope stop 先于最终终止回调及 sidecar graceful shutdown；host 以有界异步 drain 等待
  清理。共享 tracker 也追踪已退休 executor 的排队工作，但从退休 executor 接手仍未
  完成 close 的非空 drain 目前仅有合成门禁和测试替身 smoke；下述 settled-write
  owner-close 真实证据不代替该分支。退出由测试专用原生菜单触发标准 AppKit action，
  不代表产品 application menu、物理输入或 Dock 来源验证。
  活跃 token 的跨窗口 read/release 已有独立 focused gate：两端 manifest 权限均允许，
  非 owner 请求仍被拒绝，之后原 owner 成功读取并释放。测试应用只在内存比较实际 token，
  通过公开 raw NativeBridge 调用，避免把 owner-bound typed facade 当作外窗口；这不是
  同一 JVM 内互不信任 Java 代码之间的隔离保证。
  意外退出后的旧 token 重放有独立 focused gate：测试专用私有限权文件交接原 token，
  新 JVM 在保留的原 owner scope 重新捕获 raw bridge，read/release 均命中旧 grant 的
  `sidecar-exit` 退休记录并被拒绝；新选择的不同 token 仍可正常读取和释放。临时交接不是
  产品授权持久化 API，此证据不等同于直接注入 active-map generation mismatch 分支。
  host 还实现 sidecar retirement/replacement 回收，但 focused gate 尚未直接注入这些
  触发路径、更多跨 generation 组合或 in-flight read/release race；unsupported-version
  fatal 的空闲 read grant 已有独立 gate，其他 fatal/forced termination 和超出 drain
  deadline 的阻塞 I/O 仍未证明。当前 ad-hoc host 尚未启用
  App Sandbox。独立验收 clone 已覆盖真实 Sandbox 下只读 Powerbox：外部 runner 创建
  target/sibling，同 host 在选取前实际 open 均返回 EPERM；外部原生面板仅选 target 后，
  typed read 校验字节成功，sibling 仍被拒绝，release 后 token 不可用。三项测试权限为
  app-sandbox、user-selected.read-only、network.client；后者是出站网络权限，并非
  IPC-only。该证据不改变默认产品权限策略，也不代表所有 OS 动态授权在 release 后立即
  消失。继续完成其余失效路径、security-scoped bookmark 持久化与跨进程恢复。
  Sandbox 验证先证明实际策略下 JVM 与 WebKit 子进程启动及 Flow 首帧，再验证容器外
  文件的先拒绝、面板选取后读取；仅签名、Java ready 或窗口创建不构成 enforcement
  证据。验收层 initial-navigation watchdog 只收敛导航未完成的失败，不改变产品
  WebKit 恢复策略，也不单独证明上述 Sandbox 边界。临时 app-local JVM 仅用于验收，
  不属于当前目标排除的 embedded-JRE 分发交付。
- 新建专用写授权提供 path-free `saveNewWriteGrant`、64 KiB 顺序 `write` 和独立
  `releaseWrite` 权限；host 使用 exclusive/no-follow create，已有目标不截断。写入即时
  生效，release 不回滚或删除；已知 partial-write 计数保留，失败授权关闭，不自动 retry。
  独立 Sandbox 门禁覆盖选取前创建被拒绝、真实 Save 面板、完整二进制块与尾块、非法
  offset、释放后拒绝、取消面板和未选 sibling 持续拒绝，外部 runner 独立核对全部字节。
  已有目标/符号链接保护与 partial I/O 目前是文件系统/helper smoke，不是 Sandbox 下
  Replace 面板证据。独立 Sandbox owner-close 门禁已闭合 settled-write 真实窗口证据：main
  先关闭，持有写授权的 secondary 在写入完成后通过公开 Java API 关闭最后窗口，
  不显式 release；独立窗口 executor 的成功关闭回执晚于 owner 的 `windowClosed`，
  终止时两个 context 已退休，外部核对已写字节保留。真实证据覆盖关闭先完成、随后
  空 drain 成功退出；pending-one drain 时序只有合成覆盖，不证明 executor 析构时点
  或在途 I/O。活跃写授权的跨窗口 write/release 也已有真实 Sandbox focused gate：两端
  manifest 均允许操作，owner 首块写入后，main 用同一私有 token 和合法下一 offset
  写入不同字节并尝试 release，均被拒绝；owner 随后仍能写入尾块并释放，外部逐字节
  核对输出与 sibling。两个点击窗口分别捕获公开 raw bridge，不复用 owner-bound typed
  grant；这不是同一 JVM 内互不信任 Java 代码或 OS 级窗口隔离保证。
  Settled write 的意外 JVM 退出已有真实 Sandbox focused gate，标准 production bundle
  与 opaque IPC 下保留双窗口/WebView/scope，完成全部写入但不释放旧授权后终止精确
  JVM；旧授权在恢复调度前失效，异步 descriptor
  close 和 scope stop 在验收前成功完成。新 JVM 的 Java 状态归零，另一真实 Save 选取
  独立新目标，从 offset 0 写入并释放，外部核对两份完整输出与 sibling。关联按 generation
  分隔，允许旧 close 晚于新 JVM ready，但该迟到时序只有合成覆盖。应用按钮使用 generated
  AppKit event，真实 Save 面板由外部 Computer Use 操作；不是物理/AT 输入、旧 token
  恢复/重放、在途写恢复或 fsync 保证。
  写授权旧 token 的恢复后重放已有真实 Sandbox focused gate：标准 production bundle
  与 opaque IPC 下，私有容器内有界交接原 token/PID，在保留的原 owner scope 提交合法
  next-offset write 和 release，权限允许但命中原授权的 sidecar-exit 退休记录而被拒绝；
  新授权完成写入/释放/释放后拒绝，外部逐字节核对两份输出和未改变的 sibling。
  14 个应用操作使用 generated AppKit event，两次真实 Save 由外部 Computer Use 操作。
  交接只属于测试应用，不是产品持久化或直接注入 active-map generation mismatch。
  正常 AppKit Quit 下的 settled-write Sandbox 已有真实窗口 focused gate：标准 bundle
  与 opaque IPC 下保持双窗口及活跃授权、不 release，外部 Computer Use 操作 Save 和
  测试专用 AppKit Quit；单项清理 drain 成功完成后，当前契约要求 JVM 实际退出、
  graceful receipt、willTerminate 依次发生，结束后独立核对完整输出与 sibling。
  准备时 transport 已平衡；后续 UI 流量
  可止于合法因果前缀，但不得越过 invalidation fence，截断边界只有合成覆盖。
  不能以 owner-close 替代 Quit，也不代表产品 application menu、Dock Quit、物理输入
  或在途 I/O 证据。
  显式 `cancel()` 使用独立 `fileGrant.cancelWrite` 权限，关闭后续块接纳并在 OS write
  attempt 之间协作式检查取消；已越过检查点的 attempt 仍可完成。取消关闭等待真实
  prefix 在主线程发布，返回最终总 offset；未知结果不能解释为零字节。重复确认取消
  在 Java facade 本地结算，不重发 host 命令。独立 Sandbox 入口覆盖 settled-write
  取消、最终 prefix、本地重复和取消后拒绝；在途部分写入、EINTR、完成/错误优先级及
  close 屏障由确定性 helper smoke 验证，不是实际阻塞 syscall 的取消证据。
  继续闭合更广的写授权 owner-close 竞态、Quit/replacement 与 in-flight race、
  长时间/并发写入、覆盖保存、事务 commit/rollback、progress/retry/resume 和持久化边界。
- 为适用的原生能力补齐更完整的 progress、cancellation、retry 和 resume 契约。
- 最后窗口的公开 Java close 路径有独立 focused gate：generated AppKit 点击、
  Java listener/UIDL 准备状态、native command/result、窗口移除、空资源 drain 和
  graceful shutdown。资源清理及其 deadline 使用 default/modal 主 run loop，避免
  主 dispatch 回调内进入 AppKit 延迟退出时饥饿；pending worker、重复回调和超时由
  modal-loop smoke 覆盖，不外推为真实阻塞文件 I/O 或所有退出竞态已闭合。
- 基础 App/Edit 菜单使用标准 nil-target AppKit action。两个保留 WebView 的原生
  Undo/Redo owner 隔离已有 focused gate：通过公开 window API 打开副窗口，交错
  native 输入/撤销/重做，两窗各保留三次 client Java 变更与独立历史，完整 scope
  关联各自 UIDL，返回主窗口不产生额外编辑。测试通过公开 TextField API 关闭
  autocorrect/autocapitalize，并检查实际 input 属性；不把失焦自动替换当作隔离失败。
  单窗口纯文本 Cut/Paste 也有独立 focused gate：真实全选效果、可信 clipboard/input
  事件、三次 Java 变更与各自 UIDL、Cut 后原生剪贴板 epoch、Paste 保持该 epoch，
  以及原始数据表示的有界核验恢复。验收不预置剪贴板，读取失败不当作空内容，
  未确定归属或检测到竞争时不覆盖；仅是受控桌面上的 best-effort 保存，不承诺原子
  排他、进程崩溃恢复或 lazy provider 身份。输入证据为 generated AppKit event，
  不是物理硬件。继续验证 native sheet、复杂编辑器、跨窗口/非文本剪贴板、自动替换
  和更广的编辑归属。独立同 ID close/reopen gate 通过公开 open/close/open，区分
  close result 的请求接受与实际窗口退休；新 context/window/WebView/session 的空
  Undo 在至少 1000ms、三次 quiet 观察中不产生 input/Java 变化，再验证新历史及
  原主窗口历史。七次真实变更各有完整 scope 关联的 post-key UIDL，关闭命令也受
  主窗口 UIDL 因果区间约束；不为 no-op 制造状态变更，不把旧对象引用保留当作
  析构或 undo-manager 销毁证明。独立 settled-edit recovery gate 验证同一双窗口/WebView
  和 opaque scope 跨一次精确旧 JVM 退出保留，两个文档实际换代，副窗口直接恢复固定
  编辑 route、主窗口回原始根页；空撤销有界无变化后，新 insert/Undo/Redo 正常工作。
  四次实际编辑按 generation 加完整 scope 关联各自 UIDL，不以恢复 ready 代替 DOM ready，
  不把原生对象保留当作旧 Java 值/undo 历史保留。仍需验证反复 owner churn、在途编辑、
  planned replacement 和多次恢复。
  仅在 native event 与 Java callback
  correlation 契约明确后，扩展完整可定制 application menu 等更高层原生模型。

## 完成门槛

阶段 A 完成按 `docs/goal.md` 的矩阵交付条件判定；阶段 B 完成按获批的有限基线判定。
两者均不等于下面的长期全面产品 parity 完成。

只有以下条件同时满足，才能宣布 macOS 全面产品功能 parity 完成：

- 代表性组件族和行为族的共享能力矩阵已闭合，且没有组件专用 production 例外；
- 所声明能力均通过真实 WKWebView、标准 production bundle、官方 Flow client/server
  和 opaque IPC 得到端到端验证；
- framework、IPC 和 native-host production code 中不存在 fixture、组件名称、DOM
  标识或自动化逻辑泄漏；
- 原生权限、文件授权、生命周期和失败行为达到产品安全要求；
- focused validation、browser-programmatic setup、compatibility transport 和尚未覆盖
  的真实产品边界均被明确披露。

单个组件 fixture 或 focused gate 只能闭合其代表的共享能力 slice，不能单独证明全面
parity。

## 延后方向

### 开发体验

状态：延后，不阻塞当前 completion gate。

- JVM 原地 class 热替换和更完整的上游 Vaadin DevTools 集成；
- 更广的 Maven source-root 与构建模型观察；
- rebuild feedback、diagnostics 和开发态 state preservation 改进。

这些能力不得向 pure Flow 主线重新引入 Servlet UI transport。

### 分发与更新

状态：延后，不阻塞当前 completion gate。

- Embedded JRE、installer 和安装布局；
- production signing/notarization；
- package size、startup 和 memory 基线；
- update、rollback 和 release automation。

### 跨平台

状态：远期方向，尚未排期。

- Windows 和 Linux 只有成为 active product direction 后，才定义与 macOS 等价的
  native-host contract。
- 不创建只用于表达未来意图的 placeholder platform module。

不要为路线图事项创建版本化 goal 文档或过程归档。长期实现状态进入 runtime 文档，
稳定验收契约进入 acceptance 文档，单轮证据留在 git history、CI artifact、release
asset 或最终汇报中。
