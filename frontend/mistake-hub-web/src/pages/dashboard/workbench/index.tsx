import statsService from "@/api/services/statsService";
import { Chart, useChart } from "@/components/chart";
import { Icon } from "@/components/icon";
import { Card, CardAction, CardContent, CardHeader, CardTitle } from "@/ui/card";
import { Skeleton } from "@/ui/skeleton";
import { Title, Text } from "@/ui/typography";
import type { AdminOverviewResp, DailyCompletionResp, MasteryDistributionResp, SubjectStatsResp } from "#/entity";
import { useCallback, useEffect, useState } from "react";

function getMasteryColor(avgMastery: number): string {

	if (avgMastery >= 80) return "#22c55e";
	if (avgMastery >= 60) return "#f59e0b";
	return "#ef4444";
}

function formatDate(): string {

	const now = new Date();
	const weekdays = ["星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六"];
	return `${now.getFullYear()}年${now.getMonth() + 1}月${now.getDate()}日 ${weekdays[now.getDay()]}`;
}

export default function WorkbenchPage() {

	const [overview, setOverview] = useState<AdminOverviewResp | null>(null);
	const [dailyCompletion, setDailyCompletion] = useState<DailyCompletionResp[]>([]);
	const [subjectStats, setSubjectStats] = useState<SubjectStatsResp[]>([]);
	const [mastery, setMastery] = useState<MasteryDistributionResp | null>(null);
	const [masteryLoading, setMasteryLoading] = useState(true);
	const [selectedSlice, setSelectedSlice] = useState<number | null>(null);

	useEffect(() => {
		statsService.adminOverview().then(setOverview).catch(() => {});
		statsService.adminDailyCompletion().then(setDailyCompletion).catch(() => {});
		statsService.adminSubject().then(setSubjectStats).catch(() => {});
		statsService.adminMastery().then(setMastery).catch(() => {}).finally(() => setMasteryLoading(false));
	}, []);

	// ===== 统计卡片 =====
	const statCards = [
		{
			label: "总用户数",
			value: overview ? String(overview.totalUsers) : "—",
			subtitle: "累计注册",
			icon: "mdi:account-group",
			color: "text-blue-500",
			bg: "bg-blue-500/10",
		},
		{
			label: "今日活跃用户",
			value: overview ? String(overview.activeUsersToday) : "—",
			subtitle: "今日登录",
			icon: "mdi:account-check",
			color: "text-emerald-500",
			bg: "bg-emerald-500/10",
		},
		{
			label: "全平台错题总数",
			value: overview ? String(overview.totalMistakes) : "—",
			subtitle: "累计录入",
			icon: "mdi:book-open-variant",
			color: "text-orange-500",
			bg: "bg-orange-500/10",
		},
		{
			label: "近 7 天平均完成率",
			value: overview ? `${(overview.avgCompletionRate * 100).toFixed(1)}%` : "—",
			subtitle: "近 7 天平均",
			icon: "mdi:chart-line",
			color: "text-purple-500",
			bg: "bg-purple-500/10",
		},
	];

	// ===== 面积图：近 30 天复习完成率趋势 =====
	const areaCategories = dailyCompletion.map((d) => {
		const parts = d.date.split("-");
		return `${Number(parts[1])}/${Number(parts[2])}`;
	});
	const areaSeries = dailyCompletion.map((d) =>
		d.completionRate != null ? Math.round(d.completionRate * 10000) / 100 : 0,
	);

	const todayCompletion = dailyCompletion.length > 0 ? dailyCompletion[dailyCompletion.length - 1] : null;
	const yesterdayCompletion = dailyCompletion.length > 1 ? dailyCompletion[dailyCompletion.length - 2] : null;
	const todayRate = todayCompletion?.completionRate != null
		? Math.round(todayCompletion.completionRate * 10000) / 100
		: null;
	const yesterdayRate = yesterdayCompletion?.completionRate != null
		? Math.round(yesterdayCompletion.completionRate * 10000) / 100
		: null;
	const rateDiff = todayRate != null && yesterdayRate != null ? todayRate - yesterdayRate : null;

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

	// ===== 环形图：掌握度分布（后端按单题 masteryLevel 统计） =====
	const masteryLabels = ["未掌握", "掌握中", "已掌握"];
	const masteryData = mastery ? [mastery.notMastered, mastery.learning, mastery.mastered] : [0, 0, 0];
	const masteryTotal = masteryData.reduce((a, b) => a + b, 0);
	const masteryPercents = masteryData.map(v => masteryTotal > 0 ? ((v / masteryTotal) * 100).toFixed(1) : "0.0");

	const handleDataPointSelection = useCallback((_chart: unknown, _chartCtx: unknown, config: { dataPointIndex: number }) => {

		const idx = config.dataPointIndex;
		setSelectedSlice(prev => prev === idx ? null : idx);
	}, []);

	const handleSliceMouseEnter = useCallback((e: unknown) => {

		const target = (e as Event).target;
		if (target instanceof SVGElement) {
			target.style.transition = "transform 0.2s ease-out";
			target.style.transformOrigin = "center";
			target.style.transformBox = "fill-box";
			target.style.transform = "scale(1.04)";
		}
	}, []);

	const handleSliceMouseLeave = useCallback((e: unknown) => {

		const target = (e as Event).target;
		if (target instanceof SVGElement) {
			target.style.transform = "";
		}
	}, []);

	const donutChartOptions = useChart({
		labels: masteryLabels,
		colors: ["#FF5630", "#FFAB00", "#36B37E"],
		plotOptions: {
			pie: {
				expandOnClick: false,
				donut: {
					size: "68%",
					labels: {
						show: true,
						value: {
							fontSize: "24px",
							fontWeight: 700,
							offsetY: 2,
							formatter: (val: string) => `${val} 题`,
						},
						total: {
							show: true,
							label: selectedSlice != null ? masteryLabels[selectedSlice] : "总题数",
							formatter: () => {
								if (selectedSlice != null) {
									return `${masteryData[selectedSlice]} 题 · ${masteryPercents[selectedSlice]}%`;
								}
								return String(masteryTotal);
							},
						},
					},
				},
			},
		},
		chart: {
			events: {
				dataPointSelection: handleDataPointSelection,
				dataPointMouseEnter: handleSliceMouseEnter,
				dataPointMouseLeave: handleSliceMouseLeave,
			},
		},
		states: {
			// ApexCharts 运行时支持 value，但 TS 类型定义缺失该字段
			hover: { filter: { type: "lighten", value: 0.06 } as Record<string, unknown> },
			active: { filter: { type: "darken", value: 0.1 } as Record<string, unknown> },
		},
		legend: {
			position: "bottom",
			formatter: (seriesName: string, opts: { seriesIndex: number }) => {
				const idx = opts.seriesIndex;
				return `${seriesName}  ${masteryData[idx]} 题 (${masteryPercents[idx]}%)`;
			},
		},
		stroke: { colors: ["var(--card)"], width: 2 },
		tooltip: {
			fixed: { enabled: true, position: "topRight", offsetX: 0, offsetY: 0 },
			fillSeriesColor: false,
			y: {
				formatter: (val: number) => {
					const pct = masteryTotal > 0 ? ((val / masteryTotal) * 100).toFixed(1) : "0.0";
					return `${val} 题 (${pct}%)`;
				},
			},
		},
		dataLabels: { enabled: false },
		responsive: [
			{
				breakpoint: 640,
				options: {
					chart: { height: 280 },
					legend: { fontSize: "11px" },
				},
			},
		],
	});

	// ===== 柱状图：各学科错题分布 =====
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
				<Title as="h3" className="text-2xl mb-1">数据概览</Title>
				<Text variant="body2" className="text-muted-foreground">{formatDate()}</Text>
			</div>

			{/* 统计卡片 */}
			<div className="grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
				{statCards.map((s) => (
					<Card key={s.label}>
						<CardHeader>
							<CardTitle>
								<Text variant="subTitle2" className="text-muted-foreground">{s.label}</Text>
							</CardTitle>
							<CardAction>
								<div className={`rounded-full ${s.bg} p-2.5 flex items-center justify-center`}>
									<Icon icon={s.icon} size={20} className={s.color} />
								</div>
							</CardAction>
						</CardHeader>
						<CardContent>
							<Title as="h3" className="text-2xl">{s.value}</Title>
							<Text variant="caption" className="text-muted-foreground">{s.subtitle}</Text>
						</CardContent>
					</Card>
				))}
			</div>

			{/* 数据图表 */}
			<div className="grid grid-cols-12 gap-4">
				{/* 面积图：近 30 天复习完成率趋势 */}
				<Card className="col-span-12 lg:col-span-8">
					<CardHeader className="pb-2">
						<CardTitle>
							<Title as="h5">近 30 天全平台复习完成率</Title>
							{todayRate != null && (
								<div className="flex items-center gap-2 mt-1">
									<Text variant="subTitle2">今日 {todayRate.toFixed(1)}%</Text>
									{rateDiff != null && rateDiff !== 0 && (
										<Text
											variant="caption"
											className={rateDiff > 0 ? "text-emerald-500" : "text-red-500"}
										>
											{rateDiff > 0 ? "↑" : "↓"} {Math.abs(rateDiff).toFixed(1)}%
										</Text>
									)}
								</div>
							)}
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

				{/* 环形图：掌握度分布 */}
				<Card className="col-span-12 lg:col-span-4">
					<CardHeader className="pb-2">
						<CardTitle>
							<Title as="h5">掌握度分布</Title>
							{!masteryLoading && masteryTotal > 0 && (
								<Text variant="caption" className="text-muted-foreground mt-0.5">
									已掌握 {masteryPercents[2]}% / 掌握中 {masteryPercents[1]}% / 未掌握 {masteryPercents[0]}%
								</Text>
							)}
						</CardTitle>
					</CardHeader>
					<CardContent>
						{masteryLoading ? (
							<div className="flex flex-col items-center justify-center gap-6 py-6">
								<Skeleton className="h-[180px] w-[180px] rounded-full" />
								<div className="flex items-center gap-6">
									{[0, 1, 2].map(i => (
										<div key={i} className="flex items-center gap-1.5">
											<Skeleton className="h-3 w-3 rounded-full" />
											<Skeleton className="h-3 w-16" />
										</div>
									))}
								</div>
							</div>
						) : masteryTotal > 0 ? (
							<Chart
								type="donut"
								series={masteryData}
								options={donutChartOptions}
								height={350}
							/>
						) : (
							<div className="flex items-center justify-center h-[350px]">
								<Text variant="body2" className="text-muted-foreground">暂无数据</Text>
							</div>
						)}
					</CardContent>
				</Card>

				{/* 柱状图：各学科错题分布 */}
				<Card className="col-span-12">
					<CardHeader className="pb-2">
						<CardTitle>
							<Title as="h5">各学科错题分布</Title>
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
		</div>
	);
}
