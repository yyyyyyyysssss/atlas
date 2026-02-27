import { useCallback, useEffect, useMemo, useState } from 'react';
import './index.css'
import { Dropdown, Tabs, theme } from 'antd';
import { useLocation, useNavigate } from 'react-router-dom';
import { findRouteByPath } from '../../router/router';
import { useDispatch, useSelector } from 'react-redux';
import { addTabIem, removeTabItem, setTabIem, removeAllTabItem, removeLeftTabItem, removeOtherTabItem, removeRightTabItem } from '../../redux/slices/layoutSlice';
import {
    DndContext,
    closestCenter,
    KeyboardSensor,
    PointerSensor,
    useSensor,
    useSensors,
    DragOverlay,
} from '@dnd-kit/core'
import {
    arrayMove,
    SortableContext,
    sortableKeyboardCoordinates,
    horizontalListSortingStrategy
} from '@dnd-kit/sortable'
import { useTranslation } from 'react-i18next';
import SortableItem from '../../components/SortableItem';


const TopMenuTab = ({ style }) => {

    const { t } = useTranslation()

    const { token } = theme.useToken()

    const location = useLocation()

    const dispatch = useDispatch()

    const flattenMenuItems = useSelector(state => state.layout.flattenMenuItems)

    const activeKey = useSelector(state => state.layout.activeKey)

    const tabItems = useSelector(state => state.layout.tabItems)

    const [draggedTab, setDraggedTab] = useState(null)

    const navigate = useNavigate()

    const sensors = useSensors(
        useSensor(PointerSensor, {
            activationConstraint: {
                distance: 5
            }
        }),
        useSensor(KeyboardSensor, {
            coordinateGetter: sortableKeyboardCoordinates,
        })
    )

    useEffect(() => {
        if (location.pathname && location.pathname !== '/' && flattenMenuItems && flattenMenuItems.length > 0) {
            const route = findRouteByPath(location.pathname)
            if (route && route.path !== '') {
                add(location, route)
            }
        }
        // eslint-disable-next-line
    }, [flattenMenuItems, location])

    const add = (location, route) => {
        const path = location.pathname
        const closable = route.path !== 'home'
        const tabItem = {
            path: path,
            label: route.breadcrumbName,
            search: location.search,
            state: location.state,
            closable: closable
        }
        dispatch(addTabIem({ tabItem: tabItem }))
    }

    const remove = targetKey => {
        const selectKey = afterRemoveSelectKey(targetKey)
        dispatch(removeTabItem({ targetKey: targetKey, selectKey: selectKey }))
        if (selectKey) {
            switchTab(selectKey)
        }
    }

    const afterRemoveSelectKey = useCallback((targetKey) => {
        const targetIndex = tabItems.findIndex(pane => pane.key === targetKey)
        const newPanes = tabItems.filter(pane => pane.key !== targetKey)
        if (newPanes.length && targetKey === activeKey) {
            const { key } = newPanes[targetIndex === newPanes.length ? targetIndex - 1 : targetIndex]
            return key
        }
        return null
    }, [tabItems, activeKey])

    const onChange = key => {
        switchTab(key)
    }

    const handleEdit = (targetKey, action) => {
        if (action !== 'add') {
            remove(targetKey)
        }
    }

    const switchTab = useCallback((tabKey) => {
        const tabItem = tabItems.find(item => item.key === tabKey)
        if (tabItem) {
            const path = tabItem.search ? tabItem.path + tabItem.search : tabItem.path
            navigate(path, {
                state: tabItem.state
            })
        }
        // eslint-disable-next-line
    }, [tabItems])

    const items = useMemo(() => {
        return tabItems.map((item, index) => ({
            key: item.key,
            index: index,
            label: item.label,
            closable: item.closable
        }))
    }, [tabItems])

    const handleDragStart = (event) => {
        const { active } = event
        const tab = tabItems.find(t => t.key === active.id)
        setDraggedTab(tab)
    }

    const handleDragEnd = (event) => {
        const { active, over } = event

        if (document.activeElement instanceof HTMLElement) {
            document.activeElement.blur()
        }

        if (!over || active.id === over.id) {
            setDraggedTab(null)
            return
        }
        const oldIndex = tabItems.findIndex(item => item.key === active.id)
        const newIndex = tabItems.findIndex(item => item.key === over.id)
        if (oldIndex === -1 || newIndex === -1) return items // 防御性判断
        const newOrder = arrayMove(tabItems, oldIndex, newIndex)
        dispatch(setTabIem({ tabItem: newOrder }))
        setDraggedTab(null)
    }

    const handleDragCancel = () => {
        if (document.activeElement instanceof HTMLElement) {
            document.activeElement.blur()
        }
        setDraggedTab(null)
    }

    const handleMenuAction = useCallback((actionKey, tabKey, tabIndex) => {
        const tabSize = tabItems.length - 1;

        const afterCloseAllSelectKey = () => {
            if (tabSize > 0) return tabItems[0].key;
            return null;
        };

        if (actionKey === 'closeOther') {
            dispatch(removeOtherTabItem({ key: tabKey, index: tabIndex }));
            // 关闭后选中当前 tab
            if (tabKey && tabKey !== activeKey) {
                switchTab(tabKey);
            }
            return;
        }

        if (actionKey === 'closeAll') {
            dispatch(removeAllTabItem());
            const selectKey = afterCloseAllSelectKey();
            if (selectKey) switchTab(selectKey);
            return;
        }

        if (actionKey === 'closeLeft') {
            dispatch(removeLeftTabItem({ key: tabKey, index: tabIndex }));
            if (tabKey && tabKey !== activeKey) {
                switchTab(tabKey);
            }
            return;
        }

        if (actionKey === 'closeRight') {
            dispatch(removeRightTabItem({ key: tabKey, index: tabIndex }));
            if (tabKey && tabKey !== activeKey) {
                switchTab(tabKey);
            }
        }
    }, [dispatch, tabItems, activeKey, switchTab])

    const renderTabBar = (props, DefaultTabBar) => {
        const { panes } = props
        const label = draggedTab?.label ?? draggedTab?.tab
        return (
            <DndContext
                sensors={sensors}
                collisionDetection={closestCenter}
                onDragStart={handleDragStart}
                onDragEnd={handleDragEnd}
                onDragCancel={handleDragCancel}
            >
                <SortableContext
                    items={panes.map((p) => p.key)}
                    strategy={horizontalListSortingStrategy}
                >
                    <DefaultTabBar {...props}>
                        {(node) => {
                            const key = node.key
                            const tabIndex = panes.findIndex(p => p.key === key)
                            return (
                                <SortableItem id={key}>
                                    {({ ref, style, attributes, listeners, isDragging }) => (
                                        <div
                                            ref={ref}
                                            style={{
                                                ...style,
                                                opacity: isDragging ? 0 : 1, // 原 Tab 隐藏
                                            }}
                                            {...attributes}
                                            {...listeners}
                                        >
                                            <Dropdown
                                                trigger={['contextMenu']}
                                                menu={{
                                                    items: [
                                                        { key: 'closeOther', label: t('关闭其它') },
                                                        { key: 'closeAll', label: t('关闭所有') },
                                                        ...(tabIndex > 0
                                                            ? [{ key: 'closeLeft', label: t('关闭左侧') }]
                                                            : []),
                                                        ...(tabIndex < panes.length - 1
                                                            ? [{ key: 'closeRight', label: t('关闭右侧') }]
                                                            : []),
                                                    ],
                                                    onClick: ({ key: actionKey }) => {
                                                        handleMenuAction(actionKey, key, tabIndex)
                                                    },
                                                }}
                                            >
                                                {node}
                                            </Dropdown>
                                        </div>
                                    )}
                                </SortableItem>
                            );
                        }}
                    </DefaultTabBar>

                </SortableContext>
                <DragOverlay style={{ pointerEvents: 'none' }}>
                    {draggedTab ? (
                        <div
                            className="ant-tabs-tab"
                            style={{
                                background: token.colorBgContainer,
                                boxShadow: token.boxShadow,
                                borderRadius: token.borderRadius,
                                padding: '5px 12px',
                                cursor: 'grabbing',
                            }}
                        >
                            {t(label)}
                        </div>
                    ) : null}
                </DragOverlay>
            </DndContext>
        )
    }

    return (
        <div style={style} className='layout-panel-tabs'>
            <Tabs
                hideAdd
                onChange={onChange}
                activeKey={activeKey}
                type="editable-card"
                onEdit={handleEdit}
                items={items}
                tabBarStyle={{
                    borderBottom: 'none',
                }}
                renderTabBar={renderTabBar}
            />
        </div>
    )
}

export default TopMenuTab