import apiClient from "../apiClient";

export interface NonceResp {
	id: number;
	nonce: string;
}

/** 生成一次性 Nonce（无需认证） */
const generateNonce = () => apiClient.post<NonceResp>({ url: "/v1/nonce/generate" });

/**
 * 即拿即删获取 Nonce（需要认证）
 * 用于解密 reset-password / change-role 返回的加密密码
 */
const getNonce = (id: number) => apiClient.post<NonceResp>({ url: "/v1/nonce/get", params: { id } });

export default { generateNonce, getNonce };
