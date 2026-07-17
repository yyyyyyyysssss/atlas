import { useLocation } from "react-router-dom"
import { useAuth } from "./AuthProvider"
import { useSelector } from "react-redux"
import { useEffect, useMemo } from "react"
import { findRouteDomain } from "./router"
import { useDomain } from "./DomainProvider"


const DomainLoader = () => {

    const location = useLocation()

    const { domain, domainId, loadDomain } = useDomain() || {}

    const routeDomain = useMemo(() => {
        return findRouteDomain(location.pathname)
    }, [location.pathname])

    useEffect(() => {
        if (!routeDomain) {
            return
        }
        const { domain: targetDomain, domainId: targetDomainId } = routeDomain
        if (targetDomain === domain && targetDomainId === domainId) {
            return
        }
        loadDomain(targetDomain, targetDomainId)
    }, [routeDomain.domain, routeDomain.domainId])

    return null
}

export default DomainLoader