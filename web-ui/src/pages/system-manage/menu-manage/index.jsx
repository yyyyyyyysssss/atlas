import { useEffect, useMemo, useRef, useState } from 'react'
import './index.css'
import { Dropdown, Flex, Tree, Modal, Tooltip, Splitter, Typography, Input } from 'antd'
import { deleteMenu, fetchMenuTree, menuDrag } from '../../../services/SystemService'
import { Plus, Trash2 } from 'lucide-react';
import { OperationMode } from '../../../enums/common';
import Highlight from '../../../components/Highlight';
import HasPermission from '../../../components/HasPermission';
import { getMessageApi } from '../../../utils/MessageUtil';
import { useRequest } from 'ahooks';
import { useTranslation } from 'react-i18next';
import MenuDetails from './details';
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

const MenuItem = ({ item, selected, onAddMenu, onDeleteMenu }) => {

    const { t } = useTranslation()

    return (
        <Flex
            justify='space-between'
            align='center'
        >
            <Typography.Text>
                {item.title}
            </Typography.Text>
            <HasPermission requireAll={true} hasPermissions={['system:menu:write', 'system:menu:delete']}>
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
                                onAddMenu(key, item)
                            }
                        }}
                    >
                        <div
                            className='menu-ops-btn'
                            onClick={e => {
                                e.stopPropagation()
                            }}
                        >
                            <Plus size={18} />
                        </div>
                    </Dropdown>

                    <div
                        className='menu-ops-btn'
                        onClick={(e) => {
                            e.stopPropagation()
                            onDeleteMenu(item)
                        }}
                    >
                        <Tooltip title={t('删除菜单')}>
                            <Trash2 size={16} />
                        </Tooltip>
                    </div>
                </div>
            </HasPermission>
        </Flex>
    )
}

const MenuManage = () => {

    const { t } = useTranslation()

    const [modal, contextHolder] = Modal.useModal()

    const [menuData, setMenuData] = useState([])

    const [selectedMenu, setSelectedMenu] = useState(null)

    const [selectedKeys, setSelectedKeys] = useState(null)

    const [expandedKeys, setExpandedKeys] = useState([])

    const [searchValue, setSearchValue] = useState('')

    const [autoExpandParent, setAutoExpandParent] = useState(true)

    const { runAsync: getMenuTreeAsync, loading: getMenuTreeLoading } = useRequest(fetchMenuTree, {
        manual: true
    })

    const { runAsync: menuDragAsync, loading: menuDragLoading } = useRequest(menuDrag, {
        manual: true
    })

    const { runAsync: deleteMenuAsync, loading: deleteMenuLoading } = useRequest(deleteMenu, {
        manual: true
    })

    const flattenTreeRef = useRef()

    useEffect(() => {
        refreshMenuTree()
    }, [])

    useEffect(() => {
        const flattenTree = (menuData) => {
            const result = []
            const dfs = (nodes) => {
                nodes.forEach(node => {
                    result.push({ ...node, children: null })
                    if (node.children && node.children.length > 0) {
                        dfs(node.children)
                    }
                })
            }
            dfs(menuData)
            return result
        }
        flattenTreeRef.current = flattenTree(menuData)
    }, [menuData])

    const refreshMenuTree = async (options) => {
        const data = await getMenuTreeAsync()
        setMenuData(data)
        // 默认展开第一层级
        if (expandedKeys.length == 0) {
            setExpandedKeys(data.map((node) => node.id))
        }
        if (options?.selectMenuId) {
            handleSelectMenu(options.selectMenuId)
        }
    }

    const onDragEnter = (info) => {

    }

    const onDrop = (info) => {
        // 目标节点（被放下的节点）
        const dropKey = info.node.key
        //拖动的节点
        const dragKey = info.dragNode.key
        //相对于目标节点的位置（-1=上方，0=中间，1=下方）
        const dropPos = info.node.pos.split('-')
        const dropPosition = info.dropPosition - Number(dropPos[dropPos.length - 1])
        //true = 插入到目标节点的前/后；false = 插入为目标节点的子节点
        const dropToGap = info.dropToGap
        const position = dropToGap ? (dropPosition === -1 ? 'BEFORE' : 'AFTER') : 'INSIDE'
        menuDragAsync(dragKey, dropKey, position)
            .then(d => {
                if (d === true) {
                    getMessageApi().success(t('拖动成功'))
                    refreshMenuTree({selectMenuId: selectedKeys?.[0]})
                }
            })
    }

    const handleAddMenu = (type, menuItem) => {
        if (type === 'child') {
            setSelectedMenu({
                id: null,
                parentId: menuItem.id,
                parentCode: menuItem.code,
                operationMode: OperationMode.ADD.value
            })
        } else {
            const parentMenuItem = flattenTreeRef.current.find(f => f.id === menuItem.parentId)
            setSelectedMenu({
                id: null,
                parentId: parentMenuItem.id,
                parentCode: parentMenuItem.code,
                operationMode: OperationMode.ADD.value
            })
        }
        // 将选中的取消
        setSelectedKeys([])
    }


    const handleDeleteMenu = (menuItem) => {
        handleSelectMenu(menuItem.id)
        modal.confirm({
            title: t('确定删除'),
            content: (
                <>
                    是否删除 <Highlight>{menuItem.name}</Highlight> 菜单？删除后将一并移除其下所有子菜单和权限项。
                </>
            ),
            okText: t('确认'),
            cancelText: t('取消'),
            maskClosable: false,
            confirmLoading: deleteMenuLoading,
            onOk: async () => {
                await deleteMenuAsync(menuItem.id)
                getMessageApi().success(t('删除成功'))
                const newMenuData = deleteTreeNode(menuData, menuItem.id)
                setMenuData(newMenuData)
                setSelectedKeys(null)
                setSelectedMenu(null)
            },
        })
    }

    const convertToTreeData = (data, selectedKeys, searchValue) => {
        return data.map(item => {
            const selected = selectedKeys?.includes(item.id)
            const strTitle = item.name
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
                title: <MenuItem
                    item={item}
                    selected={selected}
                    onAddMenu={handleAddMenu}
                    onDeleteMenu={handleDeleteMenu}
                />,
                key: item.id,
                children: item.children && item.children.length > 0 ? convertToTreeData(item.children, selectedKeys, searchValue) : [],
            }
        })
    }

    const menuItems = useMemo(() => convertToTreeData(menuData, selectedKeys, searchValue), [menuData, selectedKeys, searchValue]);

    const handleSelect = (selectedKeys, info) => {
        const clickedKey = info.node.key
        handleSelectMenu(clickedKey)

    }

    const handleSelectMenu = async (menuId) => {
        // 不取消选中
        setSelectedKeys([menuId])
        setSelectedMenu({
            id: menuId,
            parentId: null,
            parentCode: null,
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
                if (item.name.includes(value)) {
                    return getParentKey(item.id, menuData)
                }
                return null
            })
            .filter((item, i, self) => !!(item && self.indexOf(item) === i))
        setExpandedKeys(newExpandedKeys)
        setSearchValue(value)
        setAutoExpandParent(true)
    }

    const sortBySortValue = (a, b) => {
        if (a.sort == null && b.sort == null) return 0;
        if (a.sort == null) return 1
        if (b.sort == null) return -1
        return a.sort - b.sort
    }

    const addTreeNode = (treeData, parentId, targetData) => {
        if (parentId == 0) {
            return [...(treeData || []), targetData].sort(sortBySortValue)
        }
        return treeData.map(node => {
            if (node.id === parentId) {
                const children = [...(node.children || []), targetData].sort(sortBySortValue)
                return { ...node, children }
            } else if (node.children && node.children.length > 0) {
                return {
                    ...node,
                    children: addTreeNode(node.children, parentId, targetData),
                };
            } else {
                return node
            }
        })
    }

    const updateTreeNode = (treeData, targetData) => {
        return treeData.map(node => {
            if (node.id === targetData.id) {
                return { ...node, ...targetData }
            } else if (node.children && node.children.length > 0) {
                return {
                    ...node,
                    children: updateTreeNode(node.children, targetData),
                };
            } else {
                return node
            }
        })
    }

    const deleteTreeNode = (treeData, targetId) => {
        return (treeData || []).filter(node => {
            // 如果当前节点是要删除的，直接过滤掉
            if (node.id === targetId) return false;

            // 如果当前节点有子节点，递归删除子节点
            if (node.children && node.children.length > 0) {
                node.children = deleteTreeNode(node.children, targetId);
            }

            return true;
        })
    }

    const changeOperationMode = (operationMode) => {
        setSelectedMenu({
            ...selectedMenu,
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
                        <Loading spinning={getMenuTreeLoading || menuDragLoading}>
                            <Tree
                                className="draggable-tree"
                                draggable={{
                                    icon: false
                                }}
                                blockNode
                                onDragEnter={onDragEnter}
                                onDrop={onDrop}
                                treeData={menuItems}
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
                    <MenuDetails
                        menuId={selectedMenu?.id}
                        parentId={selectedMenu?.parentId}
                        parentCode={selectedMenu?.parentCode}
                        operationMode={selectedMenu?.operationMode}
                        changeOperationMode={changeOperationMode}
                        onSuccess={(menuId) => {
                            refreshMenuTree({
                                selectMenuId: menuId
                            })
                        }}
                    />
                </Splitter.Panel>
            </Splitter>
            {contextHolder}
        </Flex>
    )
}

export default MenuManage