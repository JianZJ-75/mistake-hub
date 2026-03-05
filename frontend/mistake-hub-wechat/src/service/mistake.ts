import request from '../utils/request'
import { MistakeDetailResp, PageResult } from '../types'

export interface MistakeListReq {
  subject?: string
  tagId?: number
  masteryFilter?: number
  pageNum: number
  pageSize: number
}

export interface MistakeAddReq {
  title: string
  correctAnswer?: string
  errorReason?: string
  imageUrl?: string
  subject?: string
  tagIds?: number[]
}

export interface MistakeUpdateReq {
  id: number
  title?: string
  correctAnswer?: string
  errorReason?: string
  imageUrl?: string
  subject?: string
  tagIds?: number[]
}

export const mistakeList = (params: MistakeListReq): Promise<PageResult<MistakeDetailResp>> =>
  request({ url: '/v1/mistake/list', data: params })

export const mistakeAdd = (params: MistakeAddReq): Promise<void> =>
  request({ url: '/v1/mistake/add', data: params })

export const mistakeDetail = (id: number): Promise<MistakeDetailResp> =>
  request({ url: '/v1/mistake/detail', data: { id } })

export const mistakeUpdate = (params: MistakeUpdateReq): Promise<void> =>
  request({ url: '/v1/mistake/update', data: params })

export const mistakeDelete = (id: number): Promise<void> =>
  request({ url: '/v1/mistake/delete', data: { id } })
