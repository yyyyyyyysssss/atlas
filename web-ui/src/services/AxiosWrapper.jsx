import axios from "axios";
import Cookies from 'js-cookie'
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getMessageApi } from "../utils/MessageUtil.jsx";
import { useGlobalSignout } from "../router/auth.js";
import router from "../router/router.jsx";

const baseURL = window.location.origin

const httpWrapper = axios.create({
    baseURL: baseURL,
    timeout: 60000
})


let requestCount = 0
const startProgress = () => {
    if (requestCount === 0) {
        NProgress.start()
    }
    requestCount++
}
const stopProgress = () => {
    requestCount--
    requestCount = Math.max(requestCount, 0)
    if (requestCount === 0) {
        NProgress.done()
    }
}

httpWrapper.interceptors.request.use(
    (req) => {
        startProgress()
        const token = Cookies.get("accessToken");
        if (token) {
            req.headers['Authorization'] = `Bearer ${token}`
        }
        return req;
    },
    (error) => {
        stopProgress()
        return Promise.reject(error);
    }
)


httpWrapper.interceptors.response.use(
    (res) => {
        stopProgress()
        const contentType = res.headers['content-type'] || ''
        if (contentType.includes('application/json')) {
            const result = res.data
            if (result && result.code !== 0) {
                getMessageApi().error(result.message)
                return Promise.reject(new Error(result.message))
            }
            return result
        }
        return res
    },
    (error) => {
        stopProgress()
        if (!error.response) {
            getMessageApi().error(error.message || '网络错误，请检查您的网络');
            return Promise.reject(error);
        }
        const { status, message: errorMessage } = error.response;
        switch (status) {
            case 400:
                getMessageApi().error("请求参数有误，请检查输入")
                break
            case 401:
                if (error.config.url !== '/login') {
                    const signout = useGlobalSignout()
                    signout()
                }
                break
            case 403:
                getMessageApi().error("您没有权限执行此操作")
                break
            case 404:
                getMessageApi().error("您访问的内容不存在或已被删除")
                break
            case 429:
                getMessageApi().error("请求过于频繁，请稍后再试")
                break
            case 500:
            case 502:
            case 503:
            case 504:
                const isCritical = error.config?.meta?.critical
                if (isCritical) {
                    router.navigate('/500', { state: { title: status, subTitle: '抱歉，服务器发生错误，请稍后再试。' } })
                } else {
                    getMessageApi().error('服务器开小差了，请稍后再试')
                }

                break
            default:
                if (isCritical) {
                    router.navigate('/500', { state: { title: status, subTitle: errorMessage || '抱歉，发生未知错误，请稍后重试。' } })
                } else {
                    message.error(errorMessage || '发生未知错误，请稍后重试')
                }
                break
        }
        return Promise.reject(error);
    }
)



export default httpWrapper;
