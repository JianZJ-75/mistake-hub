import type { AdminOverviewResp, DailyCompletionResp, ForgettingCurveResp, MasteryDistributionResp, RetentionTrendResp, SubjectStatsResp } from "#/entity";
import apiClient from "../apiClient";

const adminOverview = (): Promise<AdminOverviewResp> =>
	apiClient.post({ url: "/v1/stats/admin-overview" });

const adminDailyCompletion = (): Promise<DailyCompletionResp[]> =>
	apiClient.post({ url: "/v1/stats/admin-daily-completion" });

const adminSubject = (): Promise<SubjectStatsResp[]> =>
	apiClient.post({ url: "/v1/stats/admin-subject" });

const adminMastery = (): Promise<MasteryDistributionResp> =>
	apiClient.post({ url: "/v1/stats/admin-mastery" });

const adminRetentionTrend = (): Promise<RetentionTrendResp[]> =>
	apiClient.post({ url: "/v1/stats/admin-retention-trend" });

const forgettingCurve = (mistakeId: number): Promise<ForgettingCurveResp> =>
	apiClient.post({ url: "/v1/stats/forgetting-curve", data: { mistakeId } });

export default { adminOverview, adminDailyCompletion, adminSubject, adminMastery, adminRetentionTrend, forgettingCurve };
