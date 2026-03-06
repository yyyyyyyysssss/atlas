import './index.css'
import { useEffect, useMemo, useRef, useState } from 'react'
import { Dropdown, Flex, Tree, Splitter, Typography, Input, Checkbox, theme } from 'antd'
import { Building2, Building, Component, Users, Plus } from 'lucide-react';
import { OperationMode } from '../../../enums/common';
import HasPermission from '../../../components/HasPermission';
import { useRequest } from 'ahooks';
import { useTranslation } from 'react-i18next';
import Loading from '../../../components/loading';
import { fetchOrgTree } from '../../../services/SystemService';
import OrgDetails from './details';
import { OrganizationType } from '../../../enums/system';

const getParentKey = (id, tree) => {
    let parentKey
    for (let i = 0; i < tree.length; i++) {
        const node = tree[i]
        if (node.children) {
            if (node.children.some(item => item.id === id)) {
                parentKey = node.id
            } else if (getParentKey(id, node.children)) {
                parentKey = getParentKey(id, node.children)
            }
        }
    }
    return parentKey
}

const getAllKeys = (data) => {
    const keys = []
    const traverse = (nodes) => {
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

let defaultOrgTypes = [OrganizationType.GROUP.value, OrganizationType.COMPANY.value]

let allOrgTypes = Object.values(OrganizationType).map(item => item.value)

const OrgTypeIcon = {
    [OrganizationType.GROUP.value]: <Building2 size={16} />,
    [OrganizationType.COMPANY.value]: <Building size={16} />,
    [OrganizationType.DEPT.value]: <Component size={16} />,
    [OrganizationType.TEAM.value]: <Users size={16} />,
}

const OrgManage = () => {

    const { t } = useTranslation()

    const [orgData, setOrgData] = useState([])

    const [selectedOrg, setSelectedOrg] = useState(null)

    const [selectedKeys, setSelectedKeys] = useState(null)

    const [expandedKeys, setExpandedKeys] = useState([])

    const [searchValue, setSearchValue] = useState('')

    const [autoExpandParent, setAutoExpandParent] = useState(true)

    const [showAllLevel, setShowAllLevel] = useState(false)

    const { runAsync: getOrgTreeAsync, loading: getOrgTreeLoading } = useRequest(fetchOrgTree, {
        manual: true
    })

    const flattenTreeRef = useRef()

    useEffect(() => {
        refreshOrgTree(null, showAllLevel)
    }, [showAllLevel])

    // 将树形结构扁平化，方便搜索
    useEffect(() => {
        const flattenTree = (orgData) => {
            const result = []
            const dfs = (nodes) => {
                nodes.forEach(node => {
                    result.push({ ...node, children: null })
                    if (node.children && node.children.length > 0) {
                        dfs(node.children)
                    }
                })
            }
            dfs(orgData)
            return result
        }
        flattenTreeRef.current = flattenTree(orgData)
    }, [orgData])

    // 刷新组织树数据
    const refreshOrgTree = async (options, showAllLevel) => {
        let orgTypes = showAllLevel ? allOrgTypes : defaultOrgTypes
        const data = await getOrgTreeAsync(orgTypes)
        setOrgData(data)
        if (showAllLevel) {
            const allKeys = getAllKeys(data)
            setExpandedKeys(allKeys)
        } else if (expandedKeys.length == 0) { // 默认展开第一层级
            setExpandedKeys(data.map((node) => node.id))
        }

        if (options?.selectOrgId) {
            handleSelectOrg(options.selectOrgId)
        }
    }

    const selectedOrgTreeNode = (key) => {
        handleSelectOrg(key)
    }

    const handleAddOrg = (type, orgItem) => {
        if (type === 'child') {
            setSelectedOrg({
                id: null,
                parentId: orgItem.id,
                parentCode: orgItem.code,
                orgType: OrganizationType.COMPANY.value,
                operationMode: OperationMode.ADD.value
            })
        } else {
            const parentOrgItem = flattenTreeRef.current.find(f => f.id === orgItem.parentId)
            setSelectedOrg({
                id: null,
                parentId: parentOrgItem.id,
                parentCode: parentOrgItem.code,
                orgType: orgItem.orgType,
                operationMode: OperationMode.ADD.value
            })
        }
        // 将选中的取消
        setSelectedKeys([])
    }

    const convertToTreeData = (data, selectedKeys, searchValue) => {
        return data.map(item => {
            const selected = selectedKeys?.includes(item.id)
            const strTitle = item.orgName
            const index = strTitle.indexOf(searchValue)
            const beforeStr = strTitle.substring(0, index)
            const afterStr = strTitle.slice(index + searchValue.length)
            const title = index > -1 ? (
                <span key={item.id}>
                    {beforeStr}
                    <span className="site-tree-search-value">{searchValue}</span>
                    {afterStr}
                </span>
            ) : (
                <span key={item.id}>{strTitle}</span>
            )
            item.title = title
            return {
                title: <OrgItem
                    item={item}
                    selected={selected}
                    onAddOrg={handleAddOrg}
                />,
                key: item.id,
                children: item.children && item.children.length > 0 ? convertToTreeData(item.children, selectedKeys, searchValue) : [],
            }
        })
    }

    const orgItems = useMemo(() => convertToTreeData(orgData, selectedKeys, searchValue), [orgData, selectedKeys, searchValue]);

    const handleSelect = (selectedKeys, info) => {
        const clickedKey = info.node.key
        handleSelectOrg(clickedKey)
    }

    // 选中组织
    const handleSelectOrg = async (orgId) => {
        const selectedOrg = flattenTreeRef.current.find(f => f.id == orgId)
        // 不取消选中
        setSelectedKeys([orgId])
        setSelectedOrg({
            id: orgId,
            parentId: null,
            parentCode: null,
            orgType: selectedOrg.orgType,
            operationMode: OperationMode.VIEW.value
        })
    }

    const handleExpand = (newExpandedKeys) => {
        setExpandedKeys(newExpandedKeys)
        setAutoExpandParent(false)
    }

    const handleSearchChange = (e) => {
        const { value } = e.target
        const newExpandedKeys = flattenTreeRef.current
            .map(item => {
                if (item.orgName.includes(value)) {
                    return getParentKey(item.id, orgData)
                }
                return null
            })
            .filter((item, i, self) => !!(item && self.indexOf(item) === i))
        setExpandedKeys(newExpandedKeys)
        setSearchValue(value)
        setAutoExpandParent(true)
    }

    const changeOperationMode = (operationMode) => {
        setSelectedOrg({
            ...selectedOrg,
            operationMode: operationMode
        })
    }

    return (
        <Flex flex={1} gap={10} className='h-full'>
            <Splitter>
                <Splitter.Panel style={{ padding: '10px' }} defaultSize="25%" min="20%" max="50%">
                    <Flex
                        vertical
                    >
                        <Input.Search style={{ marginBottom: 8 }} placeholder="搜索" onChange={handleSearchChange} allowClear />
                        <Flex gap={8} justify='end'>
                            <Typography.Text onClick={() => setShowAllLevel(!showAllLevel)} type="secondary" style={{ fontSize: '12px', cursor: 'pointer' }}>
                                {t('包含下级组织')}
                            </Typography.Text>
                            <Checkbox
                                checked={showAllLevel}
                                onChange={(e) => setShowAllLevel(e.target.checked)}
                            />
                        </Flex>

                        <Loading spinning={getOrgTreeLoading}>
                            <Tree
                                className="draggable-tree"
                                draggable={{
                                    icon: false
                                }}
                                blockNode
                                treeData={orgItems}
                                selectedKeys={selectedKeys}
                                onSelect={handleSelect}
                                expandedKeys={expandedKeys} // 控制展开的节点
                                onExpand={handleExpand} // 更新展开的节点
                                autoExpandParent={autoExpandParent}
                            />
                        </Loading>
                    </Flex>
                </Splitter.Panel>
                <Splitter.Panel style={{ padding: '20px' }}>
                    <OrgDetails
                        orgId={selectedOrg?.id}
                        parentId={selectedOrg?.parentId}
                        parentCode={selectedOrg?.parentCode}
                        orgType={selectedOrg?.orgType}
                        operationMode={selectedOrg?.operationMode}
                        changeOperationMode={changeOperationMode}
                        selectedOrgTreeNode={selectedOrgTreeNode}
                        onSuccess={(orgId) => {
                            refreshOrgTree({
                                selectOrgId: orgId
                            }, showAllLevel)
                        }}
                    />
                </Splitter.Panel>
            </Splitter>
        </Flex>
    )
}

const OrgItem = ({ item, selected, onAddOrg }) => {

    const { t } = useTranslation()

    return (
        <Flex
            justify='space-between'
            align='center'
        >
            <Flex align='center' gap={8}>
                {OrgTypeIcon[item.orgType]}
                <Typography.Text>
                    {item.title}
                </Typography.Text>
            </Flex>

            <HasPermission requireAll={true} hasPermissions={['system:org:write', 'system:org:delete']}>
                <div className={`flex items-center transition-opacity ${selected ? 'opacity-100' : 'opacity-0'}`}>
                    <Dropdown
                        menu={{
                            items: [
                                {
                                    key: 'child',
                                    label: t('新增子级')
                                },
                                {
                                    key: 'brother',
                                    label: t('新增同级')
                                }
                            ],
                            onClick: (info) => {
                                const event = info.domEvent
                                const key = info.key
                                event.stopPropagation()
                                onAddOrg(key, item)
                            }
                        }}
                    >
                        <div
                            className='org-ops-btn'
                            onClick={e => {
                                e.stopPropagation()
                            }}
                        >
                            <Plus size={18} />
                        </div>
                    </Dropdown>
                </div>
            </HasPermission>
        </Flex>
    )
}

export default OrgManage