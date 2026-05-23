
import { apiRequestWrapper } from "./ApiRequestWrapper"
import httpWrapper from "./AxiosWrapper"

export const fetchUserInfo = async () => {

    return apiRequestWrapper(() => httpWrapper.get('/api/user/profile'))
}


export const fetchAuthInfo = async () => {

    return apiRequestWrapper(() => httpWrapper.get('/api/user/profile/auth'))
}

export const fetchUserTeamMember = async () => {

    return apiRequestWrapper(() => httpWrapper.get('/api/user/profile/team'))
}

export const changeUserProfile = async (req) => {

    return apiRequestWrapper(() => httpWrapper.patch('/api/user/profile', req))
}







export const changeUsername = async (req) => {

    return apiRequestWrapper(() => httpWrapper.put('/api/auth/account/username', req))
}

export const fetchAccountSecurity = async () => {

    return apiRequestWrapper(() => httpWrapper.get('/api/auth/account/security'))
}

export const initPassword = async (req) => {

    return apiRequestWrapper(() => httpWrapper.put('/api/auth/account/init/password', req))
}

export const changePassword = async (req) => {

    return apiRequestWrapper(() => httpWrapper.put('/api/auth/account/password', req))
}

export const verifyPassword = async (req) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/auth/account/verify/password', req))
}

export const initEmail = async (req) => {

    return apiRequestWrapper(() => httpWrapper.put('/api/auth/account/init/email', req))
}

export const changeEmail = async (req) => {

    return apiRequestWrapper(() => httpWrapper.put('/api/auth/account/email', req))
}

export const verifyCaptcha = async (req) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/auth/account/verify/captcha', req))
}

export const getWebauthnRegisterOptions = async () => {

    return apiRequestWrapper(() => httpWrapper.post('/api/auth/webauthn/register/options'), { raw: true })
}

export const webauthnRegister = async (req) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/auth/webauthn/register', req), { raw: true })
}




export const getUserWorkSchedule = async (startDate, endDate) => {

    return apiRequestWrapper(() => httpWrapper.get('/api/user/work/schedule/user/range', {
        params: {
            startDate, endDate
        }
    }))
}

export const createUserWorkSchedule = async (req) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/user/work/schedule/create', req))
}

export const updateUserWorkSchedule = async (req) => {

    return apiRequestWrapper(() => httpWrapper.patch('/api/user/work/schedule/update', req))
}
