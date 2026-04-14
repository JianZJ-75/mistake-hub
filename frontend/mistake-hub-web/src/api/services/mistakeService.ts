import type { MistakeDetailResp, PageResult, TagResp } from "#/entity";
import apiClient from "../apiClient";

export interface ListMistakesParams {
	accountId?: number;
	tagIds?: string;
	masteryFilter?: number;
	pageNum: number;
	pageSize: number;
}

export interface AdminListMistakesParams {
	accountId?: number;
	tagId?: number;
	masteryFilter?: number;
	pageNum: number;
	pageSize: number;
}

export interface UpdateMistakeParams {
	id: number;
	title?: string;
	correctAnswer?: string;
	titleImageUrl?: string;
	answerImageUrl?: string;
	tagIds?: number[];
}

const listMistakes = (params: ListMistakesParams): Promise<PageResult<MistakeDetailResp>> =>
	apiClient.get({ url: "/v1/mistake/list", params });

const adminListMistakes = (params: AdminListMistakesParams): Promise<PageResult<MistakeDetailResp>> =>
	apiClient.post({ url: "/v1/mistake/admin-list", data: params });

const getMistake = (id: number): Promise<MistakeDetailResp> =>
	apiClient.post({ url: "/v1/mistake/detail", data: { id } });

const modifyMistake = (params: UpdateMistakeParams): Promise<void> =>
	apiClient.post({ url: "/v1/mistake/modify", data: params });

const adminUpdateMistake = (params: UpdateMistakeParams): Promise<void> =>
	apiClient.post({ url: "/v1/mistake/admin-update", data: params });

const deleteMistake = (id: number): Promise<void> =>
	apiClient.post({ url: "/v1/mistake/delete", data: { id } });

const getTagTree = (): Promise<TagResp[]> =>
	apiClient.get({ url: "/v1/tag/tree" });

export default { listMistakes, adminListMistakes, getMistake, modifyMistake, adminUpdateMistake, deleteMistake, getTagTree };
