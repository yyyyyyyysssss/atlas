import { useNavigate, useSearchParams } from 'react-router-dom';


let redirecting = false;

export const useRedirect = () => {

    const navigate = useNavigate();

    const redirect = (targetUrl = '/', accessToken = null) => {
        if (!targetUrl || redirecting) {
            return;
        }
        // 外部地址
        if (targetUrl.startsWith('http')) {
            const target = new URL(targetUrl);
            if (accessToken) {
                target.searchParams.set('access_token', accessToken);
            }
            const finalUrl = target.toString();
            const currentUrl = window.location.href;
            // 比较最终跳转地址，避免重复跳转
            if (finalUrl === currentUrl) {
                return;
            }
            redirecting = true;
            window.location.replace(finalUrl);
            return
        }
        // 内部地址
        redirecting = true;
        navigate(targetUrl, { replace: true }).finally(() => {
            redirecting = false
        })
    };

    return redirect;
};