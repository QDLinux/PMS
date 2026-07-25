# 个人事务管理系统 API 文档

## 概述

本系统提供RESTful风格的API接口，用于个人事务管理，包括用户认证、账户管理、记账管理、规划管理、目标管理、数据总览和AI助手等功能。

**基础URL**: `http://localhost:8080`

**认证方式**: 除 `/api/auth/**` 外，其他接口需要在请求头中携带JWT Token：
```
Authorization: Bearer <token>
```

**统一响应格式**:
```json
{
  "code": 200,
  "message": "操作成功",
  "data": { ... }
}
```

---

## 一、认证接口 (Auth)

### 1.1 用户注册

**请求**
```http
POST /api/auth/register
Content-Type: application/json
```

**请求体**
```json
{
  "username": "user01",
  "password": "123456"
}
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 是 | 用户名，长度4-50位 |
| password | String | 是 | 密码，长度6-50位 |

**响应示例**
```json
{
  "code": 200,
  "message": "注册成功",
  "data": null
}
```

---

### 1.2 用户登录

**请求**
```http
POST /api/auth/login
Content-Type: application/json
```

**请求体**
```json
{
  "username": "admin",
  "password": "admin123456"
}
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 是 | 用户名 |
| password | String | 是 | 密码 |

**响应示例**
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 86400000,
    "username": "admin"
  }
}
```

---

## 二、账户管理接口 (Account)

### 2.1 获取当前用户信息

**请求**
```http
GET /api/account/me
Authorization: Bearer <token>
```

**响应示例**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "username": "admin",
    "nickname": "Admin",
    "email": "admin@example.com",
    "avatarUrl": "/api/account/me/avatar"
  }
}
```

**返回字段说明**
| 字段 | 类型 | 说明 |
|------|------|------|
| username | String | 用户名 |
| nickname | String | 昵称 |
| email | String | 邮箱 |
| avatarUrl | String | 头像访问路径 |

---

### 2.2 修改昵称

**请求**
```http
PUT /api/account/me/nickname
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体**
```json
{
  "nickname": "新昵称"
}
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| nickname | String | 是 | 新昵称，长度不超过50 |

---

### 2.3 修改用户名

**请求**
```http
PUT /api/account/me/username
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体**
```json
{
  "username": "newusername"
}
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| username | String | 是 | 新用户名，长度4-50位 |

**响应示例**
```json
{
  "code": 200,
  "message": "用户名更新成功，请重新登录",
  "data": { ... }
}
```

---

### 2.4 修改邮箱

**请求**
```http
PUT /api/account/me/email
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体**
```json
{
  "email": "newemail@example.com"
}
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| email | String | 是 | 新邮箱，需符合邮箱格式，长度不超过100 |

---

### 2.5 上传头像

**请求**
```http
POST /api/account/me/avatar
Authorization: Bearer <token>
Content-Type: multipart/form-data
```

**参数**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | MultipartFile | 是 | 头像图片文件 |

---

### 2.6 获取头像

**请求**
```http
GET /api/account/me/avatar
Authorization: Bearer <token>
```

**响应**: 返回头像图片文件。若未上传过头像，返回 404。

---

### 2.7 修改密码

**请求**
```http
PUT /api/account/me/password
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体**
```json
{
  "oldPassword": "old123456",
  "newPassword": "new123456"
}
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| oldPassword | String | 是 | 旧密码 |
| newPassword | String | 是 | 新密码，长度6-50位 |

---

## 三、记账管理接口 (Accounting)

### 3.1 创建记账记录

**请求**
```http
POST /api/accounting
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体**
```json
{
  "type": "INCOME",
  "amount": 5000.00,
  "category": "工资",
  "accountDate": "2024-01-15T10:30:00",
  "note": "月工资"
}
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | TransactionType | 是 | 交易类型：INCOME(收入) / EXPENSE(支出) |
| amount | BigDecimal | 是 | 金额，必须大于0.01 |
| category | String | 是 | 分类，长度不超过50 |
| accountDate | LocalDateTime | 是 | 记账日期 (ISO格式: 2024-01-15T10:30:00) |
| note | String | 否 | 备注，长度不超过500 |

**响应示例**
```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "id": 1,
    "ownerId": 1,
    "type": "INCOME",
    "amount": 5000.00,
    "category": "工资",
    "accountDate": "2024-01-15T10:30:00",
    "note": "月工资",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

---

### 3.2 更新记账记录

**请求**
```http
PUT /api/accounting/{id}
Authorization: Bearer <token>
Content-Type: application/json
```

**路径参数**
| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 记账记录ID |

**请求体**
```json
{
  "type": "EXPENSE",
  "amount": 150.00,
  "category": "餐饮",
  "accountDate": "2024-01-16T12:00:00",
  "note": "午餐"
}
```

---

### 3.3 删除记账记录

**请求**
```http
DELETE /api/accounting/{id}
Authorization: Bearer <token>
```

**路径参数**
| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 记账记录ID |

---

### 3.4 获取记账记录详情

**请求**
```http
GET /api/accounting/{id}
Authorization: Bearer <token>
```

**路径参数**
| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 记账记录ID |

---

### 3.5 查询记账记录列表

**请求**
```http
GET /api/accounting
Authorization: Bearer <token>
```

**查询参数 (可选)**
| 参数 | 类型 | 说明 |
|------|------|------|
| startDate | LocalDate | 开始日期 (ISO格式: 2024-01-01) |
| endDate | LocalDate | 结束日期 (ISO格式: 2024-01-31) |

> 同时提供 `startDate` 和 `endDate` 时按日期范围筛选；都不提供时返回当前用户全部记录。

**示例**
```http
GET /api/accounting?startDate=2024-01-01&endDate=2024-01-31
```

---

## 四、规划管理接口 (Plan)

### 4.1 创建计划事项

**请求**
```http
POST /api/plans
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体**
```json
{
  "title": "完成项目报告",
  "description": "撰写项目报告的各个章节",
  "startDate": "2024-01-15",
  "endDate": "2024-01-30",
  "priority": "HIGH",
  "status": "TODO"
}
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | String | 是 | 标题，长度不超过100 |
| description | String | 否 | 描述，长度不超过1000 |
| startDate | LocalDate | 否 | 开始日期 |
| endDate | LocalDate | 否 | 截止日期 |
| priority | PlanPriority | 是 | 优先级：LOW / MEDIUM / HIGH |
| status | PlanStatus | 是 | 状态：TODO / IN_PROGRESS / DONE / ENDED |

**响应示例**
```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "id": 1,
    "title": "完成项目报告",
    "description": "撰写项目报告的各个章节",
    "startDate": "2024-01-15",
    "endDate": "2024-01-30",
    "priority": "HIGH",
    "status": "TODO",
    "createdAt": "2024-01-15T10:30:00",
    "updatedAt": "2024-01-15T10:30:00"
  }
}
```

---

### 4.2 更新计划事项

**请求**
```http
PUT /api/plans/{id}
Authorization: Bearer <token>
Content-Type: application/json
```

**路径参数**
| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 计划事项ID |

**请求体** 同 4.1 创建请求体

---

### 4.3 删除计划事项

**请求**
```http
DELETE /api/plans/{id}
Authorization: Bearer <token>
```

**路径参数**
| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 计划事项ID |

---

### 4.4 获取计划事项详情

**请求**
```http
GET /api/plans/{id}
Authorization: Bearer <token>
```

**路径参数**
| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 计划事项ID |

---

### 4.5 查询计划事项列表

**请求**
```http
GET /api/plans
Authorization: Bearer <token>
```

**查询参数 (可选)**
| 参数 | 类型 | 说明 |
|------|------|------|
| status | PlanStatus | 筛选状态：TODO / IN_PROGRESS / DONE / ENDED |

> 不传 `status` 时返回当前用户全部计划事项。

**示例**
```http
GET /api/plans?status=TODO
```

---

## 五、目标管理接口 (Goal)

### 5.1 创建目标

**请求**
```http
POST /api/goals
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体**
```json
{
  "name": "储蓄目标",
  "description": "年底前储蓄5万元",
  "targetAmount": 50000.00,
  "currentAmount": 10000.00,
  "startDate": "2024-01-01",
  "deadline": "2024-12-31"
}
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | String | 是 | 目标名称，长度不超过100 |
| description | String | 否 | 描述，长度不超过500 |
| targetAmount | BigDecimal | 是 | 目标金额，必须大于0.01 |
| currentAmount | BigDecimal | 是 | 当前金额，不能小于0 |
| startDate | LocalDate | 否 | 开始日期 |
| deadline | LocalDate | 否 | 截止日期 |

**响应示例**
```json
{
  "code": 200,
  "message": "创建成功",
  "data": {
    "id": 1,
    "name": "储蓄目标",
    "description": "年底前储蓄5万元",
    "targetAmount": 50000.00,
    "currentAmount": 10000.00,
    "startDate": "2024-01-01",
    "deadline": "2024-12-31",
    "status": "NOT_STARTED",
    "createdAt": "2024-01-01T00:00:00",
    "updatedAt": "2024-01-01T00:00:00"
  }
}
```

---

### 5.2 更新目标

**请求**
```http
PUT /api/goals/{id}
Authorization: Bearer <token>
Content-Type: application/json
```

**路径参数**
| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 目标ID |

**请求体** 同 5.1 创建请求体

---

### 5.3 更新目标进度

**请求**
```http
PATCH /api/goals/{id}/progress
Authorization: Bearer <token>
Content-Type: application/json
```

**路径参数**
| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 目标ID |

**请求体**
```json
{
  "increment": 1000.00
}
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| increment | BigDecimal | 是 | 进度增量，必须大于0.01 |

> 增量会累加到 `currentAmount` 上，系统自动判断目标是否达成。

---

### 5.4 删除目标

**请求**
```http
DELETE /api/goals/{id}
Authorization: Bearer <token>
```

**路径参数**
| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 目标ID |

---

### 5.5 获取目标详情

**请求**
```http
GET /api/goals/{id}
Authorization: Bearer <token>
```

**路径参数**
| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 目标ID |

---

### 5.6 查询目标列表

**请求**
```http
GET /api/goals
Authorization: Bearer <token>
```

**查询参数 (可选)**
| 参数 | 类型 | 说明 |
|------|------|------|
| status | GoalStatus | 筛选状态：NOT_STARTED / ONGOING / DONE / ENDED / ACHIEVED / EXPIRED |

> 不传 `status` 时返回当前用户全部目标。

**示例**
```http
GET /api/goals?status=ONGOING
```

---

## 六、数据总览接口 (Dashboard)

### 6.1 获取数据汇总

**请求**
```http
GET /api/dashboard/summary
Authorization: Bearer <token>
```

**响应示例**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "monthIncome": 10000.00,
    "monthExpense": 3500.00,
    "monthBalance": 6500.00,
    "totalPlans": 10,
    "donePlans": 6,
    "totalGoals": 3,
    "achievedGoals": 1
  }
}
```

**返回字段说明**
| 字段 | 类型 | 说明 |
|------|------|------|
| monthIncome | BigDecimal | 本月收入 |
| monthExpense | BigDecimal | 本月支出 |
| monthBalance | BigDecimal | 本月结余 |
| totalPlans | long | 计划总数 |
| donePlans | long | 已完成计划数 |
| totalGoals | long | 目标总数 |
| achievedGoals | long | 已达成目标数 |

---

## 七、AI 助手接口 (AI)

### 7.1 发送对话消息

**请求**
```http
POST /api/ai/chat
Authorization: Bearer <token>
Content-Type: application/json
```

**请求体**
```json
{
  "message": "我本月花了多少钱？",
  "sessionId": "uuid-xxxx",
  "context": "{\"monthIncome\":10000,\"monthExpense\":3500}"
}
```

**参数说明**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| message | String | 是 | 用户消息内容 |
| sessionId | String | 否 | 会话ID，不传则创建新会话 |
| context | String | 否 | 前端传入的用户数据上下文(JSON字符串) |

**响应示例**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "answer": "本月您的支出为3500元，收入为10000元，净结余6500元。",
    "model": "deepseek-chat",
    "sessionId": "uuid-xxxx"
  }
}
```

**返回字段说明**
| 字段 | 类型 | 说明 |
|------|------|------|
| answer | String | AI 回复内容 |
| model | String | 使用的模型名称 |
| sessionId | String | 会话ID |

---

### 7.2 查询历史消息

**请求**
```http
GET /api/ai/history?sessionId=uuid-xxxx&limit=100
Authorization: Bearer <token>
```

**查询参数**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| sessionId | String | 否 | 会话ID，不传则返回全部会话的历史 |
| limit | int | 否 | 最大返回条数，默认100 |

**响应示例**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "role": "user",
      "text": "我本月花了多少钱？",
      "time": "2024-01-15 10:30:00"
    },
    {
      "role": "assistant",
      "text": "本月您的支出为3500元，收入为10000元，净结余6500元。",
      "time": "2024-01-15 10:30:02"
    }
  ]
}
```

**返回字段说明**
| 字段 | 类型 | 说明 |
|------|------|------|
| role | String | 角色：user(用户) / assistant(AI助手) |
| text | String | 消息内容 |
| time | String | 消息时间 |

---

### 7.3 查询会话列表

**请求**
```http
GET /api/ai/sessions?limit=50
Authorization: Bearer <token>
```

**查询参数**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| limit | int | 否 | 最大返回条数，默认50 |

**响应示例**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": [
    {
      "sessionId": "uuid-xxxx",
      "title": "聊天 2024-01-15 10:30",
      "updatedAt": "2024-01-15 10:35:00"
    }
  ]
}
```

**返回字段说明**
| 字段 | 类型 | 说明 |
|------|------|------|
| sessionId | String | 会话ID |
| title | String | 会话标题 |
| updatedAt | String | 最后更新时间 |

---

### 7.4 新建会话

**请求**
```http
PUT /api/ai/sessions/new?title=个人理财咨询
Authorization: Bearer <token>
```

**查询参数**
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | String | 否 | 会话标题，不传则自动生成 |

**响应示例**
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "sessionId": "uuid-new",
    "title": "个人理财咨询",
    "updatedAt": "2024-01-15 10:40:00"
  }
}
```

---

### 7.5 删除会话

**请求**
```http
DELETE /api/ai/sessions/{sessionId}
Authorization: Bearer <token>
```

**路径参数**
| 参数 | 类型 | 说明 |
|------|------|------|
| sessionId | String | 会话ID |

---

## 附录

### 枚举值

**交易类型 (TransactionType)**
| 值 | 说明 |
|----|------|
| INCOME | 收入 |
| EXPENSE | 支出 |

**计划优先级 (PlanPriority)**
| 值 | 说明 |
|----|------|
| LOW | 低 |
| MEDIUM | 中 |
| HIGH | 高 |

**计划状态 (PlanStatus)**
| 值 | 说明 |
|----|------|
| TODO | 待办 |
| IN_PROGRESS | 进行中 |
| DONE | 已完成 |
| ENDED | 已结束 |

**目标状态 (GoalStatus)**
| 值 | 说明 |
|----|------|
| NOT_STARTED | 未开始 |
| ONGOING | 进行中 |
| DONE | 已完成 |
| ACHIEVED | 已达成 |
| EXPIRED | 已过期 |
| ENDED | 已取消 |

---

## 错误响应

**错误响应格式**
```json
{
  "code": 400,
  "message": "错误信息",
  "data": null
}
```

**常见错误码**
| 错误码 | 说明 |
|--------|------|
| 200 | 成功 |
| 400 | 请求参数错误 |
| 401 | 未授权(登录过期或未登录) |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
