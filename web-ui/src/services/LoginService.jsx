import { apiRequestWrapper } from "./ApiRequestWrapper"
import httpWrapper from "./AxiosWrapper"
import Cookies from 'js-cookie'


// 验证码登录
export const captchaLogin = async (req) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/auth/login/captcha', req))
}

// 账号密码登录
export const passwordLogin = async (req) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/auth/login/password', req))
}

// 免密登录
export const ottLogin = async (req) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/auth/login/ott', req))
}

// 通行密钥登录
export const webauthnLogin = async (webauthnId, req) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/auth/login/webauthn', req, {
        headers: {
            'X-Webauthn-Id': webauthnId
        }
    }))
}


export const sendOttLink = async (username, targetUrl = '') => {

    return apiRequestWrapper(() => httpWrapper.post('/api/auth/ott/generate', null, {
        params: {
            username: username,
            targetUrl: targetUrl
        }
    }))
}

// 登出
export const logout = () => {

    return apiRequestWrapper(() => httpWrapper.post('/api/auth/logout', null))
}

//发送登录邮箱验证码
export const sendEmailVerificationCode = (email) => {

    return apiRequestWrapper(() =>
        httpWrapper.post('/api/auth/code/email/login', {
            email: email
        })
    )
}

// 发送验证码
export const sendCaptcha = (req) => {

    return apiRequestWrapper(() =>
        httpWrapper.post('/api/auth/captcha/send', req)
    )
}

// 校验验证码
export const verifyCaptcha = (req) => {

    return apiRequestWrapper(() =>
        httpWrapper.post('/api/auth/captcha/verify', req)
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
    Cookies.set('accessToken', tokenInfo.access.value)
    Cookies.set('refreshToken', tokenInfo?.refresh?.value)
    if (tokenInfo.rememberMe) {
        localStorage.setItem('rememberMeToken', tokenInfo.rememberMe.value)
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