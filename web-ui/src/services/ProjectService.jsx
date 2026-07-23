
import { apiRequestWrapper } from "./ApiRequestWrapper"
import httpWrapper from "./AxiosWrapper"




// 获取项目列表
export const getProjectPage = async (page, size) => {

    return apiRequestWrapper(() => httpWrapper.get(`/api/auth/project/page?pageNum=${page}&pageSize=${size}`))
}

// 获取项目详情
export const getProjectDetail = async (id) => {

    return apiRequestWrapper(() => httpWrapper.get(`/api/auth/project/${id}`))
}

// 保存项目
export const saveProject = async (req) => {

    return apiRequestWrapper(() => httpWrapper.post(`/api/auth/project/save`, req))
}

// 恢复项目
export const restoreProject = async (id) => {

    return apiRequestWrapper(() => httpWrapper.patch(`/api/auth/project/${id}/restore`))
}

// 删除项目
export const deleteProject = async (id) => {

    return apiRequestWrapper(() => httpWrapper.delete(`/api/auth/project/${id}`))
}