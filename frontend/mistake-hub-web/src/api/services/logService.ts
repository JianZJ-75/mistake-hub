import type { OperationLogResp, PageResult } from "#/entity";
import apiClient from "../apiClient";

export interface LogListParams {
	accountId?: number;
	action?: string;
	startTime?: string;
	endTime?: string;
	pageNum: number;
	pageSize: number;
}

const listLogs = (params: LogListParams): Promise<PageResult<OperationLogResp>> =>
	apiClient.post({ url: "/v1/log/list", data: params });

export default { listLogs };
