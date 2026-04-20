import request from '../utils/request'
import { MistakeDetailResp, PageResult } from '../types'

export interface MistakeListReq {
  tagIds?: string
  masteryGroup?: number    // 0=未掌握(<100), 1=已掌握(=100)
  pageNum: number
  pageSize: number
}

export interface MistakeAddReq {
  title: string
  correctAnswer?: string
  titleImageUrl?: string
  answerImageUrl?: string
  tagIds?: number[]
  note?: string
  noteImageUrl?: string
}

export interface MistakeUpdateReq {
  id: number
  title?: string
  correctAnswer?: string
  titleImageUrl?: string
  answerImageUrl?: string
  tagIds?: number[]
}

export const mistakeList = (params: MistakeListReq): Promise<PageResult<MistakeDetailResp>> =>
  request({ url: '/v1/mistake/list', method: 'GET', data: params })

export const mistakeAdd = (params: MistakeAddReq): Promise<void> =>
  request({ url: '/v1/mistake/add', data: params })

export const mistakeDetail = (id: number): Promise<MistakeDetailResp> =>
  request({ url: '/v1/mistake/detail', data: { id } })

export const mistakeModify = (params: MistakeUpdateReq): Promise<void> =>
  request({ url: '/v1/mistake/modify', data: params })

export const mistakeDelete = (id: number): Promise<void> =>
  request({ url: '/v1/mistake/delete', data: { id } })
