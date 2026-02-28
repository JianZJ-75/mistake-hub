# 微信小程序迭代1实施规划 - 需求文档

基于迭代1已完成的后端接口（认证体系 + 用户管理），实现微信小程序前端的项目初始化、静默登录流程和 TabBar 框架搭建。

## Core Features

### 1.M1 项目初始化

- 使用 Taro (React) 框架创建微信小程序项目
- 封装统一请求层，所有接口调用自动附加 `Authorization: Bearer {token}` 请求头
- 统一错误处理：网络异常提示、业务异常提示
- TypeScript 全量使用，SCSS 作为样式方案

### 1.M2 静默登录流程

- 应用启动时自动检测本地是否存在有效 token
- 无 token 时调用 `wx.login()` 获取临时 code，调用后端 `POST /v1/account/login-wx` 换取 token
- token 持久化到本地存储，后续请求自动携带
- 请求遇到 403（token 失效）时，自动重新触发静默登录并重试原请求
- 全程无 UI，用户无感知

### 1.M3 TabBar 框架

- 底部导航栏包含 4 个 Tab：首页、错题、复习、我的
- 每个 Tab 配置对应图标（选中态 / 未选中态）
- 各 Tab 页为占位页面，显示页面标题文字即可，后续迭代逐步填充

## User Stories

- 作为学生用户，我打开小程序后自动完成登录，无需手动输入账号密码，直接进入首页
- 作为学生用户，我可以通过底部导航栏在首页、错题、复习、我的四个功能区之间切换
- 作为学生用户，我的登录态过期后，小程序自动重新登录，不影响我的正常使用

## Acceptance Criteria

- [ ] Taro 项目可正常编译，微信开发者工具中可运行
- [ ] 首次打开小程序，自动调用 `wx.login()` → 后端接口 → 本地存储 token，全程无 UI
- [ ] 携带 token 请求 `POST /v1/account/current-detail` 能正确返回用户信息
- [ ] token 失效（后端返回 403）时，自动重新登录并重试请求，用户无感知
- [ ] 底部 TabBar 显示 4 个 Tab（首页 / 错题 / 复习 / 我的），可正常切换
- [ ] 各 Tab 页占位内容正确渲染

## Non-functional Requirements

- **兼容性**：支持微信基础库 3.0.0+，适配 iPhone / Android 主流机型
- **性能**：小程序首屏加载时间 < 3 秒（不含网络请求）
- **安全**：token 仅存储在 `wx.setStorageSync`，不通过 URL 参数传递；403 重登最多重试 1 次，避免死循环

## 依赖的后端接口

| 接口 | URL | 说明 |
|------|-----|------|
| 微信登录 | `POST /v1/account/login-wx` | 入参 `{code: string}`，返回 token 字符串 |
| 当前用户详情 | `POST /v1/account/current-detail` | 需 Bearer token，返回 `AccountDetailResp` |
