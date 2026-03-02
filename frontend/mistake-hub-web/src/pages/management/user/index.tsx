import userService from "@/api/services/userService";
import type { UserInfo } from "#/entity";
import { Icon } from "@/components/icon";
import { Button } from "@/ui/button";
import { Dialog, DialogContent, DialogFooter, DialogHeader, DialogTitle } from "@/ui/dialog";
import { Input } from "@/ui/input";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/ui/select";
import { Copy, Loader2, RotateCcw, ShieldCheck, ShieldOff } from "lucide-react";
import { useEffect, useState } from "react";
import { toast } from "sonner";

type ActionType = "upgrade" | "downgrade" | "reset";

const ACTION_CONFIRM: Record<ActionType, string> = {
	upgrade: "确定将该用户升级为管理员？系统将自动生成新密码。",
	downgrade: "确定将该用户降级为学生？密码将被清除并强制下线。",
	reset: "确定重置该用户密码？重置后该用户将被强制下线。",
};

const PAGE_SIZE = 10;

export default function UserManagementPage() {
	// ===== 列表状态 =====
	const [codeFilter, setCodeFilter] = useState("");
	const [nicknameFilter, setNicknameFilter] = useState("");
	const [roleFilter, setRoleFilter] = useState<string>("all");
	const [loading, setLoading] = useState(false);
	const [users, setUsers] = useState<UserInfo[]>([]);
	const [total, setTotal] = useState(0);
	const [pageNum, setPageNum] = useState(1);

	// ===== 操作状态 =====
	const [confirmOpen, setConfirmOpen] = useState(false);
	const [pendingAction, setPendingAction] = useState<ActionType | null>(null);
	const [targetUser, setTargetUser] = useState<UserInfo | null>(null);
	const [actionLoading, setActionLoading] = useState(false);

	// ===== 密码展示 =====
	const [pwdOpen, setPwdOpen] = useState(false);
	const [plainPwd, setPlainPwd] = useState("");

	const fetchUsers = async (page = pageNum) => {
		setLoading(true);
		try {
			const result = await userService.listUsers({
				code: codeFilter.trim() || undefined,
				nickname: nicknameFilter.trim() || undefined,
				role: roleFilter === "all" ? undefined : roleFilter,
				pageNum: page,
				pageSize: PAGE_SIZE,
			});
			setUsers(result.records);
			setTotal(result.total || result.records.length);
		} finally {
			setLoading(false);
		}
	};

	useEffect(() => {
		fetchUsers(1);
		setPageNum(1);
	}, [roleFilter]);

	const handleSearch = () => {
		setPageNum(1);
		fetchUsers(1);
	};

	const openConfirm = (action: ActionType, user: UserInfo) => {
		setTargetUser(user);
		setPendingAction(action);
		setConfirmOpen(true);
	};

	const executeAction = async () => {
		if (!targetUser || !pendingAction) return;
		setConfirmOpen(false);
		setActionLoading(true);
		try {
			if (pendingAction === "upgrade") {
				const pwd = await userService.changeRole(targetUser.id as unknown as number, "admin");
				if (pwd) { setPlainPwd(pwd); setPwdOpen(true); }
				toast.success("升级成功");
			} else if (pendingAction === "downgrade") {
				await userService.changeRole(targetUser.id as unknown as number, "student");
				toast.success("已降级为学生");
			} else {
				const pwd = await userService.resetPassword(targetUser.id as unknown as number);
				setPlainPwd(pwd);
				setPwdOpen(true);
				toast.success("密码已重置");
			}
			fetchUsers(pageNum);
		} finally {
			setActionLoading(false);
			setPendingAction(null);
			setTargetUser(null);
		}
	};

	const copyPwd = () => {
		navigator.clipboard.writeText(plainPwd).then(() => toast.success("已复制到剪贴板"));
	};

	const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));

	return (
		<div className="flex flex-col gap-4 p-2">
			<h2 className="text-2xl font-bold">用户管理</h2>

			{/* 筛选栏 */}
			<div className="flex flex-wrap gap-2">
				<Input
					className="max-w-[160px]"
					placeholder="code"
					value={codeFilter}
					onChange={e => setCodeFilter(e.target.value)}
					onKeyDown={e => e.key === "Enter" && handleSearch()}
				/>
				<Input
					className="max-w-[160px]"
					placeholder="昵称"
					value={nicknameFilter}
					onChange={e => setNicknameFilter(e.target.value)}
					onKeyDown={e => e.key === "Enter" && handleSearch()}
				/>
				<Select value={roleFilter} onValueChange={setRoleFilter}>
					<SelectTrigger className="w-32">
						<SelectValue placeholder="角色" />
					</SelectTrigger>
					<SelectContent>
						<SelectItem value="all">全部角色</SelectItem>
						<SelectItem value="admin">管理员</SelectItem>
						<SelectItem value="student">学生</SelectItem>
					</SelectContent>
				</Select>
				<Button onClick={handleSearch} disabled={loading}>
					{loading ? <Loader2 className="h-4 w-4 animate-spin mr-1" /> : <Icon icon="solar:magnifer-bold" size={16} className="mr-1" />}
					查询
				</Button>
			</div>

			{/* 用户列表表格 */}
			<div className="rounded-xl border overflow-hidden">
				<table className="w-full text-sm">
					<thead className="bg-muted/50">
						<tr>
							<th className="px-4 py-3 text-left font-medium text-text-secondary">ID</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary">头像</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary">code</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary">昵称</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary">角色</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary">每日复习量</th>
							<th className="px-4 py-3 text-left font-medium text-text-secondary">注册时间</th>
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
						) : users.length === 0 ? (
							<tr>
								<td colSpan={6} className="py-12 text-center text-text-secondary">暂无数据</td>
							</tr>
						) : (
							users.map(user => {
								const isAdmin = (user.role as unknown as string) === "admin";
								return (
									<tr key={user.id as unknown as number} className="hover:bg-muted/30 transition-colors">
										<td className="px-4 py-3 text-text-secondary">{user.id}</td>
										<td className="px-4 py-3">
											{user.avatarUrl
												? <img src={user.avatarUrl} alt="" className="h-8 w-8 rounded-full object-cover" />
												: <span className="inline-flex h-8 w-8 items-center justify-center rounded-full bg-primary/10 text-primary text-sm font-medium">
													{(user.nickname || user.code || "?").charAt(0).toUpperCase()}
												</span>
											}
										</td>
										<td className="px-4 py-3 font-medium">{user.code}</td>
										<td className="px-4 py-3">{user.nickname || "—"}</td>
										<td className="px-4 py-3">
											<span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium ${
												isAdmin ? "bg-primary/10 text-primary" : "bg-muted text-muted-foreground"
											}`}>
												<Icon icon={isAdmin ? "solar:shield-user-bold-duotone" : "solar:user-bold-duotone"} size={12} />
												{isAdmin ? "管理员" : "学生"}
											</span>
										</td>
										<td className="px-4 py-3 text-text-secondary">{user.dailyLimit ?? "—"}</td>
										<td className="px-4 py-3 text-text-secondary text-xs whitespace-nowrap">
											{user.createdTime ? user.createdTime.replace("T", " ").substring(0, 16) : "—"}
										</td>
										<td className="px-4 py-3">
											<div className="flex items-center gap-1">
												{!isAdmin && (
													<Button
														size="sm"
														variant="ghost"
														className="h-7 px-2 text-xs"
														disabled={actionLoading}
														onClick={() => openConfirm("upgrade", user)}
													>
														<ShieldCheck className="h-3 w-3 mr-1" />升级
													</Button>
												)}
												{isAdmin && (
													<>
														<Button
															size="sm"
															variant="ghost"
															className="h-7 px-2 text-xs"
															disabled={actionLoading}
															onClick={() => openConfirm("downgrade", user)}
														>
															<ShieldOff className="h-3 w-3 mr-1" />降级
														</Button>
														<Button
															size="sm"
															variant="ghost"
															className="h-7 px-2 text-xs"
															disabled={actionLoading}
															onClick={() => openConfirm("reset", user)}
														>
															<RotateCcw className="h-3 w-3 mr-1" />重置密码
														</Button>
													</>
												)}
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
				<span>共 {total || users.length} 条记录</span>
				<div className="flex items-center gap-2">
					<Button
						size="sm"
						variant="outline"
						disabled={pageNum <= 1}
						onClick={() => { setPageNum(p => p - 1); fetchUsers(pageNum - 1); }}
					>上一页</Button>
					<span className="px-2">{pageNum} / {totalPages}</span>
					<Button
						size="sm"
						variant="outline"
						disabled={pageNum >= totalPages}
						onClick={() => { setPageNum(p => p + 1); fetchUsers(pageNum + 1); }}
					>下一页</Button>
				</div>
			</div>

			{/* 操作确认 Dialog */}
			<Dialog open={confirmOpen} onOpenChange={setConfirmOpen}>
				<DialogContent className="sm:max-w-md">
					<DialogHeader>
						<DialogTitle>操作确认</DialogTitle>
					</DialogHeader>
					<div className="py-2">
						<p className="text-sm text-muted-foreground">{pendingAction ? ACTION_CONFIRM[pendingAction] : ""}</p>
						{targetUser && (
							<p className="mt-2 text-sm">
								目标用户：<span className="font-medium">{targetUser.code}</span>
								{targetUser.nickname && <> · {targetUser.nickname}</>}
							</p>
						)}
					</div>
					<DialogFooter>
						<Button variant="outline" onClick={() => setConfirmOpen(false)}>取消</Button>
						<Button onClick={executeAction}>确认</Button>
					</DialogFooter>
				</DialogContent>
			</Dialog>

			{/* 密码展示 Dialog */}
			<Dialog open={pwdOpen} onOpenChange={setPwdOpen}>
				<DialogContent className="sm:max-w-md">
					<DialogHeader>
						<DialogTitle>新密码</DialogTitle>
					</DialogHeader>
					<div className="space-y-3 py-2">
						<p className="text-sm font-medium text-warning">⚠️ 密码仅展示一次，请务必复制保存</p>
						<div className="flex gap-2">
							<Input value={plainPwd} readOnly className="font-mono" />
							<Button size="icon" variant="outline" onClick={copyPwd}>
								<Copy className="h-4 w-4" />
							</Button>
						</div>
					</div>
					<DialogFooter>
						<Button onClick={() => setPwdOpen(false)}>关闭</Button>
					</DialogFooter>
				</DialogContent>
			</Dialog>
		</div>
	);
}
