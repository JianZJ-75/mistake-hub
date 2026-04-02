import mistakeService from "@/api/services/mistakeService";
import tagService from "@/api/services/tagService";
import userService from "@/api/services/userService";
import uploadService from "@/api/services/uploadService";
import type { MistakeDetailResp, TagResp, UserInfo } from "#/entity";
import { Badge } from "@/ui/badge";
import { Button } from "@/ui/button";
import { Checkbox } from "@/ui/checkbox";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/ui/dialog";
import { Input } from "@/ui/input";
import { Popover, PopoverContent, PopoverTrigger } from "@/ui/popover";
import { ScrollArea } from "@/ui/scroll-area";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/ui/select";
import { Textarea } from "@/ui/textarea";
import { cascadeToggleTag } from "@/utils/tagCascade";
import { ChevronDown, ChevronRight, Eye, Loader2, Pencil, Search, Trash2, Upload, X } from "lucide-react";
import { Command, CommandEmpty, CommandInput, CommandItem, CommandList } from "@/ui/command";
import { useEffect, useRef, useState } from "react";
import { toast } from "sonner";

const PAGE_SIZE = 10;

const MASTERY_OPTIONS = [
	{ label: "全部掌握度", value: "all" },
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

const TYPE_LABEL: Record<string, string> = { SUBJECT: "学科", CHAPTER: "章节", KNOWLEDGE: "知识点", CUSTOM: "自定义" };

interface EditForm {
	title: string;
	correctAnswer: string;
	errorReason: string;
	imageUrl: string;
	tagIds: number[];
}

export default function MistakeManagementPage() {
	// ===== 列表状态 =====
	const [mistakes, setMistakes] = useState<MistakeDetailResp[]>([]);
	const [total, setTotal] = useState(0);
	const [loading, setLoading] = useState(false);
	const [pageNum, setPageNum] = useState(1);

	// ===== 筛选状态 =====
	const [masteryFilterVal, setMasteryFilterVal] = useState("all");
	const [selectedTagIds, setSelectedTagIds] = useState<number[]>([]);
	const [tagTree, setTagTree] = useState<TagResp[]>([]);
	const [customTags, setCustomTags] = useState<TagResp[]>([]);
	const [tagPopoverOpen, setTagPopoverOpen] = useState(false);
	const [tagSearchQuery, setTagSearchQuery] = useState("");
	const [tagExpandedIds, setTagExpandedIds] = useState<Set<number>>(new Set());

	// ===== 学生筛选 Combobox =====
	const [studentPopoverOpen, setStudentPopoverOpen] = useState(false);
	const [studentSearchQuery, setStudentSearchQuery] = useState("");
	const [studentOptions, setStudentOptions] = useState<UserInfo[]>([]);
	const [studentSearching, setStudentSearching] = useState(false);
	const [selectedStudent, setSelectedStudent] = useState<{ id: string; label: string } | null>(null);
	const studentSearchTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

	// ===== 详情/编辑弹窗 =====
	const [detailOpen, setDetailOpen] = useState(false);
	const [selected, setSelected] = useState<MistakeDetailResp | null>(null);
	const [editMode, setEditMode] = useState(false);
	const [editForm, setEditForm] = useState<EditForm>({ title: "", correctAnswer: "", errorReason: "", imageUrl: "", tagIds: [] });
	const [saving, setSaving] = useState(false);

	// ===== 删除确认 =====
	const [deleteOpen, setDeleteOpen] = useState(false);
	const [deletingId, setDeletingId] = useState<number | null>(null);
	const [deleting, setDeleting] = useState(false);

	// ===== 图片上传 =====
	const [uploading, setUploading] = useState(false);
	const fileInputRef = useRef<HTMLInputElement>(null);

	useEffect(() => {
		tagService.getTagTree().then(data => setTagTree(data || [])).catch(() => {});
		tagService.listAllCustomTags().then(data => setCustomTags(data || [])).catch(() => {});
		fetchMistakes(1);
	}, []);

	const allFlatTags = [...flattenTags(tagTree), ...customTags];

	const fetchMistakes = async (page: number, overrides?: { mastery?: string; tagIds?: number[]; account?: string }) => {
		setLoading(true);
		try {
			const mVal = overrides?.mastery ?? masteryFilterVal;
			const tIds = overrides?.tagIds ?? selectedTagIds;
			const aVal = overrides?.account ?? (selectedStudent?.id ?? "all");
			const tagIds = tIds.length > 0 ? tIds.join(",") : undefined;
			const masteryFilter = mVal !== "all" ? Number(mVal) : undefined;
			const accountId = aVal !== "all" ? Number(aVal) : undefined;
			const res = await mistakeService.listMistakes({
				accountId,
				tagIds,
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

	const searchStudents = (query: string) => {
		if (studentSearchTimer.current) clearTimeout(studentSearchTimer.current);
		if (!query.trim()) { setStudentOptions([]); return; }
		studentSearchTimer.current = setTimeout(async () => {
			setStudentSearching(true);
			try {
				const res = await userService.listUsers({ nickname: query.trim(), role: "student", pageNum: 1, pageSize: 20 });
				setStudentOptions(res.records);
			} catch { /* apiClient 已 toast */ }
			finally { setStudentSearching(false); }
		}, 300);
	};

	const selectStudent = (student: UserInfo) => {
		setSelectedStudent({ id: String(student.id), label: student.nickname || student.code || "" });
		setStudentPopoverOpen(false);
		setStudentSearchQuery("");
		setStudentOptions([]);
		setPageNum(1);
		fetchMistakes(1, { account: String(student.id) });
	};

	const clearStudent = () => {
		setSelectedStudent(null);
		setPageNum(1);
		fetchMistakes(1, { account: "all" });
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
			await mistakeService.modifyMistake({
				id: selected.id,
				title: editForm.title.trim(),
				correctAnswer: editForm.correctAnswer.trim() || "",
				errorReason: editForm.errorReason.trim() || "",
				imageUrl: editForm.imageUrl.trim() || "",
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
			tagIds: cascadeToggleTag(tagTree, prev.tagIds, id),
		}));
	};

	const toggleFilterTag = (id: number) => {
		const next = cascadeToggleTag(tagTree, selectedTagIds, id);
		setSelectedTagIds(next);
		setPageNum(1);
		fetchMistakes(1, { tagIds: next });
	};

	const removeFilterTag = (id: number) => {
		const next = selectedTagIds.filter(t => t !== id);
		setSelectedTagIds(next);
		setPageNum(1);
		fetchMistakes(1, { tagIds: next });
	};

	const clearFilterTags = () => {
		setSelectedTagIds([]);
		setPageNum(1);
		fetchMistakes(1, { tagIds: [] });
	};

	const getTagNameById = (id: number): string => {
		return allFlatTags.find(t => t.id === id)?.name || String(id);
	};

	const toggleTagExpand = (id: number) => {
		setTagExpandedIds(prev => {
			const next = new Set(prev);
			if (next.has(id)) next.delete(id);
			else next.add(id);
			return next;
		});
	};

	const filterTagNodes = (nodes: TagResp[], query: string): TagResp[] => {
		if (!query) return nodes;
		const result: TagResp[] = [];
		for (const node of nodes) {
			const filteredChildren = node.children?.length ? filterTagNodes(node.children, query) : [];
			const selfMatch = node.name.toLowerCase().includes(query.toLowerCase());
			if (selfMatch || filteredChildren.length > 0) {
				result.push({ ...node, children: selfMatch ? node.children : filteredChildren });
			}
		}
		return result;
	};

	const renderTagOptions = (nodes: TagResp[], depth: number, checkedIds: number[], onToggle: (id: number) => void): React.ReactNode[] => {

		const rows: React.ReactNode[] = [];
		for (const node of nodes) {
			const hasChildren = (node.children?.length ?? 0) > 0;
			const isNodeExpanded = tagSearchQuery !== "" || tagExpandedIds.has(node.id);
			rows.push(
				<div
					key={node.id}
					className="flex items-center gap-1 hover:bg-muted/50 px-2 py-1.5 rounded"
					style={{ paddingLeft: `${8 + depth * 16}px` }}
				>
					{hasChildren ? (
						<button type="button" className="p-0.5 rounded hover:bg-muted" onClick={(e) => { e.stopPropagation(); toggleTagExpand(node.id); }}>
							{isNodeExpanded ? <ChevronDown className="h-3.5 w-3.5 text-muted-foreground" /> : <ChevronRight className="h-3.5 w-3.5 text-muted-foreground" />}
						</button>
					) : <span className="w-5" />}
					<Checkbox
						checked={checkedIds.includes(node.id)}
						onCheckedChange={() => onToggle(node.id)}
					/>
					<span className="text-sm flex-1 truncate cursor-pointer" onClick={() => onToggle(node.id)}>{node.name}</span>
					{node.type && (
						<span className="text-xs text-muted-foreground">{TYPE_LABEL[node.type] || ""}</span>
					)}
				</div>,
			);
			if (hasChildren && isNodeExpanded) {
				rows.push(...renderTagOptions(node.children!, depth + 1, checkedIds, onToggle));
			}
		}
		return rows;
	};

	const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
		const file = e.target.files?.[0];
		if (!file) return;
		setUploading(true);
		try {
			const url = await uploadService.uploadImage(file);
			setEditForm(prev => ({ ...prev, imageUrl: url }));
			toast.success("图片上传成功");
		} catch {
			// apiClient 已 toast
		} finally {
			setUploading(false);
			if (fileInputRef.current) fileInputRef.current.value = "";
		}
	};

	const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));

	const isFiltering = selectedStudent !== null || masteryFilterVal !== "all" || selectedTagIds.length > 0;

	return (
		<div className="flex flex-col gap-4 p-2">
			<div className="flex items-center justify-between">
				<h2 className="text-2xl font-bold">错题管理</h2>
			</div>

			{/* 筛选栏 */}
			<div className="flex flex-wrap items-center gap-2">
				<div className="flex items-center">
				<Popover open={studentPopoverOpen} onOpenChange={setStudentPopoverOpen}>
					<PopoverTrigger asChild>
						<Button variant="outline" className={`justify-between font-normal ${selectedStudent ? "rounded-r-none border-r-0 w-36" : "w-40"}`}>
							{selectedStudent ? selectedStudent.label : "全部学生"}
							{!selectedStudent && <ChevronDown className="h-4 w-4 ml-1 opacity-50" />}
						</Button>
					</PopoverTrigger>
					<PopoverContent className="w-64 p-0" align="start">
						<Command shouldFilter={false}>
							<CommandInput
								placeholder="输入昵称搜索学生"
								value={studentSearchQuery}
								onValueChange={v => { setStudentSearchQuery(v); searchStudents(v); }}
							/>
							<CommandList>
								{studentSearching && (
									<div className="flex justify-center py-4">
										<Loader2 className="h-4 w-4 animate-spin" />
									</div>
								)}
								{!studentSearching && studentSearchQuery.trim() && studentOptions.length === 0 && (
									<CommandEmpty>无匹配学生</CommandEmpty>
								)}
								{!studentSearching && !studentSearchQuery.trim() && (
									<p className="py-4 text-center text-xs text-muted-foreground">请输入昵称搜索</p>
								)}
								{studentOptions.map(s => (
									<CommandItem key={s.id} onSelect={() => selectStudent(s)}>
										<span>{s.nickname || s.code}</span>
										<span className="ml-auto text-xs text-muted-foreground">{s.code}</span>
									</CommandItem>
								))}
							</CommandList>
						</Command>
					</PopoverContent>
				</Popover>
				{selectedStudent && (
					<Button variant="outline" size="icon" className="h-9 w-8 rounded-l-none" onClick={clearStudent}>
						<X className="h-3.5 w-3.5" />
					</Button>
				)}
				</div>
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
				<Popover open={tagPopoverOpen} onOpenChange={setTagPopoverOpen}>
					<PopoverTrigger asChild>
						<Button variant="outline" className="w-40 justify-between font-normal">
							{selectedTagIds.length > 0 ? `已选 ${selectedTagIds.length} 个标签` : "全部标签"}
							<ChevronDown className="h-4 w-4 ml-1 opacity-50" />
						</Button>
					</PopoverTrigger>
					<PopoverContent className="w-72 p-0" align="start">
						<div className="p-2 border-b">
							<Input
								placeholder="搜索标签"
								value={tagSearchQuery}
								onChange={e => setTagSearchQuery(e.target.value)}
								className="h-8"
							/>
						</div>
						<ScrollArea className="max-h-64 overflow-y-auto">
							<div className="p-1">
								{/* 全局标签 */}
								<p className="px-2 py-1.5 text-xs font-medium text-muted-foreground">全局标签</p>
								{renderTagOptions(filterTagNodes(tagTree, tagSearchQuery), 0, selectedTagIds, toggleFilterTag)}
								{filterTagNodes(tagTree, tagSearchQuery).length === 0 && (
									<p className="px-2 py-2 text-xs text-muted-foreground text-center">无匹配全局标签</p>
								)}
								{/* 自定义标签 */}
								{customTags.length > 0 && (
									<>
										<div className="border-t my-1" />
										<p className="px-2 py-1.5 text-xs font-medium text-muted-foreground">自定义标签</p>
										{customTags
											.filter(t => !tagSearchQuery || t.name.toLowerCase().includes(tagSearchQuery.toLowerCase()))
											.map(tag => (
												<label
													key={tag.id}
													className="flex items-center gap-2 cursor-pointer hover:bg-muted/50 px-2 py-1.5 rounded"
												>
													<Checkbox
														checked={selectedTagIds.includes(tag.id)}
														onCheckedChange={() => toggleFilterTag(tag.id)}
													/>
													<span className="text-sm flex-1 truncate">{tag.name}</span>
													<span className="text-xs text-muted-foreground">自定义</span>
												</label>
											))}
									</>
								)}
							</div>
						</ScrollArea>
						{selectedTagIds.length > 0 && (
							<div className="border-t p-2">
								<Button variant="ghost" size="sm" className="w-full text-xs" onClick={clearFilterTags}>
									清除全部
								</Button>
							</div>
						)}
					</PopoverContent>
				</Popover>
				<Button onClick={handleSearch} disabled={loading}>
					{loading ? <Loader2 className="h-4 w-4 animate-spin mr-1" /> : <Search className="h-4 w-4 mr-1" />}
					查询
				</Button>
			</div>

			{/* 已选标签 Badge */}
			{selectedTagIds.length > 0 && (
				<div className="flex flex-wrap items-center gap-1.5">
					{selectedTagIds.map(id => (
						<Badge key={id} variant="secondary" className="gap-1 pr-1">
							{getTagNameById(id)}
							<button
								type="button"
								className="ml-0.5 rounded-full hover:bg-muted p-0.5"
								onClick={() => removeFilterTag(id)}
							>
								<X className="h-3 w-3" />
							</button>
						</Badge>
					))}
					<button type="button" className="text-xs text-muted-foreground hover:text-foreground" onClick={clearFilterTags}>
						清除
					</button>
				</div>
			)}

			{/* 错题列表表格 */}
			<div className="rounded-xl border overflow-hidden">
				<table className="w-full text-sm">
					<thead className="bg-muted/50">
						<tr>
							<th className="px-4 py-3 text-left font-medium text-text-secondary">ID</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary">所属学生</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary">题干</th>
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
								<td colSpan={8} className="py-12 text-center text-text-secondary">
									{isFiltering ? "无匹配结果，请调整筛选条件" : "暂无数据"}
								</td>
							</tr>
						) : (
							mistakes.map(m => {
								const badge = getMasteryBadge(m.masteryLevel || 0);
								const barColor = getMasteryBarColor(m.masteryLevel || 0);
								return (
									<tr key={m.id} className="hover:bg-muted/30 transition-colors">
										<td className="px-4 py-3 text-text-secondary">{m.id}</td>
										<td className="px-4 py-3 text-sm">{m.accountNickname || "—"}</td>
										<td className="px-4 py-3 max-w-[240px]">
											<span className="line-clamp-2 text-sm">{m.title}</span>
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
								<div>所属学生：{selected.accountNickname || "—"}</div>
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

							{/* 图片上传 */}
							<div>
								<label className="text-xs text-muted-foreground mb-1 block">图片</label>
								{editForm.imageUrl ? (
									<div className="relative inline-block">
										<img src={editForm.imageUrl} alt="题目图片" className="max-w-full max-h-40 rounded-lg border" />
										<button
											type="button"
											className="absolute -top-2 -right-2 bg-destructive text-white rounded-full p-0.5 hover:bg-destructive/80"
											onClick={() => setEditForm(prev => ({ ...prev, imageUrl: "" }))}
										>
											<X className="h-3 w-3" />
										</button>
									</div>
								) : (
									<div>
										<input ref={fileInputRef} type="file" accept="image/*" className="hidden" onChange={handleFileUpload} />
										<Button
											type="button"
											variant="outline"
											size="sm"
											disabled={uploading}
											onClick={() => fileInputRef.current?.click()}
										>
											{uploading ? <Loader2 className="h-4 w-4 animate-spin mr-1" /> : <Upload className="h-4 w-4 mr-1" />}
											{uploading ? "上传中..." : "选择图片"}
										</Button>
									</div>
								)}
							</div>

							{/* 标签选择 */}
							<div>
								<label className="text-xs text-muted-foreground mb-1 block">标签</label>
								<div className="max-h-40 overflow-y-auto border rounded-md p-2">
									{tagTree.length === 0 && customTags.length === 0 && (
										<p className="text-xs text-muted-foreground py-2 text-center">暂无标签</p>
									)}
									{/* 全局标签（树形展开） */}
									{renderTagOptions(tagTree, 0, editForm.tagIds, toggleEditTag)}
									{/* 自定义标签 */}
									{customTags.length > 0 && (
										<>
											<div className="border-t my-1" />
											<p className="text-xs font-medium text-muted-foreground px-2 py-1">自定义标签</p>
											{customTags.map(tag => (
												<div key={tag.id} className="flex items-center gap-1 hover:bg-muted/50 px-2 py-1.5 rounded" style={{ paddingLeft: "8px" }}>
													<span className="w-5" />
													<Checkbox
														checked={editForm.tagIds.includes(tag.id)}
														onCheckedChange={() => toggleEditTag(tag.id)}
													/>
													<span className="text-sm flex-1 truncate cursor-pointer" onClick={() => toggleEditTag(tag.id)}>{tag.name}</span>
													<span className="text-xs text-muted-foreground">自定义</span>
												</div>
											))}
										</>
									)}
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
