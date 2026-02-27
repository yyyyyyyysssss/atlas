

import { useMemo } from "react"
import { useParams, useLocation, useSearchParams } from "react-router-dom"


const useFullParams = () => {

    const routeParams = useParams()
    const [searchParams] = useSearchParams()
    const location = useLocation()

    const queryParams = useMemo(() => {
        const result = {}
        searchParams.forEach((value, key) => {
            result[key] = value;
        })
        return result
    },[searchParams])

    const stateParams = location.state || {}
    
    // 返回合并的查询参数和 state 参数
    return { ...routeParams, ...stateParams, ...queryParams };
}

export default useFullParams