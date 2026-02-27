import { Spin } from "antd"
import { useAuth } from "./AuthProvider"
import { Navigate } from 'react-router-dom';
import Loading from "../components/loading";


export const LoginRoute = ({ children }) => {
  const { isLoginIn } = useAuth()

  if (isLoginIn === null) return <Loading fullscreen />

  return isLoginIn ? <Navigate to="/home" replace /> : children
}