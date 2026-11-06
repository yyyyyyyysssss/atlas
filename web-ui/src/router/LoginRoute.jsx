import { useAuth } from "./AuthProvider"
import { Navigate } from 'react-router-dom';
import Loading from "../components/loading";
import { useEffect } from "react";
import useFullParams from "../hooks/useFullParams";
import { useRedirect } from "../hooks/useRedirect";


export const LoginRoute = ({ children }) => {
  const { isLoginIn, accessToken, checkAuth } = useAuth()

  const { targetUrl } = useFullParams()

  const redirect = useRedirect()

  useEffect(() => {
    checkAuth()
  }, [])

  if (isLoginIn === null){
    return <Loading tip="正在加载..." full />
  }

  if (isLoginIn) {
    if(targetUrl){
      redirect(targetUrl, accessToken)
      return <Loading tip="正在加载..." full />
    }
    return <Navigate to="/" replace />
  }

  return children
}