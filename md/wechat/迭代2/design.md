# 微信小程序迭代2 - 设计文档

## Overview

迭代2 小程序前端的核心目标是实现错题的完整生命周期管理：列表浏览、录入采集（含图片）、详情查看、编辑修改和删除。本迭代替换"错题" Tab 占位页为实际列表，新增录入页和详情页两个非 Tab 页面。

技术选型沿用迭代1：**Taro 4.x + React + TypeScript + SCSS**

## Architecture

### 新增/修改文件清单

```
src/
├── service/
│   ├── mistake.ts              ← 新增（错题 CRUD API）
│   ├── tag.ts                  ← 新增（标签 API）
│   └── upload.ts               ← 新增（图片上传 API）
├── pages/
│   ├── mistake/
│   │   ├── index.tsx           ← 修改（替换占位为实际列表）
│   │   ├── index.config.ts     ← 修改（更新标题）
│   │   └── index.scss          ← 修改（列表样式）
│   ├── mistake-add/
│   │   ├── index.tsx           ← 新增（录入页）
│   │   ├── index.config.ts     ← 新增
│   │   └── index.scss          ← 新增
│   └── mistake-detail/
│       ├── index.tsx           ← 新增（详情/编辑页）
│       ├── index.config.ts     ← 新增
│       └── index.scss          ← 新增
└── app.config.ts               ← 修改（注册新页面路由）
```

### 页面路由结构

```
pages/
├── index/           # TabBar - 首页（不变）
├── mistake/         # TabBar - 错题列表（替换占位）
├── mistake-add/     # 非 Tab - 错题录入（navigateTo）
├── mistake-detail/  # 非 Tab - 错题详情/编辑（navigateTo）
├── review/          # TabBar - 复习（不变）
└── profile/         # TabBar - 我的（不变）
```

### app.config.ts 修改

在 `pages` 数组中新增两个页面路由：

```typescript
pages: [
  'pages/index/index',
  'pages/mistake/index',
  'pages/mistake-add/index',      // ← 新增
  'pages/mistake-detail/index',   // ← 新增
  'pages/review/index',
  'pages/profile/index',
]
```

> 非 Tab 页面必须在 `pages` 数组中注册，但不在 `tabBar.list` 中，因此不会出现在底部导航栏。

## Components and Interfaces

### 1. service/mistake.ts — 错题 API

```typescript
import { post } from '../utils/request'

/** 错题信息 */
interface MistakeInfo {
  id: number
  title: string
  correctAnswer?: string
  errorReason?: string
  imageUrls: string[]
  subject?: string
  reviewStage: number
  masteryLevel: number
  lastReviewTime?: string
  nextReviewTime?: string
  tags: TagInfo[]
  createdTime: string
  updatedTime?: string
}

/** 标签信息 */
interface TagInfo {
  id: number
  name: string
  type: string
}

/** 分页结果 */
interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/** 列表查询参数 */
interface MistakeListParams {
  subject?: string
  tagIds?: number[]
  masteryMin?: number
  masteryMax?: number
  keyword?: string
  pageNum: number
  pageSize: number
}

/** 录入参数 */
interface MistakeAddData {
  title: string
  correctAnswer?: string
  errorReason?: string
  imageUrls?: string[]
  subject?: string
  tagIds?: number[]
}

/** 更新参数 */
interface MistakeUpdateData {
  id: number
  title?: string
  correctAnswer?: string
  errorReason?: string
  imageUrls?: string[]
  subject?: string
  tagIds?: number[]
}

/** 分页查询错题列表 */
export const listMistakes = (params: MistakeListParams) =>
  post<PageResult<MistakeInfo>>('/v1/mistake/list', params)

/** 查询错题详情 */
export const getMistakeDetail = (id: number) =>
  post<MistakeInfo>('/v1/mistake/detail', { id })

/** 录入错题 */
export const addMistake = (data: MistakeAddData) =>
  post<void>('/v1/mistake/add', data)

/** 编辑错题 */
export const updateMistake = (data: MistakeUpdateData) =>
  post<void>('/v1/mistake/update', data)

/** 删除错题 */
export const deleteMistake = (id: number) =>
  post<void>('/v1/mistake/delete', { id })

export type { MistakeInfo, TagInfo, PageResult, MistakeListParams }
```

### 2. service/tag.ts — 标签 API

```typescript
import { post } from '../utils/request'

/** 标签树节点 */
interface TagTreeNode {
  id: number
  name: string
  type: string
  children: TagTreeNode[]
}

/** 获取标签树 */
export const getTagTree = () =>
  post<TagTreeNode[]>('/v1/tag/tree')

export type { TagTreeNode }
```

### 3. service/upload.ts — 图片上传

```typescript
import Taro from '@tarojs/taro'
import { getToken } from '../utils/request'

const BASE_URL = process.env.TARO_APP_API || 'http://localhost:8080/mistake-hub/backend'

/**
 * 上传图片
 * 使用 Taro.uploadFile 发送 multipart/form-data
 * 不走 request.ts 的通用请求（uploadFile API 不同）
 */
export const uploadImage = async (filePath: string): Promise<string> => {

  const token = getToken()

  const res = await Taro.uploadFile({
    url: `${BASE_URL}/v1/upload/image`,
    filePath,
    name: 'file',
    header: {
      'Authorization': `Bearer ${token}`,
    },
  })

  // Taro.uploadFile 的 res.data 是 JSON 字符串，需要手动解析
  const data = JSON.parse(res.data) as { code: number; message: { zh_CN: string }; data: string }

  if (data.code === 0) {
    return data.data  // 返回图片相对路径 /uploads/images/xxx.jpg
  }

  throw new Error(data.message.zh_CN || '图片上传失败')
}

/** 拼接完整图片 URL（供 Image 组件使用） */
export const getImageFullUrl = (relativePath: string): string => {

  if (!relativePath) return ''
  if (relativePath.startsWith('http')) return relativePath
  return `${BASE_URL}${relativePath}`
}
```

> ⚠️ `Taro.uploadFile` 的 `res.data` 是**字符串**（不是 JSON 对象），必须用 `JSON.parse` 手动解析。这与 `Taro.request` 自动解析 JSON 不同。

### 4. 错题列表页（pages/mistake/index.tsx）

**页面结构：**

```
┌─ 我的错题 ─────────────────────────────────────────────┐
│ ┌─────────────────────────────────────────────────────┐│
│ │  [全部] [数学] [英语] [物理] [化学] ...    ← 学科标签 ││
│ └─────────────────────────────────────────────────────┘│
│                                                        │
│ ┌─ ScrollView (下拉刷新 + 上拉加载) ─────────────────┐ │
│ │ ┌──────────────────────────────────────────────┐   │ │
│ │ │ 求证勾股定理...                              │   │ │
│ │ │ 数学 · 函数    掌握度 ██░░ 20%   03-01      │   │ │
│ │ └──────────────────────────────────────────────┘   │ │
│ │ ┌──────────────────────────────────────────────┐   │ │
│ │ │ 翻译以下句子...                              │   │ │
│ │ │ 英语 · 语法    掌握度 ████ 60%   03-02      │   │ │
│ │ └──────────────────────────────────────────────┘   │ │
│ │                                                    │ │
│ │          ─── 没有更多了 ───                        │ │
│ └────────────────────────────────────────────────────┘ │
│                                                        │
│                                            ┌───┐       │
│                                            │ + │ ← FAB │
│                                            └───┘       │
└────────────────────────────────────────────────────────┘
```

**状态管理：**

```typescript
// ===== 列表数据 =====
const [mistakes, setMistakes] = useState<MistakeInfo[]>([])
const [total, setTotal] = useState(0)
const [pageNum, setPageNum] = useState(1)
const [loading, setLoading] = useState(false)
const [hasMore, setHasMore] = useState(true)

// ===== 筛选状态 =====
const [subjects, setSubjects] = useState<string[]>([])  // 从标签树提取
const [selectedSubject, setSelectedSubject] = useState<string>('')  // 空=全部

// ===== 常量 =====
const PAGE_SIZE = 10
```

**交互流程：**

```
页面挂载 (useEffect)
  → getTagTree() → 提取 SUBJECT 标签 → setSubjects
  → fetchMistakes(pageNum=1)

下拉刷新 (onPullDownRefresh)
  → setPageNum(1) → setMistakes([]) → fetchMistakes(1) → stopPullDownRefresh

上拉加载 (onReachBottom)
  → if (hasMore && !loading) → setPageNum(n+1) → fetchMistakes(n+1, append=true)

学科标签切换
  → setSelectedSubject(subject) → setPageNum(1) → setMistakes([]) → fetchMistakes(1)

点击错题卡片
  → Taro.navigateTo({ url: '/pages/mistake-detail/index?id=' + mistake.id })

点击 FAB (+)
  → Taro.navigateTo({ url: '/pages/mistake-add/index' })

从录入/详情页返回 (useDidShow)
  → 检查全局刷新标记 → 重新加载列表
```

**掌握度色标：**

| 范围 | 颜色 | CSS 类名 |
|------|------|----------|
| 0-30 | 红色 | `mastery-red` |
| 31-60 | 橙色 | `mastery-orange` |
| 61-80 | 黄色 | `mastery-yellow` |
| 81-100 | 绿色 | `mastery-green` |

### 5. 错题录入页（pages/mistake-add/index.tsx）

**页面结构：**

```
┌─ 录入错题 ─────────────────────────────────────────────┐
│                                                        │
│  题干 *                                                │
│  ┌────────────────────────────────────────────────┐    │
│  │ (多行文本输入)                                  │    │
│  │                                                │    │
│  └────────────────────────────────────────────────┘    │
│                                                        │
│  正确答案                                              │
│  ┌────────────────────────────────────────────────┐    │
│  │ (多行文本输入)                                  │    │
│  └────────────────────────────────────────────────┘    │
│                                                        │
│  错误原因                                              │
│  ┌────────────────────────────────────────────────┐    │
│  │ (多行文本输入)                                  │    │
│  └────────────────────────────────────────────────┘    │
│                                                        │
│  学科                                                  │
│  ┌─────────────────────────────┐                       │
│  │ 请选择学科               ▾  │ ← Picker              │
│  └─────────────────────────────┘                       │
│                                                        │
│  知识点标签                                            │
│  ┌────────────────────────────────────────────────┐    │
│  │ [函数 ×] [勾股定理 ×]     ← 已选标签 Badge       │    │
│  │ 点击选择标签...                                 │    │
│  └────────────────────────────────────────────────┘    │
│                                                        │
│  图片                                                  │
│  ┌──────────┐  ┌──────────┐                            │
│  │  [预览图] │  │   + 添加  │ ← 图片选择               │
│  │     ×    │  │          │                            │
│  └──────────┘  └──────────┘                            │
│                                                        │
│  ┌────────────────────────────────────────────────┐    │
│  │                    提  交                       │    │
│  └────────────────────────────────────────────────┘    │
└────────────────────────────────────────────────────────┘
```

**状态管理：**

```typescript
// ===== 表单数据 =====
const [title, setTitle] = useState('')
const [correctAnswer, setCorrectAnswer] = useState('')
const [errorReason, setErrorReason] = useState('')
const [subject, setSubject] = useState('')
const [selectedTagIds, setSelectedTagIds] = useState<number[]>([])
const [imageUrls, setImageUrls] = useState<string[]>([])  // 支持多张图片

// ===== 标签数据 =====
const [tagTree, setTagTree] = useState<TagTreeNode[]>([])
const [showTagPicker, setShowTagPicker] = useState(false)

// ===== 状态 =====
const [submitting, setSubmitting] = useState(false)
const [uploading, setUploading] = useState(false)
```

**提交流程：**

```
用户点击「提交」
  → 校验 title 不为空
  → 设置 submitting = true
  → addMistake({ title, correctAnswer, errorReason, imageUrls, subject, tagIds: selectedTagIds })
  → 成功：Taro.showToast({ title: '录入成功' })
       → 设置全局刷新标记
       → Taro.navigateBack()
  → 失败：Taro.showToast({ title: error.message })
  → 设置 submitting = false
```

**图片上传流程：**

```
用户点击「+ 添加」图片区域（最多5张）
  → Taro.chooseImage({ count: 1, sizeType: ['compressed'], sourceType: ['album', 'camera'] })
  → 获取 tempFilePath
  → 设置 uploading = true
  → uploadImage(tempFilePath)
  → 成功：setImageUrls(prev => [...prev, relativePath])
  → 失败：Taro.showToast({ title: '上传失败' })
  → 设置 uploading = false

用户点击已上传图片上的「×」
  → setImageUrls(prev => prev.filter((_, i) => i !== index))
```

**学科选择：**
- 从标签树中提取 type=SUBJECT 的标签名称列表
- 使用 Taro Picker 组件（mode="selector"）

**标签选择：**
- 点击"点击选择标签"打开标签选择面板
- 标签按学科分组展示（如果选择了学科，只展示该学科下的标签）
- 使用 Checkbox 多选，选中后以 Badge 形式展示在输入区域
- 实现方式：底部弹出半屏面板（类似 ActionSheet），内含标签 Checkbox 列表

### 6. 错题详情页（pages/mistake-detail/index.tsx）

**页面结构：**

```
┌─ 错题详情 ─────────────────────────────────────────────┐
│                                                        │
│  ┌────────────────────────────────────────────────┐    │
│  │ 求证：直角三角形中，两条直角边的平方和等于      │    │
│  │ 斜边的平方。                                   │    │
│  └────────────────────────────────────────────────┘    │
│                                                        │
│  正确答案                                              │
│  ┌────────────────────────────────────────────────┐    │
│  │ 根据勾股定理，a² + b² = c²...                  │    │
│  └────────────────────────────────────────────────┘    │
│                                                        │
│  错误原因                                              │
│  ┌────────────────────────────────────────────────┐    │
│  │ 混淆了勾股定理的逆定理                         │    │
│  └────────────────────────────────────────────────┘    │
│                                                        │
│  ┌──────────────────┐                                  │
│  │   [错题图片预览]   │   ← 点击可预览大图             │
│  └──────────────────┘                                  │
│                                                        │
│  学科：数学                                            │
│  标签：[函数] [勾股定理]                               │
│                                                        │
│  ── 复习状态 ──                                       │
│  复习阶段：2 / 6                                       │
│  掌握度：40%  ████░░░░░░                              │
│  下次复习：2026-03-05                                  │
│                                                        │
│  ┌────────────────┐  ┌────────────────┐               │
│  │      编辑      │  │      删除      │               │
│  └────────────────┘  └────────────────┘               │
└────────────────────────────────────────────────────────┘
```

**数据加载：**

```
页面挂载 (useEffect)
  → 从路由参数获取 id: useRouter().params.id
  → getMistakeDetail(id) → setMistake(data)
  → getTagTree() → setTagTree (编辑时使用)
```

**编辑模式：**
- 点击"编辑"按钮，页面切换为编辑模式（isEditing = true）
- 题干、答案、原因变为 Textarea
- 学科变为 Picker
- 标签变为多选组件
- 底部显示"取消"和"保存"按钮
- 保存后退出编辑模式，刷新详情数据

**删除流程：**

```
用户点击「删除」
  → Taro.showModal({ title: '确认删除', content: '删除后不可恢复' })
  → 确认：deleteMistake(id)
       → 成功：Taro.showToast('删除成功')
             → 设置全局刷新标记
             → Taro.navigateBack()
       → 失败：Taro.showToast(error.message)
```

**图片预览：**

```
用户点击图片
  → Taro.previewImage({ current: fullUrl, urls: imageUrls.map(getImageFullUrl) })
```

## Data Models

```typescript
/** 错题信息（对应后端 MistakeDetailResp） */
interface MistakeInfo {
  id: number
  title: string
  correctAnswer?: string
  errorReason?: string
  imageUrls: string[]
  subject?: string
  reviewStage: number
  masteryLevel: number
  lastReviewTime?: string
  nextReviewTime?: string
  tags: TagInfo[]
  createdTime: string
  updatedTime?: string
}

/** 标签信息（对应后端 TagResp） */
interface TagInfo {
  id: number
  name: string
  type: string
}

/** 标签树节点（对应后端 TagTreeResp） */
interface TagTreeNode {
  id: number
  name: string
  type: string
  children: TagTreeNode[]
}

/** 分页结果（对应后端 Page<T>） */
interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}
```

## Error Handling

| 场景 | 处理方式 |
|------|----------|
| 列表加载失败 | `Taro.showToast({ title: '加载失败', icon: 'none' })` |
| 图片上传失败 | `Taro.showToast({ title: '图片上传失败', icon: 'none' })`，保持表单状态 |
| 录入提交失败 | `Taro.showToast({ title: error.message, icon: 'none' })`，保持表单状态 |
| 详情加载失败 | `Taro.showToast({ title: '加载失败', icon: 'none' })`，显示空状态 |
| 编辑保存失败 | `Taro.showToast({ title: error.message, icon: 'none' })`，保持编辑状态 |
| 删除失败 | `Taro.showToast({ title: error.message, icon: 'none' })` |
| 网络异常 | `request.ts` 统一处理 |
| 403 token 失效 | `request.ts` 自动重登并重试 |

## Testing Strategy

| # | 测试场景 | 输入 | 预期输出 |
|---|----------|------|----------|
| 1 | 错题列表正常加载 | 进入"错题" Tab | 显示错题列表，最新的在最前 |
| 2 | 空列表状态 | 新用户无错题 | 显示"暂无错题，点击 + 开始录入"空状态 |
| 3 | 学科标签筛选 | 点击"数学" | 只显示学科为数学的错题 |
| 4 | 下拉刷新 | 下拉 | 列表重新加载 |
| 5 | 上拉加载更多 | 滚动到底 | 追加下一页数据，无更多时提示"没有更多了" |
| 6 | 录入错题（纯文字） | 填写题干后提交 | 成功，返回列表，列表刷新 |
| 7 | 录入错题（含图片） | 拍照上传后提交 | 图片上传成功，错题保存成功 |
| 8 | 查看错题详情 | 点击列表项 | 跳转详情页，展示完整信息 |
| 9 | 编辑错题 | 修改题干后保存 | 保存成功，详情页刷新 |
| 10 | 删除错题 | 点击删除 → 确认 | 删除成功，返回列表 |
