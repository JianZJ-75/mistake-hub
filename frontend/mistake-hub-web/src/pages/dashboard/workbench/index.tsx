import statsService from "@/api/services/statsService";
import { Chart, useChart } from "@/components/chart";
import { Icon } from "@/components/icon";
import { Card, CardContent, CardHeader, CardTitle } from "@/ui/card";
import { Title, Text } from "@/ui/typography";
import type { AdminOverviewResp, DailyCompletionResp, SubjectStatsResp } from "#/entity";
import { useEffect, useState } from "react";

const modules = [
	{ label: "认证体系", desc: "登录 / 权限 / 会话管理", icon: "mdi:shield-check", done: true },
	{ label: "用户管理", desc: "角色 / 密码 / 列表", icon: "mdi:account-cog", done: true },
	{ label: "错题采集", desc: "录入 / 编辑 / 标签体系", icon: "mdi:book-edit", done: true },
	{ label: "复习调度", desc: "艾宾浩斯算法 / 每日计划", icon: "mdi:brain", done: true },
	{ label: "复习执行", desc: "作答 / 跳过 / 进度反馈", icon: "mdi:clipboard-check", done: true },
	{ label: "数据统计", desc: "学习看板 / 趋势分析", icon: "mdi:chart-bar", done: true },
];

function getMasteryColor(avgMastery: number): string {
	if (avgMastery >= 80) return "#22c55e";
	if (avgMastery >= 60) return "#f59e0b";
	return "#ef4444";
}

export default function WorkbenchPage() {

	const [overview, setOverview] = useState<AdminOverviewResp | null>(null);
	const [dailyCompletion, setDailyCompletion] = useState<DailyCompletionResp[]>([]);
	const [subjectStats, setSubjectStats] = useState<SubjectStatsResp[]>([]);

	useEffect(() => {
		statsService.adminOverview().then(setOverview).catch(() => {});
		statsService.adminDailyCompletion().then(setDailyCompletion).catch(() => {});
		statsService.adminSubject().then(setSubjectStats).catch(() => {});
	}, []);

	const statCards = [
		{
			label: "总用户数",
			value: overview ? String(overview.totalUsers) : "—",
			icon: "mdi:account-group",
			color: "text-blue-500",
			bg: "bg-blue-500/10",
		},
		{
			label: "今日活跃用户",
			value: overview ? String(overview.activeUsersToday) : "—",
			icon: "mdi:account-check",
			color: "text-emerald-500",
			bg: "bg-emerald-500/10",
		},
		{
			label: "全平台错题总数",
			value: overview ? String(overview.totalMistakes) : "—",
			icon: "mdi:book-open-variant",
			color: "text-orange-500",
			bg: "bg-orange-500/10",
		},
		{
			label: "近 7 天平均完成率",
			value: overview ? `${(overview.avgCompletionRate * 100).toFixed(1)}%` : "—",
			icon: "mdi:chart-line",
			color: "text-purple-500",
			bg: "bg-purple-500/10",
		},
	];

	// ===== 图表 A：近 30 天复习完成率趋势 =====
	const areaCategories = dailyCompletion.map((d) => {
		const parts = d.date.split("-");
		return `${Number(parts[1])}/${Number(parts[2])}`;
	});
	const areaSeries = dailyCompletion.map((d) =>
		d.completionRate != null ? Math.round(d.completionRate * 10000) / 100 : 0,
	);

	const areaChartOptions = useChart({
		xaxis: {
			categories: areaCategories,
			labels: { rotate: -45, rotateAlways: dailyCompletion.length > 15 },
			tickAmount: 10,
		},
		yaxis: {
			min: 0,
			max: 100,
			labels: { formatter: (val: number) => `${val}%` },
		},
		tooltip: {
			y: { formatter: (val: number) => `${val.toFixed(1)}%` },
		},
		stroke: { curve: "smooth", width: 2 },
		fill: {
			type: "gradient",
			gradient: { opacityFrom: 0.4, opacityTo: 0.1 },
		},
	});

	// ===== 图表 B：各学科错题分布 =====
	const barCategories = subjectStats.map((s) => s.subject);
	const barData = subjectStats.map((s) => s.count);
	const barColors = subjectStats.map((s) => getMasteryColor(s.avgMastery));

	const barChartOptions = useChart({
		xaxis: { categories: barCategories },
		plotOptions: {
			bar: {
				horizontal: true,
				barHeight: subjectStats.length <= 3 ? "30%" : "50%",
				distributed: true,
			},
		},
		colors: barColors,
		legend: { show: false },
		stroke: { show: false },
		tooltip: {
			y: { formatter: (val: number) => `${val} 题` },
		},
	});

	return (
		<div className="flex flex-col gap-6 p-2">
			{/* 标题区 */}
			<div>
				<Title as="h3" className="text-2xl mb-1">欢迎回来</Title>
				<Text variant="body2" className="text-muted-foreground">
					错题复习管理端 — 迭代 5：学习数据统计与可视化
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

			{/* 数据图表 */}
			<div className="grid grid-cols-1 gap-4 lg:grid-cols-2">
				{/* 图表 A：近 30 天复习完成率趋势 */}
				<Card>
					<CardHeader className="pb-2">
						<CardTitle>
							<Title as="h3" className="text-lg">近 30 天全平台复习完成率</Title>
						</CardTitle>
					</CardHeader>
					<CardContent>
						<Chart
							type="area"
							series={[{ name: "完成率", data: areaSeries }]}
							options={areaChartOptions}
							height={320}
						/>
					</CardContent>
				</Card>

				{/* 图表 B：各学科错题分布 */}
				<Card>
					<CardHeader className="pb-2">
						<CardTitle>
							<Title as="h3" className="text-lg">各学科错题分布</Title>
						</CardTitle>
					</CardHeader>
					<CardContent>
						{subjectStats.length > 0 ? (
							<Chart
								type="bar"
								series={[{ name: "错题数", data: barData }]}
								options={barChartOptions}
								height={320}
							/>
						) : (
							<div className="flex items-center justify-center h-[320px]">
								<Text variant="body2" className="text-muted-foreground">暂无学科数据</Text>
							</div>
						)}
					</CardContent>
				</Card>
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
						当前处于 <strong>迭代 5</strong>：已完成认证体系、用户管理、错题采集与标签体系、艾宾浩斯复习调度算法、复习执行与结果反馈、学习数据统计与可视化。
					</Text>
				</CardContent>
			</Card>
		</div>
	);
}
