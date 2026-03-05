import type { MistakeDetailResp, PageResult, TagResp } from "#/entity";
import apiClient from "../apiClient";

export interface ListMistakesParams {
	subject?: string;
	tagId?: number;
	masteryFilter?: number;
	pageNum: number;
	pageSize: number;
}

export interface UpdateMistakeParams {
	id: number;
	title?: string;
	correctAnswer?: string;
	errorReason?: string;
	imageUrl?: string;
	subject?: string;
	tagIds?: number[];
}

const listMistakes = (params: ListMistakesParams): Promise<PageResult<MistakeDetailResp>> =>
	apiClient.post({ url: "/v1/mistake/list", data: params });

const getMistake = (id: number): Promise<MistakeDetailResp> =>
	apiClient.post({ url: "/v1/mistake/detail", data: { id } });

const updateMistake = (params: UpdateMistakeParams): Promise<void> =>
	apiClient.post({ url: "/v1/mistake/update", data: params });

const deleteMistake = (id: number): Promise<void> =>
	apiClient.post({ url: "/v1/mistake/delete", data: { id } });

const getTagTree = (): Promise<TagResp[]> =>
	apiClient.get({ url: "/v1/tag/tree" });

export default { listMistakes, getMistake, updateMistake, deleteMistake, getTagTree };
