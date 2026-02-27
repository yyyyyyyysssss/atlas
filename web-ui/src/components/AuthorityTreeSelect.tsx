import { Spin, TreeSelect } from "antd";
import React, { useEffect, useMemo, useState } from "react";
import { fetchAuthorityTree } from "../services/SystemService";



interface Permission {
  id: string;
  name: string;
  children?: Permission[];
}

interface PermissionTreeSelectProps {
  value?: string[];
  onChange?: (value: string[]) => void;
}


const AuthorityTreeSelect: React.FC<PermissionTreeSelectProps> = ({ value, onChange }) => {

  const [treeData, setTreeData] = useState<Permission[]>([])

  const [loading, setLoading] = useState(false)

  const [loaded, setLoaded] = useState(false)

  const fetchData = async () => {
    if (loaded) return
    setLoading(true)
    try {
      const list = await fetchAuthorityTree()
      setTreeData(list)
      setLoaded(true)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    if (value && value.length > 0) {
      fetchData()
    }
  }, [value])

  const handleDropdownVisibleChange = (open: boolean) => {
    if (open) {
      fetchData()
    }
  }

  const { parentMap, childrenMap } = useMemo(() => {
    const parentMap = new Map<string, string>();
    const childrenMap = new Map<string, string[]>();
    const walk = (nodes: Permission[], parent?: string) => {
      nodes.forEach((node) => {
        if (parent) {
          parentMap.set(node.id, parent)
          const siblings = childrenMap.get(parent) || [];
          siblings.push(node.id)
          childrenMap.set(parent, siblings)
        }
        if (node.children?.length) walk(node.children, node.id)
      })
    }
    walk(treeData)
    return { parentMap, childrenMap }
  }, [treeData])

  const getAllParents = (id: string, parentMap: Map<string, string>) => {
    const parents: string[] = []
    let current = parentMap.get(id)
    while (current) {
      parents.push(current)
      current = parentMap.get(current)
    }
    return parents
  }

  const getAllChildren = (id: string, childrenMap: Map<string, string[]>) => {
    const childrenIds: string[] = []
    const stack = [id]
    while (stack.length > 0) {
      const current = stack.pop()!
      const children = childrenMap.get(current) || []
      childrenIds.push(...children)
      stack.push(...children)
    }
    return childrenIds
  }

  const handleChange = (checked: string[]) => {
    const finalIds = new Set<string>(checked);

    checked.forEach((id) => {
      // 获取节点所有父节点id
      const parentIds = getAllParents(id, parentMap)
      parentIds.forEach(item => finalIds.add(item));
    })
    onChange?.(Array.from(finalIds))
  }

  const availableKeys = useMemo(() => {
    const keys = new Set<string>()
    const walk = (nodes: Permission[]) => {
      nodes.forEach((node) => {
        keys.add(node.id)
        if (node.children) walk(node.children)
      })
    }
    walk(treeData)
    return keys
  }, [treeData])

  const safeValue = useMemo(() => {
    const parentIds = new Set(childrenMap.keys())
    return (value || [])
      .filter((id) => availableKeys.has(id) && !parentIds.has(id))
  }, [value, availableKeys])


  return (
    <Spin spinning={loading}>
      <TreeSelect
        style={{ width: '100%' }}
        showSearch={true}
        value={safeValue}
        treeData={treeData}
        maxTagCount={3}
        onChange={handleChange}
        treeCheckable={true}
        treeCheckStrictly={false}
        showCheckedStrategy={TreeSelect.SHOW_ALL}
        placeholder="请选择权限"
        onOpenChange={handleDropdownVisibleChange}
        filterTreeNode={(input, treeNode) => (treeNode.label ?? treeNode.title ?? '').toLowerCase().includes(input.toLowerCase())}
        treeDefaultExpandAll={true}
      />
    </Spin>
  )
}

export default AuthorityTreeSelect;