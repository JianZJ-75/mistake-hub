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
  if (level >= 80) return { label: '已掌握', cls: 'badge-high' }
  if (level >= 60) return { label: '掌握中', cls: 'badge-mid' }
  return { label: '未掌握', cls: 'badge-low' }
}
