import { createContext, useContext, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { loadMenuItems, setDomain } from "../redux/slices/layoutSlice";
import { fetchUserPermissions } from "../services/UserProfileService";
import { setAuthInfo } from "../redux/slices/authSlice";


const DomainContext = createContext({
    loadDomain: async (domain, domainId, routeDomain) => { },
    domain: null,
    domainId: null,
    domainLoading: false,
    domainReady: false
})

export const DomainProvider = ({ children }) => {

    const domain = useSelector(state => state.layout.domain) || null

    const domainId = useSelector(state => state.layout.domainId) || null


    const [domainLoading, setDomainLoading] = useState(false)

    // 当前路由需要的domain
    const [routeDomain, setRouteDomain] = useState(null)


    const dispatch = useDispatch()

    const loadDomain = async (domain, domainId, routeDomain) => {
        setRouteDomain(routeDomain)
        setDomainLoading(true)
        try {
            const authInfo = await fetchUserPermissions(domain)
            dispatch(setDomain({ domain: domain, domainId: domainId }))
            dispatch(setAuthInfo({ authInfo }))
            dispatch(loadMenuItems({
                domain: domain,
                menuItems: authInfo.menus
            }))
            return authInfo
        } catch (error) {
            console.error("加载域权限失败:", error)
            setDomainLoading(false)
            throw error
        } finally {
            setDomainLoading(false)
        }
    }

    const domainReady = !routeDomain || (
            routeDomain.domain === domain &&
            routeDomain.domainId === domainId &&
            !domainLoading
        )

    return (
        <DomainContext.Provider value={{ domain, domainId, domainLoading, domainReady, loadDomain }}>
            {children}
        </DomainContext.Provider>
    )

}

export const useDomain = () => useContext(DomainContext)