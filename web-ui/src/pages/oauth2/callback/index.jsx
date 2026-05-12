import { useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { message, Flex } from 'antd';
import { useRequest } from 'ahooks';
import { useAuth } from '../../../router/AuthProvider';
import { login } from '../../../services/LoginService';
import Loading from '../../../components/loading';
import { AUTHORIZE_CODE_PKCE_VERIFIER, oauth2Callback } from '../../../services/Oauth2Service';
import { useRedirect } from '../../../hooks/useRedirect';
import useFullParams from '../../../hooks/useFullParams';

const OAuth2Callback = () => {
    const { code, error, error_description, error_uri, clientName } = useFullParams()
    const navigate = useNavigate()
    const { signin } = useAuth()

    const redirect = useRedirect()

    const { runAsync } = useRequest(oauth2Callback, { manual: true })

    useEffect(() => {
        if (!code) {
            navigate('/500', {
                state: {
                    title: error,
                    subTitle: error_description,
                    errorUri: error_uri
                }
            })
            return
        }

        const handleAuth = async (code, clientName) => {
            try {
                const verifier = sessionStorage.getItem(AUTHORIZE_CODE_PKCE_VERIFIER)
                // 根据你后端的接口调整参数
                const data = await runAsync(code, verifier, clientName)
                await signin(data)
                redirect('/', data?.access?.token)
            } catch (error) {
                navigate('/500', {
                    state: {
                        title: 'OAuth2 回调处理失败',
                        subTitle: error.message
                    }
                })
            }
        }
        handleAuth(code, clientName)
    }, [code, clientName])

    return (
        <Loading fullscreen tip="正在完成登录..." />
    )
}

export default OAuth2Callback;