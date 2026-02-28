# 微信小程序迭代1 - 设计文档

## Overview

迭代1 小程序前端的核心目标是搭建可运行的项目骨架，打通静默登录链路，建立 TabBar 导航框架。本迭代不涉及业务页面，所有 Tab 页均为占位，为后续迭代提供稳定地基。

技术选型：**Taro 4.x + React 18 + TypeScript + SCSS**

## 项目创建与运行

### 创建命令

```bash
# 在 frontend/ 目录下执行（非交互式，所有选项通过参数指定）
npx @tarojs/cli init mistake-hub-wechat --framework react --ts --css sass --compiler webpack5
cd mistake-hub-wechat && npm install
```

> ⚠️ 必须使用带参数的非交互模式，Claude 的 Bash 工具不支持交互式 CLI 输入。

创建完成后清理默认模板页面，按下方目录结构调整。

### 微信 AppID

```
wxd0173a6f4a4f49c5
```

在 `project.config.json` 中配置：
```json
{
  "appid": "wxd0173a6f4a4f49c5"
}
```

### 编译与运行

```bash
# 开发环境编译（生成微信小程序代码到 dist/ 目录）
npm run dev:weapp

# 生产环境编译
npm run build:weapp
```

编译完成后，用**微信开发者工具**打开 `dist/` 目录即可预览。

### 环境变量配置

在 Taro 构建配置中通过 `defineConstants` 注入 `BASE_URL`：

```typescript
// config/dev.ts
export default {
  defineConstants: {
    'process.env.TARO_APP_API': '"http://localhost:8080/mistake-hub/backend"'
  }
}

// config/prod.ts
export default {
  defineConstants: {
    'process.env.TARO_APP_API': '"https://你的域名/mistake-hub/backend"'
  }
}
```

> ⚠️ 值必须是双层引号包裹（外层引号给 JS 解析，内层引号是字符串字面量），否则编译后变量值不是字符串。

## Architecture

### 项目目录结构

```
frontend/mistake-hub-wechat/
├── config/                          # Taro 构建配置
│   ├── index.ts                     # 通用配置
│   ├── dev.ts                       # 开发环境
│   └── prod.ts                      # 生产环境
├── src/
│   ├── app.ts                       # 应用入口（触发静默登录）
│   ├── app.config.ts                # 全局配置（pages + tabBar）
│   ├── app.scss                     # 全局样式
│   ├── api/
│   │   ├── request.ts               # 请求封装（token 注入 / 403 重登 / 错误提示）
│   │   └── account.ts               # 用户模块接口
│   ├── utils/
│   │   └── storage.ts               # token 存取工具（getToken / setToken / clearToken）
│   ├── pages/
│   │   ├── home/
│   │   │   ├── index.tsx
│   │   │   ├── index.config.ts
│   │   │   └── index.scss
│   │   ├── mistakes/
│   │   │   ├── index.tsx
│   │   │   ├── index.config.ts
│   │   │   └── index.scss
│   │   ├── review/
│   │   │   ├── index.tsx
│   │   │   ├── index.config.ts
│   │   │   └── index.scss
│   │   └── mine/
│   │       ├── index.tsx
│   │       ├── index.config.ts
│   │       └── index.scss
│   └── assets/
│       └── tabbar/                  # TabBar 图标（选中态 + 未选中态，共 8 张）
│           ├── home.png
│           ├── home-active.png
│           ├── mistakes.png
│           ├── mistakes-active.png
│           ├── review.png
│           ├── review-active.png
│           ├── mine.png
│           └── mine-active.png
├── scripts/
│   └── generate-icons.js            # TabBar 图标生成脚本（使用 canvas 绘制）
├── project.config.json              # 微信小程序项目配置
├── package.json
├── tsconfig.json
└── babel.config.js
```

### 架构分层

```
┌──────────────────────────────────────┐
│              Pages (UI)              │  ← 各 Tab 占位页面
├──────────────────────────────────────┤
│            API Layer                 │  ← account.ts 等模块接口
├──────────────────────────────────────┤
│         request.ts (核心)            │  ← 统一请求封装
│   token 注入 │ 403 重登 │ 错误提示   │
├──────────────────────────────────────┤
│           Utils Layer                │  ← storage.ts (token 存取)
├──────────────────────────────────────┤
│         Taro.request()               │  ← Taro 原生 API
└──────────────────────────────────────┘
```

## Components and Interfaces

### 1. request.ts — 请求封装

```typescript
/**
 * 基础请求配置
 * 后端地址：http://localhost:8080/mistake-hub/backend
 * - 端口：8080
 * - context-path：/mistake-hub/backend
 *
 * 环境区分：
 * - 开发环境（config/dev.ts）：BASE_URL = 'http://localhost:8080/mistake-hub/backend'
 * - 生产环境（config/prod.ts）：BASE_URL = 'https://你的域名/mistake-hub/backend'
 * - 通过 Taro 编译时注入的环境变量 process.env.TARO_APP_API 读取，
 *   在 config/dev.ts 和 config/prod.ts 中分别配置 env.TARO_APP_API
 */
const BASE_URL = process.env.TARO_APP_API || 'http://localhost:8080/mistake-hub/backend'

/**
 * 后端统一响应格式 BaseResult<T>
 * - code: 0 表示成功，非 0 表示失败
 * - message: 双语消息对象，包含 zh_CN 和 en_US 两个 key
 * - data: 业务数据，泛型 T
 */
interface ApiResponse<T> {
  code: number
  message: {
    zh_CN: string
    en_US: string
  }
  data: T
}

/**
 * 统一请求方法
 * - 自动从 storage 读取 token，设置 Authorization: Bearer {token}
 * - 响应 HTTP statusCode === 200：
 *     → 解析 body 为 ApiResponse<T>
 *     → code === 0：返回 response.data（自动提取，调用方直接拿到业务数据）
 *     → code !== 0：Taro.showToast({ title: response.message.zh_CN })，reject
 * - 响应 HTTP statusCode === 403：
 *     → 自动重新 wx.login → login-wx → 重试原请求（最多 1 次）
 * - 网络异常：Taro.showToast({ title: '网络异常，请稍后重试' })
 */
function request<T>(options: RequestOptions): Promise<T>

/** POST 快捷方法，直接返回业务数据 T（已从 ApiResponse.data 中提取） */
function post<T>(url: string, data?: object): Promise<T>
```

**关键：`request` 方法自动提取 `ApiResponse.data`，调用方无需关心外层包装。**

示例：
```typescript
// account.ts 中调用
const token = await post<string>('/v1/account/login-wx', { code })
// token 直接就是字符串，不是 { code: 0, message: {...}, data: "xxx" }
```

**403 重登流程**：
```
请求返回 HTTP 403
  → clearToken()
  → Taro.login() 获取 code
  → 直接用 Taro.request() 裸调 POST {BASE_URL}/v1/account/login-wx {code}
  → Taro.request 返回 { statusCode, data }，其中 data 已是解析后的 ApiResponse 对象
  → 提取 token：response.data.data（第一个 .data 是 Taro 返回结构，第二个 .data 是 ApiResponse.data）
  → setToken(token)
  → 用新 token 重试原请求（仅 1 次）
  → 重试仍失败 → 弹提示，不再重试
```

**⚠️ Taro.request 返回值注意**：`Taro.request()` 返回 `{ statusCode: number, data: any }`，其中 `data` 是已解析的 JSON 对象（不是字符串）。所以裸调 login-wx 时，token 的提取路径是 `taroResponse.data.data`：
```typescript
const taroResponse = await Taro.request({ url: `${BASE_URL}/v1/account/login-wx`, method: 'POST', data: { code } })
const apiResult = taroResponse.data  // ApiResponse<string>，即 { code: 0, message: {...}, data: "token字符串" }
const token = apiResult.data         // "token字符串"
```

**⚠️ 循环依赖规避**：403 重登逻辑必须在 `request.ts` 内部直接使用 `Taro.request()` 裸调 `/v1/account/login-wx`，**禁止** import `account.ts` 的 `loginWx()`。原因：`account.ts` 依赖 `request.ts` 的 `post()`，反向 import 会形成循环依赖。裸调时也不走 token 注入和 403 重登拦截，天然避免死循环。

### 2. storage.ts — Token 存取

```typescript
const TOKEN_KEY = 'mistake_hub_token'

/** 获取 token，无则返回空字符串 */
function getToken(): string

/** 存储 token */
function setToken(token: string): void

/** 清除 token */
function clearToken(): void
```

### 3. account.ts — 用户接口

```typescript
/**
 * 微信登录
 * POST /v1/account/login-wx
 * 入参：{ code: string }（微信 wx.login 返回的临时 code）
 * 后端返回：BaseResult<String>，data 为 session token
 * 经 request 层提取后，直接返回 token 字符串
 */
function loginWx(code: string): Promise<string>

/**
 * 获取当前用户详情
 * POST /v1/account/current-detail
 * 入参：无（后端从 token 关联的 Session 中获取用户 ID）
 * 需要 Bearer token 认证
 */
function currentDetail(): Promise<AccountDetailResp>

interface AccountDetailResp {
  id: number
  code: string
  nickname: string
  avatarUrl: string
  role: string        // 'ADMIN' | 'STUDENT'
  dailyLimit: number
}
```

### 4. app.ts — 应用入口

```typescript
import { PropsWithChildren } from 'react'
import { useLaunch } from '@tarojs/taro'
import Taro from '@tarojs/taro'
import { loginWx } from './api/account'
import { getToken, setToken } from './utils/storage'
import './app.scss'

/**
 * App 组件
 * - 使用 Taro 专有的 useLaunch 钩子（不是 useEffect！Taro App 组件不支持 useEffect 做启动逻辑）
 * - 检查本地是否有 token：
 *   → 有：跳过登录（后续请求如果 403，由 request.ts 自动重登）
 *   → 无：调用 Taro.login() 获取 code → loginWx(code) → setToken(token)
 * - 登录失败仅弹 toast，不阻塞页面渲染
 * - 全程无 UI
 */
function App({ children }: PropsWithChildren) {

  useLaunch(async () => {
    const token = getToken()
    if (token) return

    try {
      const { code } = await Taro.login()
      const newToken = await loginWx(code)
      setToken(newToken)
    } catch (e) {
      Taro.showToast({ title: '微信登录失败', icon: 'none' })
    }
  })

  return children
}

export default App
```

> ⚠️ **必须用 `useLaunch`，不是 `useEffect`**。Taro 的 App 组件有专门的生命周期钩子，`useEffect` 在 App 层不会按预期触发。

### 5. TabBar 配置（app.config.ts）

```typescript
export default defineAppConfig({
  pages: [
    'pages/home/index',
    'pages/mistakes/index',
    'pages/review/index',
    'pages/mine/index',
  ],
  tabBar: {
    color: '#999999',
    selectedColor: '#1890ff',
    backgroundColor: '#ffffff',
    borderStyle: 'black',
    list: [
      { pagePath: 'pages/home/index',     text: '首页', iconPath: 'assets/tabbar/home.png', selectedIconPath: 'assets/tabbar/home-active.png' },
      { pagePath: 'pages/mistakes/index', text: '错题', iconPath: 'assets/tabbar/mistakes.png', selectedIconPath: 'assets/tabbar/mistakes-active.png' },
      { pagePath: 'pages/review/index',   text: '复习', iconPath: 'assets/tabbar/review.png', selectedIconPath: 'assets/tabbar/review-active.png' },
      { pagePath: 'pages/mine/index',     text: '我的', iconPath: 'assets/tabbar/mine.png', selectedIconPath: 'assets/tabbar/mine-active.png' },
    ]
  },
  window: {
    backgroundTextStyle: 'light',
    navigationBarBackgroundColor: '#fff',
    navigationBarTitleText: '错题复习',
    navigationBarTextStyle: 'black',
  }
})
```

### 6. 占位页面（4 个 Tab 页）

每个页面结构相同，仅展示页面标题居中显示：

```tsx
import { View, Text } from '@tarojs/components'
import './index.scss'

const HomePage: React.FC = () => {
  return (
    <View className='page'>
      <Text>首页</Text>
    </View>
  )
}

export default HomePage
```

### 7. TabBar 图标生成方案

使用 Node.js 脚本 + `canvas` 包程序化生成 8 张 81×81 PNG 图标：

| Tab | 图标元素 | 未选中色 | 选中色 |
|-----|---------|---------|--------|
| 首页 | 房子轮廓（三角屋顶 + 方形门体） | `#999999` | `#1890ff` |
| 错题 | 文档 + × 号（矩形 + 叉号） | `#999999` | `#1890ff` |
| 复习 | 循环箭头（圆弧 + 箭头） | `#999999` | `#1890ff` |
| 我的 | 人形轮廓（圆形头 + 弧形身体） | `#999999` | `#1890ff` |

生成脚本 `scripts/generate-icons.js`：
- 依赖：`canvas`（npm 包 `@napi-rs/canvas`，纯 JS 实现无需系统依赖）
- 执行：`node scripts/generate-icons.js`
- 输出：`src/assets/tabbar/*.png`

## Data Models

本迭代不涉及复杂数据模型，核心类型定义：

```typescript
/**
 * 后端统一响应格式
 * 对应后端 BaseResult<T> 类
 *
 * code = 0 表示成功，非 0 表示失败
 * message 为双语 Map：{ zh_CN: "中文消息", en_US: "English message" }
 * data 为业务数据
 */
interface ApiResponse<T> {
  code: number
  message: {
    zh_CN: string
    en_US: string
  }
  data: T
}

/** 用户详情（对应后端 AccountDetailResp） */
interface AccountDetailResp {
  id: number
  code: string
  nickname: string
  avatarUrl: string
  role: string        // 'ADMIN' | 'STUDENT'
  dailyLimit: number
}
```

## Error Handling

| 场景 | HTTP 状态码 | 业务 code | 处理方式 |
|------|-----------|-----------|----------|
| 请求成功 | 200 | 0 | 返回 `response.data` |
| 业务异常 | 200 | -1 | `Taro.showToast({ title: response.message.zh_CN, icon: 'none' })` |
| 参数校验失败 | 200 | 400 | `Taro.showToast({ title: response.message.zh_CN, icon: 'none' })` |
| 未认证 / token 失效 | 403 | -1 | 自动重新静默登录 → 重试原请求（最多 1 次） |
| 403 重试仍失败 | 403 | -1 | `Taro.showToast({ title: '登录失败，请重新打开小程序', icon: 'none' })` |
| wx.login() 失败 | — | — | `Taro.showToast({ title: '微信登录失败', icon: 'none' })` |
| 网络异常（无网络/超时） | — | — | `Taro.showToast({ title: '网络异常，请稍后重试', icon: 'none' })` |

**核心判断逻辑**：
```typescript
// HTTP 层
if (statusCode === 403) → 触发重登
if (statusCode !== 200) → 网络/服务端异常

// 业务层（HTTP 200 时）
if (response.code === 0) → 成功，返回 response.data
if (response.code !== 0) → 业务异常，弹 response.message.zh_CN
```

## Testing Strategy

| # | 测试场景 | 输入 | 预期输出 |
|---|----------|------|----------|
| 1 | 首次打开，无本地 token | 打开小程序 | 自动 wx.login → 调后端 → token 存入 storage |
| 2 | 已有有效 token | 打开小程序 | 跳过登录，直接渲染首页 |
| 3 | 携带 token 请求用户详情 | 调用 currentDetail() | 返回 AccountDetailResp，直接拿到 data |
| 4 | token 过期，请求返回 403 | 调用任意接口 | 自动重登 → 获取新 token → 重试成功 |
| 5 | 重登也失败（微信 code 无效） | 403 → 重登 → 再次 403 | 弹提示，不死循环 |
| 6 | 网络断开 | 发起请求 | 弹 "网络异常，请稍后重试" toast |
| 7 | 后端返回业务异常 | code !== 0 | 弹 message.zh_CN toast |
| 8 | 点击首页 Tab | 点击 | 显示首页占位内容 |
| 9 | 切换到错题 Tab | 点击 | 显示错题占位内容 |
| 10 | 切换到我的 Tab | 点击 | 显示我的占位内容 |
