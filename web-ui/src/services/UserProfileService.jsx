
import { apiRequestWrapper } from "./ApiRequestWrapper"
import httpWrapper from "./AxiosWrapper"

export const fetchUserInfo = async () => {

    return apiRequestWrapper(() => httpWrapper.get('/api/user/profile/user/info'))
}


export const fetchAuthInfo = async () => {

    return apiRequestWrapper(() => httpWrapper.get('/api/user/profile/auth/info'))
}

export const fetchUserTeamMember = async () => {

    return apiRequestWrapper(() => httpWrapper.get('/api/user/profile/team'))
}

export const changePassword = async (req) => {

    return apiRequestWrapper(() => httpWrapper.put('/api/user/profile/password', req))
}



export const changeAvatar = async (newAvatarUrl) => {
    const req = {
        newAvatarUrl: newAvatarUrl
    }
    return apiRequestWrapper(() => httpWrapper.put('/api/user/profile/avatar', req))
}

export const changeWorkbenchShortcuts = async (shortcuts) => {
    const req = {
        shortcuts: shortcuts
    }
    return apiRequestWrapper(() => httpWrapper.put('/api/user/profile/workbench/shortcuts', req))
}

export const changeAppearance = async (appearance) => {

    return apiRequestWrapper(() => httpWrapper.put('/api/user/profile/appearance/settings', appearance))
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
