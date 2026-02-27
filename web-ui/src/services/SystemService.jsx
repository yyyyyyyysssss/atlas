import { apiRequestWrapper } from "./ApiRequestWrapper"
import httpWrapper from "./AxiosWrapper"

// 用户列表
export const fetchUserList = async (queryParam) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/user/system/user/query', queryParam, { meta: { critical: true } }))
}

// 用户详情
export const fetchUserDetails = async (userId, includeSensitiveData = false) => {
    const headers = {
        'x-sensitive-data-access': includeSensitiveData ? 'true' : 'false'
    }
    return apiRequestWrapper(() => httpWrapper.get(`/api/user/system/user/${userId}`, { headers }))
}

// 用户停启用
export const updateUserEnabled = async (userId, enabled) => {
    const roleBody = {
        id: userId,
        enabled: enabled
    }
    return apiRequestWrapper(() => httpWrapper.patch('/api/user/system/user', roleBody))
}

// 创建用户
export const createUser = async (userBody) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/user/system/user', userBody))
}

// 更新用户
export const updateUser = async (userBody) => {

    return apiRequestWrapper(() => httpWrapper.put('/api/user/system/user', userBody))
}

// 用户搜索
export const fetchSearchUser = async (keyword, pageNum, pageSize) => {
    const searchUserReq = {
        pageNum: pageNum,
        pageSize: pageSize
    }
    if (Array.isArray(keyword)) {
        searchUserReq.ids = keyword
    } else {
        searchUserReq.keyword = keyword
    }
    return apiRequestWrapper(() => httpWrapper.post('/api/user/system/user/search', searchUserReq))
}

// 用户分配角色
export const bindRoleByUserId = async (userId, roleIds) => {
    const req = {
        roleIds: roleIds
    }
    return apiRequestWrapper(() => httpWrapper.post(`/api/user/system/user/${userId}/roles`, req))
}

// 删除用户
export const deleteUserById = async (userId) => {

    return apiRequestWrapper(() => httpWrapper.delete(`/api/user/system/user/${userId}`))
}

// 用户选择
export const fetchUserOptions = async () => {

    return apiRequestWrapper(() => httpWrapper.get('/api/user/option/user'))
}

// 重置用户密码
export const resetPassword = async (userId) => {

    return apiRequestWrapper(() => httpWrapper.put(`/api/user/system/user/${userId}/password`))
}

// 创建角色
export const createRole = async (roleBody) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/user/system/role', roleBody))
}


// 更新角色
export const updateRole = async (roleBody) => {

    return apiRequestWrapper(() => httpWrapper.put('/api/user/system/role', roleBody))
}

// 角色停启用
export const updateRoleEnabled = async (roleId, enabled) => {
    const roleBody = {
        id: roleId,
        enabled: enabled
    }
    return apiRequestWrapper(() => httpWrapper.patch('/api/user/system/role', roleBody))
}

// 删除角色
export const deleteRoleById = async (roleId) => {

    return apiRequestWrapper(() => httpWrapper.delete(`/api/user/system/role/${roleId}`))
}

// 角色列表
export const fetchRoleList = async (queryParam) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/user/system/role/query', queryParam))
}

// 角色详情
export const fetchRoleDetails = async (roleId) => {

    return apiRequestWrapper(() => httpWrapper.get(`/api/user/system/role/${roleId}`))
}

// 角色下的用户
export const fetchUserIdByRoleId = async (roleId) => {
    return apiRequestWrapper(() => httpWrapper.get(`/api/user/system/role/${roleId}/users`))
}
// 角色绑定用户
export const bindRoleUser = async (roleId, userIds) => {
    const req = {
        userIds: userIds
    }
    return apiRequestWrapper(() => httpWrapper.post(`/api/user/system/role/${roleId}/users`, req))
}

// 角色选择
export const fetchRoleOptions = async () => {

    return apiRequestWrapper(() => httpWrapper.get('/api/user/option/role'))
}

// 角色分配权限
export const bindAuthorityByRoleId = async (roleId, authorityIds) => {
    const req = {
        authorityIds: authorityIds
    }
    return apiRequestWrapper(() => httpWrapper.post(`/api/user/system/role/${roleId}/authorities`, req))
}

// 菜单树
export const fetchMenuTree = async () => {

    return apiRequestWrapper(() => httpWrapper.get('/api/user/system/menu/tree'))
}

// 菜单详情
export const fetchMenuDetails = async (menuId) => {

    return apiRequestWrapper(() => httpWrapper.get(`/api/user/system/menu/${menuId}`))
}

// 菜单拖动
export const menuDrag = async (dragId, targetId, position) => {
    const req = {
        dragId: dragId,
        targetId: targetId,
        position: position
    }
    return apiRequestWrapper(() => httpWrapper.post('/api/user/system/menu/drag', req))
}

// 创建菜单
export const createMenu = async (req) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/user/system/menu', req))
}

// 更新菜单
export const updateMenu = async (req) => {

    return apiRequestWrapper(() => httpWrapper.put('/api/user/system/menu', req))
}

// 删除菜单
export const deleteMenu = async (id) => {

    return apiRequestWrapper(() => httpWrapper.delete(`/api/user/system/menu/${id}`))
}

// 添加权限
export const addAuthority = async (req) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/user/system/authority', req))
}

// 获取菜单下的权限
export const fetchAuthorityByMenuId = async (menuId) => {

    return apiRequestWrapper(() => httpWrapper.get(`/api/user/system/authority/menu/${menuId}`))
}

// 更新权限
export const updateAuthority = async (req) => {

    return apiRequestWrapper(() => httpWrapper.put('/api/user/system/authority', req))
}

// 删除权限
export const deleteAuthorityById = async (id) => {

    return apiRequestWrapper(() => httpWrapper.delete(`/api/user/system/authority/${id}`))
}

// 更新权限api路径
export const updateAuthorityUrlsById = async (id, authorityUrls) => {
    const req = {
        id: id,
        urls: authorityUrls,
    }
    return apiRequestWrapper(() => httpWrapper.patch('/api/user/system/authority', req))
}

// 获取权限树
export const fetchAuthorityTree = async () => {

    return apiRequestWrapper(() => httpWrapper.get('/api/user/option/authority'))
}


// 获取字典列表
export const fetchDictList = async (queryParam) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/user/system/dict/query', queryParam))
}

// 获取字典详情
export const fetchDictDetails = async (dictId, includeSensitiveData = false) => {
    const headers = {
        'x-sensitive-data-access': includeSensitiveData ? 'true' : 'false'
    }
    return apiRequestWrapper(() => httpWrapper.get(`/api/user/system/dict/${dictId}`, { headers }))
}

// 创建字典
export const createDict = async (req) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/user/system/dict', req))
}

// 更新字典
export const updateDict = async (req) => {

    return apiRequestWrapper(() => httpWrapper.put('/api/user/system/dict', req))
}

// 字典停启用
export const updateDictEnabled = async (dictId, enabled) => {
    const req = {
        id: dictId,
        enabled: enabled
    }
    return apiRequestWrapper(() => httpWrapper.patch('/api/user/system/dict', req))
}

// 删除字典
export const deleteDictById = async (dictId) => {

    return apiRequestWrapper(() => httpWrapper.delete(`/api/user/system/dict/${dictId}`))
}

// 获取字典项列表
export const fetchDictItemList = async (queryParam) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/user/system/dict/item/query', queryParam))
}

// 获取字典详情
export const fetchDictItemDetails = async (dictItemId, includeSensitiveData = false) => {
    const headers = {
        'x-sensitive-data-access': includeSensitiveData ? 'true' : 'false'
    }
    return apiRequestWrapper(() => httpWrapper.get(`/api/user/system/dict/item/${dictItemId}`, { headers }))
}

// 获取字典项子节点
export const fetchDictItemChildren = async (dictItemId) => {

    return apiRequestWrapper(() => httpWrapper.get(`/api/user/system/dict/item/${dictItemId}/children`))
}

// 批量获取字典项子节点
export const batchDictItemChildren = async (dictItemIdList) => {

    return apiRequestWrapper(() => httpWrapper.post(`/api/user/system/dict/item/children/batch`,dictItemIdList))
}

// 创建字典项
export const createDictItem = async (req) => {

    return apiRequestWrapper(() => httpWrapper.post('/api/user/system/dict/item', req))
}

// 更新字典项
export const updateDictItem = async (req) => {

    return apiRequestWrapper(() => httpWrapper.put('/api/user/system/dict/item', req))
}

// 字典项停启用
export const updateDictItemEnabled = async (dictItemId, enabled) => {
    if (enabled) {
        return apiRequestWrapper(() => httpWrapper.patch(`/api/user/system/dict/item/${dictItemId}/enable`))
    } else {
        return apiRequestWrapper(() => httpWrapper.patch(`/api/user/system/dict/item/${dictItemId}/disable`))
    }

}

// 删除字典项子节点
export const deleteDictItemById = async (dictItemId) => {

    return apiRequestWrapper(() => httpWrapper.delete(`/api/user/system/dict/item/${dictItemId}`))
}

// 获取字典
export const fetchDictByCode = async (code, category = '') => {

    return apiRequestWrapper(() => httpWrapper.get(`/api/user/option/dict?code=${code}&category=${category}`))
}
// 获取字典
export const fetchDictTreeByCode = async (code, category = null) => {

    return apiRequestWrapper(() => httpWrapper.get(`/api/user/option/dict/tree?code=${code}&category=${category}`))
}



//公司树
export const fetchCompanyTree = async () => {

    return apiRequestWrapper(() => httpWrapper.get('/api/user/system/company/tree'))
}


// 创建公司
export const createCompany = async (req) => {
    return apiRequestWrapper(() => httpWrapper.post('/api/user/system/company/create', req))
}

// 修改公司
export const updateCompany = async (req) => {
    return apiRequestWrapper(() => httpWrapper.put('/api/user/system/company/update', req))
}

// 查询公司详情
export const fetchCompanyDetails = async (companyId) => {
    return apiRequestWrapper(() => httpWrapper.get(`/api/user/system/company/${companyId}`))
}

// 公司
export const fetchCompanyOptions = async () => {

    return apiRequestWrapper(() => httpWrapper.get('/api/user/option/company'))
}