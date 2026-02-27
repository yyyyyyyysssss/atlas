import { apiRequestWrapper } from "./ApiRequestWrapper"
import httpWrapper from "./AxiosWrapper"
import Cookies from 'js-cookie'


//登录
export const login = async (req) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/auth/login', req))
}

// 登出
export const logout = () => {

    return apiRequestWrapper(() => httpWrapper.post('/api/auth/logout', null))
}

//发送邮箱验证码
export const sendEmailVerificationCode = (email) => {

    return apiRequestWrapper(() =>
        httpWrapper.get('/open/sendEmailVerificationCode', {
            params: {
                email: email
            }
        })
    )
}

// 验证token是否有效
export const tokenValid = (token, tokenType = 'ACCESS_TOKEN') => {

    return apiRequestWrapper(() =>
        httpWrapper.get('/api/auth/open/tokenValid', {
            params: {
                token: token,
                tokenType: tokenType
            }
        })
    )
}

export const saveToken = (tokenInfo) => {
    Cookies.set('accessToken', tokenInfo.access.token)
    Cookies.set('refreshToken', tokenInfo.refresh.token)
    if (tokenInfo.rememberMe) {
        localStorage.setItem('rememberMeToken', tokenInfo.rememberMe.token)
    }
}

export const clearToken = () => {
    Cookies.remove('accessToken')
    Cookies.remove('refreshToken')
    localStorage.clear()
}

export const checkTokenValid = async () => {
    const token = Cookies.get("accessToken")
    if (!token) return false
    const result = await tokenValid(token)
    return result.active
}