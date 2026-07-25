# 个人事务后台管理系统 (Spring Boot + Security + JWT)

功能：
- 记账管理（收入/支出）
- 规划管理（计划事项）
- 目标管理（目标金额与进度）
- 总览统计（本月收支、计划完成、目标达成）
- 用户认证（Spring Security + JWT）
- 用户与数据隔离（每个用户只能访问自己的数据）

## 技术栈
- Java 17
- Spring Boot 3.3.x
- Spring Security
- JWT (jjwt)
- Spring Data JPA
- MySQL (目标环境：5.5.20)

## 数据库准备（MySQL 5.5.20）
1. 创建数据库：

```sql
CREATE DATABASE personal_manager DEFAULT CHARACTER SET utf8;
```

2. 执行初始化脚本：
- 脚本路径：`src/main/resources/db/init_mysql.sql`
- 包含：表结构、索引、默认管理员账号

默认管理员：
- username: `admin`
- password: `admin123456`

3. 修改 `src/main/resources/application.yml` 里的数据库账号密码。

## 运行
```bash
mvn spring-boot:run
```

## Vue3 前端重构（Vite）

已新增独立前端工程：`frontend/`，用于逐步替换原 `src/main/resources/static` 页面。

### 前端开发启动
```bash
cd frontend
npm install
npm run dev
```

- 默认地址：`http://localhost:5173`
- 已配置 `/api` 代理到 `http://localhost:8080`

### 前端构建
```bash
cd frontend
npm run build
```

- 构建产物会输出到后端静态目录：`src/main/resources/static/app`
- 登录后默认会跳转到 Vue 控制台：`/app/index.html`

### 控制台回滚开关（保留旧版 dashboard）

- 默认模式：`vue`（登录和 `/dashboard.html` 都会优先进入 Vue 控制台）
- 回滚到旧版：在浏览器控制台执行

```js
localStorage.setItem("pm_dashboard_mode", "legacy")
```

- 恢复 Vue 版：

```js
localStorage.setItem("pm_dashboard_mode", "vue")
```

- 临时强制打开旧版（不改本地模式）：
  - `http://localhost:8080/dashboard.html?legacyDashboard=1`

## 认证流程
1. 注册：`POST /api/auth/register`
2. 登录：`POST /api/auth/login`
3. 业务请求头：`Authorization: Bearer <token>`

## 业务接口
- 记账：`/api/accounting/**`
- 规划：`/api/plans/**`
- 目标：`/api/goals/**`
- 总览：`GET /api/dashboard/summary`

除 `/api/auth/**` 外，其余接口都需要 JWT。
