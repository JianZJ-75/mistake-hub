import { useLoginStateContext } from "@/pages/sys/login/providers/login-provider";
import { useRouter } from "@/routes/hooks";
import { useUserInfo, useClearUserInfoAndToken } from "@/store/userStore";
import { Button } from "@/ui/button";
import {
	Dialog,
	DialogContent,
	DialogFooter,
	DialogHeader,
	DialogTitle,
} from "@/ui/dialog";
import {
	DropdownMenu,
	DropdownMenuContent,
	DropdownMenuItem,
	DropdownMenuSeparator,
	DropdownMenuTrigger,
} from "@/ui/dropdown-menu";
import { Input } from "@/ui/input";
import { Label } from "@/ui/label";
import { Loader2 } from "lucide-react";
import { useState } from "react";
import { toast } from "sonner";
import userService from "@/api/services/userService";

/**
 * 顶部栏右侧用户下拉菜单
 */
export default function AccountDropdown() {
	const { replace } = useRouter();
	const { nickname, avatarUrl, code } = useUserInfo();
	const clearUserInfoAndToken = useClearUserInfoAndToken();
	const { backToLogin } = useLoginStateContext();

	const [changePwdOpen, setChangePwdOpen] = useState(false);
	const [oldPwd, setOldPwd] = useState("");
	const [newPwd, setNewPwd] = useState("");
	const [confirmPwd, setConfirmPwd] = useState("");
	const [loading, setLoading] = useState(false);

	const logout = () => {
		try {
			clearUserInfoAndToken();
			backToLogin();
		} catch (error) {
			console.log(error);
		} finally {
			replace("/auth/login");
		}
	};

	const handleChangePwd = async () => {
		if (!oldPwd || !newPwd || !confirmPwd) {
			toast.error("请填写所有密码字段");
			return;
		}
		if (newPwd !== confirmPwd) {
			toast.error("两次输入的新密码不一致");
			return;
		}
		if (newPwd.length < 6) {
			toast.error("新密码长度不能少于 6 位");
			return;
		}
		setLoading(true);
		try {
			await userService.changePassword(oldPwd, newPwd);
			toast.success("密码修改成功，请重新登录");
			setChangePwdOpen(false);
			// 清除 token 强制重新登录
			clearUserInfoAndToken();
			replace("/auth/login");
		} finally {
			setLoading(false);
		}
	};

	const handleOpenChange = (open: boolean) => {
		if (!open) {
			setOldPwd("");
			setNewPwd("");
			setConfirmPwd("");
		}
		setChangePwdOpen(open);
	};

	return (
		<>
			<DropdownMenu>
				<DropdownMenuTrigger asChild>
					<Button variant="ghost" size="icon" className="rounded-full">
						{avatarUrl ? (
							<img className="h-6 w-6 rounded-full" src={avatarUrl} alt={nickname} />
						) : (
							<span className="h-6 w-6 rounded-full bg-primary text-primary-foreground text-xs flex items-center justify-center font-medium">
								{(nickname || code || "A").charAt(0).toUpperCase()}
							</span>
						)}
					</Button>
				</DropdownMenuTrigger>
				<DropdownMenuContent className="w-56">
					<div className="flex items-center gap-2 p-2">
						{avatarUrl ? (
							<img className="h-10 w-10 rounded-full" src={avatarUrl} alt={nickname} />
						) : (
							<span className="h-10 w-10 rounded-full bg-primary text-primary-foreground text-sm flex items-center justify-center font-medium">
								{(nickname || code || "A").charAt(0).toUpperCase()}
							</span>
						)}
						<div className="flex flex-col items-start">
							<div className="text-text-primary text-sm font-medium">{nickname || code}</div>
							<div className="text-text-secondary text-xs">{code}</div>
						</div>
					</div>
					<DropdownMenuSeparator />
					<DropdownMenuItem onClick={() => setChangePwdOpen(true)}>修改密码</DropdownMenuItem>
					<DropdownMenuSeparator />
					<DropdownMenuItem className="font-bold text-warning" onClick={logout}>
						退出登录
					</DropdownMenuItem>
				</DropdownMenuContent>
			</DropdownMenu>

			{/* 修改密码 Modal */}
			<Dialog open={changePwdOpen} onOpenChange={handleOpenChange}>
				<DialogContent className="sm:max-w-md">
					<DialogHeader>
						<DialogTitle>修改密码</DialogTitle>
					</DialogHeader>
					<div className="space-y-4 py-2">
						<div className="space-y-1">
							<Label>旧密码</Label>
							<Input
								type="password"
								value={oldPwd}
								onChange={(e) => setOldPwd(e.target.value)}
								placeholder="请输入旧密码"
							/>
						</div>
						<div className="space-y-1">
							<Label>新密码</Label>
							<Input
								type="password"
								value={newPwd}
								onChange={(e) => setNewPwd(e.target.value)}
								placeholder="请输入新密码（至少 6 位）"
							/>
						</div>
						<div className="space-y-1">
							<Label>确认新密码</Label>
							<Input
								type="password"
								value={confirmPwd}
								onChange={(e) => setConfirmPwd(e.target.value)}
								placeholder="再次输入新密码"
								onKeyDown={(e) => e.key === "Enter" && handleChangePwd()}
							/>
						</div>
					</div>
					<DialogFooter>
						<Button variant="outline" onClick={() => handleOpenChange(false)} disabled={loading}>
							取消
						</Button>
						<Button onClick={handleChangePwd} disabled={loading}>
							{loading && <Loader2 className="animate-spin mr-2 h-4 w-4" />}
							确认修改
						</Button>
					</DialogFooter>
				</DialogContent>
			</Dialog>
		</>
	);
}
