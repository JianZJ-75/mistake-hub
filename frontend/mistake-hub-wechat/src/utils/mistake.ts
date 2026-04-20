import { TagResp } from '../types'

/**
 * 将标签树展开为平铺列表（保留层级顺序）
 */
export const flattenTags = (tags: TagResp[]): TagResp[] => {
  const result: TagResp[] = []
  for (const tag of tags) {
    result.push(tag)
    if (tag.children && tag.children.length) result.push(...flattenTags(tag.children))
  }
  return result
}

/**
 * 递归遍历标签树，查找指定 ID 对应的标签名
 */
export const findTagNamesByIds = (tree: TagResp[], ids: number[]): string[] => {
  const names: string[] = []
  const idSet = new Set(ids)
  const walk = (nodes: TagResp[]) => {
    for (const node of nodes) {
      if (idSet.has(node.id)) names.push(node.name)
      if (node.children && node.children.length) walk(node.children)
    }
  }
  walk(tree)
  return names
}

/**
 * 根据掌握度返回标签信息
 */
export const getMasteryInfo = (level: number) => {

  if (level >= 100) return { label: '已掌握', cls: 'badge-high', depthLevel: 5, depthText: '彻底掌握', actionText: '查看详情' }
  if (level >= 80) return { label: '已掌握', cls: 'badge-high', depthLevel: 4, depthText: '熟练掌握', actionText: '查看详情' }
  if (level >= 60) return { label: '掌握中', cls: 'badge-mid', depthLevel: 3, depthText: '基本掌握', actionText: '查看详情' }
  if (level >= 20) return { label: '未掌握', cls: 'badge-low', depthLevel: 2, depthText: '初步了解', actionText: '查看详情' }
  return { label: '未掌握', cls: 'badge-low', depthLevel: 1, depthText: '完全陌生', actionText: '查看详情' }
}

/** 学科中文 → 英文缩写映射 */
const SUBJECT_ABBR: Record<string, string> = {
  '数学': 'MATH', '英语': 'ENG', '物理': 'PHYS', '化学': 'CHEM',
  '生物': 'BIO', '历史': 'HIST', '地理': 'GEO', '政治': 'POL', '语文': 'CHN',
}

/**
 * 从 tags 中找 SUBJECT 类型标签，格式化为 "MATH-042"
 */
export const getFormattedId = (id: number, tags?: TagResp[]): string => {

  const subjectTag = tags?.find(t => t.type === 'SUBJECT')
  const abbr = subjectTag ? (SUBJECT_ABBR[subjectTag.name] || subjectTag.name.substring(0, 4).toUpperCase()) : 'Q'
  return `${abbr}-${String(id).padStart(3, '0')}`
}

/**
 * 递归收集标签及其所有子孙 ID
 */
export const collectTagIds = (tag: TagResp): number[] => {

  const ids = [tag.id]
  if (tag.children && tag.children.length) {
    for (const child of tag.children) {
      ids.push(...collectTagIds(child))
    }
  }
  return ids
}
