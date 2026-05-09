import { apiRequestWrapper } from "./ApiRequestWrapper"
import httpWrapper from "./AxiosWrapper"


export const fetchAuthorizeUrl = async (clientName) => {

    return apiRequestWrapper(() => httpWrapper.get(`/api/auth/thirdParty/authorizeUrl/${clientName}`))
}


export const fetchDeviceCode = async () => {
    const params = new URLSearchParams()
    params.append('client_id', '32b00b1e89af-90d2e0e46d20ebb92f6c')
    params.append('client_secret', '45546ecf428b4f86adfa93798d82dca4')
    params.append('scope', 'profile')
    return apiRequestWrapper(() => httpWrapper.post(`/api/auth/oauth2/device_authorization`, params, {
        headers: {
            'Content-Type': 'application/x-www-form-urlencoded'
        }
    }), { raw: true })
}


export const oauth2Callback = async (code, clientName) => {

    return apiRequestWrapper(() => httpWrapper.get(`/api/auth/thirdParty/callback/${clientName}`, {
        params: { code: code }
    }))
}