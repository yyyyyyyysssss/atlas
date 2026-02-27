import { useEffect, useMemo, useRef, useState } from 'react'
import './index.css'
import { Dropdown, Flex, Tree, Splitter, Typography, Input } from 'antd'
import { fetchCompanyTree } from '../../../services/SystemService'
import { Plus } from 'lucide-react';
import CompanyDetails from './details';
import HasPermission from '../../../components/HasPermission';
import { useRequest } from 'ahooks';
import { useTranslation } from 'react-i18next';
import { OperationMode } from '../../../enums/common';
import Loading from '../../../components/loading';

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

const CompanyManage = () => {

    const { t } = useTranslation()

    const [companyData, setCompanyData] = useState([])

    const [selectedKeys, setSelectedKeys] = useState([])

    const [expandedKeys, setExpandedKeys] = useState([])

    const [selectedCompany, setSelectedCompany] = useState(null)

    const [searchValue, setSearchValue] = useState('')

    const [autoExpandParent, setAutoExpandParent] = useState(true)

    const { runAsync: getCompanyTreeAsync, loading: getCompanyTreeLoading } = useRequest(fetchCompanyTree, {
        manual: true
    })

    const flattenTreeRef = useRef()

    useEffect(() => {
        refreshCompanyTree()
    }, [])

    const getAllKeys = (nodes) => {
        let keys = [];
        nodes.forEach((node) => {
            keys.push(node.id)  // 获取当前节点的id
            if (node.children && node.children.length > 0) {
                // 如果节点有子节点，则递归获取子节点的id
                keys = keys.concat(getAllKeys(node.children))
            }
        })
        return keys
    }

    const refreshCompanyTree = async (options) => {
        const data = await getCompanyTreeAsync()
        setCompanyData(data)
        if (expandedKeys.length == 0) {
            // 获取所有节点的 id 并展开所有节点
            const allKeys = getAllKeys(data)
            setExpandedKeys(allKeys)  // 展开所有节点
        }
        if (options?.selectCompanyId) {
            handleSelectCompany(options.selectCompanyId)
        }
    }

    useEffect(() => {
        const flattenTree = (companyData) => {
            const result = []
            const dfs = (nodes) => {
                nodes.forEach(node => {
                    result.push({ ...node, children: null })
                    if (node.children && node.children.length > 0) {
                        dfs(node.children)
                    }
                })
            }
            dfs(companyData)
            return result
        }
        flattenTreeRef.current = flattenTree(companyData)
    }, [companyData])

    const handleAddCompany = (type, companyItem) => {
        if (type === 'child') {
            setSelectedCompany({
                id: null,
                parentId: companyItem.id,
                parentName: companyItem.companyName,
                operationMode: OperationMode.ADD.value
            })
        } else {
            const parentCompany = flattenTreeRef.current.find(f => f.id === companyItem.parentId)
            setSelectedCompany({
                id: null,
                parentId: parentCompany?.id,
                parentName: parentCompany?.companyName,
                operationMode: OperationMode.ADD.value
            })
        }
        // 将选中的取消
        setSelectedKeys([])
    }

    const convertToTreeData = (data, selectedKeys, searchValue) => {

        return data.map(item => {
            const selected = selectedKeys.includes(item.id)
            const strTitle = item.companyName
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
                title: <CompanyItem
                    item={item}
                    selected={selected}
                    onAddCompany={handleAddCompany}
                />,
                key: item.id,
                children: item.children && item.children.length > 0 ? convertToTreeData(item.children, selectedKeys, searchValue) : [],
            }
        })
    }

    const companyItems = useMemo(() => convertToTreeData(companyData, selectedKeys, searchValue), [companyData, selectedKeys, searchValue])

    const handleExpand = (newExpandedKeys) => {
        setExpandedKeys(newExpandedKeys)
        setAutoExpandParent(false)
    }

    const handleSearchChange = (e) => {
        const { value } = e.target
        const newExpandedKeys = flattenTreeRef.current
            .map(item => {
                if (item.companyName.includes(value)) {
                    return getParentKey(item.id, companyData)
                }
                return null
            })
            .filter((item, i, self) => !!(item && self.indexOf(item) === i))
        setExpandedKeys(newExpandedKeys)
        setSearchValue(value)
        setAutoExpandParent(true)
    }

    const handleSelect = (selectedKeys, info) => {
        const clickedKey = info.node.key
        handleSelectCompany(clickedKey)
    }

    const handleSelectCompany = async (companyId) => {
        // 不取消选中
        setSelectedKeys([companyId])
        // 显示详情
        setSelectedCompany({
            id: companyId,
            parentId: null,
            parentName: null,
            operationMode: OperationMode.VIEW.value
        })
    }

    const changeOperationMode = (operationMode) => {
        setSelectedCompany({
            ...selectedCompany,
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
                        <Input.Search style={{ marginBottom: 8 }} placeholder="搜索" onChange={handleSearchChange} allowClear/>
                        <Loading spinning={getCompanyTreeLoading}>
                            <Tree
                                className="draggable-tree"
                                draggable={{
                                    icon: false
                                }}
                                blockNode
                                treeData={companyItems}
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
                    <CompanyDetails
                        companyId={selectedCompany?.id}
                        parentId={selectedCompany?.parentId}
                        parentName={selectedCompany?.parentName}
                        operationMode={selectedCompany?.operationMode}
                        changeOperationMode={changeOperationMode}
                        onSuccess={(companyId) => {
                            refreshCompanyTree({
                                selectCompanyId: companyId
                            })
                        }}
                    />
                </Splitter.Panel>
            </Splitter>
        </Flex>
    )
}

const CompanyItem = ({ item, selected, onAddCompany }) => {

    const { t } = useTranslation()

    return (
        <Flex
            justify='space-between'
            align='center'
        >
            <Typography.Text>
                {item.title}
            </Typography.Text>
            <HasPermission requireAll={true} hasPermissions={['system:company:write']}>
                <div className={`flex items-center transition-opacity ${selected ? 'opacity-100' : 'opacity-0'}`}>
                    <Dropdown
                        menu={{
                            items: [
                                {
                                    key: 'child',
                                    label: t('新增下级公司')
                                },
                                {
                                    key: 'brother',
                                    label: t('新增同级公司')
                                }
                            ],
                            onClick: (info) => {
                                const event = info.domEvent
                                const key = info.key
                                event.stopPropagation()
                                onAddCompany(key, item)
                            }
                        }}
                    >
                        <div
                            className='company-ops-btn'
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

export default CompanyManage