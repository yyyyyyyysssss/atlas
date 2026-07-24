
import { apiRequestWrapper } from "./ApiRequestWrapper"
import httpWrapper from "./AxiosWrapper"



// 创建oauth2应用
export const saveApplication = async (projectCode, body) => {

    return apiRequestWrapper(() => httpWrapper.post(`/api/auth/${projectCode}/application/oauth2/save`, body))
}

// 获取oauth2应用详情
export const getApplicationDetail = async (projectCode, id) => {

    return apiRequestWrapper(() => httpWrapper.get(`/api/auth/${projectCode}/application/oauth2/${id}`))
}

// 获取oauth2应用列表
export const getApplicationPage = async (projectCode, page, size) => {

    return apiRequestWrapper(() => httpWrapper.get(`/api/auth/${projectCode}/application/oauth2/page?pageNum=${page}&pageSize=${size}`))
}

// 删除oauth2应用
export const deleteApplication = async (projectCode, id) => {

    return apiRequestWrapper(() => httpWrapper.delete(`/api/auth/${projectCode}/application/oauth2/${id}`))
}

// oauth2应用添加新的客户端密钥
export const addClientSecret = async (projectCode, id) => {

    return apiRequestWrapper(() => httpWrapper.post(`/api/auth/${projectCode}/application/oauth2/${id}/secret`))
}

// 删除oauth2应用密钥
export const deleteClientSecret = async (projectCode, id, clientSecretId) => {

    return apiRequestWrapper(() => httpWrapper.delete(`/api/auth/${projectCode}/application/oauth2/${id}/secret/${clientSecretId}`))
}