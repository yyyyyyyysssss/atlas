import React, { lazy } from "react";
import { Navigate, createBrowserRouter, Outlet } from 'react-router-dom';
import { matchPath, matchRoutes } from "react-router"
import { Settings, UserCog, Menu, ShieldUser, ShieldCheck, Building2, NotepadText, Gauge, LayoutDashboard, AppWindow, Bell, Megaphone, Mail, UserPen, Code, Octagon, Egg, LayoutGrid, Boxes } from "lucide-react";
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

const OAuth2Consent = lazy(() => import('../pages/login/oauth2/consent'))
const OAuth2Activate = lazy(() => import('../pages/login/oauth2/activate'))
const OAuth2Activated = lazy(() => import('../pages/login/oauth2/activated'))
const OAuth2QrScan = lazy(() => import('../pages/login/oauth2/qr-scan'))
const OAuth2Callback = lazy(() => import('../pages/login/oauth2/callback'))

const Saml2Callback = lazy(() => import('../pages/login/saml2/callback'))


const OAuth2ClientApplication = lazy(() => import('../pages/project/application/oauth2'))
const OAuth2ClientApplicationList = lazy(() => import('../pages/project/application/oauth2/list'))
const OAuth2ClientApplicationEdit = lazy(() => import('../pages/project/application/oauth2/edit'))


const Project = lazy(() => import('../pages/project'))
const ProjectEdit = lazy(() => import('../pages/project/edit'))
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
                element: <OAuth2Consent />,
            },
            {
                path: 'activate',
                element: <OAuth2Activate />,
            },
            {
                path: 'qr/scan',
                element: <OAuth2QrScan />,
            }
        ]
    },
    {
        path: 'oauth2/activated',
        element: <OAuth2Activated />,
        protected: false,
    },
    {
        path: 'oauth2/callback/:clientName',
        element: <OAuth2Callback />,
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
        meta: {
            domain: 'global'
        },
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
                path: 'project-workspace',
                breadcrumbName: '项目空间',
                defaultIcon: <Boxes size={18} />,
                element: <Outlet />,
                protected: true,
                requiredPermissions: ['project:workspace'],
                children: [
                    {
                        index: true,
                        breadcrumbName: '项目空间',
                        element: <Project />
                    },
                    {
                        path: 'create',
                        element: <ProjectEdit />,
                        breadcrumbName: '创建',
                    },
                    {
                        path: ':id',
                        element: <ProjectEdit />,
                        breadcrumbName: '编辑',
                    }
                ]
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
                        element: <Outlet />,
                        breadcrumbName: '用户管理',
                        defaultIcon: <UserCog size={18} />,
                        protected: true,
                        requiredPermissions: ['system:user'],
                        children: [
                            {
                                index: true,
                                breadcrumbName: '用户管理',
                                element: <UserManage />
                            },
                            {
                                path: 'details',
                                element: <UserDetails />,
                                breadcrumbName: '编辑',
                            },
                        ]
                    },
                    {
                        path: 'role',
                        element: <Outlet />,
                        breadcrumbName: '角色管理',
                        defaultIcon: <ShieldUser size={18} />,
                        protected: true,
                        requiredPermissions: ['system:role'],
                        children: [
                            {
                                index: true,
                                breadcrumbName: '角色管理',
                                element: <RoleManage />
                            },
                            {
                                path: 'details',
                                element: <RoleDetails />,
                                breadcrumbName: '编辑',
                            },
                        ]
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
                path: 'project/:domainId',
                breadcrumbName: '项目',
                defaultIcon: <Octagon size={18} />,
                meta: {
                    domain: 'project'
                },
                children: [
                    {
                        path: '',
                        element: <Navigate to="overview" />
                    },
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
                        requiredPermissions: ['project:application'],
                        children: [
                            {
                                index: true,
                                breadcrumbName: '应用',
                                element: <Navigate to="oauth2" />
                            },
                            {
                                path: 'oauth2',
                                breadcrumbName: 'OAuth2 应用',
                                element: <OAuth2ClientApplication />,
                                protected: true,
                                children: [
                                    {
                                        index: true,
                                        breadcrumbName: 'OAuth2 应用',
                                        element: <OAuth2ClientApplicationList />
                                    },
                                    {
                                        path: 'create',
                                        element: <OAuth2ClientApplicationEdit />,
                                        breadcrumbName: '创建',
                                    },
                                    {
                                        path: ':id',
                                        element: <OAuth2ClientApplicationEdit />,
                                        breadcrumbName: '编辑',
                                    }
                                ]
                            }
                        ]
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

export const findRouteByPath = (targetPath) => {
    const matches = getMatches(targetPath)

    if (!matches || matches.length === 0) {
        return null
    }
    // 最后一个匹配的就是当前页面route
    const match = matches[matches.length - 1]

    const result = {
        ...match.route,
        // 完整匹配路径
        fullPath: match.pathname,
        // 动态参数
        params: match.params
    }
    return result
}

export const findBreadcrumbRoutes = (pathname) => {

    const matches = getMatches(pathname)

    if (!matches) {
        return []
    }

    return matches
        .filter(({ route }) => !route.index)
        .map(({ route, pathname }) => ({
            ...route,
            fullPath: pathname
        }))
}

export const findRouteDomain = (pathname) => {
    const matches = getMatches(pathname)
    if (!matches) {
        return null
    }
    const route = [...matches]
        .reverse()
        .find(item => item.route.meta?.domain)

    if (!route) {
        return {
            domain: 'global',
            domainId: null
        }
    }

    return {
        domain: route.route.meta.domain,
        domainId: route.params.domainId ?? null
    }
}

const matchesCache = new Map()

const getMatches = (pathname) => {
    if (matchesCache.has(pathname)) {
        return matchesCache.get(pathname)
    }

    const matches = matchRoutes(routes, pathname)
    matchesCache.set(pathname, matches || null)
    return matches
}


const routeMap = new Map()

export const lookupRouteByPath = (targetPath) => {
    if (routeMap.has(targetPath)) {
        return routeMap.get(targetPath)
    }
    const findRoute = (route, fullPath, targetPath) => {
        if (route.path === '*') {
            return null;
        }
        if (route.path !== '') {
            fullPath = fullPath + (route.path?.startsWith('/') ? route.path : '/' + route.path)
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
    let result = null
    for (const route of routes) {
        result = findRoute(route, '', targetPath)
        if (result) break
    }
    routeMap.set(targetPath, result)
    return result
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