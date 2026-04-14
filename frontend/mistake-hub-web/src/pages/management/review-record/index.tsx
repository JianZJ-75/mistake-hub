import reviewService from "@/api/services/reviewService";
import userService from "@/api/services/userService";
import type { ReviewRecordAdminResp, UserInfo } from "#/entity";
import { Badge } from "@/ui/badge";
import { Button } from "@/ui/button";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/ui/dialog";
import { Input } from "@/ui/input";
import { Popover, PopoverContent, PopoverTrigger } from "@/ui/popover";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/ui/select";
import { Command, CommandEmpty, CommandInput, CommandItem, CommandList } from "@/ui/command";
import { ChevronDown, Eye, Loader2, Search, X } from "lucide-react";
import { useEffect, useRef, useState } from "react";

const PAGE_SIZE = 10;

const CORRECT_OPTIONS = [
	{ label: "全部结果", value: "all" },
	{ label: "答对", value: "1" },
	{ label: "答错", value: "0" },
];

export default function ReviewRecordPage() {

	// ===== 列表状态 =====
	const [records, setRecords] = useState<ReviewRecordAdminResp[]>([]);
	const [total, setTotal] = useState(0);
	const [loading, setLoading] = useState(false);
	const [pageNum, setPageNum] = useState(1);

	// ===== 筛选状态 =====
	const [correctFilter, setCorrectFilter] = useState("all");
	const [startTime, setStartTime] = useState("");
	const [endTime, setEndTime] = useState("");

	// ===== 学生筛选 Combobox =====
	const [studentPopoverOpen, setStudentPopoverOpen] = useState(false);
	const [studentSearchQuery, setStudentSearchQuery] = useState("");
	const [studentOptions, setStudentOptions] = useState<UserInfo[]>([]);
	const [studentSearching, setStudentSearching] = useState(false);
	const [selectedStudent, setSelectedStudent] = useState<{ id: string; label: string } | null>(null);
	const studentSearchTimer = useRef<ReturnType<typeof setTimeout> | null>(null);

	// ===== 备注详情弹窗 =====
	const [noteOpen, setNoteOpen] = useState(false);
	const [noteRecord, setNoteRecord] = useState<ReviewRecordAdminResp | null>(null);
	const [previewUrl, setPreviewUrl] = useState("");

	useEffect(() => {
		fetchRecords(1);
	}, []);

	const fetchRecords = async (page: number, overrides?: { correct?: string; account?: string; start?: string; end?: string }) => {

		setLoading(true);
		try {
			const cVal = overrides?.correct ?? correctFilter;
			const aVal = overrides?.account ?? (selectedStudent?.id ?? "all");
			const sVal = overrides?.start ?? startTime;
			const eVal = overrides?.end ?? endTime;
			const res = await reviewService.listReviewRecords({
				accountId: aVal !== "all" ? Number(aVal) : undefined,
				isCorrect: cVal !== "all" ? Number(cVal) : undefined,
				startTime: sVal || undefined,
				endTime: eVal || undefined,
				pageNum: page,
				pageSize: PAGE_SIZE,
			});
			setRecords(res.records);
			setTotal(res.total || res.records.length);
		} catch {
			// apiClient 已 toast
		} finally {
			setLoading(false);
		}
	};

	const handleSearch = () => {

		setPageNum(1);
		fetchRecords(1);
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
		fetchRecords(1, { account: String(student.id) });
	};

	const clearStudent = () => {

		setSelectedStudent(null);
		setPageNum(1);
		fetchRecords(1, { account: "all" });
	};

	const openNote = (record: ReviewRecordAdminResp) => {

		setNoteRecord(record);
		setNoteOpen(true);
	};

	const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));
	const isFiltering = selectedStudent !== null || correctFilter !== "all" || startTime !== "" || endTime !== "";

	return (
		<div className="flex flex-col gap-4 p-2">
			<div className="flex items-center justify-between">
				<h2 className="text-2xl font-bold">复习记录</h2>
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

				<Select value={correctFilter} onValueChange={v => { setCorrectFilter(v); setPageNum(1); fetchRecords(1, { correct: v }); }}>
					<SelectTrigger className="w-32">
						<SelectValue placeholder="答题结果" />
					</SelectTrigger>
					<SelectContent>
						{CORRECT_OPTIONS.map(o => (
							<SelectItem key={o.value} value={o.value}>{o.label}</SelectItem>
						))}
					</SelectContent>
				</Select>

				<Input
					type="datetime-local"
					className="w-48"
					value={startTime}
					onChange={e => { setStartTime(e.target.value); setPageNum(1); fetchRecords(1, { start: e.target.value }); }}
					placeholder="开始时间"
				/>
				<span className="text-muted-foreground text-sm">至</span>
				<Input
					type="datetime-local"
					className="w-48"
					value={endTime}
					onChange={e => { setEndTime(e.target.value); setPageNum(1); fetchRecords(1, { end: e.target.value }); }}
					placeholder="结束时间"
				/>

				<Button onClick={handleSearch} disabled={loading}>
					{loading ? <Loader2 className="h-4 w-4 animate-spin mr-1" /> : <Search className="h-4 w-4 mr-1" />}
					查询
				</Button>
			</div>

			{/* 列表表格 */}
			<div className="rounded-xl border overflow-hidden">
				<table className="w-full text-sm">
					<thead className="bg-muted/50">
						<tr>
							<th className="px-4 py-3 text-left font-medium text-text-secondary">复习时间</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary">所属用户</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary">关联错题</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary">答题结果</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary">阶段变化</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary">掌握度变化</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary">备注</th>
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
						) : records.length === 0 ? (
							<tr>
								<td colSpan={8} className="py-12 text-center text-text-secondary">
									{isFiltering ? "无匹配结果，请调整筛选条件" : "暂无数据"}
								</td>
							</tr>
						) : (
							records.map(r => (
								<tr key={r.id} className="hover:bg-muted/30 transition-colors">
									<td className="px-4 py-3 text-xs whitespace-nowrap">
										{r.reviewTime?.replace("T", " ").substring(0, 16) || "—"}
									</td>
									<td className="px-4 py-3 text-sm">{r.accountNickname || r.accountCode || "—"}</td>
									<td className="px-4 py-3 max-w-[200px]">
										<span className="line-clamp-1 text-sm">{r.mistakeTitle || "—"}</span>
									</td>
									<td className="px-4 py-3">
										<Badge variant={r.isCorrect === 1 ? "default" : "destructive"} className="text-xs">
											{r.isCorrect === 1 ? "答对" : "答错"}
										</Badge>
									</td>
									<td className="px-4 py-3 text-xs text-text-secondary">
										{r.reviewStageBefore} → {r.reviewStageAfter}
									</td>
									<td className="px-4 py-3 text-xs text-text-secondary">
										{r.masteryBefore}% → {r.masteryAfter}%
									</td>
									<td className="px-4 py-3 text-xs max-w-[120px]">
										<span className="line-clamp-1">{r.note || "—"}</span>
									</td>
									<td className="px-4 py-3">
										<Button size="sm" variant="ghost" className="h-7 px-2 text-xs" onClick={() => openNote(r)}>
											<Eye className="h-3 w-3 mr-1" />查看
										</Button>
									</td>
								</tr>
							))
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
						onClick={() => { setPageNum(p => p - 1); fetchRecords(pageNum - 1); }}
					>上一页</Button>
					<span className="px-2">{pageNum} / {totalPages}</span>
					<Button
						size="sm"
						variant="outline"
						disabled={pageNum >= totalPages}
						onClick={() => { setPageNum(p => p + 1); fetchRecords(pageNum + 1); }}
					>下一页</Button>
				</div>
			</div>

			{/* 备注详情弹窗 */}
			<Dialog open={noteOpen} onOpenChange={v => { if (!previewUrl) setNoteOpen(v); }}>
				<DialogContent className="sm:max-w-lg">
					<DialogHeader>
						<DialogTitle>复习记录详情</DialogTitle>
					</DialogHeader>
					{noteRecord && (
						<div className="space-y-4 py-2 max-h-[60vh] overflow-y-auto">
							<div className="grid grid-cols-2 gap-2 text-sm">
								<div>
									<span className="text-muted-foreground">用户：</span>
									{noteRecord.accountNickname || noteRecord.accountCode}
								</div>
								<div>
									<span className="text-muted-foreground">时间：</span>
									{noteRecord.reviewTime?.replace("T", " ").substring(0, 16)}
								</div>
								<div>
									<span className="text-muted-foreground">结果：</span>
									<Badge variant={noteRecord.isCorrect === 1 ? "default" : "destructive"} className="text-xs ml-1">
										{noteRecord.isCorrect === 1 ? "答对" : "答错"}
									</Badge>
								</div>
								<div>
									<span className="text-muted-foreground">掌握度：</span>
									{noteRecord.masteryBefore}% → {noteRecord.masteryAfter}%
								</div>
							</div>

							<div>
								<p className="text-xs text-muted-foreground mb-1">关联错题</p>
								<p className="text-sm">{noteRecord.mistakeTitle || "—"}</p>
							</div>

							<div>
								<p className="text-xs text-muted-foreground mb-1">备注</p>
								<p className="text-sm leading-relaxed whitespace-pre-wrap">{noteRecord.note || "无备注"}</p>
							</div>

							{noteRecord.noteImageUrl && (
								<div>
									<p className="text-xs text-muted-foreground mb-1">备注图片</p>
									<img
										src={noteRecord.noteImageUrl}
										alt="备注图片"
										className="max-w-full max-h-[300px] object-contain rounded-lg border cursor-pointer hover:opacity-80 transition-opacity"
										onClick={() => setPreviewUrl(noteRecord.noteImageUrl!)}
									/>
								</div>
							)}
						</div>
					)}
					<DialogFooter>
						<Button variant="outline" onClick={() => setNoteOpen(false)}>关闭</Button>
					</DialogFooter>
				</DialogContent>
			</Dialog>

			{/* 图片放大预览 */}
			{previewUrl && (
				<>
					<div className="fixed inset-0 z-[100] bg-black/70" onClick={e => { e.stopPropagation(); setPreviewUrl(""); }} onPointerDown={e => e.stopPropagation()} />
					<div className="fixed inset-0 z-[101] flex items-center justify-center pointer-events-none">
						<div className="relative pointer-events-auto">
							<img
								src={previewUrl}
								alt="预览"
								className="max-w-[90vw] max-h-[90vh] object-contain rounded-lg"
							/>
							<button
								type="button"
								className="absolute -top-3 -right-3 w-8 h-8 rounded-full bg-white text-black flex items-center justify-center shadow-lg hover:bg-gray-100 transition-colors"
								onClick={() => setPreviewUrl("")}
							>
								<X className="h-4 w-4" />
							</button>
						</div>
					</div>
				</>
			)}
		</div>
	);
}
