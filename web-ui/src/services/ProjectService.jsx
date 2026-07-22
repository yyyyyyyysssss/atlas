
import { apiRequestWrapper } from "./ApiRequestWrapper"
import httpWrapper from "./AxiosWrapper"




// 获取oauth2应用列表
export const getProjectPage = async (page, size) => {

    return apiRequestWrapper(() => httpWrapper.get(`/api/auth/project/page?pageNum=${page}&pageSize=${size}`))
}

// 删除oauth2应用
export const deleteProject = async (id) => {

    return apiRequestWrapper(() => httpWrapper.delete(`/api/auth/project/${id}`))
}