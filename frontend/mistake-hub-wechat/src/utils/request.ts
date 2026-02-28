import Taro from '@tarojs/taro'

// 后端接口基础地址
const BASE_URL = 'http://localhost:8080/mistake-hub/backend'

// 本地存储 Token 的 key
const TOKEN_KEY = 'mistake_hub_token'

// ===== Token 工具方法 =====

export const getToken = (): string => {

  return Taro.getStorageSync(TOKEN_KEY) || ''
}

export const setToken = (token: string): void => {

  Taro.setStorageSync(TOKEN_KEY, token)
}

export const removeToken = (): void => {

  Taro.removeStorageSync(TOKEN_KEY)
}

// ===== 请求封装 =====

interface RequestOptions {
  url: string
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE'
  data?: Record<string, unknown>
}

interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

/**
 * 统一请求方法，自动附加 Bearer token
 */
const request = <T = unknown>(options: RequestOptions): Promise<T> => {

  const { url, method = 'POST', data = {} } = options
  const token = getToken()

  const header: Record<string, string> = {
    'Content-Type': 'application/json',
  }

  if (token) {
    header['Authorization'] = `Bearer ${token}`
  }

  return new Promise((resolve, reject) => {
    Taro.request({
      url: `${BASE_URL}${url}`,
      method,
      data,
      header,
      success: (res) => {
        const body = res.data as ApiResponse<T>
        // 后端统一响应：code 为 0 表示成功
        if (body.code === 0) {
          resolve(body.data)
        } else {
          // 401 未授权，清除 token 并重新触发登录
          if (body.code === 401) {
            removeToken()
          }
          reject(new Error(body.message || '请求失败'))
        }
      },
      fail: (err) => {
        reject(new Error(err.errMsg || '网络异常'))
      },
    })
  })
}

export default request
