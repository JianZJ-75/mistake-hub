import type { AdminOverviewResp, DailyCompletionResp, SubjectStatsResp } from "#/entity";
import apiClient from "../apiClient";

const adminOverview = (): Promise<AdminOverviewResp> =>
	apiClient.post({ url: "/v1/stats/admin-overview" });

const adminDailyCompletion = (): Promise<DailyCompletionResp[]> =>
	apiClient.post({ url: "/v1/stats/admin-daily-completion" });

const adminSubject = (): Promise<SubjectStatsResp[]> =>
	apiClient.post({ url: "/v1/stats/admin-subject" });

export default { adminOverview, adminDailyCompletion, adminSubject };
