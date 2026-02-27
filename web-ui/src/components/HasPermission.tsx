import React, { ReactNode, useMemo } from "react";
import { shallowEqual, useSelector } from 'react-redux';

interface HasPermissionProps {
    hasPermissions: string | string[] | undefined // 需要的权限编码，支持单个或多个
    requireAll?: boolean  //是否需要全部权限满足，默认 false（满足任一即可）
    children: ReactNode
    fallback?: ReactNode // 无权限时展示内容，默认null即不展示
}


export const useHasPermission = (
    hasPermissions?: string | string[],
    requireAll: boolean = false
): boolean => {

    const permissionCodes = useSelector((state: any) => state.auth.userInfo.permissionCodes || [], shallowEqual)

    const permissions = useMemo(() => {
        if (!hasPermissions) return []
        return Array.isArray(hasPermissions) ? hasPermissions : [hasPermissions]
    }, [hasPermissions])

    const isAllowed = useMemo(() => {
        if (permissions.length === 0) {
            return true
        }
        if (!permissionCodes || permissionCodes.length === 0) {
            return false
        }
        // 用户权限中包含 'all'，直接允许
        if (permissionCodes.includes('all')) {
            return true
        }
        return requireAll ? permissions.every(p => permissionCodes.includes(p)) : permissions.some(p => permissionCodes.includes(p))
    }, [permissions, permissionCodes, requireAll])

    return isAllowed
}

const HasPermission: React.FC<HasPermissionProps> = ({ hasPermissions, requireAll = false, fallback = null, children }) => {

    const isAllowed = useHasPermission(hasPermissions, requireAll)

    return <>{isAllowed ? children : fallback}</>
}

export default HasPermission