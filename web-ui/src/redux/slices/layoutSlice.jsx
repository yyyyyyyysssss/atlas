import { createSlice } from '@reduxjs/toolkit'
import { generatePath } from 'react-router-dom'
import { findRouteByPath } from '../../router/router'

export const initialState = {
    domain: null,
    domainId: null,
    domainLoading: false,
    activeKey: '',
    menuCollapsed: false,
    openKeys: [],
    tabItems: {
        global: [],
        project: []
    },
    menus: {
        global: [],
        project: []
    },
    flattenMenuItems: [],
}

const findMenuByKey = (targetKey, menus) => {
    if (menus && menus.length > 0) {
        return menus.find(item => item.id === targetKey)
    }
    return null
}

const findBestMatchMenu = (targetPath, menus, domainId) => {
    const tp = targetPath.split('?')[0].split('#')[0]
    let bestMatchMenu = null;
    for (const menuItem of menus) {
        let routePath = menuItem.routePath
        if (domainId) {
            routePath = generatePath(routePath, { domainId: domainId })
        }
        if (tp === routePath || (tp.startsWith(routePath) && tp.charAt(routePath.length) === '/')) {
            // 如果更长就替换
            if (!bestMatchMenu || routePath.length > bestMatchMenu.routePath.length) {
                bestMatchMenu = menuItem;
            }
        }
    }
    return bestMatchMenu;
}

const flattenMenus = (menuItems) => {
    const result = []
    function recurse(nodes, parentId = null, parentPath = []) {
        for (const node of nodes) {
            const { children, ...rest } = node;

            const currentPath = [...parentPath, parentId].filter(Boolean); // 去除 null
            result.push({ ...rest, parentId, parentPath: currentPath });

            if (children && children.length > 0) {
                recurse(children, node.id, currentPath);
            }
        }
    }
    //对树形结构进行扁平化
    recurse(menuItems)
    return result
}

export const layoutSlice = createSlice({
    name: 'layout',
    initialState: initialState,
    reducers: {
        reset: () => initialState,
        setOpenKeys: (state, action) => {
            const { payload } = action
            const { keys } = payload
            state.openKeys = keys
        },
        setActiveKey: (state, action) => {
            const { payload } = action
            const { key, path } = payload
            let menuItem
            if (key) {
                menuItem = findMenuByKey(key, state.flattenMenuItems)
            } else {
                menuItem = findBestMatchMenu(path, state.flattenMenuItems, state.domainId)
            }
            if (menuItem) {
                state.activeKey = menuItem.id
                state.openKeys = state.menuCollapsed ? [] : menuItem.parentPath
            }
        },
        menuCollapsed: (state, action) => {
            state.menuCollapsed = !state.menuCollapsed
            const menuItem = findMenuByKey(state.activeKey, state.flattenMenuItems)
            if (menuItem) {
                state.openKeys = state.menuCollapsed ? [] : menuItem.parentPath
            }
        },
        setTabIem: (state, action) => {
            const { payload } = action
            const { tabItem, domain } = payload
            state.tabItems[domain] = tabItem
        },
        addTabIem: (state, action) => {
            const { payload } = action
            const { tabItem, domain } = payload
            if (!tabItem.label) {
                state.activeKey = null
                return
            }
            const path = tabItem.path
            const menuItem = findBestMatchMenu(path, state.flattenMenuItems, state.domainId)
            if (!menuItem) {
                return
            }
            tabItem.key = menuItem.id || path
            tabItem.routePath = menuItem.routePath
            tabItem.label = menuItem.name
            // 先检查是否存在
            const checkTabItem = state.tabItems[domain]?.find(f => f.routePath === menuItem.routePath)
            if (checkTabItem) {
                state.activeKey = checkTabItem.key
                if (menuItem) {
                    state.openKeys = state.menuCollapsed ? [] : menuItem.parentPath
                }
                return
            }
            // 不存在且在菜单中则新增
            const item = state.tabItems[domain]?.find(item => item.key === menuItem.id)
            if (item) {
                state.activeKey = menuItem.id
                return
            }
            if (Array.isArray(state.tabItems)) {
                state.tabItems = {
                    global: [],
                    project: []
                }
            }
            state.tabItems[domain] = [...(state.tabItems[domain] || []), tabItem]
            state.activeKey = menuItem.id || path
            state.openKeys = state.menuCollapsed ? [] : menuItem.parentPath
        },
        removeTabItem: (state, action) => {
            const { payload } = action
            const { targetKey, selectKey, domain } = payload
            const newPanes = state.tabItems[domain]?.filter(pane => pane.key !== targetKey)
            if (selectKey) {
                state.activeKey = selectKey
                const menuItem = findMenuByKey(selectKey, state.flattenMenuItems)
                state.openKeys = state.menuCollapsed ? [] : menuItem?.parentPath
            }
            state.tabItems[domain] = newPanes
        },
        removeAllTabItem: (state, action) => {
            const { payload } = action
            const { domain } = payload
            const newItems = state.tabItems[domain]?.filter(item => item.closable === false)
            if (newItems.length) {
                const key = newItems[0].key
                state.activeKey = key
                const menuItem = findMenuByKey(key, state.flattenMenuItems)
                state.openKeys = state.menuCollapsed ? [] : menuItem.parentPath
            }
            state.tabItems[domain] = newItems
        },
        removeOtherTabItem: (state, action) => {
            const { payload } = action
            const { key, index, domain } = payload
            const newItems = state.tabItems[domain]?.filter((item, i) => item.closable === false || i === index)
            if (newItems.length) {
                state.activeKey = key
                const menuItem = findMenuByKey(key, state.flattenMenuItems)
                state.openKeys = state.menuCollapsed ? [] : menuItem.parentPath
            }
            state.tabItems[domain] = newItems
        },
        removeLeftTabItem: (state, action) => {
            const { payload } = action
            const { key, index, domain } = payload
            const newItems = state.tabItems[domain]?.filter((item, i) => i >= index || item.closable === false)
            if (newItems.length) {
                state.activeKey = key
                const menuItem = findMenuByKey(key, state.flattenMenuItems)
                state.openKeys = state.menuCollapsed ? [] : menuItem.parentPath
            }
            state.tabItems[domain] = newItems
        },
        removeRightTabItem: (state, action) => {
            const { payload } = action
            const { key, index, domain } = payload
            const newItems = state.tabItems[domain]?.filter((item, i) => i <= index || item.closable === false)
            if (newItems.length) {
                state.activeKey = key
                const menuItem = findMenuByKey(key, state.flattenMenuItems)
                state.openKeys = state.menuCollapsed ? [] : menuItem.parentPath
            }
            state.tabItems[domain] = newItems
        },
        loadMenuItems: (state, action) => {
            const { payload } = action
            const { domain, menuItems } = payload
            state.menus[domain] = menuItems
            if (state.domain === domain) {
                state.flattenMenuItems = flattenMenus(menuItems)
            }
        },
        setDomain: (state, action) => {
            const { payload } = action
            const { domain, domainId } = payload
            if (domain !== state.domain || domainId !== state.domainId) {
                state.domain = domain
                state.domainId = domainId
                const menuItems = state.menus[domain] || []
                state.flattenMenuItems = flattenMenus(menuItems)
                state.activeKey = ''
                state.openKeys = []
            }
        },
        setDomainLoading: (state, action) => {
            state.domainLoading = action.payload
        },
    }
})

export const { reset, setActiveKey, menuCollapsed, setOpenKeys, setTabIem, addTabIem, removeTabItem, removeAllTabItem, removeOtherTabItem, removeLeftTabItem, removeRightTabItem, loadMenuItems, setDomain, setDomainLoading } = layoutSlice.actions

export default layoutSlice.reducer