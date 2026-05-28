
import { apiRequestWrapper } from "./ApiRequestWrapper"
import httpWrapper from "./AxiosWrapper"



export const changeUsername = async (req) => {

    return apiRequestWrapper(() => httpWrapper.put('/api/auth/account/username', req))
}

export const fetchAccountSecurity = async () => {

    return apiRequestWrapper(() => httpWrapper.get('/api/auth/account/security'))
}

export const initPassword = async (req) => {

    return apiRequestWrapper(() => httpWrapper.put('/api/auth/account/password/init', req))
}

export const changePassword = async (req) => {

    return apiRequestWrapper(() => httpWrapper.put('/api/auth/account/password', req))
}

export const verifyPassword = async (req) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/auth/account/password/verify', req))
}

export const initEmail = async (req) => {

    return apiRequestWrapper(() => httpWrapper.put('/api/auth/account/email/init', req))
}

export const changeEmail = async (req) => {

    return apiRequestWrapper(() => httpWrapper.put('/api/auth/account/email', req))
}

export const verifyCaptcha = async (req) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/auth/account/captcha/verify', req))
}

export const webauthnRegisterOptions = async () => {

    return apiRequestWrapper(() => httpWrapper.post('/api/auth/webauthn/register/options'), { raw: true })
}

export const webauthnRegister = async (webauthnId, req) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/auth/webauthn/register', req, {
        headers: {
            'X-Webauthn-Id': webauthnId
        }
    }), { raw: true })
}

export const webauthnAuthenticateOptions = async () => {

    return apiRequestWrapper(() => httpWrapper.post('/api/auth/webauthn/authenticate/options'), { raw: true })
}

export const verifyWebauthn = async (webauthnId, req, securityScene) => {

    return apiRequestWrapper(() => httpWrapper.post(
        `/api/auth/account/webauthn/verify?securityScene=${securityScene}`,
        req,
        {
            headers: {
                'X-Webauthn-Id': webauthnId
            }
        }
    ))
}

export const unbindWebauthn = async (req) => {

    return apiRequestWrapper(() => httpWrapper.delete('/api/auth/account/webauthn/unbind', { data: req }))
}


export const initTotp = async () => {

    return apiRequestWrapper(() => httpWrapper.post('/api/auth/account/totp'))
}

export const activateTotp = async (req) => {

    return apiRequestWrapper(() => httpWrapper.put('/api/auth/account/totp', req))
}

export const unbindTotp = async (req) => {

    return apiRequestWrapper(() => httpWrapper.delete('/api/auth/account/totp', req))
}

export const verifyTotp = async (req) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/auth/account/totp/verify', req))
}

export const refreshTotpBackupCode = async (req) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/auth/account/totp/backupCode/refresh', req))
}

