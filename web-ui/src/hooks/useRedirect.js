import { useNavigate, useSearchParams } from 'react-router-dom';

export const useRedirect = () => {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();

    const redirect = (defaultPath = '/', accessToken = null) => {
        const targetUrl = searchParams.get('targetUrl');
        
        if (targetUrl) {
            if (targetUrl.startsWith('http')) {
                const separator = targetUrl.includes('?') ? '&' : '?';
                const finalUrl = accessToken 
                    ? `${targetUrl}${separator}access_token=${encodeURIComponent(accessToken)}`
                    : targetUrl;
                window.location.replace(finalUrl);
            } else {
                navigate(targetUrl, { replace: true });
            }
        } else {
            navigate(defaultPath, { replace: true });
        }
    };

    return redirect;
};