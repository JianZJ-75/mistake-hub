import Taro from '@tarojs/taro'

// 后端接口基础地址
export const BASE_URL = 'http://localhost:8080/mistake-hub/backend'

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
  data?: unknown
}

/**
 * 后端统一响应格式，对应 BaseResult<T>
 * message 为双语对象 { zh_CN: string, en_US: string }
 */
interface ApiResponse<T = unknown> {
  code: number
  message: { zh_CN: string; en_US: string }
  data: T
}

/**
 * 静默重登（403 时内部调用，不走 request 封装避免循环）
 */
const silentReLogin = async (): Promise<void> => {

  const { code } = await new Promise<{ code: string }>((resolve, reject) => {
    Taro.login({
      success: (res) => res.code ? resolve({ code: res.code }) : reject(new Error('wx.login 无 code')),
      fail: (err) => reject(new Error(err.errMsg)),
    })
  })

  const res = await Taro.request<ApiResponse<string>>({
    url: `${BASE_URL}/v1/account/login-wx`,
    method: 'POST',
    data: { code },
    header: { 'Content-Type': 'application/json' },
  })

  // Taro.request 的 data 已经是解析后的对象
  const token = res.data?.data
  if (token) {
    setToken(token)
  } else {
    throw new Error('重新登录失败')
  }
}

/**
 * 统一请求方法，自动附加 Bearer token
 * - HTTP 200 + code 0：返回 data
 * - HTTP 200 + code 非0：reject 并提示中文错误
 * - HTTP 403：自动重登后重试一次
 * - 网络异常：reject 并提示
 */
const request = <T = unknown>(options: RequestOptions, isRetry = false): Promise<T> => {

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
      success: async (res) => {
        // HTTP 403：token 失效，自动重登后重试（最多 1 次）
        if (res.statusCode === 403) {
          if (isRetry) {
            Taro.showToast({ title: '登录失效，请重新打开小程序', icon: 'none' })
            reject(new Error('登录失效'))
            return
          }
          try {
            removeToken()
            await silentReLogin()
            const result = await request<T>(options, true)
            resolve(result)
          } catch (e) {
            Taro.showToast({ title: '登录失效，请重新打开小程序', icon: 'none' })
            reject(e)
          }
          return
        }

        const body = res.data as ApiResponse<T>
        if (body.code === 0) {
          resolve(body.data)
        } else {
          const msg = body.message?.zh_CN || '请求失败'
          Taro.showToast({ title: msg, icon: 'none' })
          reject(new Error(msg))
        }
      },
      fail: (err) => {
        const msg = err.errMsg || '网络异常，请稍后重试'
        Taro.showToast({ title: '网络异常，请稍后重试', icon: 'none' })
        reject(new Error(msg))
      },
    })
  })
}

export default request
