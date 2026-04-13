import type { ReviewRecordResp } from "#/entity";
import apiClient from "../apiClient";

const getReviewHistory = (mistakeId: number): Promise<ReviewRecordResp[]> =>
	apiClient.post({ url: "/v1/review/history", data: { mistakeId } });

export default { getReviewHistory };
