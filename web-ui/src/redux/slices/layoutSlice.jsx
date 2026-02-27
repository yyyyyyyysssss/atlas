import { createSlice } from '@reduxjs/toolkit'

export const initialState = {
    activeKey: '',
    menuCollapsed: false,
    openKeys: [],
    tabItems: [],
    menuItems: [],
    flattenMenuItems: [],
    theme: 'dark',
    colorPrimary: '#1DA57A',
    language: 'zh',
}

const findMenuByKey = (targetKey, menus) => {
    if (menus && menus.length > 0) {
        return menus.find(item => item.id === targetKey)
    }
    return null
}

const findBestMatchMenu = (targetPath, menus) => {
    const tp = targetPath.split('?')[0].split('#')[0]
    let bestMatchMenu = null;
    for (const menuItem of menus) {
        const routePath = menuItem.routePath
        if (tp === routePath || (tp.startsWith(routePath) && tp.charAt(routePath.length) === '/')) {
            // 如果更长就替换
            if (!bestMatchMenu || routePath.length > bestMatchMenu.routePath.length) {
                bestMatchMenu = menuItem;
            }
        }
    }
    return bestMatchMenu;
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
                menuItem = findBestMatchMenu(path, state.flattenMenuItems)
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
        switchTheme: (state, action) => {
            const { payload } = action
            const { theme } = payload
            state.theme = theme
        },
        switchColorPrimary: (state, action) => {
            const { payload } = action
            const { colorPrimary } = payload
            state.colorPrimary = colorPrimary
        },
        switchLanguage: (state, action) => {
            const { payload } = action
            const { language } = payload
            state.language = language
        },
        setTabIem: (state, action) => {
            const { payload } = action
            const { tabItem } = payload
            state.tabItems = tabItem
        },
        addTabIem: (state, action) => {
            const { payload } = action
            const { tabItem } = payload
            if (!tabItem.label) {
                state.activeKey = null
                return
            }
            const path = tabItem.path
            const menuItem = findBestMatchMenu(path, state.flattenMenuItems)
            // 先检查是否存在
            const checkTabItem = state.tabItems.find(f => f.path === path)
            if (checkTabItem) {
                state.activeKey = checkTabItem.key
                if (menuItem) {
                    state.openKeys = state.menuCollapsed ? [] : menuItem.parentPath
                }
                return
            }
            // 不存在且在菜单中则新增
            if (!menuItem) {
                return
            }
            const item = state.tabItems.find(item => item.key === menuItem.id)
            if (item) {
                state.activeKey = menuItem.id
                return
            }
            tabItem.key = menuItem.id
            state.tabItems.push(tabItem)
            state.activeKey = menuItem.id
            state.openKeys = state.menuCollapsed ? [] : menuItem.parentPath
        },
        removeTabItem: (state, action) => {
            const { payload } = action
            const { targetKey, selectKey } = payload
            const newPanes = state.tabItems.filter(pane => pane.key !== targetKey)
            if (selectKey) {
                state.activeKey = selectKey
                const menuItem = findMenuByKey(selectKey, state.flattenMenuItems)
                state.openKeys = state.menuCollapsed ? [] : menuItem?.parentPath
            }
            state.tabItems = newPanes
        },
        removeAllTabItem: (state, action) => {
            const newItems = state.tabItems.filter(item => item.closable === false)
            if (newItems.length) {
                const key = newItems[0].key
                state.activeKey = key
                const menuItem = findMenuByKey(key, state.flattenMenuItems)
                state.openKeys = state.menuCollapsed ? [] : menuItem.parentPath
            }
            state.tabItems = newItems
        },
        removeOtherTabItem: (state, action) => {
            const { payload } = action
            const { key, index } = payload
            const newItems = state.tabItems.filter((item, i) => item.closable === false || i === index)
            if (newItems.length) {
                state.activeKey = key
                const menuItem = findMenuByKey(key, state.flattenMenuItems)
                state.openKeys = state.menuCollapsed ? [] : menuItem.parentPath
            }
            state.tabItems = newItems
        },
        removeLeftTabItem: (state, action) => {
            const { payload } = action
            const { key, index } = payload
            const newItems = state.tabItems.filter((item, i) => i >= index || item.closable === false)
            if (newItems.length) {
                state.activeKey = key
                const menuItem = findMenuByKey(key, state.flattenMenuItems)
                state.openKeys = state.menuCollapsed ? [] : menuItem.parentPath
            }
            state.tabItems = newItems
        },
        removeRightTabItem: (state, action) => {
            const { payload } = action
            const { key, index } = payload
            const newItems = state.tabItems.filter((item, i) => i <= index || item.closable === false)
            if (newItems.length) {
                state.activeKey = key
                const menuItem = findMenuByKey(key, state.flattenMenuItems)
                state.openKeys = state.menuCollapsed ? [] : menuItem.parentPath
            }
            state.tabItems = newItems
        },
        loadMenuItems: (state, action) => {
            const { payload } = action
            const { menuItems } = payload
            state.menuItems = menuItems
            const flattenMenuItems = []
            function recurse(nodes, parentId = null, parentPath = []) {
                for (const node of nodes) {
                    const { children, ...rest } = node;

                    const currentPath = [...parentPath, parentId].filter(Boolean); // 去除 null
                    flattenMenuItems.push({ ...rest, parentId, parentPath: currentPath });

                    if (children && children.length > 0) {
                        recurse(children, node.id, currentPath);
                    }
                }
            }
            //对树形结构进行扁平化
            recurse(menuItems)
            state.flattenMenuItems = flattenMenuItems
        }
    }
})

export const { reset, setActiveKey, menuCollapsed, switchTheme, switchColorPrimary, switchLanguage, setOpenKeys, setTabIem, addTabIem, removeTabItem, removeAllTabItem, removeOtherTabItem, removeLeftTabItem, removeRightTabItem, loadMenuItems } = layoutSlice.actions

export default layoutSlice.reducer