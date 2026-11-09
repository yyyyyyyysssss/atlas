import { Flex } from 'antd';
import { createContext, startTransition, useContext, useEffect, useState } from 'react';
import { setGlobalSignout } from './auth';
import reduxStore from '../redux/store';
import { loadMenuItems, reset as resetLayout } from '../redux/slices/layoutSlice';
import { reset as resetUser, setUserInfo } from '../redux/slices/userSlice';
import { reset as resetAuth, setAuthInfo } from '../redux/slices/authSlice';
import Loading from '../components/loading';
import Cookies from 'js-cookie'
import { fetchUserPermissions, fetchUserInfo, checkCurrentUser } from '../services/UserProfileService';
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
            authEventBus.emitAuthUnload()
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
        if (Cookies.get('authenticated') !== 'true') {
            setIsLoginIn(false)
            return false
        }
        await checkCurrentUser()
        setIsLoginIn(true)
        await signin()
        return true;
    }

    const signin = async () => {
        setIsLoginIn(true)
        const userInfo = await fetchUserInfo()
        dispatch(setUserInfo({ userInfo }))
    }

    const signout = async () => {
        authEventBus.emitSignout()
        // 清理持久化数据
        clearAuthStorage();
        // 清理 Redux 状态
        clearAuthState();
        // 最后切换 Context 状态，触发 UI 跳转
        setIsLoginIn(false)
    }

    // 清理前端认证及用户状态
    const clearAuthState = () => {
        reduxStore.dispatch(resetLayout());
        reduxStore.dispatch(resetUser());
        reduxStore.dispatch(resetAuth());
    }

    // 理认证相关持久化数据
    const clearAuthStorage = () => {
        localStorage.clear()
    }

    return (
        <AuthContext.Provider value={{ isLoginIn, accessToken, signin, signout, checkAuth }}>
            {children}
        </AuthContext.Provider>
    )
}


export const useAuth = () => useContext(AuthContext)