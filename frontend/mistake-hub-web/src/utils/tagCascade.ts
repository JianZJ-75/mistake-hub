import type { TagResp } from "#/entity";

/** 收集节点的所有子孙 ID */
const getDescendantIds = (nodes: TagResp[], targetId: number): number[] => {
	const ids: number[] = [];
	const walk = (list: TagResp[]) => {
		for (const node of list) {
			if (node.id === targetId) {
				collectAll(node.children);
				return;
			}
			if (node.children?.length) walk(node.children);
		}
	};
	const collectAll = (children?: TagResp[]) => {
		if (!children) return;
		for (const c of children) {
			ids.push(c.id);
			collectAll(c.children);
		}
	};
	walk(nodes);
	return ids;
};

/** 收集节点的所有祖先 ID（从父到根） */
const getAncestorIds = (nodes: TagResp[], targetId: number): number[] => {
	const ids: number[] = [];
	const find = (list: TagResp[], path: number[]): boolean => {
		for (const node of list) {
			if (node.id === targetId) {
				ids.push(...path);
				return true;
			}
			if (node.children?.length && find(node.children, [...path, node.id])) {
				return true;
			}
		}
		return false;
	};
	find(nodes, []);
	return ids;
};

/** 检查某节点的所有直接子节点是否都在 selectedIds 中 */
const areAllChildrenSelected = (nodes: TagResp[], parentId: number, selectedIds: Set<number>): boolean => {
	const findNode = (list: TagResp[]): TagResp | null => {
		for (const node of list) {
			if (node.id === parentId) return node;
			if (node.children?.length) {
				const found = findNode(node.children);
				if (found) return found;
			}
		}
		return null;
	};
	const parent = findNode(nodes);
	if (!parent?.children?.length) return false;
	return parent.children.every(c => selectedIds.has(c.id));
};

/**
 * 级联切换标签选中状态
 * - 选中：加入自身 + 所有子孙，然后向上检查祖先是否应自动选中
 * - 取消：移除自身 + 所有子孙 + 所有祖先
 */
export const cascadeToggleTag = (tree: TagResp[], currentIds: number[], toggleId: number): number[] => {
	const isSelected = currentIds.includes(toggleId);
	const set = new Set(currentIds);

	if (isSelected) {
		// 取消：移除自身 + 所有子孙 + 所有祖先
		set.delete(toggleId);
		for (const id of getDescendantIds(tree, toggleId)) set.delete(id);
		for (const id of getAncestorIds(tree, toggleId)) set.delete(id);
	} else {
		// 选中：加入自身 + 所有子孙
		set.add(toggleId);
		for (const id of getDescendantIds(tree, toggleId)) set.add(id);
		// 向上检查祖先链，若祖先的所有直接子节点都已选中则也加入
		const ancestors = getAncestorIds(tree, toggleId);
		for (const ancestorId of ancestors) {
			if (areAllChildrenSelected(tree, ancestorId, set)) {
				set.add(ancestorId);
			} else {
				break;
			}
		}
	}

	return [...set];
};
