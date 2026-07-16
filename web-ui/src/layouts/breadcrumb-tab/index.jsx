import './index.css'
import { useMemo } from 'react';
import { Breadcrumb } from 'antd';
import { Link, useLocation } from 'react-router-dom';
import { findBreadcrumbRoutes } from '../../router/router';
import { useTranslation } from 'react-i18next';
import { OperationMode } from '../../enums/common';
import useFullParams from '../../hooks/useFullParams';

const TopBreadcrumbTab = () => {

    const location = useLocation()

    const { t } = useTranslation()

    const { operationMode } = useFullParams()

    const breadcrumbItems = useMemo(() => {
        const pathname = location.pathname
        const routes = findBreadcrumbRoutes(pathname)

        return routes.map((route, index) => {

            const isLast = index === routes.length - 1

            let breadcrumbName = t(route.breadcrumbName)

            if (route.hideOperationMode === false && isLast && OperationMode[operationMode]) {
                breadcrumbName = OperationMode[operationMode].description + breadcrumbName
            }

            return {
                key: route.path,
                title: route?.element ? <Link to={route.fullPath} state={location.state}>{breadcrumbName}</Link> : t(route?.breadcrumbName),
            }
        })
    }, [location, t, operationMode])

    return <Breadcrumb items={breadcrumbItems} />
}

export default TopBreadcrumbTab
