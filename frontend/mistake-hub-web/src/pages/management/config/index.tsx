import configService from "@/api/services/configService";
import type { SystemConfigResp } from "#/entity";
import { Button } from "@/ui/button";
import { Input } from "@/ui/input";
import { Loader2, Save } from "lucide-react";
import { useEffect, useState } from "react";
import { toast } from "sonner";

const CONFIG_HINTS: Record<string, string> = {
	"review.intervals": "7 个逗号分隔的非负整数，单调递增，如 0,1,2,4,7,15,30",
	"review.mastery_correct_delta": "正整数 1~100",
	"review.mastery_wrong_delta": "正整数 1~100",
	"review.stage_wrong_back": "正整数 1~6",
	"review.default_daily_limit": "正整数 1~200",
};

export default function ConfigManagementPage() {

	const [configs, setConfigs] = useState<SystemConfigResp[]>([]);
	const [loading, setLoading] = useState(false);
	const [saving, setSaving] = useState(false);
	const [editValues, setEditValues] = useState<Record<string, string>>({});

	const fetchConfigs = async () => {

		setLoading(true);
		try {
			const data = await configService.listConfigs();
			setConfigs(data || []);
			const values: Record<string, string> = {};
			for (const c of data || []) {
				values[c.configKey] = c.configValue;
			}
			setEditValues(values);
		} catch {
			// apiClient 已 toast
		} finally {
			setLoading(false);
		}
	};

	useEffect(() => {
		fetchConfigs();
	}, []);

	const handleChange = (key: string, value: string) => {

		setEditValues(prev => ({ ...prev, [key]: value }));
	};

	const hasChanges = configs.some(c => editValues[c.configKey] !== c.configValue);

	const handleSave = async () => {

		const changed = configs.filter(c => editValues[c.configKey] !== c.configValue);
		if (changed.length === 0) {
			toast.info("没有需要保存的修改");
			return;
		}
		setSaving(true);
		try {
			for (const c of changed) {
				await configService.updateConfig(c.configKey, editValues[c.configKey]);
			}
			toast.success(`已保存 ${changed.length} 项配置`);
			await fetchConfigs();
		} catch {
			// apiClient 已 toast
		} finally {
			setSaving(false);
		}
	};

	return (
		<div className="flex flex-col gap-4 p-2">
			<div className="flex items-center justify-between">
				<h2 className="text-2xl font-bold">参数配置</h2>
			</div>

			{loading ? (
				<div className="py-12 text-center text-text-secondary">
					<Loader2 className="h-6 w-6 animate-spin mx-auto" />
				</div>
			) : (
				<div className="rounded-xl border overflow-hidden">
					<table className="w-full text-sm">
						<thead className="bg-muted/50">
							<tr>
								<th className="px-4 py-3 text-left font-medium text-text-secondary w-56">配置键</th>
								<th className="px-4 py-3 text-left font-medium text-text-secondary">说明</th>
								<th className="px-4 py-3 text-left font-medium text-text-secondary w-64">配置值</th>
								<th className="px-4 py-3 text-left font-medium text-text-secondary w-40">更新时间</th>
							</tr>
						</thead>
						<tbody className="divide-y">
							{configs.length === 0 ? (
								<tr>
									<td colSpan={4} className="py-12 text-center text-text-secondary">暂无配置项</td>
								</tr>
							) : (
								configs.map(c => (
									<tr key={c.configKey} className="hover:bg-muted/30 transition-colors">
										<td className="px-4 py-3 font-mono text-xs">{c.configKey}</td>
										<td className="px-4 py-3 text-muted-foreground text-sm">{c.description || "—"}</td>
										<td className="px-4 py-3">
											<Input
												value={editValues[c.configKey] ?? c.configValue}
												onChange={e => handleChange(c.configKey, e.target.value)}
												placeholder={CONFIG_HINTS[c.configKey] || ""}
												className={`h-8 text-sm ${editValues[c.configKey] !== c.configValue ? "border-primary" : ""}`}
											/>
											{CONFIG_HINTS[c.configKey] && (
												<p className="text-xs text-muted-foreground mt-1">{CONFIG_HINTS[c.configKey]}</p>
											)}
										</td>
										<td className="px-4 py-3 text-text-secondary text-xs whitespace-nowrap">
											{c.updatedTime?.replace("T", " ").substring(0, 16) || "—"}
										</td>
									</tr>
								))
							)}
						</tbody>
					</table>
				</div>
			)}

			{configs.length > 0 && (
				<div className="flex justify-end">
					<Button onClick={handleSave} disabled={saving || !hasChanges}>
						{saving ? <Loader2 className="h-4 w-4 animate-spin mr-1" /> : <Save className="h-4 w-4 mr-1" />}
						保存修改
					</Button>
				</div>
			)}
		</div>
	);
}
