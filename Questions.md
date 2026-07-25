# 毕业设计答辩问题与回答

---

## 1. AI 辅助用的哪个 AI？

调用的是 **DeepSeek 大模型**（`deepseek-chat`），通过 OpenAI 兼容接口接入，具体请求地址为：

```
https://api.deepseek.com/v1/chat/completions
```

API Key 和 Base URL 均在 `src/main/resources/application.yml` 中 `app.ai` 配置节下声明：

| 配置项           | 值                                           |
| ---------------- | -------------------------------------------- |
| `base-url`       | `https://api.deepseek.com`                   |
| `model`          | `deepseek-chat`                              |
| `temperature`    | `0.5`                                        |
| `max-tokens`     | `2000`                                       |
| `timeout-ms`     | `30000`                                      |

后端在 `AiProperties.java` 中通过 `@ConfigurationProperties(prefix = "app.ai")` 读取配置，使用 Java 11 原生 `java.net.http.HttpClient` 发送 POST 请求，不依赖任何第三方 SDK。

---

## 2. JWT 用在哪块地方？

JWT 用于整个系统的**身份认证与授权**，涉及以下核心文件：

| 文件                                 | 职责                                                     |
| ------------------------------------ | -------------------------------------------------------- |
| `JwtService.java`                    | 使用 JJWT 库，提供 Token 生成、用户名提取、有效性校验    |
| `JwtAuthenticationFilter.java`       | 继承 `OncePerRequestFilter`，每个请求拦截校验 JWT          |
| `SecurityConfig.java`                | 安全配置，设定白名单路径，插入 JWT 过滤器，会话策略设为 STATELESS |
| `AuthService.java`                   | 登录成功时调用 `generateToken()` 生成 Token                |
| `CustomUserDetailsService.java`      | 从数据库加载用户，统一分配 `ROLE_USER` 权限                |

**Token 提取方式（两种）：**

- `Authorization` 请求头：`Bearer <token>`
- Cookie：`PM_TOKEN`

**白名单路径（无需认证）：**

- `/`、`/index.html`、`/dashboard.html`
- `/app/**`、`/assets/**`、`/favicon.ico`
- `/api/auth/**`
- `/error`

**前端处理（`http.js`）：**

- Token 以 `pm_token` 为 key 存储在 `localStorage`
- 每次请求自动附加 `Authorization: Bearer <token>`
- 有效期：86400000ms（24 小时）

---

## 3. AI 小助手是什么？怎么实现的？

AI 小助手是基于 **OpenAI 兼容 Tool Calling（函数调用）机制**的智能对话引擎，位于 `src/main/java/com/example/personalmanager/ai/` 目录，共 9 个文件。

### 目录结构

```
ai/
├── config/
│   └── AiProperties.java              # 配置读取
├── controller/
│   └── AiAssistantController.java     # REST API
├── dto/
│   ├── AiChatRequest.java
│   ├── AiChatResponse.java
│   ├── AiChatSessionResponse.java
│   └── AiChatHistoryItemResponse.java
└── service/
    ├── AiAssistantService.java         # 核心对话引擎
    ├── AiToolExecutor.java             # 工具执行器
    └── AiChatLogService.java           # 日志与历史管理
```

### API 端点

| 方法     | 路径                                | 功能       |
| -------- | ----------------------------------- | ---------- |
| `POST`   | `/api/ai/chat`                      | 发送消息   |
| `GET`    | `/api/ai/history`                   | 获取历史   |
| `GET`    | `/api/ai/sessions`                  | 会话列表   |
| `PUT`    | `/api/ai/sessions/new`              | 新建会话   |
| `DELETE` | `/api/ai/sessions/{sessionId}`      | 删除会话   |

### 对话流程

1. 构建 `messages` 列表（system prompt + 当前日期 + 用户消息 + 上下文）
2. 循环最多 **5 轮**（`MAX_TOOL_ROUNDS = 5`）
3. 每轮向 DeepSeek API 发送请求：
   - 若响应包含 `tool_calls` → 调用 `AiToolExecutor` 执行对应工具 → 结果追加到 messages → 继续下一轮
   - 若无 `tool_calls` → 提取 `content` 作为最终回答
4. 记录日志和聊天历史

### 4 个预定义 Tool

| 工具名称                         | 功能描述                     |
| -------------------------------- | ---------------------------- |
| `queryAccountingRecords`         | 按时间/类型查询账单明细      |
| `getAccountingMonthlySummary`    | 月度收支汇总统计             |
| `queryPlans`                     | 按状态/优先级查询待办计划    |
| `queryGoals`                     | 查询储蓄目标达成进度         |

---

## 4. AI 助手怎么实现个性化？

个性化从**两个层面**实现：

### 第一层：系统提示词（System Prompt）

在 `application.yml` 的 `app.ai.system-prompt` 中配置助手定位，设定为"个人财务与事务管理助手"，每次请求附加上下文日期，AI 据此理解当前情境。

### 第二层：用户数据隔离

4 个预定义 Tool 在执行时：

1. 通过 `SecurityContextHolder` 获取当前登录用户的 `username`（JWT 认证后设置）
2. 调用对应 Service 层方法查询该用户的**专属数据**（记账、计划、目标）
3. 不同用户调用同一 Tool 返回各自数据，模型基于实际信息生成针对性分析

聊天历史按用户名分目录存储：

```
logs/ai-chat/history/<用户名>.jsonl
```

确保不同用户的对话上下文完全隔离。

---

## 5. 这些图的生成用的是什么插件？

**未使用任何第三方图表库**（如 ECharts、Chart.js）。

`frontend/package.json` 中仅有一个运行时依赖：

```json
"dependencies": {
  "vue": "^3.5.13"
}
```

### 实现方式

| 图表类型     | 实现方式                                                                                                                                             |
| ------------ | ---------------------------------------------------------------------------------------------------------------------------------------------------- |
| 环形进度条   | `RingProgress.vue` 组件，使用 SVG `<circle>` 元素，`stroke-dasharray` + `stroke-dashoffset` 属性手工计算渲染                                         |
| 统计卡片     | 纯 CSS flex/grid 布局，通过颜色区分展示                                                                                                              |
| AI 分析文本  | Markdown 格式化文本，前端解析渲染为 HTML                                                                                                             |

---

## 6. 分类是自己输入，还是固定的？

### Java 枚举（固定值，下拉选择）

| 枚举类             | 值                                                                    |
| ------------------ | --------------------------------------------------------------------- |
| `PlanPriority`     | `LOW`（低）、`MEDIUM`（中）、`HIGH`（高）                               |
| `PlanStatus`       | `TODO`（待开始）、`IN_PROGRESS`（进行中）、`DONE`（已完成）、`ENDED`（已结束） |
| `GoalStatus`       | `NOT_STARTED`、`ONGOING`、`DONE`、`ENDED`、`ACHIEVED`、`EXPIRED`       |
| `TransactionType`  | `INCOME`（收入）、`EXPENSE`（支出）                                     |

均通过前端下拉选择框选取，**不允许自由输入**。前端通过 `PlansModule.vue` 中的映射函数将枚举转为中文显示。

### 记账分类（固定预设选项）

`category` 字段为 `String` 类型，前端提供预设下拉选项（餐饮、交通、购物、工资、理财等），同样不开放自由输入。

---

## 7. 保存记录后调用了哪个页面？

保存记录（记账、计划、目标）后有两种处理方式：

- **同页刷新**：在列表页组件中直接调用数据刷新方法，重新从后端拉取最新数据并更新视图
- **路由跳转**：编辑/新增页面提交成功后，通过 Vue Router（`router.push` / `router.back`）返回列表展示页面

整个过程基于 Vue 3 响应式数据绑定机制，无需浏览器整页刷新，实现 SPA（单页应用）式流畅体验。

---

## 8. DTO、Entity 是做什么的？

### Entity（实体）

- 使用 JPA 注解 `@Entity`、`@Table` 直接映射数据库表

| Entity               | 对应表               | 用途       |
| -------------------- | -------------------- | ---------- |
| `SysUser`            | `sys_user`           | 用户账户   |
| `AccountingRecord`   | `accounting_record`  | 记账记录   |
| `PlanItem`           | `plan_item`          | 计划事项   |
| `Goal`               | `goal`               | 储蓄目标   |

- 通过 `@Id`、`@GeneratedValue`、`@Column` 定义主键和字段映射
- `@PrePersist` / `@PreUpdate` 自动设置时间戳
- **仅用于 Repository 持久层**，不直接暴露给 Controller

### DTO（数据传输对象）

- 使用 Lombok `@Data` + `@Builder` 注解
- **Request DTO**（如 `PlanItemRequest`）带有 `@NotBlank`、`@NotNull` JSR-303 校验
- **Response DTO**（如 `DashboardSummaryResponse`）用于格式化返回数据，可聚合和裁剪字段
- Service 层负责 Entity ↔ DTO 转换，实现**持久层与接口层解耦**

---

## 9. 日志存储在哪，调用 AI 时能看到所有日志吗？

### 系统级日志（Logback）

| 输出通道   | 路径/说明                                                                    |
| ---------- | ---------------------------------------------------------------------------- |
| 控制台     | 实时输出，格式：`yyyy-MM-dd HH:mm:ss.SSS [线程] [级别] 类名 - 消息`           |
| 文件滚动   | `logs/system/system.log`                                                     |

- 单文件最大 20MB，按日期滚动，保留 30 天，总容量上限 1GB
- root 级别 `INFO`，`com.example.personalmanager` 包级别 `DEBUG`
- JPA SQL 日志已开启（`show-sql: true`）

### AI 模块独立日志

| 内容         | 路径                                              |
| ------------ | ------------------------------------------------- |
| 请求日志     | `logs/ai-chat/*.log`（按天拆分）                   |
| 聊天历史     | `logs/ai-chat/history/<用户名>.jsonl`（JSONL 格式）|
| 会话元数据   | `logs/ai-chat/history/<用户名>-sessions.json`      |

**结论：** 调用 AI 对话时可查看完整的 API 请求参数（含所有轮次的 tool_calls）、工具执行结果、最终回答内容，所有日志均可追溯。

---

## 10. 高低优先级有什么区别？

后端 `PlanPriority` 枚举定义 `LOW` / `MEDIUM` / `HIGH` 三个级别，区别体现在：

| 维度     | HIGH（高）                                                     | MEDIUM（中）      | LOW（低）          |
| -------- | -------------------------------------------------------------- | ----------------- | ------------------ |
| 样式     | 红色高亮（CSS class `high`）                                   | 默认色/橙色       | 灰色               |
| 排序     | 权重 1，排在最前                                               | 权重 2，中间位置  | 权重 3，排在最后   |
| 提醒     | ≥3 个未完成时触发提醒："建议1核心+2辅助的日计划节奏"            | 不触发            | 不触发             |
| AI 分析  | AI 工具查询时返回 priority 字段，系统提示词指导模型优先关注高优先级 | —                 | —                  |
| 筛选     | 支持按优先级独立筛选                                           | 支持              | 支持               |

---

## 11. AI API 回复是 Markdown 格式，有较多符号不便于直接阅读，如何解决？

DeepSeek API 返回的 `content` 字段本身为 Markdown 格式（含标题 `#`、列表 `-`、粗体 `**`、表格 `|` 等语法）。系统在前端做了解析渲染：

- 后端不作格式转换，直接以原始 Markdown 文本返回
- 前端 AI 对话页面中，将 Markdown 文本通过自定义解析函数转换为 HTML 渲染
- 解析逻辑位于 `frontend/src` 构建产物中的 `dashboard-ai.js`：识别标题行（`#` 开头）、无序列表（`-` 或 `*` 开头）、有序列表（数字 + `.`/`、` 开头）、表格（含 `|` 分隔符）等模式，映射为对应的 HTML 标签（`h1`-`h5`、`ul`/`ol`/`li`、`table`/`thead`/`tbody`/`tr`/`th`/`td`），最终通过 `innerHTML` 渲染

---

## 12. 启动项目后访问 `localhost:8080` 直接进入主页而非登录页，原因是什么？

这是系统设计的"**自动登录**"行为，具体原理如下：

### 触发链

```
index.html 加载 → auth.js mounted() → tryAutoLogin() → 读取 localStorage 中的 pm_token → 请求后端验证 → 成功则跳转主页
```

### 关键代码路径

| 文件               | 行号    | 作用                                                         |
| ------------------ | ------- | ------------------------------------------------------------ |
| `auth.js`          | L103    | Vue `mounted()` 钩子自动调用 `tryAutoLogin()`                  |
| `auth.js`          | L40-47  | 从 `localStorage` 读取 token → 请求 `/api/dashboard/summary` 验证 → 成功执行 `window.location.href` 跳转主页 |
| `api.js`           | L42-52  | `setAuth()` 接收了 `expiresInSeconds` 参数但**完全未使用**，Token 无前端主动过期清理机制 |

### 结论

只要浏览器 `localStorage` 中存有上次登录的有效 JWT，访问 `localhost:8080` 就会被无缝带入主页，跳过登录界面。这是设计上的便利性设计，而非 Bug。但由于 `setAuth()` 未利用过期时间参数，Token 会在 localStorage 中长时间驻留，导致用户感知不到登录流程的存在。
