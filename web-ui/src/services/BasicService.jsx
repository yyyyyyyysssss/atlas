import { apiRequestWrapper } from "./ApiRequestWrapper"
import httpWrapper from "./AxiosWrapper"



// 获取所有城市
export const fetchAllCityOptions = () => {

    return apiRequestWrapper(() => httpWrapper.get('/api/options/city'))
}