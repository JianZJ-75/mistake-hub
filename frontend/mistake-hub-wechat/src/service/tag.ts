import request from '../utils/request'
import { TagResp } from '../types'

export const tagTree = (): Promise<TagResp[]> =>
  request({ url: '/v1/tag/tree', method: 'GET' })
