import type { SystemConfigResp } from "#/entity";
import apiClient from "../apiClient";

const listConfigs = (): Promise<SystemConfigResp[]> =>
	apiClient.post({ url: "/v1/config/list" });

const updateConfig = (configKey: string, configValue: string): Promise<void> =>
	apiClient.post({ url: "/v1/config/update", data: { configKey, configValue } });

export default { listConfigs, updateConfig };
