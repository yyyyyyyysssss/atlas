
import { apiRequestWrapper } from "./ApiRequestWrapper"
import httpWrapper from "./AxiosWrapper"



// 创建oauth2应用
export const saveApplication = async (body) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/auth/developer/oauth2/application/save', body))
}