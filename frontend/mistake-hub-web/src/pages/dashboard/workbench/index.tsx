import { Icon } from "@/components/icon";

export default function WorkbenchPage() {
	return (
		<div className="flex flex-col gap-6 p-2">
			<div className="flex flex-col gap-1">
				<h2 className="text-2xl font-bold">欢迎回来 👋</h2>
				<p className="text-text-secondary text-sm">错题复习管理端 — 迭代 1：认证体系 + 用户管理</p>
			</div>

			<div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
				<div className="rounded-xl border p-4 flex items-center gap-4 bg-background-paper">
					<div className="rounded-lg bg-primary/10 p-3">
						<Icon icon="solar:users-group-bold-duotone" size={28} className="text-primary" />
					</div>
					<div>
						<p className="text-sm text-text-secondary">用户管理</p>
						<p className="text-base font-semibold">✅ 已完成</p>
					</div>
				</div>
				<div className="rounded-xl border p-4 flex items-center gap-4 bg-background-paper">
					<div className="rounded-lg bg-warning/10 p-3">
						<Icon icon="solar:book-bold-duotone" size={28} className="text-warning" />
					</div>
					<div>
						<p className="text-sm text-text-secondary">错题管理</p>
						<p className="text-base font-semibold">🚧 迭代 2</p>
					</div>
				</div>
				<div className="rounded-xl border p-4 flex items-center gap-4 bg-background-paper">
					<div className="rounded-lg bg-success/10 p-3">
						<Icon icon="solar:refresh-bold-duotone" size={28} className="text-success" />
					</div>
					<div>
						<p className="text-sm text-text-secondary">复习调度</p>
						<p className="text-base font-semibold">🚧 迭代 3</p>
					</div>
				</div>
			</div>

			<div className="rounded-xl border p-6 bg-background-paper">
				<p className="text-text-secondary text-sm leading-relaxed">
					当前处于 <strong>迭代 1</strong>：已完成登录鉴权、用户角色管理（升级/降级管理员）、重置密码、修改密码等核心功能。
					错题采集、艾宾浩斯复习调度等功能将在后续迭代中陆续上线。
				</p>
			</div>
		</div>
	);
}
