import { Flex } from 'antd';
import { createContext, useContext, useEffect, useState } from 'react';
import { checkTokenValid, clearToken, saveToken } from '../services/LoginService';
import { setGlobalSignout } from './auth';
import reduxStore from '../redux/store';
import { reset as resetLayout } from '../redux/slices/layoutSlice';
import { reset as resetUser } from '../redux/slices/userSlice';
import { reset as resetAuth } from '../redux/slices/authSlice';
import Loading from '../components/loading';

const AuthContext = createContext({
    isLoginIn: null,
    setIsLoginIn: () => { },
    signin: async (tokenInfo) => { },
    signout: async () => { }
})

export const AuthProvider = ({ children }) => {

    const [isLoginIn, setIsLoginIn] = useState(null)

    useEffect(() => {
        const check = async () => {
            const isValid = await checkTokenValid()
            if (isValid) {
                signin()
            } else {
                signout()
            }
        }
        check()
        setGlobalSignout(signout)
    }, [])

    const signin = async (tokenInfo) => {
        if (tokenInfo) {
            saveToken(tokenInfo)
        }
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

    if (isLoginIn === null) {
        return <Flex justify='center' align='center' style={{width: '100vw',height: '100vh'}}><Loading fullscreen /></Flex>
    }

    return (
        <AuthContext.Provider value={{ isLoginIn, setIsLoginIn, signin, signout }}>
            {children}
        </AuthContext.Provider>
    )
}


export const useAuth = () => useContext(AuthContext)