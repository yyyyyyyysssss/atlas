import { useRequest } from "ahooks"
import { forwardRef, Key, SetStateAction, useEffect, useImperativeHandle, useMemo, useState } from "react"
import { fetchOrgTree } from "../services/SystemService"
import { OrganizationType } from "../enums/system"
import { Checkbox, Flex, Input, Tree, TreeProps, Typography } from "antd"
import Loading from "./loading"
import { useTranslation } from 'react-i18next';

interface OrgTreeProps extends TreeProps{
    onSelect: (selectedKey: any | null, record: any) => void
    showAll?: boolean
    itemRender?: (record: any) => React.ReactNode
    selectFirst?: boolean
}

interface OrgTreeAction {
    refresh: () => void;
}

const getAllKeys = (data: any) => {
    const keys: any[] = []
    const traverse = (nodes: any[]) => {
        nodes.forEach(node => {
            keys.push(node.id)
            if (node.children && node.children.length > 0) {
                traverse(node.children)
            }
        })
    }
    traverse(data)
    return keys
}

let allOrgTypes = Object.values(OrganizationType).map(item => item.value)

let defaultOrgTypes = [OrganizationType.GROUP.value, OrganizationType.COMPANY.value]

const OrgTree = forwardRef<OrgTreeAction, OrgTreeProps>(({
    onSelect,
    showAll = false,
    itemRender,
    selectFirst = false,
    ...restProps
}, ref) => {

    const { t } = useTranslation()

    const [treeData, setTreeData] = useState<any[]>([])

    const [searchValue, setSearchValue] = useState('')

    const [showAllLevel, setShowAllLevel] = useState(showAll)

    const [expandedKeys, setExpandedKeys] = useState<React.Key[]>([])

    const [autoExpandParent, setAutoExpandParent] = useState(true)

    const [selectedKey, setSelectedKey] = useState<any>()

    const { runAsync: getOrgTreeAsync, loading: getOrgTreeLoading } = useRequest(fetchOrgTree, {
        manual: true
    })


    const fetchTreeData = async (selectKey?: any, showAllLevel?: boolean) => {
        let orgTypes = showAllLevel ? allOrgTypes : defaultOrgTypes
        const data = await getOrgTreeAsync(orgTypes)
        setTreeData(data)
        const allKeys = getAllKeys(data)
        setExpandedKeys(allKeys)
        if (selectKey) {
            handleSelectOrg(selectKey)
        } else if (selectFirst && data.length > 0) {
            const firstKey = data[0].id
            selectOrg(firstKey, data[0])
        }
    }

    useImperativeHandle(ref, () => ({
        refresh: () => {
            fetchTreeData(selectedKey, showAllLevel)
        }
    }))

    useEffect(() => {
        fetchTreeData(null, showAllLevel)
    }, [])

    const handleShowAllLevelChange = (checked: boolean) => {
        setShowAllLevel(checked)
        fetchTreeData(selectedKey, checked)
    }

    const flattenTree = useMemo(() => {
        const result: any[] = []
        const parentMap: any = {}
        const dfs = (nodes: any[], parentId = null) => {
            nodes.forEach(node => {
                result.push({ ...node, children: null })
                if (parentId !== null) {
                    parentMap[node.id] = parentId
                }
                if (node.children && node.children.length > 0) {
                    dfs(node.children, node.id)
                }
            })
        }
        dfs(treeData)
        return { flattenList: result, parentMap }
    }, [treeData])

    const convertToTreeData = (data: any, selectedKey: any, searchValue: string) => {
        return data.map((item: any) => {
            const selected = selectedKey === item.id
            const strTitle = item.orgName
            const index = strTitle.indexOf(searchValue)
            const beforeStr = strTitle.substring(0, index)
            const afterStr = strTitle.slice(index + searchValue.length)
            const title = index > -1 ? (
                <Typography.Text key={item.id}>
                    {beforeStr}
                    <Typography.Text style={{ color: '#f50' }}>{searchValue}</Typography.Text>
                    {afterStr}
                </Typography.Text>
            ) : (
                <Typography.Text key={item.id}>
                    {strTitle}
                </Typography.Text>
            )
            item.title = title
            item.selected = selected
            return {
                title: itemRender ? itemRender(item) : (
                    <Flex align='center' gap={8} style={{ height: '38px' }}>
                        <Typography.Text>
                            {item.title}
                        </Typography.Text>
                    </Flex>
                ),
                key: item.id,
                children: item.children && item.children.length > 0 ? convertToTreeData(item.children, selectedKey, searchValue) : [],
            }
        })
    }

    const orgItems = useMemo(() => convertToTreeData(treeData, selectedKey, searchValue), [treeData, selectedKey, searchValue]);


    const handleSelect = (selectedKeys: any, info: { node: { key: any } }) => {
        const clickedKey = info.node.key
        handleSelectOrg(clickedKey)
    }

    const handleSelectOrg = (orgId: any) => {
        const selectedOrg = flattenTree.flattenList.find(f => f.id == orgId)
        selectOrg(orgId, selectedOrg)
    }

    const selectOrg = (orgId: any, orgRecord: any) => {
        // 不取消选中
        setSelectedKey(orgId)
        const record = { ...orgRecord, children: null }
        onSelect?.(orgId, record)
    }

    const handleExpand = (newExpandedKeys: SetStateAction<Key[]>) => {
        setExpandedKeys(newExpandedKeys)
        setAutoExpandParent(false)
    }

    const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { value } = e.target
        const newExpandedKeys = flattenTree.flattenList
            .map(item => {
                if (item.orgName.includes(value)) {
                    return flattenTree.parentMap[item.id]
                }
                return null
            })
            .filter((item, i, self) => !!(item && self.indexOf(item) === i))
        setExpandedKeys(newExpandedKeys)
        setSearchValue(value)
        setAutoExpandParent(true)
    }


    return (
        <Flex
            vertical
        >
            <Input.Search style={{ marginBottom: 8 }} placeholder="搜索" onChange={handleSearchChange} allowClear />
            <Flex gap={8} justify='end'>
                <Typography.Text onClick={() => handleShowAllLevelChange(!showAllLevel)} type="secondary" style={{ fontSize: '12px', cursor: 'pointer' }}>
                    {t('包含下级组织')}
                </Typography.Text>
                <Checkbox
                    checked={showAllLevel}
                    onChange={(e) => handleShowAllLevelChange(e.target.checked)}
                />
            </Flex>

            <Loading spinning={getOrgTreeLoading}>
                <Tree
                    blockNode
                    treeData={orgItems}
                    selectedKeys={[selectedKey]} // 受控选中节点
                    onSelect={handleSelect}
                    expandedKeys={expandedKeys} // 控制展开的节点
                    onExpand={handleExpand} // 更新展开的节点
                    autoExpandParent={autoExpandParent}
                    {...restProps}
                />
            </Loading>
        </Flex>
    )
})

export default OrgTree