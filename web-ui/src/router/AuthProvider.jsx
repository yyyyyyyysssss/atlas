import { Flex } from 'antd';
import { createContext, startTransition, useContext, useEffect, useState } from 'react';
import { checkTokenValid, clearToken, saveToken } from '../services/LoginService';
import { setGlobalSignout } from './auth';
import reduxStore from '../redux/store';
import { loadMenuItems, reset as resetLayout } from '../redux/slices/layoutSlice';
import { reset as resetUser, setUserInfo } from '../redux/slices/userSlice';
import { reset as resetAuth, setAuthInfo } from '../redux/slices/authSlice';
import Loading from '../components/loading';
import Cookies from 'js-cookie'
import { fetchUserPermissions, fetchUserInfo } from '../services/UserProfileService';
import { useDispatch, useSelector } from 'react-redux';
import { authEventBus } from '../events/AuthEventBus';

const AuthContext = createContext({
    isLoginIn: null,
    setIsLoginIn: () => { },
    signin: async (tokenInfo) => { },
    signout: async () => { },
    checkAuth: async () => { },
})

export const AuthProvider = ({ children }) => {

    const [isLoginIn, setIsLoginIn] = useState(null)

    const [accessToken, setAccessToken] = useState(null)

    const dispatch = useDispatch()

    useEffect(() => {
        const handleBeforeUnload = () => {
            if (isLoginIn !== true) return
            const navEntries = performance.getEntriesByType('navigation')
            const isReload = navEntries.length > 0 && navEntries[0].type === 'reload'
            // 如果是刷新操作，直接跳过，不触发 signout
            if (isReload) {
                return
            }
            authEventBus.emitUnload()
        }

        window.addEventListener('beforeunload', handleBeforeUnload)
        return () => {
            window.removeEventListener('beforeunload', handleBeforeUnload)
        }
    }, [isLoginIn])

    useEffect(() => {
        setGlobalSignout(signout)
    }, [])

    const checkAuth = async () => {
        if (isLoginIn !== null) {
            return isLoginIn
        }
        try {
            const isValid = await checkTokenValid()
            if (isValid) {
                await signin()
                return true
            }
        } catch (error) {
            console.error("Token 校验失败:", error)
        }
        setIsLoginIn(false)
        return false
    }

    const signin = async (tokenInfo) => {
        let token
        if (tokenInfo) {
            saveToken(tokenInfo)
            token = tokenInfo.access.value
        } else {
            token = Cookies.get("accessToken")
        }
        setAccessToken(token)
        const userInfo = await fetchUserInfo()
        dispatch(setUserInfo({ userInfo }))
        setIsLoginIn(true)
    }

    const signout = async () => {
        authEventBus.emitSignout()
        // 清理持久化存储
        clearToken()
        // 清理内存状态 (Redux)
        reduxStore.dispatch(resetLayout())
        reduxStore.dispatch(resetUser())
        reduxStore.dispatch(resetAuth())
        // 最后切换 Context 状态，触发 UI 跳转
        setIsLoginIn(false)
    }

    return (
        <AuthContext.Provider value={{ isLoginIn, accessToken, signin, signout, checkAuth }}>
            {children}
        </AuthContext.Provider>
    )
}


export const useAuth = () => useContext(AuthContext)