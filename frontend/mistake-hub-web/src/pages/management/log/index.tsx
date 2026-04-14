import logService from "@/api/services/logService";
import type { OperationLogResp } from "#/entity";
import { Button } from "@/ui/button";
import { Input } from "@/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/ui/select";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/ui/dialog";
import { Eye, Loader2, Search } from "lucide-react";
import { useEffect, useState } from "react";

const PAGE_SIZE = 15;

const ACTION_OPTIONS = [
	{ label: "全部操作", value: "all" },
	{ label: "角色变更", value: "account/change-role" },
	{ label: "重置密码", value: "account/reset-password" },
	{ label: "新增标签", value: "tag/add" },
	{ label: "更新标签", value: "tag/update" },
	{ label: "删除标签", value: "tag/delete" },
	{ label: "更新配置", value: "config/update" },
	{ label: "管理员编辑错题", value: "mistake/admin-update" },
];

export default function LogManagementPage() {

	// ===== 列表状态 =====
	const [logs, setLogs] = useState<OperationLogResp[]>([]);
	const [total, setTotal] = useState(0);
	const [loading, setLoading] = useState(false);
	const [pageNum, setPageNum] = useState(1);

	// ===== 筛选状态 =====
	const [codeFilter, setCodeFilter] = useState("");
	const [actionFilter, setActionFilter] = useState("all");
	const [startDate, setStartDate] = useState("");
	const [endDate, setEndDate] = useState("");

	// ===== 详情弹窗 =====
	const [detailOpen, setDetailOpen] = useState(false);
	const [selectedLog, setSelectedLog] = useState<OperationLogResp | null>(null);

	const fetchLogs = async (page = pageNum) => {

		setLoading(true);
		try {
			const res = await logService.listLogs({
				accountId: undefined,
				action: actionFilter !== "all" ? actionFilter : undefined,
				startTime: startDate || undefined,
				endTime: endDate || undefined,
				pageNum: page,
				pageSize: PAGE_SIZE,
			});
			setLogs(res.records);
			setTotal(res.total || res.records.length);
		} catch {
			// apiClient 已 toast
		} finally {
			setLoading(false);
		}
	};

	useEffect(() => {
		fetchLogs(1);
	}, []);

	const handleSearch = () => {

		setPageNum(1);
		fetchLogs(1);
	};

	const openDetail = (log: OperationLogResp) => {

		setSelectedLog(log);
		setDetailOpen(true);
	};

	const formatDetail = (detail?: string): string => {

		if (!detail) return "—";
		try {
			return JSON.stringify(JSON.parse(detail), null, 2);
		} catch {
			return detail;
		}
	};

	const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));

	const isFiltering = codeFilter.trim() !== "" || actionFilter !== "all" || startDate !== "" || endDate !== "";

	return (
		<div className="flex flex-col gap-4 p-2">
			<div className="flex items-center justify-between">
				<h2 className="text-2xl font-bold">操作日志</h2>
			</div>

			{/* 筛选栏 */}
			<div className="flex flex-wrap items-center gap-2">
				<div className="relative w-40">
					<Search className="absolute left-2.5 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
					<Input
						className="pl-8"
						placeholder="操作人 code"
						value={codeFilter}
						onChange={e => setCodeFilter(e.target.value)}
						onKeyDown={e => e.key === "Enter" && handleSearch()}
					/>
				</div>
				<Select value={actionFilter} onValueChange={v => { setActionFilter(v); }}>
					<SelectTrigger className="w-44">
						<SelectValue placeholder="操作类型" />
					</SelectTrigger>
					<SelectContent>
						{ACTION_OPTIONS.map(o => (
							<SelectItem key={o.value} value={o.value}>{o.label}</SelectItem>
						))}
					</SelectContent>
				</Select>
				<Input
					type="date"
					className="w-36"
					value={startDate}
					onChange={e => setStartDate(e.target.value)}
					placeholder="开始日期"
				/>
				<span className="text-muted-foreground">—</span>
				<Input
					type="date"
					className="w-36"
					value={endDate}
					onChange={e => setEndDate(e.target.value)}
					placeholder="结束日期"
				/>
				<Button onClick={handleSearch} disabled={loading}>
					{loading ? <Loader2 className="h-4 w-4 animate-spin mr-1" /> : <Search className="h-4 w-4 mr-1" />}
					查询
				</Button>
			</div>

			{/* 日志列表表格 */}
			<div className="rounded-xl border overflow-hidden">
				<table className="w-full text-sm">
					<thead className="bg-muted/50">
						<tr>
							<th className="px-4 py-3 text-left font-medium text-text-secondary">时间</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary">操作人</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary">操作类型</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary">目标类型</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary">目标ID</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary">操作</th>
						</tr>
					</thead>
					<tbody className="divide-y">
						{loading ? (
							<tr>
								<td colSpan={6} className="py-12 text-center text-text-secondary">
									<Loader2 className="h-6 w-6 animate-spin mx-auto" />
								</td>
							</tr>
						) : logs.length === 0 ? (
							<tr>
								<td colSpan={6} className="py-12 text-center text-text-secondary">
									{isFiltering ? "无匹配结果，请调整筛选条件" : "暂无数据"}
								</td>
							</tr>
						) : (
							logs.map(log => (
								<tr key={log.id} className="hover:bg-muted/30 transition-colors">
									<td className="px-4 py-3 text-xs whitespace-nowrap">
										{log.createdTime?.replace("T", " ").substring(0, 19) || "—"}
									</td>
									<td className="px-4 py-3 text-sm">{log.accountCode || "—"}</td>
									<td className="px-4 py-3">
										<span className="px-2 py-0.5 rounded-full text-xs font-medium bg-muted">
											{log.action}
										</span>
									</td>
									<td className="px-4 py-3 text-sm text-muted-foreground">{log.targetType || "—"}</td>
									<td className="px-4 py-3 text-sm text-muted-foreground">{log.targetId || "—"}</td>
									<td className="px-4 py-3">
										<Button size="sm" variant="ghost" className="h-7 px-2 text-xs" onClick={() => openDetail(log)}>
											<Eye className="h-3 w-3 mr-1" />详情
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
						onClick={() => { setPageNum(p => p - 1); fetchLogs(pageNum - 1); }}
					>上一页</Button>
					<span className="px-2">{pageNum} / {totalPages}</span>
					<Button
						size="sm"
						variant="outline"
						disabled={pageNum >= totalPages}
						onClick={() => { setPageNum(p => p + 1); fetchLogs(pageNum + 1); }}
					>下一页</Button>
				</div>
			</div>

			{/* 详情弹窗 */}
			<Dialog open={detailOpen} onOpenChange={setDetailOpen}>
				<DialogContent className="sm:max-w-2xl max-h-[85vh] overflow-y-auto">
					<DialogHeader>
						<DialogTitle>操作日志详情</DialogTitle>
					</DialogHeader>
					{selectedLog && (
						<div className="space-y-3 py-2 text-sm">
							<div className="grid grid-cols-2 gap-3">
								<div>
									<span className="text-muted-foreground">操作时间：</span>
									{selectedLog.createdTime?.replace("T", " ").substring(0, 19) || "—"}
								</div>
								<div>
									<span className="text-muted-foreground">操作人：</span>
									{selectedLog.accountCode || "—"}
								</div>
								<div>
									<span className="text-muted-foreground">操作类型：</span>
									{selectedLog.action}
								</div>
								<div>
									<span className="text-muted-foreground">目标类型：</span>
									{selectedLog.targetType || "—"}
								</div>
								<div>
									<span className="text-muted-foreground">目标ID：</span>
									{selectedLog.targetId || "—"}
								</div>
							</div>
							<div>
								<span className="text-muted-foreground block mb-1">操作详情：</span>
								<pre className="bg-muted/50 rounded-lg p-3 text-xs overflow-x-auto whitespace-pre-wrap break-all max-h-80">
									{formatDetail(selectedLog.detail)}
								</pre>
							</div>
						</div>
					)}
					<DialogFooter>
						<Button variant="outline" onClick={() => setDetailOpen(false)}>关闭</Button>
					</DialogFooter>
				</DialogContent>
			</Dialog>
		</div>
	);
}
