import { Flex, Input, Spin, Tree } from "antd"
import { useEffect, useMemo, useRef, useState } from "react"
import { fetchAuthorityTree } from "../services/SystemService"
import { DataNode } from "antd/es/tree"
import Loading from "./loading"


const getParentKey = (id: any, tree: any): any => {
    let parentKey
    for (let i = 0; i < tree.length; i++) {
        const node = tree[i]
        if (node.children) {
            if (node.children.some((item: any) => item.id === id)) {
                parentKey = node.id
            } else if (getParentKey(id, node.children)) {
                parentKey = getParentKey(id, node.children)
            }
        }
    }
    return parentKey
}

interface AuthorityTreeProps {
    value?: string[]
    onChange?: (value: string[]) => void
}

const AuthorityTree: React.FC<AuthorityTreeProps> = ({ value = [], onChange }) => {

    const [treeData, setTreeData] = useState<DataNode[]>([])

    const [expandedKeys, setExpandedKeys] = useState<React.Key[]>([])

    const [loading, setLoading] = useState(false)

    const [loaded, setLoaded] = useState(false)

    const [searchValue, setSearchValue] = useState('')

    const [autoExpandParent, setAutoExpandParent] = useState(true)

    const flattenTreeRef = useRef<any>(null)

    const fetchData = async () => {
        if (loaded) return
        setLoading(true)
        try {
            const list = await fetchAuthorityTree()
            setTreeData(list)
            const keys: string[] = []
            const walk = (nodes: any[]) => {
                nodes.forEach((node) => {
                    keys.push(node.id)
                    if (node.children) walk(node.children)
                })
            }
            walk(list)
            setExpandedKeys(keys)
            setLoaded(true)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        fetchData()
    }, [])

    useEffect(() => {
        const flattenTree = (treeData: any) => {
            const result: any = []
            const dfs = (nodes: any) => {
                nodes.forEach((node: any) => {
                    result.push({ ...node, children: null })
                    if (node.children && node.children.length > 0) {
                        dfs(node.children)
                    }
                })
            }
            dfs(treeData)
            return result
        }
        flattenTreeRef.current = flattenTree(treeData)
    }, [treeData])

    const { parentMap, childrenMap } = useMemo(() => {
        const parentMap = new Map<string, string>();
        const childrenMap = new Map<string, string[]>();
        const walk = (nodes: any[], parent?: string) => {
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

    const handleExpand = (newExpandedKeys: any[]) => {
        setExpandedKeys(newExpandedKeys)
        setAutoExpandParent(false)
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
        const walk = (nodes: any[]) => {
            nodes.forEach((node) => {
                keys.add(node.id)
                if (node.children) walk(node.children)
            })
        }
        walk(treeData)
        return keys
    }, [treeData])

    const handleSearchChange = (e: any) => {
        const { value } = e.target
        const newExpandedKeys = flattenTreeRef.current
            .map((item: any) => {
                if (item.label.includes(value)) {
                    return getParentKey(item.id, treeData)
                }
                return null
            })
            .filter((item: any, i: any, self: any) => !!(item && self.indexOf(item) === i))
        setExpandedKeys(newExpandedKeys)
        setSearchValue(value)
        setAutoExpandParent(true)
    }

    const convertToTreeData = (data: any, searchValue: any) => {

        return data.map((item: any) => {
            const strTitle = item.label
            const index = strTitle.indexOf(searchValue)
            const beforeStr = strTitle.substring(0, index)
            const afterStr = strTitle.slice(index + searchValue.length)
            const title = index > -1 ? (
                <span key={item.id}>
                    {beforeStr}
                    <span style={{ color: '#f50' }}>{searchValue}</span>
                    {afterStr}
                </span>
            ) : (
                <span key={item.id}>{strTitle}</span>
            )
            item.title = title
            return {
                title: title,
                key: item.id,
                children: item.children && item.children.length > 0 ? convertToTreeData(item.children, searchValue) : [],
            }
        })
    }

    const treeItems = useMemo(() => convertToTreeData(treeData, searchValue), [treeData, searchValue])

    const safeValue = useMemo(() => {
        const parentIds = new Set(childrenMap.keys())
        return (value || []).filter((id) => availableKeys.has(id) && !parentIds.has(id))
    }, [value, availableKeys])

    return (
        <Loading spinning={loading}>
            <Flex
                vertical
            >
                <Input.Search style={{ marginBottom: 8 }} placeholder="搜索" onChange={handleSearchChange} allowClear />
                <Tree
                    checkable
                    onExpand={handleExpand}
                    expandedKeys={expandedKeys}
                    checkedKeys={safeValue}
                    treeData={treeItems}
                    selectable={false}
                    onCheck={(checkedKeys) => handleChange(checkedKeys as string[])}
                    autoExpandParent={autoExpandParent}
                />
            </Flex>
        </Loading>
    )
}

export default AuthorityTree