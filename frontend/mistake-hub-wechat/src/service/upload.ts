import Taro from '@tarojs/taro'
import { BASE_URL, getToken, removeToken, setToken } from '../utils/request'

/**
 * 上传图片到服务器，返回图片 URL
 * 支持 403 自动重登后重试（最多 1 次）
 */
export const uploadImage = (filePath: string, isRetry = false): Promise<string> =>
  new Promise((resolve, reject) => {
    Taro.uploadFile({
      url: `${BASE_URL}/v1/upload/image`,
      filePath,
      name: 'file',
      header: { Authorization: `Bearer ${getToken()}` },
      success: async (res) => {
        // 403：token 失效，自动重登后重试（最多 1 次）
        if (res.statusCode === 403) {
          if (isRetry) {
            Taro.showToast({ title: '登录失效，请重新打开小程序', icon: 'none' })
            reject(new Error('登录失效'))
            return
          }
          try {
            removeToken()
            await silentReLoginForUpload()
            const url = await uploadImage(filePath, true)
            resolve(url)
          } catch (e) {
            Taro.showToast({ title: '登录失效，请重新打开小程序', icon: 'none' })
            reject(e)
          }
          return
        }

        try {
          const body = JSON.parse(res.data)
          if (body.code === 0) {
            resolve(body.data as string)
          } else {
            const msg = body.message?.zh_CN || '上传失败'
            Taro.showToast({ title: msg, icon: 'none' })
            reject(new Error(msg))
          }
        } catch {
          reject(new Error('解析上传响应失败'))
        }
      },
      fail: (err) => {
        Taro.showToast({ title: '图片上传失败', icon: 'none' })
        reject(new Error(err.errMsg || '图片上传失败'))
      },
    })
  })

/**
 * 上传专用的静默重登（不走 request 封装，避免循环依赖）
 */
const silentReLoginForUpload = async (): Promise<void> => {

  const { code } = await new Promise<{ code: string }>((resolve, reject) => {
    Taro.login({
      success: (res) => res.code ? resolve({ code: res.code }) : reject(new Error('wx.login 无 code')),
      fail: (err) => reject(new Error(err.errMsg)),
    })
  })

  const res = await Taro.request<{ code: number; data: string }>({
    url: `${BASE_URL}/v1/account/login-wx`,
    method: 'POST',
    data: { code },
    header: { 'Content-Type': 'application/json' },
  })

  const token = res.data?.data
  if (token) {
    setToken(token)
  } else {
    throw new Error('重新登录失败')
  }
}
