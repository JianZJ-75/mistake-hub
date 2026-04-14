import type { EnumOption, OperationLogResp, PageResult } from "#/entity";
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

const getActionTypes = (): Promise<EnumOption[]> =>
	apiClient.get({ url: "/v1/log/action-types" });

const getTargetTypes = (): Promise<EnumOption[]> =>
	apiClient.get({ url: "/v1/log/target-types" });

export default { listLogs, getActionTypes, getTargetTypes };
