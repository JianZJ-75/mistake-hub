import Taro from '@tarojs/taro'

// 后端接口基础地址
// 本地开发用 localhost，真机调试时改为内网 IP 或线上域名
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
export interface ApiResponse<T = unknown> {
  code: number
  message: { zh_CN: string; en_US: string }
  data: T
}

// 登录就绪 promise 单例，防止并发重复登录
let _pendingLogin: Promise<void> | null = null

/**
 * 注册登录 promise 单例
 * account.ts 在 login() 中调用，让页面请求在无 token 时等待登录完成再发出
 */
export const setPendingLogin = (p: Promise<void>): void => {

  _pendingLogin = p.finally(() => {
    _pendingLogin = null
  })
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
  const token = res.data && res.data.data
  if (token) {
    setToken(token)
  } else {
    throw new Error('重新登录失败')
  }
}

/**
 * 统一请求方法，自动附加 Bearer token
 * - 无 token 且登录进行中：等待登录完成后再发请求，消除冷启动 403
 * - HTTP 200 + code 0：返回 data
 * - HTTP 200 + code 非0：reject 并提示中文错误
 * - HTTP 403：使用单例重登，防止并发，重试一次
 * - 网络异常：reject 并提示
 */
const request = <T = unknown>(options: RequestOptions, isRetry = false): Promise<T> => {

  const doRequest = async (): Promise<T> => {

    // 冷启动保护：无 token 且登录正在进行中，等待 token 就绪再发请求
    if (!isRetry && !getToken() && _pendingLogin) {
      await _pendingLogin.catch(() => {})
    }

    const { url, method = 'POST', data } = options
    const token = getToken()
    const isGet = method === 'GET'

    // GET 请求过滤 undefined/null 参数，避免序列化为字符串 "undefined"
    const cleanData = isGet && data && typeof data === 'object' && !Array.isArray(data)
      ? Object.fromEntries(Object.entries(data as Record<string, unknown>).filter(([, v]) => v !== undefined && v !== null))
      : data

    const header: Record<string, string> = {}
    if (!isGet) {
      header['Content-Type'] = 'application/json'
    }
    if (token) {
      header['Authorization'] = `Bearer ${token}`
    }

    return new Promise<T>((resolve, reject) => {
      Taro.request({
        url: `${BASE_URL}${url}`,
        method,
        ...(cleanData !== undefined ? { data: cleanData } : {}),
        header,
        success: async (res) => {
          // HTTP 403：token 失效，共享单例重登后重试（最多 1 次）
          if (res.statusCode === 403) {
            if (isRetry) {
              Taro.showToast({ title: '登录失效，请重新打开小程序', icon: 'none' })
              reject(new Error('登录失效'))
              return
            }
            try {
              removeToken()
              // 使用单例，多个并发 403 共享同一次重登
              if (!_pendingLogin) {
                _pendingLogin = silentReLogin().finally(() => {
                  _pendingLogin = null
                })
              }
              await _pendingLogin
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
            const msg = (body.message && body.message.zh_CN) || '请求失败'
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

  return doRequest()
}

export default request
