import type { ReviewRecordResp, ReviewRecordAdminResp, PageResult } from "#/entity";
import apiClient from "../apiClient";

const getReviewHistory = (mistakeId: number): Promise<ReviewRecordResp[]> =>
	apiClient.post({ url: "/v1/review/history", data: { mistakeId } });

const listReviewRecords = (params: {
	accountId?: number;
	mistakeId?: number;
	isCorrect?: number;
	startTime?: string;
	endTime?: string;
	pageNum: number;
	pageSize: number;
}): Promise<PageResult<ReviewRecordAdminResp>> =>
	apiClient.post({ url: "/v1/review/admin-records", data: params });

export default { getReviewHistory, listReviewRecords };
