import { TagResp } from '../types'

/**
 * 将标签树展开为平铺列表（保留层级顺序）
 */
export const flattenTags = (tags: TagResp[]): TagResp[] => {
  const result: TagResp[] = []
  for (const tag of tags) {
    result.push(tag)
    if (tag.children?.length) result.push(...flattenTags(tag.children))
  }
  return result
}

/**
 * 根据掌握度返回标签信息
 */
export const getMasteryInfo = (level: number) => {
  if (level >= 80) return { label: '已掌握', cls: 'badge-high' }
  if (level >= 60) return { label: '掌握中', cls: 'badge-mid' }
  return { label: '未掌握', cls: 'badge-low' }
}
