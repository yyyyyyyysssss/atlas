import { Flex } from 'antd';
import { createContext, useContext, useEffect, useState } from 'react';
import { checkTokenValid, clearToken, saveToken } from '../services/LoginService';
import { setGlobalSignout } from './auth';
import reduxStore from '../redux/store';
import { loadMenuItems, reset as resetLayout } from '../redux/slices/layoutSlice';
import { reset as resetUser, setUserInfo } from '../redux/slices/userSlice';
import { reset as resetAuth, setAuthInfo } from '../redux/slices/authSlice';
import Loading from '../components/loading';
import Cookies from 'js-cookie'
import { fetchUserPermissions, fetchUserInfo } from '../services/UserProfileService';
import { useDispatch } from 'react-redux';
import { useDisconnect } from 'wagmi';

const AuthContext = createContext({
    isLoginIn: null,
    setIsLoginIn: () => { },
    signin: async (tokenInfo) => { },
    signout: async () => { },
    checkAuth: async () => { }
})

export const AuthProvider = ({ children }) => {

    const [isLoginIn, setIsLoginIn] = useState(null)

    const [accessToken, setAccessToken] = useState(null)

    const dispatch = useDispatch()

    const { disconnect } = useDisconnect();

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
        if (tokenInfo) {
            saveToken(tokenInfo)
            setAccessToken(tokenInfo.access.value)
        } else {
            setAccessToken(Cookies.get("accessToken"))
        }
        const [userInfo, authInfo] = await Promise.all([
            fetchUserInfo(),
            fetchUserPermissions()
        ])
        dispatch(setUserInfo({ userInfo }))
        dispatch(setAuthInfo({ authInfo }))
        dispatch(loadMenuItems({ menuItems: authInfo.menus }))
        setIsLoginIn(true)
    }

    const signout = async () => {
        // 强行断开 Web3 钱包连接（防止业务退出后，钱包还挂着）
        try {
            disconnect()
        } catch (error) {
            console.error("Wagmi 断开钱包失败:", error);
        }

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