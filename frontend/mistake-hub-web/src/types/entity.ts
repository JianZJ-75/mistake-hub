import type { NavItemDataProps } from "@/components/nav/types";
import type { BasicStatus, PermissionType } from "./enum";

export interface UserToken {
	accessToken?: string;
}

/** 对应后端 AccountDetailResp */
export interface UserInfo {
	id?: number;
	code?: string;
	nickname?: string;
	avatarUrl?: string;
	role?: string;
	dailyLimit?: number;
	totalMistakes?: number;
	masteredCount?: number;
	currentStreak?: number;
	lastActiveTime?: string;
	createdTime?: string;
	permissions?: Permission[];
	// 模板兼容字段（供示例页面使用）
	username?: string;
	email?: string;
	avatar?: string;
	roles?: Role[];
}

export interface Permission_Old {
	id: string;
	parentId: string;
	name: string;
	label: string;
	type: PermissionType;
	route: string;
	status?: BasicStatus;
	order?: number;
	icon?: string;
	component?: string;
	hide?: boolean;
	hideTab?: boolean;
	frameSrc?: URL;
	newFeature?: boolean;
	children?: Permission_Old[];
}

export interface Role_Old {
	id: string;
	name: string;
	code: string;
	status: BasicStatus;
	order?: number;
	desc?: string;
	permission?: Permission_Old[];
}

export interface CommonOptions {
	status?: BasicStatus;
	desc?: string;
	createdAt?: string;
	updatedAt?: string;
}
export interface User extends CommonOptions {
	id: string; // uuid
	username: string;
	password: string;
	email: string;
	phone?: string;
	avatar?: string;
}

export interface Role extends CommonOptions {
	id: string; // uuid
	name: string;
	code: string;
}

export interface Permission extends CommonOptions {
	id: string; // uuid
	name: string;
	code: string; // resource:action  example: "user-management:read"
}

export interface Menu extends CommonOptions, MenuMetaInfo {
	id: string; // uuid
	parentId: string;
	name: string;
	code: string;
	order?: number;
	type: PermissionType;
}

export type MenuMetaInfo = Partial<Pick<NavItemDataProps, "path" | "icon" | "caption" | "info" | "disabled" | "auth" | "hidden">> & {
	externalLink?: URL;
	component?: string;
};

export type MenuTree = Menu & {
	children?: MenuTree[];
};

/** 通用分页结果，对应后端 MyBatis Plus Page */
export interface PageResult<T> {
	records: T[];
	total: number;
	size: number;
	current: number;
	pages: number;
}

/** 对应后端 TagResp */
export interface TagResp {
	id: number;
	name: string;
	type: string;
	parentId: number;
	accountId?: number | null;
	createdTime: string;
	children?: TagResp[];
}

/** 对应后端 MistakeDetailResp */
export interface MistakeDetailResp {
	id: number;
	accountId?: number;
	accountCode?: string;
	accountNickname?: string;
	title: string;
	correctAnswer?: string;
	titleImageUrl?: string;
	answerImageUrl?: string;
	reviewStage: number;
	masteryLevel: number;
	lastReviewTime?: string;
	nextReviewTime?: string;
	createdTime?: string;
	updatedTime?: string;
	tags?: TagResp[];
}

/** 对应后端 ReviewRecordResp */
export interface ReviewRecordResp {
	id: number;
	isCorrect: number;
	reviewStageBefore: number;
	reviewStageAfter: number;
	masteryBefore: number;
	masteryAfter: number;
	note?: string;
	noteImageUrl?: string;
	reviewTime: string;
}

/** 对应后端 AdminOverviewResp */
export interface AdminOverviewResp {
	totalUsers: number;
	activeUsersToday: number;
	totalMistakes: number;
	avgCompletionRate: number;
}

/** 对应后端 ReviewRecordAdminResp */
export interface ReviewRecordAdminResp {
	id: number;
	accountCode: string;
	accountNickname: string;
	mistakeId: number;
	mistakeTitle: string;
	isCorrect: number;
	reviewStageBefore: number;
	reviewStageAfter: number;
	masteryBefore: number;
	masteryAfter: number;
	note?: string;
	noteImageUrl?: string;
	reviewTime: string;
}

/** 对应后端 DailyCompletionResp */
export interface DailyCompletionResp {
	date: string;
	totalPlanned: number;
	completed: number;
	completionRate: number | null;
}

/** 对应后端 SubjectStatsResp */
export interface SubjectStatsResp {
	subject: string;
	count: number;
	avgMastery: number;
}

/** 对应后端 SystemConfig */
export interface SystemConfigResp {
	id: number;
	configKey: string;
	configValue: string;
	description?: string;
	updatedTime?: string;
}

/** 对应后端 OperationLog */
export interface OperationLogResp {
	id: number;
	accountId?: number;
	accountCode?: string;
	action: string;
	targetType?: string;
	targetId?: string;
	detail?: string;
	createdTime: string;
}
