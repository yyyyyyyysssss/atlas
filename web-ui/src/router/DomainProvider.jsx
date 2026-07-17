import { createContext, useContext } from "react";
import { useDispatch, useSelector } from "react-redux";
import { loadMenuItems, setDomain, setDomainLoading } from "../redux/slices/layoutSlice";
import { fetchUserPermissions } from "../services/UserProfileService";
import { setAuthInfo } from "../redux/slices/authSlice";


const DomainContext = createContext({
    loadDomain: async (domain, domainId) => { },
    domain: null,
    domainId: null,
    domainLoading: false
})

export const DomainProvider = ({ children }) => {

    const domain = useSelector(state => state.layout.domain) || null

    const domainId = useSelector(state => state.layout.domainId) || null

    const domainLoading = useSelector(state => state.layout.domainLoading) || false

    const dispatch = useDispatch()

    const loadDomain = async (domain, domainId) => {
        dispatch(setDomainLoading(true))
        try {
            const authInfo = await fetchUserPermissions(domain)
            dispatch(setAuthInfo({ authInfo }))
            dispatch(loadMenuItems({
                domain: domain,
                menuItems: authInfo.menus
            }))
            dispatch(setDomain({ domain: domain, domainId: domainId }))
            return authInfo
        } catch (error) {
            console.error("加载域权限失败:", error)
            dispatch(setDomainLoading(false))
            throw error
        } finally {
            dispatch(setDomainLoading(false))
        }
    }

    return (
        <DomainContext.Provider value={{ domain, domainId, domainLoading, loadDomain }}>
            {children}
        </DomainContext.Provider>
    )

}

export const useDomain = () => useContext(DomainContext)