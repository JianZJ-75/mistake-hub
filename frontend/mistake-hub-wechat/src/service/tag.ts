import request from '../utils/request'
import { TagResp } from '../types'

export const tagTree = (): Promise<TagResp[]> =>
  request({ url: '/v1/tag/tree', method: 'GET' })

export const tagCustomList = (): Promise<TagResp[]> =>
  request({ url: '/v1/tag/custom/list', method: 'GET' })

export const tagCustomAdd = (name: string): Promise<void> =>
  request({ url: '/v1/tag/custom/add', data: { name } })

export const tagCustomDelete = (id: number): Promise<void> =>
  request({ url: '/v1/tag/custom/delete', data: { id } })
