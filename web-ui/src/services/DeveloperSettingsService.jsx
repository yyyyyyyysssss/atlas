
import { apiRequestWrapper } from "./ApiRequestWrapper"
import httpWrapper from "./AxiosWrapper"



// 创建oauth2应用
export const saveApplication = async (body) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/auth/developer/oauth2/application/save', body))
}

// 获取oauth2应用详情
export const getApplicationDetail = async (id) => {

    return apiRequestWrapper(() => httpWrapper.get(`/api/auth/developer/oauth2/application/${id}`))
}

// 获取oauth2应用列表
export const getApplicationPage = async (page, size) => {

    return apiRequestWrapper(() => httpWrapper.get(`/api/auth/developer/oauth2/application/page?pageNum=${page}&pageSize=${size}`))
}

// 删除oauth2应用
export const deleteApplication = async (id) => {

    return apiRequestWrapper(() => httpWrapper.delete(`/api/auth/developer/oauth2/application/${id}`))
}

// oauth2应用添加新的客户端密钥
export const addClientSecret = async (id) => {

    return apiRequestWrapper(() => httpWrapper.post(`/api/auth/developer/oauth2/application/${id}/secret`))
}

// 删除oauth2应用密钥
export const deleteClientSecret = async (clientSecretId) => {

    return apiRequestWrapper(() => httpWrapper.delete(`/api/auth/developer/oauth2/application/secret/${clientSecretId}`))
}