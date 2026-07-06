
import { apiRequestWrapper } from "./ApiRequestWrapper"
import httpWrapper from "./AxiosWrapper"



// 创建oauth2应用
export const saveApplication = async (body) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/auth/developer/oauth2/application/save', body))
}

// 获取应用详情
export const getApplicationDetail = async (id) => {

    return apiRequestWrapper(() => httpWrapper.get(`/api/auth/developer/oauth2/application/${id}`))
}

// 获取应用列表
export const getApplicationPage = async (page, size) => {

    return apiRequestWrapper(() => httpWrapper.get(`/api/auth/developer/oauth2/application/page?pageNum=${page}&pageSize=${size}`))
}

// 获取应用列表
export const deleteApplication = async (id) => {

    return apiRequestWrapper(() => httpWrapper.delete(`/api/auth/developer/oauth2/application/${id}`))
}