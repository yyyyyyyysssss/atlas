import { apiRequestWrapper } from "./ApiRequestWrapper"
import httpWrapper from "./AxiosWrapper"



export const agreeAuthorize = (requestData) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/auth/oauth2/authorize', requestData))
}

export const agreeDeviceAuthorize = (requestData) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/auth/oauth2/device_verification', requestData))
}
