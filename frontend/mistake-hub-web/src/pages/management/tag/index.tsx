import tagService from "@/api/services/tagService";
import type { EnumOption, TagResp } from "#/entity";
import { Button } from "@/ui/button";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/ui/dialog";
import { Input } from "@/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/ui/select";
import { Loader2, Pencil, Plus, Trash2, Search, X } from "lucide-react";
import { useEffect, useMemo, useState } from "react";
import { toast } from "sonner";

interface FormState {
	id?: number;
	name: string;
	type: string;
	parentId: number;
}

interface FlatTag {
	id: number;
	name: string;
	type: string;
	parentId: number;
	path: string;
	createdTime: string;
	isCustom: boolean;
	accountId?: number | null;
	accountNickname?: string | null;
}

const EMPTY_FORM: FormState = { name: "", type: "", parentId: 0 };

const flattenTree = (nodes: TagResp[], parentPath: string): FlatTag[] => {
	const result: FlatTag[] = [];
	for (const node of nodes) {
		result.push({
			id: node.id,
			name: node.name,
			type: node.type,
			parentId: node.parentId,
			path: parentPath || "—",
			createdTime: node.createdTime,
			isCustom: false,
		});
		if (node.children?.length) {
			const childPath = parentPath ? `${parentPath} > ${node.name}` : node.name;
			result.push(...flattenTree(node.children, childPath));
		}
	}
	return result;
};

export default function TagManagementPage() {
	// ===== 枚举选项（从后端拉取） =====
	const [tagTypeOptions, setTagTypeOptions] = useState<EnumOption[]>([]);

	// ===== 列表状态 =====
	const [tree, setTree] = useState<TagResp[]>([]);
	const [customTags, setCustomTags] = useState<TagResp[]>([]);
	const [loading, setLoading] = useState(false);

	// ===== 筛选状态 =====
	const [nameQuery, setNameQuery] = useState("");
	const [typeFilter, setTypeFilter] = useState("all");
	const [appliedNameQuery, setAppliedNameQuery] = useState("");
	const [appliedTypeFilter, setAppliedTypeFilter] = useState("all");

	// ===== 新建/编辑弹窗 =====
	const [formOpen, setFormOpen] = useState(false);
	const [form, setForm] = useState<FormState>(EMPTY_FORM);
	const [saving, setSaving] = useState(false);
	const isEdit = form.id !== undefined;

	// ===== 删除确认 =====
	const [deleteOpen, setDeleteOpen] = useState(false);
	const [deletingId, setDeletingId] = useState<number | null>(null);
	const [deletingCustom, setDeletingCustom] = useState(false);
	const [deleting, setDeleting] = useState(false);

	useEffect(() => {
		tagService.getTagTypes().then(setTagTypeOptions).catch(() => {});
		fetchData();
	}, []);

	const typeLabelMap = new Map(tagTypeOptions.map(o => [o.code, o.displayNameCn]));

	const fetchData = async () => {
		setLoading(true);
		try {
			const [treeData, customData] = await Promise.all([
				tagService.getTagTree(),
				tagService.listAllCustomTags(),
			]);
			setTree(treeData || []);
			setCustomTags(customData || []);
		} catch {
			// apiClient 已 toast
		} finally {
			setLoading(false);
		}
	};

	const flattenForParentSelect = (tags: TagResp[]): { id: number; name: string; depth: number }[] => {
		const result: { id: number; name: string; depth: number }[] = [];
		const walk = (nodes: TagResp[], depth: number) => {
			for (const node of nodes) {
				result.push({ id: node.id, name: node.name, depth });
				if (node.children?.length) walk(node.children, depth + 1);
			}
		};
		walk(tags, 0);
		return result;
	};

	const allFlatTags = useMemo(() => {
		const treeTags = flattenTree(tree, "");
		const custom: FlatTag[] = customTags.map(t => ({
			id: t.id,
			name: t.name,
			type: t.type || "CUSTOM",
			parentId: 0,
			path: "—",
			createdTime: t.createdTime,
			isCustom: true,
			accountId: t.accountId,
			accountNickname: t.accountNickname,
		}));
		return [...treeTags, ...custom];
	}, [tree, customTags]);

	const handleSearch = () => {
		setAppliedNameQuery(nameQuery.trim());
		setAppliedTypeFilter(typeFilter);
	};

	const isFiltering = appliedNameQuery !== "" || appliedTypeFilter !== "all";

	const displayTags = allFlatTags.filter(tag => {
		const nameMatch = !appliedNameQuery || tag.name.toLowerCase().includes(appliedNameQuery.toLowerCase());
		const typeMatch = appliedTypeFilter === "all" || tag.type === appliedTypeFilter;
		return nameMatch && typeMatch;
	});

	const openAdd = () => {
		setForm(EMPTY_FORM);
		setFormOpen(true);
	};

	const openEdit = (tag: FlatTag) => {
		setForm({ id: tag.id, name: tag.name, type: tag.type || "", parentId: tag.parentId });
		setFormOpen(true);
	};

	const handleSave = async () => {
		if (!form.name.trim()) {
			toast.error("标签名称不能为空");
			return;
		}
		setSaving(true);
		try {
			if (isEdit) {
				await tagService.modifyTag({ id: form.id!, name: form.name.trim(), type: form.type || undefined, parentId: form.parentId });
				toast.success("编辑成功");
			} else if (form.type === "CUSTOM") {
				await tagService.addCustomTag(form.name.trim());
				toast.success("新建成功");
			} else {
				await tagService.addTag({ name: form.name.trim(), type: form.type || undefined, parentId: form.parentId });
				toast.success("新建成功");
			}
			setFormOpen(false);
			fetchData();
		} catch {
			// apiClient 已 toast
		} finally {
			setSaving(false);
		}
	};

	const openDelete = (id: number, isCustom: boolean = false) => {
		setDeletingId(id);
		setDeletingCustom(isCustom);
		setDeleteOpen(true);
	};

	const confirmDelete = async () => {
		if (!deletingId) return;
		setDeleting(true);
		try {
			if (deletingCustom) {
				await tagService.deleteCustomTag(deletingId);
			} else {
				await tagService.deleteTag(deletingId);
			}
			toast.success("已删除");
			setDeleteOpen(false);
			setDeletingId(null);
			fetchData();
		} catch {
			// apiClient 已 toast（后端返回引用/子标签拒删原因）
		} finally {
			setDeleting(false);
		}
	};

	const parentOptions = flattenForParentSelect(tree);

	return (
		<div className="flex flex-col gap-4 p-2">
			<div className="flex items-center justify-between">
				<h2 className="text-2xl font-bold whitespace-nowrap">标签管理</h2>
				<Button onClick={openAdd}>
					<Plus className="h-4 w-4 mr-1" />新建标签
				</Button>
			</div>

			{/* 筛选栏 */}
			<div className="flex flex-wrap items-center gap-2">
				<div className="relative w-48">
					<Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
					<Input
						className="pl-8"
						placeholder="搜索标签名称"
						value={nameQuery}
						onChange={e => setNameQuery(e.target.value)}
						onKeyDown={e => e.key === "Enter" && handleSearch()}
					/>
				</div>
				<Select value={typeFilter} onValueChange={v => { setTypeFilter(v); setAppliedTypeFilter(v); }}>
					<SelectTrigger className="w-32">
						<SelectValue placeholder="全部类型" />
					</SelectTrigger>
					<SelectContent>
						<SelectItem value="all">不限</SelectItem>
						{tagTypeOptions.map(o => (
							<SelectItem key={o.code} value={o.code}>{o.displayNameCn}</SelectItem>
						))}
					</SelectContent>
				</Select>
				<Button onClick={handleSearch} disabled={loading}>
					{loading ? <Loader2 className="h-4 w-4 animate-spin mr-1" /> : <Search className="h-4 w-4 mr-1" />}
					查询
				</Button>
				{isFiltering && (
					<Button variant="ghost" size="sm" onClick={() => { setNameQuery(""); setTypeFilter("all"); setAppliedNameQuery(""); setAppliedTypeFilter("all"); }}>
						<X className="h-4 w-4 mr-1" />清除筛选
					</Button>
				)}
			</div>

			{/* 标签表格 */}
			<div className="rounded-xl border overflow-hidden">
				<table className="w-full text-sm">
					<thead className="bg-muted/50">
						<tr>
							<th className="px-4 py-3 text-left font-medium text-text-secondary whitespace-nowrap w-20">ID</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary whitespace-nowrap">名称</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary whitespace-nowrap w-28">类型</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary whitespace-nowrap">路径</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary whitespace-nowrap w-28">所属用户</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary whitespace-nowrap w-36">创建时间</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary whitespace-nowrap w-36">操作</th>
						</tr>
					</thead>
					<tbody className="divide-y">
						{loading ? (
							<tr>
								<td colSpan={7} className="py-12 text-center text-text-secondary">
									<Loader2 className="h-6 w-6 animate-spin mx-auto" />
								</td>
							</tr>
						) : displayTags.length === 0 ? (
							<tr>
								<td colSpan={7} className="py-12 text-center text-text-secondary">
									{isFiltering ? "无匹配结果，请调整筛选条件" : "暂无标签数据"}
								</td>
							</tr>
						) : (
							displayTags.map(tag => (
								<tr key={`${tag.isCustom ? "c" : "t"}-${tag.id}`} className="hover:bg-muted/30 transition-colors">
									<td className="px-4 py-3 text-text-secondary">{tag.id}</td>
									<td className="px-4 py-3 text-sm max-w-[120px] truncate" title={tag.name || ""}>{tag.name}</td>
									<td className="px-4 py-3">
										{tag.type ? (
											<span className="px-2 py-0.5 rounded-full text-xs bg-muted">{typeLabelMap.get(tag.type) || tag.type}</span>
										) : (
											<span className="text-xs text-muted-foreground">—</span>
										)}
									</td>
									<td className="px-4 py-3 text-text-secondary text-xs max-w-[160px] truncate" title={tag.path || ""}>{tag.path}</td>
									<td className="px-4 py-3 text-text-secondary text-xs max-w-[120px] truncate" title={tag.accountNickname || ""}>{tag.accountNickname || "—"}</td>
									<td className="px-4 py-3 text-text-secondary text-xs whitespace-nowrap">
										{tag.createdTime?.replace("T", " ").substring(0, 16) || "—"}
									</td>
									<td className="px-4 py-3">
										<div className="flex items-center gap-1">
											<Button
												size="sm"
												variant="ghost"
												className={`h-7 px-2 text-xs ${tag.isCustom ? "invisible" : ""}`}
												onClick={() => openEdit(tag)}
												tabIndex={tag.isCustom ? -1 : undefined}
											>
												<Pencil className="h-3 w-3 mr-1" />编辑
											</Button>
											<Button
												size="sm"
												variant="ghost"
												className="h-7 px-2 text-xs text-destructive hover:text-destructive"
												onClick={() => openDelete(tag.id, tag.isCustom)}
											>
												<Trash2 className="h-3 w-3 mr-1" />删除
											</Button>
										</div>
									</td>
								</tr>
							))
						)}
					</tbody>
				</table>
			</div>

			{/* 新建/编辑弹窗 */}
			<Dialog open={formOpen} onOpenChange={open => { if (!open) setFormOpen(false); }}>
				<DialogContent className="sm:max-w-md">
					<DialogHeader>
						<DialogTitle>{isEdit ? "编辑标签" : "新建标签"}</DialogTitle>
					</DialogHeader>
					<div className="space-y-4 py-2">
						<div>
							<label className="text-xs text-muted-foreground mb-1 block">名称 <span className="text-destructive">*</span></label>
							<Input
								value={form.name}
								onChange={e => setForm(prev => ({ ...prev, name: e.target.value }))}
								placeholder="请输入标签名称"
								maxLength={128}
							/>
						</div>
						<div>
							<label className="text-xs text-muted-foreground mb-1 block">类型（可选）</label>
							<Select value={form.type || "none"} onValueChange={v => setForm(prev => ({ ...prev, type: v === "none" ? "" : v, parentId: v === "CUSTOM" ? 0 : prev.parentId }))}>
								<SelectTrigger>
									<SelectValue placeholder="选择类型" />
								</SelectTrigger>
								<SelectContent>
									<SelectItem value="none">无</SelectItem>
									{tagTypeOptions.map(o => (
										<SelectItem key={o.code} value={o.code}>{o.displayNameCn}</SelectItem>
									))}
								</SelectContent>
							</Select>
						</div>
						{form.type !== "CUSTOM" && (
							<div>
								<label className="text-xs text-muted-foreground mb-1 block">父标签</label>
								<Select value={String(form.parentId)} onValueChange={v => setForm(prev => ({ ...prev, parentId: Number(v) }))}>
									<SelectTrigger>
										<SelectValue placeholder="选择父标签" />
									</SelectTrigger>
									<SelectContent>
										<SelectItem value="0">无（顶级）</SelectItem>
										{parentOptions
											.filter(p => p.id !== form.id)
											.map(p => (
												<SelectItem key={p.id} value={String(p.id)}>
													{"　".repeat(p.depth)}{p.name}
												</SelectItem>
											))}
									</SelectContent>
								</Select>
							</div>
						)}
					</div>
					<DialogFooter>
						<Button variant="outline" onClick={() => setFormOpen(false)}>取消</Button>
						<Button onClick={handleSave} disabled={saving}>
							{saving ? <Loader2 className="h-4 w-4 animate-spin mr-1" /> : null}
							{isEdit ? "保存" : "新建"}
						</Button>
					</DialogFooter>
				</DialogContent>
			</Dialog>

			{/* 删除确认弹窗 */}
			<Dialog open={deleteOpen} onOpenChange={setDeleteOpen}>
				<DialogContent className="sm:max-w-md">
					<DialogHeader>
						<DialogTitle>确认删除</DialogTitle>
					</DialogHeader>
					<p className="text-sm text-muted-foreground py-2">删除后无法恢复，确认删除该标签吗？</p>
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
