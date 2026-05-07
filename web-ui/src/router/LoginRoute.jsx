import { useAuth } from "./AuthProvider"
import { Navigate, useSearchParams } from 'react-router-dom';
import Loading from "../components/loading";
import Cookies from 'js-cookie'


export const LoginRoute = ({ children }) => {
  const { isLoginIn } = useAuth()
  const [searchParams] = useSearchParams()

  if (isLoginIn === null) return <Loading fullscreen />

  if (isLoginIn) {
    const targetUrl = searchParams.get('targetUrl')
    if (targetUrl) {
      if (targetUrl.startsWith('http')) {
        const separator = targetUrl.includes('?') ? '&' : '?';
        const accessToken = Cookies.get("accessToken")
        const finalUrl = `${targetUrl}${separator}access_token=${encodeURIComponent(accessToken)}`;
        window.location.replace(finalUrl)
        return null
      }
      return <Navigate to={targetUrl} replace />
    }
    return <Navigate to="/" replace />
  }

  return children
}