# Web 管理端迭代1 - 设计文档

## Overview

迭代1 Web 管理端的核心目标是搭建可运行的项目骨架，实现完整的登录流程（含 Nonce 加密），建立带侧边栏的管理布局框架。本迭代不涉及业务页面，所有子页面均为占位，为后续迭代提供稳定地基。

技术选型：**React 18 + Vite + TypeScript + Ant Design + axios + crypto-js + react-router-dom v6**

## 项目创建与运行

### 创建命令

```bash
# 在 frontend/ 目录下执行
npm create vite@latest mistake-hub-web -- --template react-ts
cd mistake-hub-web
npm install
npm install antd @ant-design/icons axios crypto-js react-router-dom
npm install -D @types/crypto-js
```

### 编译与运行

```bash
# 开发环境（默认端口 5173）
npm run dev

# 生产环境构建
npm run build
```

### 环境变量配置

```bash
# .env.development
VITE_API_BASE_URL=http://localhost:8080/mistake-hub/backend

# .env.production
VITE_API_BASE_URL=https://你的域名/mistake-hub/backend
```

在代码中通过 `import.meta.env.VITE_API_BASE_URL` 读取。

## Architecture

### 项目目录结构

```
frontend/mistake-hub-web/
├── public/                          # 静态资源
├── src/
│   ├── main.tsx                     # 应用入口
│   ├── App.tsx                      # 根组件（配置路由）
│   ├── api/
│   │   ├── request.ts               # axios 封装（token 注入 / 403 跳转 / 错误提示）
│   │   ├── account.ts               # 用户模块接口
│   │   └── nonce.ts                 # Nonce 模块接口
│   ├── utils/
│   │   ├── storage.ts               # token 存取工具（getToken / setToken / clearToken）
│   │   └── crypto.ts                # 加密工具（sha256 / aesEncrypt / aesDecrypt）
│   ├── layouts/
│   │   └── AdminLayout.tsx          # 管理端布局（侧边栏 + 顶部栏 + Outlet）
│   ├── pages/
│   │   ├── login/
│   │   │   └── index.tsx            # 登录页
│   │   └── home/
│   │       └── index.tsx            # 首页占位
│   └── router/
│       └── index.tsx                # 路由配置（含路由守卫）
├── .env.development
├── .env.production
├── index.html
├── package.json
├── tsconfig.json
└── vite.config.ts
```

### 架构分层

```
┌──────────────────────────────────────┐
│           Pages / Layouts (UI)       │  ← 登录页、布局框架、占位页面
├──────────────────────────────────────┤
│             API Layer                │  ← account.ts / nonce.ts
├──────────────────────────────────────┤
│          request.ts (核心)           │  ← 统一 axios 封装
│   token 注入 │ 403 跳转 │ 错误提示   │
├──────────────────────────────────────┤
│           Utils Layer                │  ← storage.ts / crypto.ts
├──────────────────────────────────────┤
│              axios                   │  ← HTTP 客户端
└──────────────────────────────────────┘
```

## Components and Interfaces

### 1. request.ts — axios 封装

```typescript
/**
 * axios 实例配置
 * baseURL 从环境变量读取，超时 10 秒
 */
const instance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 10000,
})

/**
 * 后端统一响应格式 BaseResult<T>
 * code = 0 表示成功，非 0 表示失败
 * message 为双语对象 { zh_CN: string, en_US: string }
 * data 为业务数据，泛型 T
 */
interface ApiResponse<T> {
  code: number
  message: { zh_CN: string; en_US: string }
  data: T
}

/**
 * 请求拦截器：自动注入 Authorization: Bearer {token}
 */

/**
 * 响应拦截器：
 * - HTTP 200 + code === 0 → 返回 response.data（直接提取业务数据）
 * - HTTP 200 + code !== 0 → message.error(response.message.zh_CN)，reject
 * - HTTP 403 → clearToken() → 跳转 /login → reject
 * - 网络异常 / 其他错误 → message.error('网络异常，请稍后重试')，reject
 */

/** POST 快捷方法，直接返回业务数据 T */
function post<T>(url: string, data?: object): Promise<T>
```

**关键：响应拦截器自动提取 `ApiResponse.data`，调用方无需关心外层包装。**

示例：
```typescript
// account.ts 中调用
const token = await post<string>('/v1/account/login-web', { code, cipherDigest, nonceId })
// token 直接就是字符串
```

**403 处理流程**：
```
响应拦截器检测到 HTTP 403
  → clearToken()
  → 使用 window.location.href 跳转到 /login（避免路由上下文依赖）
  → reject，不重试（区别于小程序的自动重登，Web 端直接跳登录页）
```

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

### 3. crypto.ts — 加密工具

> ⚠️ **核心：必须与后端 `AesUtil` 严格对齐**
>
> 后端密钥派生：`SHA256(rawKey)` → 取前 32 字符作为 AES key（32 bytes = AES-256）
> 后端输出格式：`cipherBase64 + "_" + ivBase64`（下划线分隔，非 Base64 字符，避免冲突）

```typescript
import CryptoJS from 'crypto-js'

/**
 * SHA-256 哈希
 * 返回 64 位小写 hex 字符串
 */
function sha256(input: string): string {
  return CryptoJS.SHA256(input).toString()
}

/**
 * AES-256-CBC 加密
 * 对应后端 AesUtil.encrypt(plainText, key)
 *
 * 密钥派生：SHA256(rawKey) → 取前 32 字符 → UTF-8 bytes 作为 AES key
 * IV：16 字节随机，Base64 编码
 * 输出格式：cipherBase64 + "_" + ivBase64
 */
function aesEncrypt(plainText: string, rawKey: string): string {
  // 1. 密钥派生（与后端 AesUtil.generateKey 一致）
  const keyHex = CryptoJS.SHA256(rawKey).toString().substring(0, 32)
  const key = CryptoJS.enc.Utf8.parse(keyHex)

  // 2. 随机 IV（16 字节）
  const iv = CryptoJS.lib.WordArray.random(16)
  const ivBase64 = CryptoJS.enc.Base64.stringify(iv)

  // 3. AES-CBC 加密
  const encrypted = CryptoJS.AES.encrypt(plainText, key, {
    iv,
    mode: CryptoJS.mode.CBC,
    padding: CryptoJS.pad.Pkcs7,
  })
  const cipherBase64 = encrypted.ciphertext.toString(CryptoJS.enc.Base64)

  // 4. 拼接格式：cipherBase64_ivBase64
  return `${cipherBase64}_${ivBase64}`
}

/**
 * AES-256-CBC 解密
 * 对应后端 AesUtil.decrypt(cipherText, key)
 *
 * 输入格式：cipherBase64_ivBase64（下划线分隔）
 */
function aesDecrypt(cipherTextWithIv: string, rawKey: string): string {
  // 1. 拆分密文和 IV（以第一个 _ 为分隔符）
  const separatorIndex = cipherTextWithIv.indexOf('_')
  const cipherBase64 = cipherTextWithIv.substring(0, separatorIndex)
  const ivBase64 = cipherTextWithIv.substring(separatorIndex + 1)

  // 2. 密钥派生
  const keyHex = CryptoJS.SHA256(rawKey).toString().substring(0, 32)
  const key = CryptoJS.enc.Utf8.parse(keyHex)

  // 3. 解析 IV
  const iv = CryptoJS.enc.Base64.parse(ivBase64)

  // 4. AES-CBC 解密
  const cipherParams = CryptoJS.lib.CipherParams.create({
    ciphertext: CryptoJS.enc.Base64.parse(cipherBase64),
  })
  const decrypted = CryptoJS.AES.decrypt(cipherParams, key, {
    iv,
    mode: CryptoJS.mode.CBC,
    padding: CryptoJS.pad.Pkcs7,
  })
  return decrypted.toString(CryptoJS.enc.Utf8)
}
```

> ⚠️ **拆分逻辑**：后端用 `split(IV_SEPARATOR, 2)` 按第一个 `_` 拆分。前端同样取第一个 `_` 的索引拆分，因为 Base64 字符串中不含 `_`（标准 Base64 只含 `A-Z a-z 0-9 + /`），所以可以安全地按第一个 `_` 分割。

### 4. nonce.ts — Nonce 接口

```typescript
interface NonceResp {
  id: number
  nonce: string
}

/** 生成 Nonce（无需认证），返回 {id, nonce} */
function generateNonce(): Promise<NonceResp>
```

### 5. account.ts — 用户接口

```typescript
interface AccountDetailResp {
  id: number
  code: string
  nickname: string
  avatarUrl: string
  role: string       // 'ADMIN' | 'STUDENT'
  dailyLimit: number
}

/**
 * Web 管理端登录
 * POST /v1/account/login-web
 * 入参：{ code, cipherDigest, nonceId }
 * 返回：token 字符串
 */
function loginWeb(code: string, cipherDigest: string, nonceId: number): Promise<string>

/**
 * 获取当前用户详情
 * POST /v1/account/current-detail
 * 需要 Bearer token 认证
 */
function currentDetail(): Promise<AccountDetailResp>
```

### 6. router/index.tsx — 路由配置

```tsx
/**
 * 路由结构：
 *
 * /login              → 登录页（无需认证）
 * /                   → AdminLayout（需要认证，PrivateRoute 守卫）
 *   └── index (home)  → 首页占位
 *
 * 路由守卫 PrivateRoute：
 * - 检查 getToken()，有 token → 渲染 <Outlet />
 * - 无 token → <Navigate to="/login" replace />
 */

const router = createBrowserRouter([
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/',
    element: <PrivateRoute />,    // 守卫层
    children: [
      {
        element: <AdminLayout />, // 布局层
        children: [
          { index: true, element: <HomePage /> },
        ],
      },
    ],
  },
])
```

### 7. AdminLayout.tsx — 管理布局

```tsx
/**
 * 布局结构（Ant Design Layout）：
 *
 * ┌────────────────────────────────────────────────┐
 * │  Sider（固定宽 220px，可折叠至 80px）           │
 * │  ┌─────────────────────┐  ┌──────────────────┐ │
 * │  │ Logo + 系统名        │  │  Header           │ │
 * │  │─────────────────────│  │  左：当前页面标题  │ │
 * │  │ Menu                │  │  右：昵称 + 退出   │ │
 * │  │  ┣ 首页             │  │──────────────────│ │
 * │  │  （后续迭代添加更多）│  │  Content          │ │
 * │  │                     │  │  <Outlet />       │ │
 * │  └─────────────────────┘  └──────────────────┘ │
 * └────────────────────────────────────────────────┘
 *
 * 顶部栏右侧：
 * - 显示 currentDetail().nickname
 * - 退出登录按钮：clearToken() → navigate('/login')
 */
```

### 8. 登录页（pages/login/index.tsx）

登录流程：
```
用户点击「登录」按钮
  → generateNonce() → { id: nonceId, nonce }
  → digest = sha256(password)
  → cipherDigest = aesEncrypt(digest, nonce)
  → loginWeb(code, cipherDigest, nonceId)
  → setToken(token)
  → navigate('/')
```

页面设计：
```
┌──────────────────────────────────────┐
│              错题复习管理端           │
│                                      │
│         ┌────────────────────┐       │
│  用户名  │ admin              │       │
│         └────────────────────┘       │
│         ┌────────────────────┐       │
│  密 码  │ ••••••••           │       │
│         └────────────────────┘       │
│         ┌────────────────────┐       │
│         │       登  录        │       │
│         └────────────────────┘       │
└──────────────────────────────────────┘
```

- 居中布局，Card 包裹，宽 400px
- 密码框支持显示/隐藏切换（Ant Design Input.Password）
- 登录按钮点击时显示 loading 状态
- 支持回车触发提交

## Data Models

```typescript
/** 后端统一响应格式，对应 BaseResult<T> */
interface ApiResponse<T> {
  code: number
  message: { zh_CN: string; en_US: string }
  data: T
}

/** Nonce 响应，对应后端 Nonce 实体 */
interface NonceResp {
  id: number
  nonce: string
}

/** 用户详情，对应后端 AccountDetailResp */
interface AccountDetailResp {
  id: number
  code: string
  nickname: string
  avatarUrl: string
  role: string
  dailyLimit: number
}
```

## Error Handling

| 场景 | HTTP 状态码 | 业务 code | 处理方式 |
|------|-----------|-----------|----------|
| 请求成功 | 200 | 0 | 返回 `response.data` |
| 业务异常（如密码错误） | 200 | -1 | `message.error(response.message.zh_CN)` |
| 参数校验失败 | 200 | 400 | `message.error(response.message.zh_CN)` |
| 未认证 / token 失效 | 403 | — | clearToken() → window.location.href = '/login' |
| 网络异常 / 超时 | — | — | `message.error('网络异常，请稍后重试')` |
| nonce 生成失败 | — | — | `message.error('服务异常，请稍后重试')`，中止登录 |

**核心判断逻辑**：
```typescript
// HTTP 层（axios 响应拦截器）
if (status === 403) → clearToken() → 跳转 /login
if (status !== 200) → 网络/服务端异常，弹错误提示

// 业务层（HTTP 200 时）
if (response.code === 0) → 成功，返回 response.data
if (response.code !== 0) → 业务异常，弹 response.message.zh_CN
```

## Testing Strategy

| # | 测试场景 | 输入 | 预期输出 |
|---|----------|------|----------|
| 1 | 未登录访问首页 | 浏览器访问 `/` | 自动重定向到 `/login` |
| 2 | 已有有效 token 访问登录页 | 浏览器访问 `/login` | 重定向到 `/`（首页） |
| 3 | 正确账号密码登录 | admin / admin-1q2w3e4R | nonce → 加密 → token → 跳首页 |
| 4 | 错误密码登录 | admin / wrongpassword | 弹错误提示，停留登录页 |
| 5 | 登录后顶部栏显示用户信息 | 进入首页 | 顶部右侧显示「管理员」昵称 |
| 6 | 退出登录 | 点击退出 | localStorage 清除 token，跳转 `/login` |
| 7 | token 过期，请求受保护接口 | 携带过期 token 发请求 | 后端返回 403，前端清除 token 跳登录页 |
| 8 | 网络断开时登录 | 断网状态下点击登录 | 弹「网络异常，请稍后重试」 |
| 9 | 侧边栏折叠 | 点击折叠按钮 | 侧边栏收缩至 80px，只显示图标 |
| 10 | 密码框显示/隐藏切换 | 点击眼睛图标 | 密码明文/密文切换 |
