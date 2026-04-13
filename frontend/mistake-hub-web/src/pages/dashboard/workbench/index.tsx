import { Icon } from "@/components/icon";
import { Card, CardContent, CardHeader, CardTitle } from "@/ui/card";
import { Title, Text } from "@/ui/typography";

const statCards = [
	{
		label: "总用户数",
		value: "—",
		icon: "mdi:account-group",
		color: "text-blue-500",
		bg: "bg-blue-500/10",
	},
	{
		label: "今日活跃用户",
		value: "—",
		icon: "mdi:account-check",
		color: "text-emerald-500",
		bg: "bg-emerald-500/10",
	},
	{
		label: "全平台错题总数",
		value: "—",
		icon: "mdi:book-open-variant",
		color: "text-orange-500",
		bg: "bg-orange-500/10",
	},
	{
		label: "近 7 天平均完成率",
		value: "—",
		icon: "mdi:chart-line",
		color: "text-purple-500",
		bg: "bg-purple-500/10",
	},
];

const modules = [
	{ label: "认证体系", desc: "登录 / 权限 / 会话管理", icon: "mdi:shield-check", done: true },
	{ label: "用户管理", desc: "角色 / 密码 / 列表", icon: "mdi:account-cog", done: true },
	{ label: "错题采集", desc: "录入 / 编辑 / 标签体系", icon: "mdi:book-edit", done: true },
	{ label: "复习调度", desc: "艾宾浩斯算法 / 每日计划", icon: "mdi:brain", done: true },
	{ label: "复习执行", desc: "作答 / 跳过 / 进度反馈", icon: "mdi:clipboard-check", done: true },
	{ label: "数据统计", desc: "学习看板 / 趋势分析", icon: "mdi:chart-bar", done: false },
];

export default function WorkbenchPage() {

	return (
		<div className="flex flex-col gap-6 p-2">
			{/* 标题区 */}
			<div>
				<Title as="h3" className="text-2xl mb-1">欢迎回来</Title>
				<Text variant="body2" className="text-muted-foreground">
					错题复习管理端 — 迭代 4：复习执行与结果反馈
				</Text>
			</div>

			{/* 统计卡片 */}
			<div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
				{statCards.map((s) => (
					<Card key={s.label}>
						<CardContent className="flex items-center gap-4 p-5">
							<div className={`rounded-xl ${s.bg} p-3 flex items-center justify-center`}>
								<Icon icon={s.icon} size={26} className={s.color} />
							</div>
							<div className="flex flex-col">
								<Text variant="body2" className="text-muted-foreground">{s.label}</Text>
								<Title as="h3" className="text-2xl">{s.value}</Title>
							</div>
						</CardContent>
					</Card>
				))}
			</div>

			{/* 模块进度 */}
			<Card>
				<CardHeader className="pb-2">
					<CardTitle>
						<Title as="h3" className="text-lg">功能模块</Title>
					</CardTitle>
				</CardHeader>
				<CardContent>
					<div className="grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-3">
						{modules.map((m) => (
							<div
								key={m.label}
								className={`flex items-center gap-3 rounded-lg border p-3 ${m.done ? "" : "opacity-50"}`}
							>
								<div className={`rounded-lg p-2 ${m.done ? "bg-emerald-500/10" : "bg-muted"}`}>
									<Icon
										icon={m.icon}
										size={22}
										className={m.done ? "text-emerald-500" : "text-muted-foreground"}
									/>
								</div>
								<div className="flex-1 min-w-0">
									<Text variant="subTitle2">{m.label}</Text>
									<Text variant="caption" className="text-muted-foreground">{m.desc}</Text>
								</div>
								{m.done ? (
									<Icon icon="mdi:check-circle" size={20} className="text-emerald-500 flex-shrink-0" />
								) : (
									<Text variant="caption" className="text-muted-foreground flex-shrink-0">待开发</Text>
								)}
							</div>
						))}
					</div>
				</CardContent>
			</Card>

			{/* 说明 */}
			<Card>
				<CardContent className="p-5">
					<Text variant="body2" className="text-muted-foreground leading-relaxed">
						当前处于 <strong>迭代 4</strong>：已完成认证体系、用户管理、错题采集与标签体系、艾宾浩斯复习调度算法、复习执行与结果反馈。
						顶部统计卡片将在迭代 5（学习数据统计）完成后接入真实数据。
					</Text>
				</CardContent>
			</Card>
		</div>
	);
}
