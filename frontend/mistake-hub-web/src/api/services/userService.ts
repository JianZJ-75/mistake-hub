import { sha256, aesEncrypt, aesDecryptDirect } from "@/utils/crypto";
import type { EnumOption, PageResult, UserInfo } from "#/entity";
import apiClient from "../apiClient";
import nonceService from "./nonceService";

/** 登录表单入参 */
export interface SignInReq {
	username: string;
	password: string;
}

/** 保留枚举供 mock 层使用 */
export enum UserApi {
	SignIn = "/v1/account/login-web",
	CurrentDetail = "/v1/account/current-detail",
}

/** 修改用户角色请求 */
export interface ChangeRoleReq {
	accountId: number;
	role: string; // 'admin' | 'student'
	nonceId?: number;
}

/** 修改用户角色响应 */
export interface ChangeRoleResp {
	cipherPassword?: string;
}

/** 重置密码响应 */
export interface ResetPasswordResp {
	nonceId: number;
	cipherPassword: string;
}

/** 修改密码请求 */
export interface ChangePasswordReq {
	oldCipherDigest: string;
	oldNonceId: number;
	newCipherDigest: string;
	newNonceId: number;
}

/**
 * Web 管理端登录
 * 内部完成 Nonce 生成 → SHA-256 哈希 → AES-256-CBC 加密 → POST /v1/account/login-web
 * 返回 token 字符串
 */
const loginWeb = async (data: SignInReq): Promise<string> => {
	const { id: nonceId, nonce } = await nonceService.generateNonce();
	const digest = await sha256(data.password);
	const cipherDigest = await aesEncrypt(digest, nonce);
	return apiClient.post<string>({
		url: UserApi.SignIn,
		data: { code: data.username, cipherDigest, nonceId },
	});
};

/** 获取当前用户详情 */
const currentDetail = () => apiClient.post<UserInfo>({ url: UserApi.CurrentDetail });

/** 用户列表查询参数 */
export interface ListUsersParams {
	code?: string;
	nickname?: string;
	role?: string;
	pageNum: number;
	pageSize: number;
}

/**
 * 分页查询用户列表（GET + query params）
 */
const listUsers = (params: ListUsersParams) =>
	apiClient.get<PageResult<UserInfo>>({
		url: "/v1/account/list",
		params,
	});

/**
 * 修改用户角色
 * 升级为管理员时：内部先获取 Nonce，发给后端，后端返回 cipherPassword，再解密得到明文密码
 * 降级为学生时：直接发请求
 * 返回明文密码（升级时）或 undefined（降级时）
 */
const changeRole = async (accountId: number, role: string): Promise<string | undefined> => {
	if (role === "admin") {
		// 升级：前端生成 nonce → 后端加密返回 → 前端解密
		const { id: nonceId, nonce } = await nonceService.generateNonce();
		const resp = await apiClient.post<ChangeRoleResp>({
			url: "/v1/account/change-role",
			data: { accountId, role, nonceId },
		});
		if (resp.cipherPassword) {
			return aesDecryptDirect(resp.cipherPassword, nonce);
		}
	} else {
		await apiClient.post({ url: "/v1/account/change-role", data: { accountId, role } });
	}
	return undefined;
};

/**
 * 重置密码
 * 后端服务端生成 nonce 并返回 {nonceId, cipherPassword}
 * 前端用 nonce/get 取到 nonce 值后直接解密
 * 返回明文新密码
 */
const resetPassword = async (accountId: number): Promise<string> => {
	const resp = await apiClient.post<ResetPasswordResp>({
		url: "/v1/account/reset-password",
		data: { accountId },
	});
	const nonceResp = await nonceService.getNonce(resp.nonceId);
	return aesDecryptDirect(resp.cipherPassword, nonceResp.nonce);
};

/**
 * 修改密码（自服务）
 * 旧密码和新密码各用独立 Nonce 加密
 */
const changePassword = async (oldPassword: string, newPassword: string): Promise<void> => {
	const [nonce1, nonce2] = await Promise.all([
		nonceService.generateNonce(),
		nonceService.generateNonce(),
	]);
	const oldDigest = await sha256(oldPassword);
	const newDigest = await sha256(newPassword);
	const oldCipherDigest = await aesEncrypt(oldDigest, nonce1.nonce);
	const newCipherDigest = await aesEncrypt(newDigest, nonce2.nonce);
	await apiClient.post({
		url: "/v1/account/change-password",
		data: {
			oldCipherDigest,
			oldNonceId: nonce1.id,
			newCipherDigest,
			newNonceId: nonce2.id,
		},
	});
};

/** 修改个人资料 */
const modifyProfile = (data: { nickname?: string; avatarUrl?: string }) =>
	apiClient.post<void>({ url: "/v1/account/modify-profile", data });

/** 获取角色枚举列表 */
const getRoles = (): Promise<EnumOption[]> =>
	apiClient.get({ url: "/v1/account/roles" });

/** 注册（暂未实现，保留签名供模板页面编译通过） */
const signup = (_data: { username: string; password: string; email: string }): Promise<never> => {
	return Promise.reject(new Error("暂不支持注册"));
};

export default { loginWeb, currentDetail, listUsers, getRoles, changeRole, resetPassword, changePassword, modifyProfile, signup };
