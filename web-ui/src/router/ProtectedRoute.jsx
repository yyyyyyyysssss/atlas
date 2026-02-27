import { Navigate } from 'react-router-dom';
import { useAuth } from "./AuthProvider";
import Forbidden from "../pages/Forbidden";
import { useHasPermission } from "../components/HasPermission";
import Loading from "../components/loading";

export const ProtectedRoute = ({ children, requiredPermissions, fallback, requireAll = false }) => {

  const { isLoginIn } = useAuth()
  
  // 等待登录状态
  if (isLoginIn === null) return <Loading fullscreen />

  // 未登录
  if (!isLoginIn) return <Navigate to="/login" replace />

  const isAllowed = useHasPermission(requiredPermissions, requireAll)

  // 权限不足
  if (!isAllowed) return fallback || <Forbidden />

  return children
}