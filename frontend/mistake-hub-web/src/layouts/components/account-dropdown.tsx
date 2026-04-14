import { useLoginStateContext } from "@/pages/sys/login/providers/login-provider";
import { useRouter } from "@/routes/hooks";
import { useUserInfo, useClearUserInfoAndToken, useSetUserInfo } from "@/store/userStore";
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
import { useRef, useState } from "react";
import { toast } from "sonner";
import userService from "@/api/services/userService";
import uploadService from "@/api/services/uploadService";

/**
 * 顶部栏右侧用户下拉菜单
 */
export default function AccountDropdown() {
	const { replace } = useRouter();
	const { nickname, avatarUrl, code } = useUserInfo();
	const clearUserInfoAndToken = useClearUserInfoAndToken();
	const setUserInfo = useSetUserInfo();
	const { backToLogin } = useLoginStateContext();

	// ===== 修改密码状态 =====
	const [changePwdOpen, setChangePwdOpen] = useState(false);
	const [oldPwd, setOldPwd] = useState("");
	const [newPwd, setNewPwd] = useState("");
	const [confirmPwd, setConfirmPwd] = useState("");
	const [loading, setLoading] = useState(false);

	// ===== 编辑资料状态 =====
	const [profileOpen, setProfileOpen] = useState(false);
	const [editNickname, setEditNickname] = useState("");
	const [editAvatarUrl, setEditAvatarUrl] = useState("");
	const [profileLoading, setProfileLoading] = useState(false);
	const [uploading, setUploading] = useState(false);
	const fileInputRef = useRef<HTMLInputElement>(null);

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

	const handleOpenProfile = () => {
		setEditNickname(nickname || "");
		setEditAvatarUrl(avatarUrl || "");
		setProfileOpen(true);
	};

	const handleProfileOpenChange = (open: boolean) => {
		if (!open) {
			setEditNickname("");
			setEditAvatarUrl("");
		}
		setProfileOpen(open);
	};

	const handleAvatarUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
		const file = e.target.files?.[0];
		if (!file) return;
		if (!["image/jpeg", "image/png"].includes(file.type)) {
			toast.error("仅支持 JPG/PNG 格式");
			return;
		}
		if (file.size > 2 * 1024 * 1024) {
			toast.error("图片大小不能超过 2MB");
			return;
		}
		setUploading(true);
		try {
			const url = await uploadService.uploadImage(file);
			setEditAvatarUrl(url);
		} finally {
			setUploading(false);
			if (fileInputRef.current) fileInputRef.current.value = "";
		}
	};

	const handleSaveProfile = async () => {
		const data: { nickname?: string; avatarUrl?: string } = {};
		if (editNickname && editNickname !== nickname) {
			data.nickname = editNickname;
		}
		if (editAvatarUrl !== (avatarUrl || "")) {
			data.avatarUrl = editAvatarUrl;
		}
		if (!data.nickname && data.avatarUrl === undefined) {
			toast.error("未做任何修改");
			return;
		}
		setProfileLoading(true);
		try {
			await userService.modifyProfile(data);
			const user = await userService.currentDetail();
			setUserInfo(user);
			toast.success("资料修改成功");
			setProfileOpen(false);
		} finally {
			setProfileLoading(false);
		}
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
					<DropdownMenuItem onClick={handleOpenProfile}>编辑资料</DropdownMenuItem>
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

			{/* 编辑资料 Modal */}
			<Dialog open={profileOpen} onOpenChange={handleProfileOpenChange}>
				<DialogContent className="sm:max-w-md">
					<DialogHeader>
						<DialogTitle>编辑资料</DialogTitle>
					</DialogHeader>
					<div className="space-y-4 py-2">
						<div className="space-y-1">
							<Label>头像</Label>
							<div className="flex items-center gap-4">
								{editAvatarUrl ? (
									<img className="h-16 w-16 rounded-full object-cover" src={editAvatarUrl} alt="头像" />
								) : (
									<span className="h-16 w-16 rounded-full bg-primary text-primary-foreground text-lg flex items-center justify-center font-medium">
										{(editNickname || code || "A").charAt(0).toUpperCase()}
									</span>
								)}
								<div className="flex gap-2">
									<Button
										variant="outline"
										size="sm"
										disabled={uploading}
										onClick={() => fileInputRef.current?.click()}
									>
										{uploading && <Loader2 className="animate-spin mr-2 h-4 w-4" />}
										上传头像
									</Button>
									{editAvatarUrl && (
										<Button variant="outline" size="sm" onClick={() => setEditAvatarUrl("")}>
											清除
										</Button>
									)}
								</div>
								<input
									ref={fileInputRef}
									type="file"
									accept="image/jpeg,image/png"
									className="hidden"
									onChange={handleAvatarUpload}
								/>
							</div>
						</div>
						<div className="space-y-1">
							<Label>昵称</Label>
							<Input
								value={editNickname}
								onChange={(e) => setEditNickname(e.target.value)}
								placeholder="请输入昵称"
							/>
						</div>
					</div>
					<DialogFooter>
						<Button variant="outline" onClick={() => handleProfileOpenChange(false)} disabled={profileLoading}>
							取消
						</Button>
						<Button onClick={handleSaveProfile} disabled={profileLoading || uploading}>
							{profileLoading && <Loader2 className="animate-spin mr-2 h-4 w-4" />}
							保存
						</Button>
					</DialogFooter>
				</DialogContent>
			</Dialog>
		</>
	);
}
