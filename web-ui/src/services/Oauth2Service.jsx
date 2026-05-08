import { apiRequestWrapper } from "./ApiRequestWrapper"
import httpWrapper from "./AxiosWrapper"


export const fetchAuthorizeUrl = async (clientName) => {

    return apiRequestWrapper(() => httpWrapper.get(`/api/auth/thirdParty/authorizeUrl/${clientName}`))
}


export const oauth2Callback = async (code, clientName) => {

    return apiRequestWrapper(() => httpWrapper.get(`/api/auth/thirdParty/callback/${clientName}`, {
        params: { code: code }
    }))
}