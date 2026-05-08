import { useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { message, Flex } from 'antd';
import { useRequest } from 'ahooks';
import { useAuth } from '../../../router/AuthProvider';
import { login } from '../../../services/LoginService';
import Loading from '../../../components/loading';
import { oauth2Callback } from '../../../services/Oauth2Service';
import { useRedirect } from '../../../hooks/useRedirect';
import useFullParams from '../../../hooks/useFullParams';

const OAuth2Callback = () => {
    const { code, clientName } = useFullParams()
    const navigate = useNavigate()
    const { signin } = useAuth()

    const redirect = useRedirect()

    const { runAsync } = useRequest(oauth2Callback, { manual: true })

    useEffect(() => {
        if (!code) {
            message.error("无效的授权回调")
            navigate('/login')
            return
        }

        const handleAuth = async (code, clientName) => {
            try {
                // 根据你后端的接口调整参数
                const data = await runAsync(code, clientName)
                console.log('data', data)
                // await signin(data)
                // redirect('/', data?.access?.token)
            } catch (error) {
                console.error('OAuth2 回调处理失败:', error);
                message.error("登录处理失败，请重试");
                navigate('/login');
            }
        }

        handleAuth(code, clientName)
    }, [code, clientName, navigate, signin, runAsync])

    return (
        <Loading fullscreen tip="正在完成登录..." />
    )
}

export default OAuth2Callback;