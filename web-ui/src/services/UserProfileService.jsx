
import { apiRequestWrapper } from "./ApiRequestWrapper"
import httpWrapper from "./AxiosWrapper"

export const fetchUserInfo = async () => {

    return apiRequestWrapper(() => httpWrapper.get('/api/user/profile/user/info'))
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
