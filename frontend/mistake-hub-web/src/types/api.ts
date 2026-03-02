/**
 * 后端统一响应格式，对应 Java BaseResult<T>
 * code = 0 表示成功，非 0 表示失败
 */
export interface Result<T = unknown> {
	code: number;
	message: { zh_CN: string; en_US: string };
	data: T;
}
