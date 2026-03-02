# Web 管理端迭代1实施规划 - 需求文档

基于迭代1已完成的后端接口（认证体系 + 用户管理），实现 Web 管理端前端的项目初始化、登录页（含 Nonce 加密）和布局框架搭建。

## Core Features

### 1.W1 项目初始化

- 使用 React 18 + Vite + TypeScript 创建项目
- 集成 Ant Design 组件库（UI 框架）
- 集成 axios 作为 HTTP 客户端，封装统一请求层（自动附加 `Authorization: Bearer {token}`）
- 集成 crypto-js 实现前端加密（SHA-256 + AES）
- 集成 react-router-dom v6 管理路由
- 统一错误处理：业务异常 toast 提示，403 自动跳转登录页

### 1.W2 登录页

- 展示用户名（code）+ 密码输入框，支持回车提交
- 登录前先调用 `POST /v1/nonce/generate` 获取一次性密钥
- 前端计算 `digest = SHA-256(password)`，再用 nonce 进行 AES-256-CBC 加密得到 `cipherDigest`
- 提交 `{code, cipherDigest, nonceId}` 给后端 `POST /v1/account/login-web`
- 登录成功将 token 存入 `localStorage`，跳转到管理首页（`/`）
- 已有有效 token 则直接跳过登录页
- 403 响应时清除 token，跳转登录页

### 1.W3 布局框架

- 左侧固定侧边栏导航（折叠/展开），包含：首页（占位）
- 顶部栏：左侧显示当前页面标题，右侧显示当前登录用户昵称 + 退出登录按钮
- 退出登录：清除本地 token → 跳转登录页
- 内容区域：`<Outlet />` 渲染子路由页面
- 各子页面为占位内容，显示页面标题，后续迭代逐步填充

## User Stories

- 作为管理员，我访问 Web 管理端时，如果未登录则自动跳转到登录页，已登录则直接进入首页
- 作为管理员，我在登录页输入用户名和密码，点击登录后自动完成密码加密并完成认证
- 作为管理员，我登录后可以通过侧边栏在各功能模块间导航
- 作为管理员，我点击退出登录后被强制下线，跳转到登录页
- 作为管理员，我的 token 过期后（请求返回 403），自动跳转到登录页重新登录

## Acceptance Criteria

- [ ] React + Vite 项目可正常编译启动，访问 `http://localhost:5173`
- [ ] 未登录访问任何路由，自动跳转到 `/login`
- [ ] 登录页提交后调用 nonce/generate → 加密 → login-web，成功后跳转首页
- [ ] 携带 token 请求 `POST /v1/account/current-detail` 能正确返回用户信息并展示在顶部栏
- [ ] token 失效（后端返回 403）时，清除 token 并跳转到登录页
- [ ] 侧边栏可折叠，当前激活菜单高亮
- [ ] 退出登录后 localStorage 中的 token 被清除，页面跳转到 `/login`

## Non-functional Requirements

- **兼容性**：支持 Chrome / Firefox / Edge 最新版本
- **安全**：token 仅存储在 `localStorage`，不通过 URL 参数传递；密码明文不出现在网络请求中
- **加密对齐**：crypto-js AES-CBC 参数必须与后端 `AesUtil` 严格一致（密钥派生、IV 格式、分隔符）

## 依赖的后端接口

| 接口 | URL | 说明 |
|------|-----|------|
| 生成 Nonce | `POST /v1/nonce/generate` | 无需认证，返回 `{id, nonce}`，用于登录前加密 |
| Web 管理端登录 | `POST /v1/account/login-web` | 无需认证，入参 `{code, cipherDigest, nonceId}`，返回 token 字符串 |
| 当前用户详情 | `POST /v1/account/current-detail` | 需 Bearer token，返回 `AccountDetailResp` |
