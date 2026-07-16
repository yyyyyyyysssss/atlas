import { useCallback, useEffect, useMemo, useState, useTransition, memo } from 'react';
import './index.css'
import { Avatar, Flex, Menu, Typography } from 'antd';
import { generatePath, useLocation, useNavigate } from 'react-router-dom';
import { shallowEqual, useDispatch, useSelector } from 'react-redux';
import { setActiveKey, setOpenKeys } from '../../redux/slices/layoutSlice';
import { findRouteByPath, lookupRouteByPath } from '../../router/router';
import { Square } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import { createSelector } from '@reduxjs/toolkit';


const getMenuItems = (items, t) => {
    return items.map(item => {
        const route = lookupRouteByPath(item.routePath);
        return {
            key: item.id,
            label: t(item.name),
            icon: item.icon || route?.defaultIcon || <Square size={18} />,
            children: item.children?.length > 0 ? getMenuItems(item.children, t) : undefined,
        };
    });
};


const siderSelector = createSelector(
    state => state.user.userInfo?.settings?.appearance?.theme || 'dark',
    state => state.layout.domain,
    state => state.layout.domainId,
    state => state.layout.menus,
    state => state.layout.flattenMenuItems,
    state => state.layout.menuCollapsed,
    state => state.layout.activeKey,
    state => state.layout.openKeys,
    (themeValue, domain, domainId, menus, flattenMenuItems, collapsed, activeKey, openKeys) => ({
        themeValue,
        domain,
        domainId,
        menuItems: menus[domain] || [],
        flattenMenuItems,
        collapsed,
        activeKey,
        openKeys,
    })
);

const Sider = () => {

    const { t } = useTranslation()

    const { themeValue, domain, domainId, menuItems, flattenMenuItems, collapsed, activeKey, openKeys } = useSelector(siderSelector, shallowEqual)

    const dispatch = useDispatch()

    const navigate = useNavigate()

    const location = useLocation()
    
    useEffect(() => {
        if (location.pathname && location.pathname !== '/' && flattenMenuItems && flattenMenuItems.length > 0) {
            dispatch(setActiveKey({ path: location.pathname }))
        }

    }, [flattenMenuItems, location.pathname, dispatch])

    const handleOpenChange = useCallback((keys) => {
        dispatch(setOpenKeys({ keys: keys }))
    }, [])

    const switchMenu = (e) => {
        const menuItem = flattenMenuItems.find(item => item.id === e.key)
        if (!menuItem) {
            return
        }
        const path = generatePath(menuItem.routePath, { domainId: domainId})
        navigate(path)
    }

    const items = useMemo(() => {
        return getMenuItems(menuItems, t)
    }, [menuItems, t])


    return (
        <>
            <Menu
                style={{
                    maxHeight: 'calc(100vh - 64px)',
                    borderRight: 'none',
                    overflowY: 'auto',
                    scrollbarGutter: 'stable',
                }}
                theme={themeValue}
                inlineCollapsed={collapsed}
                selectedKeys={activeKey ? [activeKey] : []}
                openKeys={openKeys}
                onOpenChange={handleOpenChange}
                onClick={switchMenu}
                mode='inline'
                items={items}
            />
        </>

    )
}


const Logo = memo(({ collapsed, goHome }) => (
    <Flex
        onClick={goHome}
        gap={10}
        justify='center'
        align='center'
        style={{
            height: '64px',
            cursor: 'pointer',
        }}
    >
        <Avatar src={'/logo128.png'} size={40} />
        {!collapsed && <Typography.Text style={{ fontSize: '20px' }}>Atlas</Typography.Text>}
    </Flex>
))

export default Sider