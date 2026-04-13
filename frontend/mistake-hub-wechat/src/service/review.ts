import request from '../utils/request'
import { ReviewTaskResp, ReviewSubmitResp, ReviewProgressResp, ReviewRecordResp } from '../types'

/** 获取今日复习任务列表 */
export const reviewToday = (): Promise<ReviewTaskResp[]> =>
  request({ url: '/v1/review/today' })

/** 提交复习结果 */
export const reviewSubmit = (
  mistakeId: number,
  isCorrect: boolean,
  note?: string,
  noteImageUrl?: string,
): Promise<ReviewSubmitResp> =>
  request({ url: '/v1/review/submit', data: { mistakeId, isCorrect, note, noteImageUrl } })

/** 跳过复习 */
export const reviewSkip = (mistakeId: number): Promise<void> =>
  request({ url: '/v1/review/skip', data: { mistakeId } })

/** 获取今日复习进度 */
export const reviewProgress = (): Promise<ReviewProgressResp> =>
  request({ url: '/v1/review/progress' })

/** 查询错题的复习历史记录 */
export const reviewHistory = (mistakeId: number): Promise<ReviewRecordResp[]> =>
  request({ url: '/v1/review/history', data: { mistakeId } })
