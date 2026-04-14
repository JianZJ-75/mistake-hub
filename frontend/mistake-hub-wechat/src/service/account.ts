import Taro from '@tarojs/taro'
import request, { BASE_URL, setToken, getToken, setPendingLogin, ApiResponse } from '../utils/request'
import { AccountDetailResp } from '../types'

// ===== 登录逻辑 =====

/**
 * 静默登录：wx.login 获取 code → 调用后端 → 缓存 token
 * 若 token 已存在则跳过，避免重复登录
 * 使用 Taro.request 直调，避免与 request.ts 的 _pendingLogin preflight 形成死锁
 */
export const login = (): Promise<void> => {

  // 已有 token，跳过登录
  if (getToken()) {
    return Promise.resolve()
  }

  const loginPromise = (async () => {
    try {
      const { code } = await wxLogin()
      const res = await Taro.request<ApiResponse<string>>({
        url: `${BASE_URL}/v1/account/login-wx`,
        method: 'POST',
        data: { code },
        header: { 'Content-Type': 'application/json' },
      })
      const token = res.data && res.data.data
      if (token) {
        setToken(token)
      }
    } catch (err) {
      // 静默登录失败不阻塞页面，只打印日志
      console.error('[login] 微信登录失败:', err)
    }
  })()

  // 注册到 request.ts 单例，页面请求无 token 时自动等待登录完成
  setPendingLogin(loginPromise)
  return loginPromise
}

/**
 * 主动刷新登录（如 token 过期后调用）
 */
export const refreshLogin = async (): Promise<void> => {

  try {
    const { code } = await wxLogin()
    const token = await request<string>({
      url: '/v1/account/login-wx',
      data: { code },
    })
    setToken(token)
  } catch (err) {
    console.error('[refreshLogin] 刷新登录失败:', err)
    throw err
  }
}

// ===== 工具方法 =====

/**
 * 获取当前用户详情
 */
export const currentDetail = (): Promise<AccountDetailResp> =>
  request({ url: '/v1/account/current-detail' })

/**
 * 修改每日复习量
 */
export const modifyDailyLimit = (dailyLimit: number): Promise<void> =>
  request({ url: '/v1/account/modify-daily-limit', data: { dailyLimit } })

/**
 * 修改个人资料（昵称、头像）
 */
export const modifyProfile = (data: { nickname?: string; avatarUrl?: string }): Promise<void> =>
  request({ url: '/v1/account/modify-profile', data })

/**
 * 封装 wx.login，返回 Promise
 */
const wxLogin = (): Promise<{ code: string }> => {

  return new Promise((resolve, reject) => {
    Taro.login({
      success: (res) => {
        if (res.code) {
          resolve({ code: res.code })
        } else {
          reject(new Error('wx.login 未返回 code'))
        }
      },
      fail: (err) => {
        reject(new Error(err.errMsg || 'wx.login 调用失败'))
      },
    })
  })
}
