import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from "./AuthProvider";
import Forbidden from "../pages/Forbidden";
import { useHasPermission } from "../components/HasPermission";
import Loading from "../components/loading";

export const ProtectedRoute = ({ children, requiredPermissions, fallback, requireAll = false }) => {

  const { isLoginIn } = useAuth()
  const location = useLocation()
  
  // 等待登录状态
  if (isLoginIn === null) return <Loading fullscreen />

  // 未登录，跳转到登录页，并携带当前页面的 state 作为 targetUrl
  if (!isLoginIn) {
        // 记录用户想要访问的原始路径和查询参数
    const targetUrl = location.pathname + location.search;
    const loginPath = (targetUrl && targetUrl !== '/') 
      ? `/login?targetUrl=${encodeURIComponent(targetUrl)}` 
      : '/login';
    return <Navigate to={loginPath} replace />;

  }

  const isAllowed = useHasPermission(requiredPermissions, requireAll)

  // 权限不足
  if (!isAllowed) return fallback || <Forbidden />

  return children
}