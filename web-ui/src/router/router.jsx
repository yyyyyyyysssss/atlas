import React, { lazy } from "react";
import { Navigate, createBrowserRouter, Outlet } from 'react-router-dom';
import { matchPath } from "react-router"
import { Settings, UserCog, Menu, ShieldUser, ShieldCheck, Building2, NotepadText, Gauge, LayoutDashboard, AppWindow, Bell, Megaphone, Mail, UserPen, Code, Octagon, Egg } from "lucide-react";
import { LoginRoute } from "./LoginRoute";
import { ProtectedRoute } from "./ProtectedRoute";
import NotFound from "../pages/NotFound";
import Forbidden from "../pages/Forbidden";
import ServerError from "../pages/ServerError";
import Success from "../pages/Success";

const AppLayout = lazy(() => import('../layouts'))
const Workbench = lazy(() => import('../pages/workbench'))
const ProjectWorkbench = lazy(() => import('../pages/project-workbench'))
const Overview = lazy(() => import('../pages/dashboard/overview'))
const Login = lazy(() => import('../pages/login'))
const LoginMfa = lazy(() => import('../pages/login/mfa'))
const OrgManage = lazy(() => import('../pages/system-manage/org-manage'))
const UserManage = lazy(() => import('../pages/system-manage/user-manage'))
const UserDetails = lazy(() => import('../pages/system-manage/user-manage/details'))
const RoleManage = lazy(() => import('../pages/system-manage/role-manage'))
const RoleDetails = lazy(() => import('../pages/system-manage/role-manage/details'))
const MenuManage = lazy(() => import('../pages/system-manage/menu-manage'))
const PositionManage = lazy(() => import('../pages/system-manage/pos-manage'))
const DictManage = lazy(() => import('../pages/system-manage/dict-manage'))
const DictItemManage = lazy(() => import('../pages/system-manage/dict-manage/dict-item'))

const NotificationAnnouncement = lazy(() => import('../pages/notification-center/announcement'))
const AnnouncementDetails = lazy(() => import('../pages/notification-center/announcement/details'))
const NotificationMessage = lazy(() => import('../pages/notification-center/message'))

const AccountSettings = lazy(() => import('../pages/account-settings'))

const Oauth2Consent = lazy(() => import('../pages/login/oauth2/consent'))
const Oauth2Activate = lazy(() => import('../pages/login/oauth2/activate'))
const Oauth2Activated = lazy(() => import('../pages/login/oauth2/activated'))
const Oauth2QrScan = lazy(() => import('../pages/login/oauth2/qr-scan'))
const Oauth2Callback = lazy(() => import('../pages/login/oauth2/callback'))

const Saml2Callback = lazy(() => import('../pages/login/saml2/callback'))


const DeveloperSettings = lazy(() => import('../pages/developer-settings'))
const OAuth2ClientApplication = lazy(() => import('../pages/developer-settings/oauth2-client-application'))
const OAuth2ClientApplicationEdit = lazy(() => import('../pages/developer-settings/oauth2-client-application/oauth2-client-application-edit'))


const ProjectOverview = lazy(() => import('../pages/project/overview'))
const ProjectApplication = lazy(() => import('../pages/project/application'))

export const routes = [
    {
        path: 'login',
        element: <LoginRoute><Login /></LoginRoute>,
        protected: false,
    },
    {
        path: 'login/mfa',
        element: <LoginRoute><LoginMfa /></LoginRoute>,
        protected: false,
    },
    { path: '/404', element: <NotFound />, protected: false },
    { path: '/403', element: <Forbidden />, protected: false },
    { path: '/500', element: <ServerError />, protected: false },
    {
        path: 'oauth2',
        element: (
            <div style={{ height: '100vh', width: '100vm' }}>
                <Outlet />
            </div>
        ),
        protected: true,
        children: [
            {
                path: 'consent',
                element: <Oauth2Consent />,
            },
            {
                path: 'activate',
                element: <Oauth2Activate />,
            },
            {
                path: 'qr/scan',
                element: <Oauth2QrScan />,
            }
        ]
    },
    {
        path: 'oauth2/activated',
        element: <Oauth2Activated />,
        protected: false,
    },
    {
        path: 'oauth2/callback/:clientName',
        element: <Oauth2Callback />,
        protected: false,
    },
    {
        path: 'saml2/callback/:providerName?',
        element: <Saml2Callback />,
        protected: false,
    },
    {
        path: '',
        element: <AppLayout />,
        protected: true,
        children: [
            {
                path: '',
                element: <Navigate to="/workbench" />,
            },
            {
                path: 'workbench',
                breadcrumbName: '工作台',
                defaultIcon: <AppWindow size={18} />,
                element: <ProjectWorkbench />,
                protected: true,
                requiredPermissions: ['workbench']
            },
            {
                path: 'account/settings',
                breadcrumbName: '个人中心',
                defaultIcon: <UserPen size={18} />,
                element: <AccountSettings />,
            },
            {
                path: 'dashboard',
                breadcrumbName: '仪表盘',
                defaultIcon: <Gauge size={18} />,
                children: [
                    {
                        path: 'overview',
                        element: <Overview />,
                        breadcrumbName: '总览',
                        defaultIcon: <LayoutDashboard size={18} />,
                        protected: true,
                        requiredPermissions: ['dashboard:overview']
                    }
                ]
            },
            {
                path: 'system',
                breadcrumbName: '系统管理',
                defaultIcon: <Settings size={18} />,
                children: [
                    {
                        path: 'org',
                        element: <OrgManage />,
                        breadcrumbName: '组织架构',
                        defaultIcon: <Building2 size={18} />,
                        protected: true,
                        requiredPermissions: ['system:org']
                    },
                    {
                        path: 'user',
                        element: <UserManage />,
                        breadcrumbName: '用户管理',
                        defaultIcon: <UserCog size={18} />,
                        protected: true,
                        requiredPermissions: ['system:user']
                    },
                    {
                        path: 'user/details',
                        element: <UserDetails />,
                        breadcrumbName: '用户',
                        hideOperationMode: false
                    },
                    {
                        path: 'role',
                        element: <RoleManage />,
                        breadcrumbName: '角色管理',
                        defaultIcon: <ShieldUser size={18} />,
                        protected: true,
                        requiredPermissions: ['system:role']
                    },
                    {
                        path: 'role/details',
                        element: <RoleDetails />,
                        breadcrumbName: '角色',
                        hideOperationMode: false
                    },
                    {
                        path: 'position',
                        element: <PositionManage />,
                        breadcrumbName: '岗位管理',
                        defaultIcon: <ShieldCheck size={18} />,
                        protected: true,
                        requiredPermissions: ['system:position']
                    },
                    {
                        path: 'menu',
                        element: <MenuManage />,
                        breadcrumbName: '菜单管理',
                        defaultIcon: <Menu size={18} />,
                        protected: true,
                        requiredPermissions: ['system:menu']
                    },
                    {
                        path: 'dict',
                        element: <DictManage />,
                        breadcrumbName: '字典管理',
                        defaultIcon: <NotepadText size={18} />,
                        protected: true,
                        requiredPermissions: ['system:dict']
                    },
                    {
                        path: 'dict/:dictId',
                        element: <DictItemManage />,
                        breadcrumbName: '字典项',
                    },
                ]
            },
            {
                path: 'notification',
                breadcrumbName: '通知中心',
                defaultIcon: <Bell size={18} />,
                children: [
                    {
                        path: 'announcement',
                        element: <NotificationAnnouncement />,
                        breadcrumbName: '公告管理',
                        defaultIcon: <Megaphone size={18} />,
                        protected: true,
                        requiredPermissions: ['notification:announcement']
                    },
                    {
                        path: 'announcement/details',
                        element: <AnnouncementDetails />,
                        breadcrumbName: '公告',
                        hideOperationMode: false
                    },
                    {
                        path: 'message',
                        element: <NotificationMessage />,
                        breadcrumbName: '消息通知',
                        defaultIcon: <Mail size={18} />,
                        protected: true,
                        requiredPermissions: ['notification:message']
                    },
                ]
            },
            {
                path: 'developer/settings',
                breadcrumbName: '开发者设置',
                defaultIcon: <Code size={18} />,
                element: <DeveloperSettings />,
                protected: true,
                children: [
                    {
                        path: '',
                        breadcrumbName: 'OAuth2 应用',
                        element: <OAuth2ClientApplication />,
                        protected: true,
                        requiredPermissions: ['developer:settings:oauth2']
                    },
                    {
                        path: 'oauth2',
                        breadcrumbName: 'OAuth2 应用',
                        element: <OAuth2ClientApplication />,
                        protected: true,
                        requiredPermissions: ['developer:settings:oauth2']
                    },
                    {
                        path: 'oauth2/application/create',
                        breadcrumbName: '创建',
                        element: <OAuth2ClientApplicationEdit />,
                        protected: true,
                        requiredPermissions: ['developer:settings:oauth2']
                    },
                    {
                        path: 'oauth2/application/:id',
                        breadcrumbName: '编辑',
                        element: <OAuth2ClientApplicationEdit />,
                        protected: true,
                        requiredPermissions: ['developer:settings:oauth2']
                    },
                ]
            },
            {
                path: 'project',
                breadcrumbName: '项目',
                defaultIcon: <Octagon size={18} />,
                children: [
                    {
                        path: 'overview',
                        element: <ProjectOverview />,
                        breadcrumbName: '概览',
                        defaultIcon: <LayoutDashboard size={18} />,
                        protected: true,
                        requiredPermissions: ['project:overview']
                    },
                    {
                        path: 'application',
                        element: <ProjectApplication />,
                        breadcrumbName: '应用',
                        defaultIcon: <Egg size={18} />,
                        protected: true,
                        requiredPermissions: ['project:application']
                    }
                ]
            },
            {
                path: 'status', // 统一前缀：/status
                children: [
                    { path: 'success', element: <Success /> },
                    { path: '403', element: <Forbidden /> },
                    { path: '500', element: <ServerError /> },
                ]
            },
            {
                path: '*',
                element: <NotFound />
            },
        ]
    },
    {
        path: '*',
        element: <NotFound />,
        protected: false,
    }
]

const findRoute = (route, fullPath, targetPath) => {
    if (route.path === '*') {
        return null;
    }
    if (route.path !== '') {
        fullPath = fullPath + (route.path.startsWith('/') ? route.path : '/' + route.path)
    }
    const result = matchPath({ path: fullPath }, targetPath)
    if (result) {
        return {
            ...route,
            fullPath: fullPath
        }
    }
    if (route.children && targetPath.includes(route.path)) {
        for (const childrenRoute of route.children) {
            const result = findRoute(childrenRoute, fullPath, targetPath)
            if (result) {
                return result
            }
        }
    }
    return null
}

const routeCache = new Map()

export const findRouteByPath = (targetPath) => {
    if (routeCache.has(targetPath)) {
        return routeCache.get(targetPath)
    }
    let result = null
    for (const route of routes) {
        result = findRoute(route, '', targetPath)
        if (result) break
    }
    routeCache.set(targetPath, result)
    return result
}

const findRouteHierarchy = (paths, routes) => {
    if (paths.length === 0) {
        return null
    }
    const currentPath = paths[0]
    for (const route of routes) {
        if (route.path === currentPath) {
            if (paths.length === 1) {
                return route
            }
            if (route.children) {
                return findRouteHierarchy(paths.slice(1), route.children)
            }
        }
    }
    return null
}

export const findRouteByHierarchy = (paths) => {
    return findRouteHierarchy(paths, routes)
}

const wrapProtectedRoute = (route) => {
    const wrappedRoute = {
        ...route,
        element: route.protected === true ? (
            <ProtectedRoute requiredPermissions={route.requiredPermissions}>
                {route.element}
            </ProtectedRoute>
        ) : route.element,
    }
    if (route.children && route.children.length > 0) {
        wrappedRoute.children = route.children.map(wrapProtectedRoute)
    }

    return wrappedRoute
}

const finalRoutes = routes.map(wrapProtectedRoute)

const router = createBrowserRouter(finalRoutes)

export default router;