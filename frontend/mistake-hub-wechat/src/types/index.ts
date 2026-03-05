export interface TagResp {
  id: number
  name: string
  type: string
  parentId: number
  createdTime: string
  children?: TagResp[]
}

export interface MistakeDetailResp {
  id: number
  title: string
  correctAnswer?: string
  errorReason?: string
  imageUrl?: string
  subject?: string
  reviewStage: number
  masteryLevel: number
  lastReviewTime?: string
  nextReviewTime?: string
  createdTime?: string
  updatedTime?: string
  tags?: TagResp[]
}

export interface PageResult<T> {
  records: T[]
  total: number
  size: number
  current: number
  pages: number
}
