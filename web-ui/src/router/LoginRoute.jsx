import { useAuth } from "./AuthProvider"
import { Navigate, useSearchParams } from 'react-router-dom';
import Loading from "../components/loading";
import { useEffect } from "react";


export const LoginRoute = ({ children }) => {
  const { isLoginIn, accessToken, checkAuth } = useAuth()
  const [searchParams] = useSearchParams()

  useEffect(() => {
    checkAuth()
  }, [])

  if (isLoginIn === null){
    return <Loading fullscreen />
  }

  if (isLoginIn) {
    const targetUrl = searchParams.get('targetUrl')
    if (targetUrl) {
      if (targetUrl.startsWith('http')) {
        const separator = targetUrl.includes('?') ? '&' : '?';
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