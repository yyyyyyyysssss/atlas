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
import { fetchAuthInfo, fetchUserInfo } from '../services/UserProfileService';
import { useDispatch } from 'react-redux';

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
            setAccessToken(tokenInfo.access.token)
        } else {
            setAccessToken(Cookies.get("accessToken"))
        }
        const [userInfo, authInfo] = await Promise.all([
            fetchUserInfo(),
            fetchAuthInfo()
        ])
        dispatch(setUserInfo({ userInfo }))
        dispatch(setAuthInfo({ authInfo }))
        dispatch(loadMenuItems({ menuItems: authInfo.menus }))
        setIsLoginIn(true)
    }

    const signout = async () => {
        // 清理持久化存储
        clearToken()
        // 清理内存状态 (Redux)
        reduxStore.dispatch(resetLayout())
        reduxStore.dispatch(resetUser())
        reduxStore.dispatch(resetAuth())
        // 最后切换 Context 状态，触发 UI 跳转
        setIsLoginIn(false)
    }

    // if (isLoginIn === null) {
    //     return <Flex justify='center' align='center' style={{ width: '100vw', height: '100vh' }}><Loading fullscreen /></Flex>
    // }

    return (
        <AuthContext.Provider value={{ isLoginIn, accessToken, signin, signout, checkAuth }}>
            {children}
        </AuthContext.Provider>
    )
}


export const useAuth = () => useContext(AuthContext)