# Web 管理端迭代2 - 任务清单

## Implementation Tasks

- [ ] 1. **类型定义与 API 层**
    - [ ] 1.1. 新增类型定义
        - *Goal*: 在 `src/types/entity.ts` 中添加错题和标签相关类型
        - *Details*: 新增 `MistakeInfo`（id, title, correctAnswer, errorReason, imageUrl, subject, reviewStage, masteryLevel, lastReviewTime, nextReviewTime, tags: TagInfo[], createdTime, updatedTime）、`TagInfo`（id, name, type）、`TagTreeNode`（id, name, type, children: TagTreeNode[]）三个接口。所有字段与后端 Resp 对齐
        - *Requirements*: 2.W1, 2.W2
    - [ ] 1.2. 创建 mistakeService.ts
        - *Goal*: 在 `src/api/services/` 下封装错题 CRUD API
        - *Details*: 实现四个方法：`list(params)` → POST `/v1/mistake/list`；`detail(id)` → POST `/v1/mistake/detail`；`update(data)` → POST `/v1/mistake/update`；`delete(id)` → POST `/v1/mistake/delete`。定义 `MistakeListParams`、`MistakeUpdateData` 等请求接口。全部通过 `apiClient.post` 发起请求
        - *Requirements*: 2.W1, 2.W2
    - [ ] 1.3. 创建 tagService.ts
        - *Goal*: 在 `src/api/services/` 下封装标签 API
        - *Details*: 实现 `tree()` → POST `/v1/tag/tree`，返回 `TagTreeNode[]`。通过 `apiClient.post` 发起请求
        - *Requirements*: 2.W2

- [ ] 2. **路由与菜单配置**
    - [ ] 2.1. 注册错题管理页路由
        - *Goal*: 在前端路由中新增 `/management/mistake` 路径
        - *Details*: 在 `src/routes/sections/dashboard/frontend.tsx`（或对应路由配置文件）中添加 `{ path: "management/mistake", component: "/pages/management/mistake" }`。确保路由守卫和布局组件正确包裹
        - *Requirements*: 2.W1
    - [ ] 2.2. 新增侧边栏菜单项
        - *Goal*: 在侧边栏导航中添加"错题管理"入口
        - *Details*: 在 `src/layouts/dashboard/nav/nav-data/nav-data-frontend.tsx` 中，在 management 菜单组下新增 `{ label: "错题管理", key: "/management/mistake", icon: <Iconify icon="..." /> }`。放在"用户管理"下方
        - *Requirements*: 2.W1

- [ ] 3. **2.W1 错题管理页**
    - [ ] 3.1. 实现错题列表页面
        - *Goal*: 创建 `src/pages/management/mistake/index.tsx`，展示错题列表 + 筛选 + 分页
        - *Details*: 页面顶部为筛选区域（学科 Select + 关键字 Input + 查询 Button），下方为 shadcn/ui Table 展示列表。列定义：序号、题干摘要（截断50字）、学科、掌握度（色标进度条）、标签（Badge 列表）、创建时间（格式化为 yyyy-MM-dd）、操作（详情 Button + 删除 Button）。分页组件放在表格底部。页面挂载时调用 `mistakeService.list` 和 `tagService.tree` 加载数据。学科下拉选项从标签树中提取 type=SUBJECT 的节点
        - *Requirements*: 2.W1

- [ ] 4. **2.W2 错题详情/编辑弹窗**
    - [ ] 4.1. 实现详情 Dialog（查看模式）
        - *Goal*: 点击列表行"详情"按钮弹出 Dialog，展示完整错题信息
        - *Details*: 使用 shadcn/ui Dialog 组件。展示字段：题干（全文）、正确答案、错误原因、学科、标签（Badge 列表）、图片预览（imageUrl 不为空时展示，点击可放大）、复习阶段（x/6）、掌握度（百分比 + 色标进度条）、下次复习时间。右上角有"编辑"按钮切换到编辑模式。图片 URL 需拼接后端 baseURL 前缀
        - *Requirements*: 2.W2
    - [ ] 4.2. 实现编辑模式
        - *Goal*: Dialog 切换到编辑模式，题干/答案/原因变为 Textarea，学科变为 Select，标签变为多选
        - *Details*: 编辑模式下：题干（Textarea，必填）、正确答案（Textarea）、错误原因（Textarea）、学科（Select，选项来自标签树 SUBJECT 级别）、标签（Checkbox 组，按学科分组展示 CHAPTER/KNOWLEDGE 级别标签，可多选）。底部有"取消"和"保存"按钮。保存时调用 `mistakeService.update`，成功后 `toast.success`、关闭 Dialog、刷新列表
        - *Requirements*: 2.W2
    - [ ] 4.3. 实现删除确认
        - *Goal*: 点击"删除"按钮弹出 AlertDialog 确认
        - *Details*: 使用 shadcn/ui AlertDialog，标题"确认删除"，描述"删除后该错题将不再显示在列表中"。确认后调用 `mistakeService.delete(id)`，成功后 `toast.success("删除成功")`、刷新列表。取消则关闭弹窗无操作
        - *Requirements*: 2.W2

## Task Dependencies

- 任务 1（类型定义 + API）必须最先完成，页面依赖 API 和类型
- 任务 2（路由 + 菜单）可与任务 1 并行
- 任务 3（列表页）依赖任务 1 + 2
- 任务 4（详情/编辑弹窗）依赖任务 3（在列表页基础上扩展）

```
1 (类型/API) ──→ 3 (列表页) ──→ 4 (详情/编辑弹窗)
2 (路由/菜单) ──→ 3
```
