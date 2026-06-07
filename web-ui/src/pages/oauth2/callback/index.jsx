import { useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { message, Flex, App } from 'antd';
import { useRequest } from 'ahooks';
import { useAuth } from '../../../router/AuthProvider';
import Loading from '../../../components/loading';
import { AUTHORIZE_CODE_PKCE_VERIFIER, oauth2Callback, QR_SCAN_PKCE_VERIFIER } from '../../../services/Oauth2Service';
import { useRedirect } from '../../../hooks/useRedirect';
import useFullParams from '../../../hooks/useFullParams';

const OAuth2Callback = () => {
    const { code, error, error_description, error_uri, clientName, login_mode } = useFullParams()
    const navigate = useNavigate()
    const { signin } = useAuth()

    const { message } = App.useApp()

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

        const handleAuth = async (code, clientName, loginMode) => {
            try {
                let verifier
                if (loginMode && loginMode === 'qr') {
                    verifier = sessionStorage.getItem(QR_SCAN_PKCE_VERIFIER)
                } else {
                    verifier = sessionStorage.getItem(AUTHORIZE_CODE_PKCE_VERIFIER)
                }
                // 根据你后端的接口调整参数
                const loginResponse = await runAsync(code, verifier, clientName)
                if (loginResponse.status === 'SUCCESS') {
                    const { token } = loginResponse
                    await signin(token)
                    redirect('/', token.access.value)
                    return
                }

                if (loginResponse.status === 'MFA_REQUIRED') {
                    const { mfaTicket, mfaType, activeMfaStrategies } = loginResponse
                    navigate('/login/mfa', {
                        state: {
                            ticket: mfaTicket,
                            mfaType: mfaType,
                            activeMfaStrategies: activeMfaStrategies
                        }
                    })
                    return
                }
                message.error('登录失败')
            } catch (error) {
                navigate('/500', {
                    state: {
                        title: 'OAuth2 回调处理失败',
                        subTitle: error.message
                    }
                })
            }
        }
        handleAuth(code, clientName, login_mode)
    }, [code, clientName, login_mode])

    return (
        <Loading fullscreen tip="正在完成登录..." />
    )
}

export default OAuth2Callback;