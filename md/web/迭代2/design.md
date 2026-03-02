# Web 管理端迭代2 - 设计文档

## Overview

迭代2 Web 管理端的核心目标是实现错题管理页面，包含表格列表（分页 + 多条件筛选）和详情/编辑弹窗。本迭代新增 1 个页面组件、2 个 API service 文件、若干类型定义，并修改路由和侧边栏菜单配置。

技术选型沿用迭代1：**React 19 + Vite + TypeScript + Tailwind CSS + shadcn/ui**

## Architecture

### 新增/修改文件清单

```
src/
├── api/services/
│   ├── mistakeService.ts           ← 新增（错题 CRUD API）
│   └── tagService.ts               ← 新增（标签 API）
├── pages/management/mistake/
│   └── index.tsx                    ← 新增（错题管理页）
├── types/
│   └── entity.ts                    ← 修改（新增 MistakeInfo、TagInfo、TagTreeNode 类型）
└── routes/sections/dashboard/
    └── frontend.tsx                 ← 修改（新增错题管理路由）
```

修改文件：
- `src/layouts/dashboard/nav/nav-data/nav-data-frontend.tsx`：侧边栏新增"错题管理"菜单项
- `src/routes/sections/dashboard/frontend.tsx`：新增 `management/mistake` 路由

### 页面结构

```
┌─ DashboardLayout ──────────────────────────────────────────────────────┐
│ ┌─ Sidebar ─┐  ┌─ Content ──────────────────────────────────────────┐ │
│ │ 工作台     │  │ 错题管理                                           │ │
│ │           │  │ ┌──────────────────────────────────────────────────┐│ │
│ │ 用户管理  │  │ │ 学科: [全部▾]  关键字: [_______]  掌握度: [全部▾] ││ │
│ │           │  │ │                                       [查询]    ││ │
│ │ ★错题管理 │  │ ├──────────────────────────────────────────────────┤│ │
│ │           │  │ │ # │ 题干摘要     │学科│掌握度│标签  │创建时间│操作││ │
│ │           │  │ │ 1 │ 求证勾股...  │数学│ ██░░ │函数  │03-01  │⊙ ✕││ │
│ │           │  │ │ 2 │ 翻译以下...  │英语│ ████ │语法  │03-02  │⊙ ✕││ │
│ │           │  │ ├──────────────────────────────────────────────────┤│ │
│ │           │  │ │                    < 1 2 3 >                    ││ │
│ │           │  │ └──────────────────────────────────────────────────┘│ │
│ └───────────┘  └────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────────────┘
```

### 详情/编辑 Dialog

```
┌─ 错题详情 ────────────────────────────────────────────┐
│                                            [编辑] [×] │
│ ┌────────────────────────────────────────────────────┐│
│ │ 题干：求证：直角三角形中，两条直角边的平方和等于...   ││
│ │                                                    ││
│ │ 正确答案：根据勾股定理，a² + b² = c²...            ││
│ │                                                    ││
│ │ 错误原因：混淆了勾股定理的逆定理                    ││
│ │                                                    ││
│ │ 学科：数学                                         ││
│ │ 标签：函数 · 勾股定理                              ││
│ │                                                    ││
│ │ 图片：┌──────────┐                                ││
│ │       │  [预览图]  │                                ││
│ │       └──────────┘                                ││
│ │                                                    ││
│ │ 复习阶段：2/6    掌握度：40%   ████░░░░░░          ││
│ │ 下次复习：2026-03-05                               ││
│ └────────────────────────────────────────────────────┘│
│                                                       │
│ 编辑模式下：                                           │
│ 题干、答案、原因变为 Textarea                           │
│ 学科变为 Select                                        │
│ 标签变为多选 Tree（从 tag/tree 接口获取）               │
│                                        [取消] [保存]   │
└───────────────────────────────────────────────────────┘
```

## Components and Interfaces

### 1. mistakeService.ts — 错题 API

```typescript
import apiClient from '../apiClient'

/** 错题列表查询参数 */
interface MistakeListParams {
  subject?: string
  tagIds?: number[]
  masteryMin?: number
  masteryMax?: number
  keyword?: string
  pageNum: number
  pageSize: number
}

/** 错题录入参数 */
interface MistakeAddData {
  title: string
  correctAnswer?: string
  errorReason?: string
  imageUrls?: string[]
  subject?: string
  tagIds?: number[]
}

/** 错题更新参数 */
interface MistakeUpdateData {
  id: number
  title?: string
  correctAnswer?: string
  errorReason?: string
  imageUrls?: string[]
  subject?: string
  tagIds?: number[]
}

const mistakeService = {
  /** 分页查询错题列表 */
  list: (params: MistakeListParams) =>
    apiClient.post<PageResult<MistakeInfo>>({ url: '/v1/mistake/list', data: params }),

  /** 查询错题详情 */
  detail: (id: number) =>
    apiClient.post<MistakeInfo>({ url: '/v1/mistake/detail', data: { id } }),

  /** 编辑错题 */
  update: (data: MistakeUpdateData) =>
    apiClient.post<void>({ url: '/v1/mistake/update', data }),

  /** 删除错题 */
  delete: (id: number) =>
    apiClient.post<void>({ url: '/v1/mistake/delete', data: { id } }),
}

export default mistakeService
```

### 2. tagService.ts — 标签 API

```typescript
import apiClient from '../apiClient'

const tagService = {
  /** 获取标签树 */
  tree: () =>
    apiClient.post<TagTreeNode[]>({ url: '/v1/tag/tree' }),
}

export default tagService
```

### 3. entity.ts — 新增类型定义

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
```

### 4. 错题管理页（pages/management/mistake/index.tsx）

**状态管理：**
```typescript
// ===== 列表状态 =====
const [mistakes, setMistakes] = useState<MistakeInfo[]>([])
const [total, setTotal] = useState(0)
const [loading, setLoading] = useState(false)
const [pageNum, setPageNum] = useState(1)
const [pageSize] = useState(10)

// ===== 筛选状态 =====
const [subject, setSubject] = useState<string>('')
const [keyword, setKeyword] = useState('')

// ===== 弹窗状态 =====
const [detailOpen, setDetailOpen] = useState(false)
const [editMode, setEditMode] = useState(false)
const [currentMistake, setCurrentMistake] = useState<MistakeInfo | null>(null)

// ===== 标签数据 =====
const [tagTree, setTagTree] = useState<TagTreeNode[]>([])
```

**数据加载流程：**
```
页面挂载（useEffect）
  → fetchMistakes()
  → fetchTagTree()

筛选条件变化 → setPageNum(1) → fetchMistakes()
分页切换 → fetchMistakes()
```

**关键交互：**
- 学科筛选：从标签树中提取 SUBJECT 级别标签作为下拉选项
- 掌握度色标：
  - 0-30：红色（bg-red-500）
  - 31-60：橙色（bg-orange-500）
  - 61-80：黄色（bg-yellow-500）
  - 81-100：绿色（bg-green-500）
- 删除确认：使用 shadcn/ui AlertDialog，确认后调用 `mistakeService.delete(id)` 并刷新列表

**编辑模式标签选择：**
- 使用标签树数据构建多选组件
- 展示为 Checkbox 组（按学科分组）
- 选中的标签以 Badge 形式展示

### 5. 路由配置修改

在 `frontend.tsx` 中新增：
```typescript
{ path: "management/mistake", component: "/pages/management/mistake" }
```

### 6. 侧边栏菜单修改

在 `nav-data-frontend.tsx` 的 management 菜单组下新增：
```typescript
{
  label: "错题管理",
  key: "/management/mistake",
  icon: /* Iconify 图标 */,
}
```

## Data Models

```typescript
/** 分页结果（已在迭代1定义，复用） */
interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}

/** 后端统一响应格式（已在 apiClient.ts 中处理，前端无需关心） */
```

## Error Handling

| 场景 | 处理方式 |
|------|----------|
| 列表加载失败 | `toast.error` 提示，保持当前数据 |
| 详情加载失败 | `toast.error` 提示，关闭 Dialog |
| 编辑保存失败 | `toast.error` 提示，保持编辑状态 |
| 删除失败 | `toast.error` 提示，保持当前数据 |
| 网络异常 | `apiClient` 统一处理 |
| 403 未认证 | `apiClient` 统一跳转登录页 |

## Testing Strategy

| # | 测试场景 | 输入 | 预期输出 |
|---|----------|------|----------|
| 1 | 错题列表正常加载 | 访问错题管理页 | 表格显示错题数据，分页组件正确 |
| 2 | 按学科筛选 | 选择"数学" | 只显示学科为数学的错题 |
| 3 | 关键字搜索 | 输入"勾股" | 只显示题干含"勾股"的错题 |
| 4 | 分页切换 | 点击第2页 | 表格数据切换到第2页 |
| 5 | 查看详情 | 点击"详情"按钮 | Dialog 弹出，展示完整错题信息 |
| 6 | 编辑并保存 | 修改题干后点击保存 | 保存成功，列表自动刷新，toast 提示成功 |
| 7 | 删除确认 | 点击"删除"→ 确认 | 错题从列表消失，toast 提示成功 |
| 8 | 删除取消 | 点击"删除"→ 取消 | 无变化 |
| 9 | 空列表状态 | 无错题数据 | 表格显示"暂无数据"空状态 |
| 10 | 侧边栏导航 | 点击"错题管理"菜单 | 跳转到错题管理页，菜单高亮 |
