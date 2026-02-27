import './index.css'
import { useMemo } from 'react';
import { Breadcrumb } from 'antd';
import { Link, useLocation } from 'react-router-dom';
import { findRouteByPath } from '../../router/router';
import { useTranslation } from 'react-i18next';
import { OperationMode } from '../../enums/common';
import useFullParams from '../../hooks/useFullParams';

const TopBreadcrumbTab = () => {

    const location = useLocation()

    const { t } = useTranslation()

    const { operationMode } = useFullParams()

    const breadcrumbItems = useMemo(() => {
        const pathnames = location.pathname.split('/').filter(item => item !== '')
        let path = ''
        return pathnames.map((value, index) => {
            path += `/${value}`
            const route = findRouteByPath(path)
            if (index === pathnames.length - 1 && location.search) {
                path += location.search
            }
            let breadcrumbName
            if(OperationMode[operationMode] && (pathnames.length - 1) === index){
                breadcrumbName = OperationMode[operationMode].description + t(route?.breadcrumbName)
            } else {
                breadcrumbName = t(route?.breadcrumbName)
            }
            return {
                key: path,
                title: route?.element ? <Link to={path} state={location.state}>{breadcrumbName}</Link> : t(route?.breadcrumbName),
            }
        })
    }, [location, t])

    return <Breadcrumb items={breadcrumbItems} />
}

export default TopBreadcrumbTab
