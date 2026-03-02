# 微信小程序迭代2 - 任务清单

## Implementation Tasks

- [ ] 1. **API 层搭建**
    - [ ] 1.1. 创建 service/mistake.ts
        - *Goal*: 封装错题 CRUD API
        - *Details*: 在 `src/service/mistake.ts` 中定义 `MistakeInfo`、`TagInfo`、`PageResult`、`MistakeListParams`、`MistakeAddData`、`MistakeUpdateData` 接口。实现五个方法：`listMistakes(params)` → POST `/v1/mistake/list`；`getMistakeDetail(id)` → POST `/v1/mistake/detail`；`addMistake(data)` → POST `/v1/mistake/add`；`updateMistake(data)` → POST `/v1/mistake/update`；`deleteMistake(id)` → POST `/v1/mistake/delete`。全部通过 `request.ts` 的 `post` 方法发起
        - *Requirements*: 2.M1, 2.M2, 2.M4
    - [ ] 1.2. 创建 service/tag.ts
        - *Goal*: 封装标签 API
        - *Details*: 在 `src/service/tag.ts` 中定义 `TagTreeNode` 接口。实现 `getTagTree()` → POST `/v1/tag/tree`
        - *Requirements*: 2.M2
    - [ ] 1.3. 创建 service/upload.ts
        - *Goal*: 封装图片上传功能
        - *Details*: 在 `src/service/upload.ts` 中实现 `uploadImage(filePath: string): Promise<string>`。使用 `Taro.uploadFile` 发送 `multipart/form-data` 到 `${BASE_URL}/v1/upload/image`，手动设置 `Authorization: Bearer ${getToken()}`。⚠️ `Taro.uploadFile` 返回的 `res.data` 是 JSON 字符串，必须用 `JSON.parse` 解析。另实现 `getImageFullUrl(relativePath)` 工具函数，拼接 `BASE_URL + relativePath` 为完整 URL
        - *Requirements*: 2.M3

- [ ] 2. **页面注册**
    - [ ] 2.1. 修改 app.config.ts 注册新页面
        - *Goal*: 将新增的录入页和详情页注册到路由
        - *Details*: 在 `src/app.config.ts` 的 `pages` 数组中新增 `'pages/mistake-add/index'` 和 `'pages/mistake-detail/index'`。位置放在 `'pages/mistake/index'` 之后、`'pages/review/index'` 之前。tabBar 配置不变
        - *Requirements*: 2.M2, 2.M4

- [ ] 3. **2.M1 错题列表页**
    - [ ] 3.1. 替换错题 Tab 占位为实际列表
        - *Goal*: 重写 `pages/mistake/index.tsx`，展示错题列表 + 学科筛选 + 分页
        - *Details*: 页面顶部为横向滚动的学科标签栏（从标签树提取 SUBJECT 标签，首项为"全部"），下方为 ScrollView 列表。每个列表项（View 卡片）展示：题干摘要（截断 2 行）、学科 + 标签 Badge、掌握度色标进度条（红/橙/黄/绿）、创建时间。点击卡片 → `Taro.navigateTo({ url: '/pages/mistake-detail/index?id=' + id })`。右下角固定位置 FAB 按钮（+）→ `Taro.navigateTo({ url: '/pages/mistake-add/index' })`。实现 `onPullDownRefresh`（重置 pageNum=1，清空列表，重新加载）和 `onReachBottom`（pageNum+1，追加数据，无更多时 setHasMore(false)）。`useDidShow` 钩子检查全局刷新标记，有则重新加载列表。列表为空时显示居中空状态提示
        - *Requirements*: 2.M1

- [ ] 4. **2.M3 图片上传组件**
    - [ ] 4.1. 实现图片选择与上传
        - *Goal*: 在录入页和编辑模式中使用的图片上传功能
        - *Details*: 图片区域展示逻辑：已有图片时显示预览图 + 删除按钮（×）；无图片时显示"+ 添加"占位。点击"+ 添加" → `Taro.chooseImage({ count: 1, sizeType: ['compressed'], sourceType: ['album', 'camera'] })` → 获取 `tempFilePaths[0]` → 调用 `uploadImage(filePath)` → 成功后设置 imageUrl（相对路径）。上传中显示 loading 提示（`Taro.showLoading`）。预览图使用 `getImageFullUrl(imageUrl)` 拼接完整 URL。点击预览图 → `Taro.previewImage({ urls: [fullUrl] })` 查看大图
        - *Requirements*: 2.M3

- [ ] 5. **2.M2 错题录入页**
    - [ ] 5.1. 创建录入页面
        - *Goal*: 创建 `pages/mistake-add/` 目录及 index.tsx、index.config.ts、index.scss
        - *Details*: `index.config.ts` 设置 `navigationBarTitleText: '录入错题'`。页面表单包含：题干（Textarea，必填，placeholder="请输入题干内容"）、正确答案（Textarea）、错误原因（Textarea）、学科选择（Picker mode="selector"，选项从 tagTree 中提取 SUBJECT 标签名）、标签选择（点击弹出半屏面板，展示 CHAPTER/KNOWLEDGE 标签 Checkbox 列表，按学科分组，已选标签以 Badge 展示在表单区域）、图片上传（复用 4.1 的功能）。底部固定"提交"按钮。提交前校验 title 非空。提交调用 `addMistake`，成功后 `Taro.showToast('录入成功')` → 设置全局刷新标记 → `Taro.navigateBack()`
        - *Requirements*: 2.M2

- [ ] 6. **2.M4 错题详情页**
    - [ ] 6.1. 创建详情页面（查看模式）
        - *Goal*: 创建 `pages/mistake-detail/` 目录及 index.tsx、index.config.ts、index.scss
        - *Details*: `index.config.ts` 设置 `navigationBarTitleText: '错题详情'`。从路由参数获取 id（`useRouter().params.id`），调用 `getMistakeDetail(id)` 加载数据。展示所有字段：题干（全文）、正确答案、错误原因、图片预览（点击大图）、学科、标签 Badge、复习阶段（x/6）、掌握度（百分比 + 色标进度条）、下次复习时间。底部两个按钮："编辑"和"删除"
        - *Requirements*: 2.M4
    - [ ] 6.2. 实现编辑模式
        - *Goal*: 点击"编辑"切换为表单模式，可修改错题信息
        - *Details*: 设置 `isEditing` 状态。编辑模式下：题干/答案/原因变为 Textarea（预填当前值）、学科变为 Picker、标签变为多选组件、图片可替换/删除。底部变为"取消"和"保存"按钮。保存调用 `updateMistake`，成功后退出编辑模式 → 设置全局刷新标记 → 重新加载详情。取消则恢复原始数据并退出编辑模式
        - *Requirements*: 2.M4
    - [ ] 6.3. 实现删除功能
        - *Goal*: 点击"删除"按钮弹出确认弹窗
        - *Details*: 使用 `Taro.showModal({ title: '确认删除', content: '删除后该错题将不再显示' })` 弹出确认。确认后调用 `deleteMistake(id)`，成功后 `Taro.showToast('删除成功')` → 设置全局刷新标记 → `Taro.navigateBack()`
        - *Requirements*: 2.M4

## Task Dependencies

- 任务 1（API 层）必须最先完成，所有页面依赖 API 和类型定义
- 任务 2（页面注册）可与任务 1 并行
- 任务 3（列表页）依赖任务 1.1 + 1.2 + 2
- 任务 4（图片上传）依赖任务 1.3
- 任务 5（录入页）依赖任务 1 + 2 + 4
- 任务 6（详情页）依赖任务 1 + 2 + 4

```
1.1 (mistake API) ──→ 3 (列表页)
1.2 (tag API)     ──→ 5 (录入页)
1.3 (upload API)  ──→ 4 (图片上传) ──→ 5, 6
2   (页面注册)    ──→ 3, 5, 6
                           ↓
6 (详情页，含编辑/删除)
```
