import { apiRequestWrapper } from "./ApiRequestWrapper"
import httpWrapper from "./AxiosWrapper"


// 公告列表
export const fetchAnnouncementList = async (queryParam) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/notification/announcement/query', queryParam))
}

// 创建公告
export const createAnnouncement = async (userBody) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/notification/announcement/create', userBody))
}

// 更新公告
export const updateAnnouncement = async (userBody) => {

    return apiRequestWrapper(() => httpWrapper.patch('/api/notification/announcement/update', userBody))
}

// 公告详情
export const getAnnouncementDetails = async (announcementId) => {

    return apiRequestWrapper(() => httpWrapper.get(`/api/notification/announcement/${announcementId}`))
}

// 删除公告
export const deleteAnnouncementById = async (announcementId) => {

    return apiRequestWrapper(() => httpWrapper.delete(`/api/notification/announcement/${announcementId}`))
}


// 最新公告
export const getAnnouncementLatest = async () => {

    return apiRequestWrapper(() => httpWrapper.get(`/api/notification/user/announcement/latest`))
}


// 用户公告列表
export const fetchAnnouncementUserList = async (queryParam) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/notification/user/announcement/list', queryParam))
}

// 用户公告详情
export const getAnnouncementUserDetails = async (announcementId) => {

    return apiRequestWrapper(() => httpWrapper.get(`/api/notification/user/announcement/${announcementId}`))
}