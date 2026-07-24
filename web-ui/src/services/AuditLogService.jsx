
import { apiRequestWrapper } from "./ApiRequestWrapper"
import httpWrapper from "./AxiosWrapper"



// 审计日志列表
export const getAuditLogPage = async (pageNum, pageSize) => {

    return apiRequestWrapper(() => httpWrapper.get(`/api/auth/audit/log/page`, {
        params: {
            pageNum,
            pageSize,
        }
    }))
}