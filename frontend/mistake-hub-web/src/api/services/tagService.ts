import type { EnumOption, TagResp } from "#/entity";
import apiClient from "../apiClient";

export interface TagAddParams {
	name: string;
	type?: string;
	parentId: number;
}

export interface TagUpdateParams {
	id: number;
	name: string;
	type?: string;
	parentId: number;
}

const getTagTree = (): Promise<TagResp[]> =>
	apiClient.get({ url: "/v1/tag/tree" });

const addTag = (params: TagAddParams): Promise<void> =>
	apiClient.post({ url: "/v1/tag/add", data: params });

const modifyTag = (params: TagUpdateParams): Promise<void> =>
	apiClient.post({ url: "/v1/tag/modify", data: params });

const deleteTag = (id: number): Promise<void> =>
	apiClient.post({ url: "/v1/tag/delete", data: { id } });

const listCustomTags = (): Promise<TagResp[]> =>
	apiClient.get({ url: "/v1/tag/custom/list" });

const listAllCustomTags = (): Promise<TagResp[]> =>
	apiClient.get({ url: "/v1/tag/custom/list/all" });

const addCustomTag = (name: string): Promise<void> =>
	apiClient.post({ url: "/v1/tag/custom/add", data: { name } });

const deleteCustomTag = (id: number): Promise<void> =>
	apiClient.post({ url: "/v1/tag/custom/delete", data: { id } });

const getTagTypes = (): Promise<EnumOption[]> =>
	apiClient.get({ url: "/v1/tag/types" });

export default { getTagTree, getTagTypes, addTag, modifyTag, deleteTag, listCustomTags, listAllCustomTags, addCustomTag, deleteCustomTag };
