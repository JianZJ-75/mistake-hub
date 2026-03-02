/**
 * 与后端 AesUtil / Sha256Util 严格对齐的加密工具
 *
 * 后端规范：
 * - 哈希：SHA-256，输出 64 位小写 hex
 * - AES 模式：AES/CBC/PKCS5Padding（等同于 PKCS7）
 * - 密钥派生：SHA256(rawKey) → 取前 32 字符作为 AES key（UTF-8 bytes，32 bytes = AES-256）
 * - IV：16 字节随机，标准 Base64 编码
 * - 输出格式：cipherBase64 + "_" + ivBase64（下划线分隔，对应后端 IV_SEPARATOR = "_"）
 */

/**
 * SHA-256 哈希，返回 64 位小写 hex 字符串
 * 对应后端 Sha256Util.encrypt()
 */
export async function sha256(input: string): Promise<string> {

	const data = new TextEncoder().encode(input);
	const hashBuffer = await crypto.subtle.digest("SHA-256", data);
	return Array.from(new Uint8Array(hashBuffer))
		.map((b) => b.toString(16).padStart(2, "0"))
		.join("");
}

/**
 * AES-256-CBC 加密
 * 对应后端 AesUtil.encryptWithRawKey(plainText, key) / AesUtil.encrypt(plainText, key) 带 SHA256 派生版本
 *
 * 密钥派生：SHA256(rawKey) → 前 32 字符 → UTF-8 bytes
 * 输出格式：cipherBase64_ivBase64
 */
export async function aesEncrypt(plainText: string, rawKey: string): Promise<string> {

	const keyHex = await sha256(rawKey);
	const keyBytes = new TextEncoder().encode(keyHex.substring(0, 32));
	const key = await crypto.subtle.importKey("raw", keyBytes, { name: "AES-CBC" }, false, ["encrypt"]);

	const iv = crypto.getRandomValues(new Uint8Array(16));
	const plainBytes = new TextEncoder().encode(plainText);
	const cipherBuffer = await crypto.subtle.encrypt({ name: "AES-CBC", iv }, key, plainBytes);

	const cipherBase64 = btoa(String.fromCharCode(...new Uint8Array(cipherBuffer)));
	const ivBase64 = btoa(String.fromCharCode(...iv));
	return `${cipherBase64}_${ivBase64}`;
}

/**
 * AES-256-CBC 解密（带 SHA256 密钥派生）
 * 对应后端 AesUtil.decryptWithRawKey(cipherText, rawKey)
 *
 * 用于解密登录时前端加密的内容（对称解密）
 */
export async function aesDecrypt(cipherTextWithIv: string, rawKey: string): Promise<string> {

	const separatorIndex = cipherTextWithIv.indexOf("_");
	const cipherBase64 = cipherTextWithIv.substring(0, separatorIndex);
	const ivBase64 = cipherTextWithIv.substring(separatorIndex + 1);

	const keyHex = await sha256(rawKey);
	const keyBytes = new TextEncoder().encode(keyHex.substring(0, 32));
	const key = await crypto.subtle.importKey("raw", keyBytes, { name: "AES-CBC" }, false, ["decrypt"]);

	const cipherBytes = Uint8Array.from(atob(cipherBase64), (c) => c.charCodeAt(0));
	const iv = Uint8Array.from(atob(ivBase64), (c) => c.charCodeAt(0));
	const plainBuffer = await crypto.subtle.decrypt({ name: "AES-CBC", iv }, key, cipherBytes);
	return new TextDecoder().decode(plainBuffer);
}

/**
 * AES-256-CBC 直接解密（key 直接用，不做 SHA256 派生）
 * 对应后端 AesUtil.decrypt(cipherText, key)
 *
 * 用于解密后端返回的加密密码（reset-password / change-role 升级场景）：
 * 后端用 encryptionUtil.encrypt(rawPassword, nonce.getNonce()) 加密，
 * key = nonce 字符串本身，不经过 SHA256 派生。
 */
export async function aesDecryptDirect(cipherTextWithIv: string, key: string): Promise<string> {

	const separatorIndex = cipherTextWithIv.indexOf("_");
	const cipherBase64 = cipherTextWithIv.substring(0, separatorIndex);
	const ivBase64 = cipherTextWithIv.substring(separatorIndex + 1);

	// key 直接转 UTF-8 bytes（32 字节 = AES-256）
	const keyBytes = new TextEncoder().encode(key);
	const cryptoKey = await crypto.subtle.importKey("raw", keyBytes, { name: "AES-CBC" }, false, ["decrypt"]);

	const cipherBytes = Uint8Array.from(atob(cipherBase64), (c) => c.charCodeAt(0));
	const iv = Uint8Array.from(atob(ivBase64), (c) => c.charCodeAt(0));
	const plainBuffer = await crypto.subtle.decrypt({ name: "AES-CBC", iv }, cryptoKey, cipherBytes);
	return new TextDecoder().decode(plainBuffer);
}
