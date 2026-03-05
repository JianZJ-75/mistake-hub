import mistakeService from "@/api/services/mistakeService";
import type { MistakeDetailResp, TagResp } from "#/entity";
import { Button } from "@/ui/button";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/ui/dialog";
import { Input } from "@/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/ui/select";
import { Textarea } from "@/ui/textarea";
import { Eye, Loader2, Pencil, Trash2 } from "lucide-react";
import { useEffect, useState } from "react";
import { toast } from "sonner";

const PAGE_SIZE = 10;

const MASTERY_OPTIONS = [
	{ label: "全部", value: "all" },
	{ label: "未掌握（<60）", value: "0" },
	{ label: "掌握中（60-79）", value: "1" },
	{ label: "已掌握（≥80）", value: "2" },
];

const STAGE_INTERVALS = [0, 1, 2, 4, 7, 15, 30];

const getMasteryBadge = (level: number) => {
	if (level >= 80) return { label: "已掌握", cls: "bg-green-100 text-green-700" };
	if (level >= 60) return { label: "掌握中", cls: "bg-orange-100 text-orange-700" };
	return { label: "未掌握", cls: "bg-red-100 text-red-700" };
};

const getMasteryBarColor = (level: number) => {
	if (level >= 80) return "bg-green-500";
	if (level >= 60) return "bg-orange-500";
	return "bg-red-500";
};

const flattenTags = (tags: TagResp[] | null | undefined): TagResp[] => {
	if (!tags) return [];
	const result: TagResp[] = [];
	for (const tag of tags) {
		result.push(tag);
		if (tag.children?.length) result.push(...flattenTags(tag.children));
	}
	return result;
};

const TYPE_LABEL: Record<string, string> = { SUBJECT: "学科", CHAPTER: "章节", KNOWLEDGE: "知识点" };

interface EditForm {
	title: string;
	correctAnswer: string;
	errorReason: string;
	imageUrl: string;
	subject: string;
	tagIds: number[];
}

export default function MistakeManagementPage() {
	// ===== 列表状态 =====
	const [mistakes, setMistakes] = useState<MistakeDetailResp[]>([]);
	const [total, setTotal] = useState(0);
	const [loading, setLoading] = useState(false);
	const [pageNum, setPageNum] = useState(1);

	// ===== 筛选状态 =====
	const [subjectFilter, setSubjectFilter] = useState("");
	const [masteryFilterVal, setMasteryFilterVal] = useState("all");
	const [tagFilterId, setTagFilterId] = useState<string>("all");
	const [allTags, setAllTags] = useState<TagResp[]>([]);

	// ===== 详情/编辑弹窗 =====
	const [detailOpen, setDetailOpen] = useState(false);
	const [selected, setSelected] = useState<MistakeDetailResp | null>(null);
	const [editMode, setEditMode] = useState(false);
	const [editForm, setEditForm] = useState<EditForm>({ title: "", correctAnswer: "", errorReason: "", imageUrl: "", subject: "", tagIds: [] });
	const [saving, setSaving] = useState(false);

	// ===== 删除确认 =====
	const [deleteOpen, setDeleteOpen] = useState(false);
	const [deletingId, setDeletingId] = useState<number | null>(null);
	const [deleting, setDeleting] = useState(false);

	useEffect(() => {
		mistakeService.getTagTree().then(tree => setAllTags(flattenTags(tree))).catch(() => {});
		fetchMistakes(1);
	}, []);

	const fetchMistakes = async (page: number, overrides?: { mastery?: string; tag?: string }) => {
		setLoading(true);
		try {
			const mVal = overrides?.mastery ?? masteryFilterVal;
			const tVal = overrides?.tag ?? tagFilterId;
			const tagId = tVal !== "all" ? Number(tVal) : undefined;
			const masteryFilter = mVal !== "all" ? Number(mVal) : undefined;
			const res = await mistakeService.listMistakes({
				subject: subjectFilter.trim() || undefined,
				tagId,
				masteryFilter,
				pageNum: page,
				pageSize: PAGE_SIZE,
			});
			setMistakes(res.records);
			setTotal(res.total || res.records.length);
		} catch {
			// apiClient 已 toast
		} finally {
			setLoading(false);
		}
	};

	const handleSearch = () => {
		setPageNum(1);
		fetchMistakes(1);
	};

	const openDetail = (mistake: MistakeDetailResp) => {
		setSelected(mistake);
		setEditMode(false);
		setDetailOpen(true);
	};

	const enterEdit = () => {
		if (!selected) return;
		setEditForm({
			title: selected.title || "",
			correctAnswer: selected.correctAnswer || "",
			errorReason: selected.errorReason || "",
			imageUrl: selected.imageUrl || "",
			subject: selected.subject || "",
			tagIds: selected.tags?.map(t => t.id) || [],
		});
		setEditMode(true);
	};

	const handleSave = async () => {
		if (!selected || !editForm.title.trim()) {
			toast.error("题干不能为空");
			return;
		}
		setSaving(true);
		try {
			await mistakeService.updateMistake({
				id: selected.id,
				title: editForm.title.trim(),
				correctAnswer: editForm.correctAnswer.trim() || undefined,
				errorReason: editForm.errorReason.trim() || undefined,
				imageUrl: editForm.imageUrl.trim() || undefined,
				subject: editForm.subject.trim() || undefined,
				tagIds: editForm.tagIds,
			});
			toast.success("保存成功");
			const updated = await mistakeService.getMistake(selected.id);
			setSelected(updated);
			setEditMode(false);
			fetchMistakes(pageNum);
		} catch {
			// apiClient 已 toast
		} finally {
			setSaving(false);
		}
	};

	const openDelete = (id: number) => {
		setDeletingId(id);
		setDeleteOpen(true);
	};

	const confirmDelete = async () => {
		if (!deletingId) return;
		setDeleting(true);
		try {
			await mistakeService.deleteMistake(deletingId);
			toast.success("已删除");
			setDeleteOpen(false);
			setDeletingId(null);
			setDetailOpen(false);
			// 当前页只剩一条且不是第一页时，回退到上一页
			const nextPage = mistakes.length <= 1 && pageNum > 1 ? pageNum - 1 : pageNum;
			setPageNum(nextPage);
			fetchMistakes(nextPage);
		} catch {
			// apiClient 已 toast
		} finally {
			setDeleting(false);
		}
	};

	const toggleEditTag = (id: number) => {
		setEditForm(prev => ({
			...prev,
			tagIds: prev.tagIds.includes(id) ? prev.tagIds.filter(t => t !== id) : [...prev.tagIds, id],
		}));
	};

	const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));

	return (
		<div className="flex flex-col gap-4 p-2">
			<h2 className="text-2xl font-bold">错题管理</h2>

			{/* 筛选栏 */}
			<div className="flex flex-wrap gap-2">
				<Input
					className="max-w-[140px]"
					placeholder="学科"
					value={subjectFilter}
					onChange={e => setSubjectFilter(e.target.value)}
					onKeyDown={e => e.key === "Enter" && handleSearch()}
				/>
				<Select value={masteryFilterVal} onValueChange={v => { setMasteryFilterVal(v); setPageNum(1); fetchMistakes(1, { mastery: v }); }}>
					<SelectTrigger className="w-40">
						<SelectValue placeholder="掌握度" />
					</SelectTrigger>
					<SelectContent>
						{MASTERY_OPTIONS.map(o => (
							<SelectItem key={o.value} value={o.value}>{o.label}</SelectItem>
						))}
					</SelectContent>
				</Select>
				<Select value={tagFilterId} onValueChange={v => { setTagFilterId(v); setPageNum(1); fetchMistakes(1, { tag: v }); }}>
					<SelectTrigger className="w-36">
						<SelectValue placeholder="知识点" />
					</SelectTrigger>
					<SelectContent>
						<SelectItem value="all">全部知识点</SelectItem>
						{allTags.map(t => (
							<SelectItem key={t.id} value={String(t.id)}>{t.name}</SelectItem>
						))}
					</SelectContent>
				</Select>
				<Button onClick={handleSearch} disabled={loading}>
					{loading ? <Loader2 className="h-4 w-4 animate-spin mr-1" /> : null}
					查询
				</Button>
			</div>

			{/* 错题列表表格 */}
			<div className="rounded-xl border overflow-hidden">
				<table className="w-full text-sm">
					<thead className="bg-muted/50">
						<tr>
							<th className="px-4 py-3 text-left font-medium text-text-secondary">ID</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary">题干</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary">学科</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary">掌握度</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary">复习阶段</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary">标签</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary">录入时间</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary">操作</th>
						</tr>
					</thead>
					<tbody className="divide-y">
						{loading ? (
							<tr>
								<td colSpan={8} className="py-12 text-center text-text-secondary">
									<Loader2 className="h-6 w-6 animate-spin mx-auto" />
								</td>
							</tr>
						) : mistakes.length === 0 ? (
							<tr>
								<td colSpan={8} className="py-12 text-center text-text-secondary">暂无数据</td>
							</tr>
						) : (
							mistakes.map(m => {
								const badge = getMasteryBadge(m.masteryLevel || 0);
								const barColor = getMasteryBarColor(m.masteryLevel || 0);
								return (
									<tr key={m.id} className="hover:bg-muted/30 transition-colors">
										<td className="px-4 py-3 text-text-secondary">{m.id}</td>
										<td className="px-4 py-3 max-w-[240px]">
											<span className="line-clamp-2 text-sm">{m.title}</span>
										</td>
										<td className="px-4 py-3">
											{m.subject
												? <span className="px-2 py-0.5 rounded-full text-xs bg-blue-100 text-blue-700">{m.subject}</span>
												: <span className="text-text-secondary">—</span>
											}
										</td>
										<td className="px-4 py-3">
											<div className="flex items-center gap-2">
												<div className="h-2 w-16 bg-muted rounded-full overflow-hidden">
													<div className={`h-full rounded-full ${barColor}`} style={{ width: `${m.masteryLevel || 0}%` }} />
												</div>
												<span className={`text-xs px-1.5 py-0.5 rounded-full font-medium ${badge.cls}`}>
													{badge.label}
												</span>
											</div>
										</td>
										<td className="px-4 py-3 text-text-secondary text-xs">
											第 {m.reviewStage} 阶段
											<span className="text-muted-foreground ml-1">
												({STAGE_INTERVALS[m.reviewStage] ?? 30}天)
											</span>
										</td>
										<td className="px-4 py-3">
											<div className="flex flex-wrap gap-1">
												{m.tags?.slice(0, 2).map(t => (
													<span key={t.id} className="px-1.5 py-0.5 text-xs bg-muted rounded">{t.name}</span>
												))}
												{(m.tags?.length ?? 0) > 2 && (
													<span className="text-xs text-text-secondary">+{m.tags!.length - 2}</span>
												)}
											</div>
										</td>
										<td className="px-4 py-3 text-text-secondary text-xs whitespace-nowrap">
											{m.createdTime?.replace("T", " ").substring(0, 16) || "—"}
										</td>
										<td className="px-4 py-3">
											<div className="flex items-center gap-1">
												<Button size="sm" variant="ghost" className="h-7 px-2 text-xs" onClick={() => openDetail(m)}>
													<Eye className="h-3 w-3 mr-1" />详情
												</Button>
												<Button
													size="sm"
													variant="ghost"
													className="h-7 px-2 text-xs text-destructive hover:text-destructive"
													onClick={() => openDelete(m.id)}
												>
													<Trash2 className="h-3 w-3 mr-1" />删除
												</Button>
											</div>
										</td>
									</tr>
								);
							})
						)}
					</tbody>
				</table>
			</div>

			{/* 分页 */}
			<div className="flex items-center justify-between text-sm text-text-secondary">
				<span>共 {total} 条记录</span>
				<div className="flex items-center gap-2">
					<Button
						size="sm"
						variant="outline"
						disabled={pageNum <= 1}
						onClick={() => { setPageNum(p => p - 1); fetchMistakes(pageNum - 1); }}
					>上一页</Button>
					<span className="px-2">{pageNum} / {totalPages}</span>
					<Button
						size="sm"
						variant="outline"
						disabled={pageNum >= totalPages}
						onClick={() => { setPageNum(p => p + 1); fetchMistakes(pageNum + 1); }}
					>下一页</Button>
				</div>
			</div>

			{/* 详情/编辑弹窗（2.W2） */}
			<Dialog open={detailOpen} onOpenChange={open => { if (!open) { setDetailOpen(false); setEditMode(false); } }}>
				<DialogContent className="sm:max-w-2xl max-h-[85vh] overflow-y-auto">
					<DialogHeader>
						<DialogTitle>{editMode ? "编辑错题" : "错题详情"}</DialogTitle>
					</DialogHeader>

					{selected && !editMode && (
						<div className="space-y-4 py-2">
							{/* 状态行 */}
							<div className="flex flex-wrap items-center gap-2">
								<span className={`px-2 py-0.5 rounded-full text-xs font-medium ${getMasteryBadge(selected.masteryLevel || 0).cls}`}>
									{getMasteryBadge(selected.masteryLevel || 0).label}
								</span>
								{selected.subject && (
									<span className="px-2 py-0.5 rounded-full text-xs bg-blue-100 text-blue-700">{selected.subject}</span>
								)}
								<span className="text-xs text-muted-foreground">
									第 {selected.reviewStage} 阶段 · 掌握度 {selected.masteryLevel || 0}%
								</span>
							</div>

							{/* 题干 */}
							<div>
								<p className="text-xs text-muted-foreground mb-1">题干</p>
								<p className="text-sm leading-relaxed">{selected.title}</p>
							</div>

							{/* 图片 */}
							{selected.imageUrl && (
								<div>
									<p className="text-xs text-muted-foreground mb-1">图片</p>
									<img src={selected.imageUrl} alt="题目图片" className="max-w-full rounded-lg border" />
								</div>
							)}

							{/* 正确答案 */}
							{selected.correctAnswer && (
								<div>
									<p className="text-xs text-muted-foreground mb-1">正确答案</p>
									<p className="text-sm leading-relaxed text-green-700">{selected.correctAnswer}</p>
								</div>
							)}

							{/* 错误原因 */}
							{selected.errorReason && (
								<div>
									<p className="text-xs text-muted-foreground mb-1">错误原因</p>
									<p className="text-sm leading-relaxed">{selected.errorReason}</p>
								</div>
							)}

							{/* 标签 */}
							{(selected.tags?.length ?? 0) > 0 && (
								<div>
									<p className="text-xs text-muted-foreground mb-1">标签</p>
									<div className="flex flex-wrap gap-1">
										{selected.tags!.map(t => (
											<span key={t.id} className="px-2 py-0.5 text-xs bg-muted rounded-full">{t.name}</span>
										))}
									</div>
								</div>
							)}

							{/* 时间信息 */}
							<div className="grid grid-cols-2 gap-2 text-xs text-muted-foreground">
								<div>录入时间：{selected.createdTime?.replace("T", " ").substring(0, 16) || "—"}</div>
								<div>下次复习：{selected.nextReviewTime?.replace("T", " ").substring(0, 16) || "—"}</div>
							</div>
						</div>
					)}

					{editMode && (
						<div className="space-y-4 py-2">
							{/* 题干 */}
							<div>
								<label className="text-xs text-muted-foreground mb-1 block">题干 <span className="text-destructive">*</span></label>
								<Textarea
									className="min-h-[80px] resize-none"
									value={editForm.title}
									onChange={e => setEditForm(prev => ({ ...prev, title: e.target.value }))}
									maxLength={2000}
								/>
							</div>

							{/* 正确答案 */}
							<div>
								<label className="text-xs text-muted-foreground mb-1 block">正确答案</label>
								<Textarea
									className="min-h-[60px] resize-none"
									value={editForm.correctAnswer}
									onChange={e => setEditForm(prev => ({ ...prev, correctAnswer: e.target.value }))}
									maxLength={2000}
								/>
							</div>

							{/* 错误原因 */}
							<div>
								<label className="text-xs text-muted-foreground mb-1 block">错误原因</label>
								<Textarea
									className="min-h-[60px] resize-none"
									value={editForm.errorReason}
									onChange={e => setEditForm(prev => ({ ...prev, errorReason: e.target.value }))}
									maxLength={2000}
								/>
							</div>

							{/* 图片 URL */}
							<div>
								<label className="text-xs text-muted-foreground mb-1 block">图片 URL</label>
								<Input
									value={editForm.imageUrl}
									onChange={e => setEditForm(prev => ({ ...prev, imageUrl: e.target.value }))}
									placeholder="留空则清除图片"
								/>
							</div>

							{/* 学科 */}
							<div>
								<label className="text-xs text-muted-foreground mb-1 block">学科</label>
								<Input
									value={editForm.subject}
									onChange={e => setEditForm(prev => ({ ...prev, subject: e.target.value }))}
									maxLength={64}
								/>
							</div>

							{/* 标签选择 */}
							<div>
								<label className="text-xs text-muted-foreground mb-1 block">标签</label>
								<div className="max-h-40 overflow-y-auto border rounded-md p-2 space-y-1">
									{allTags.length === 0 && (
										<p className="text-xs text-muted-foreground py-2 text-center">暂无标签</p>
									)}
									{allTags.map(tag => (
										<label key={tag.id} className="flex items-center gap-2 cursor-pointer hover:bg-muted/50 px-2 py-1 rounded">
											<input
												type="checkbox"
												checked={editForm.tagIds.includes(tag.id)}
												onChange={() => toggleEditTag(tag.id)}
												className="h-4 w-4 rounded border-input"
											/>
											<span className="text-sm flex-1">{tag.name}</span>
											<span className="text-xs text-muted-foreground">{TYPE_LABEL[tag.type] || ""}</span>
										</label>
									))}
								</div>
							</div>
						</div>
					)}

					<DialogFooter className="gap-2">
						{!editMode && (
							<>
								<Button variant="outline" onClick={() => { setDetailOpen(false); setEditMode(false); }}>关闭</Button>
								<Button
									variant="outline"
									className="text-destructive border-destructive hover:bg-destructive/10"
									onClick={() => { setDetailOpen(false); if (selected) openDelete(selected.id); }}
								>
									<Trash2 className="h-4 w-4 mr-1" />删除
								</Button>
								<Button onClick={enterEdit}>
									<Pencil className="h-4 w-4 mr-1" />编辑
								</Button>
							</>
						)}
						{editMode && (
							<>
								<Button variant="outline" onClick={() => setEditMode(false)}>取消</Button>
								<Button onClick={handleSave} disabled={saving}>
									{saving ? <Loader2 className="h-4 w-4 animate-spin mr-1" /> : null}
									保存
								</Button>
							</>
						)}
					</DialogFooter>
				</DialogContent>
			</Dialog>

			{/* 删除确认弹窗 */}
			<Dialog open={deleteOpen} onOpenChange={setDeleteOpen}>
				<DialogContent className="sm:max-w-md">
					<DialogHeader>
						<DialogTitle>确认删除</DialogTitle>
					</DialogHeader>
					<p className="text-sm text-muted-foreground py-2">删除后无法恢复，确认删除该错题吗？</p>
					<DialogFooter>
						<Button variant="outline" onClick={() => setDeleteOpen(false)}>取消</Button>
						<Button variant="destructive" onClick={confirmDelete} disabled={deleting}>
							{deleting ? <Loader2 className="h-4 w-4 animate-spin mr-1" /> : null}
							确认删除
						</Button>
					</DialogFooter>
				</DialogContent>
			</Dialog>
		</div>
	);
}
