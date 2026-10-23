
import { apiRequestWrapper } from "./ApiRequestWrapper"
import httpWrapper from "./AxiosWrapper"



// 创建oauth2应用
export const saveOAuth2Client = async (projectCode, applicationId, body) => {

    return apiRequestWrapper(() => httpWrapper.post(`/api/auth/${projectCode}/${applicationId}/oauth2/save`, body))
}


// 获取oauth2客户端详情
export const getOAuth2ClientDetail = async (projectCode, applicationId, id) => {

    return apiRequestWrapper(() => httpWrapper.get(`/api/auth/${projectCode}/${applicationId}/oauth2/${id}`))
}

// 获取oauth2客户端列表
export const getOAuth2ClientPage = async (projectCode, applicationId, page, size) => {

    return apiRequestWrapper(() => httpWrapper.get(`/api/auth/${projectCode}/${applicationId}/oauth2/page?pageNum=${page}&pageSize=${size}`))
}

// 删除oauth2客户端
export const deleteOAuth2Client = async (projectCode, applicationId, id) => {

    return apiRequestWrapper(() => httpWrapper.delete(`/api/auth/${projectCode}/${applicationId}/oauth2/${id}`))
}

// oauth2客户端添加新的密钥
export const addClientSecret = async (projectCode, applicationId, id) => {

    return apiRequestWrapper(() => httpWrapper.post(`/api/auth/${projectCode}/${applicationId}/oauth2/${id}/secret`))
}

// 删除oauth2客户端密钥
export const deleteClientSecret = async (projectCode, applicationId, id, clientSecretId) => {

    return apiRequestWrapper(() => httpWrapper.delete(`/api/auth/${projectCode}/${applicationId}/oauth2/${id}/secret/${clientSecretId}`))
}



// 获取应用列表
export const getApplicationPage = async (projectCode, pageNum, pageSize, applicationName) => {

    return apiRequestWrapper(() => httpWrapper.get(`/api/auth/${projectCode}/application/page`, {
        params: {
            pageNum,
            pageSize,
            applicationName
        }
    }))
}

// 获取应用详情
export const getApplicationDetail = async (projectCode, id) => {

    return apiRequestWrapper(() => httpWrapper.get(`/api/auth/${projectCode}/application/${id}`))
}

// 保存应用
export const saveApplication = async (projectCode, req) => {

    return apiRequestWrapper(() => httpWrapper.post(`/api/auth/${projectCode}/application/save`, req))
}

// 修改应用状态
export const updateApplicationEnabled = async (projectCode, id, enabled) => {
    const req = {
        enabled: enabled
    }
    return apiRequestWrapper(() => httpWrapper.post(`/api/auth/${projectCode}/application/${id}/enabled`, req))
}