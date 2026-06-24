import { useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { message, Flex, App } from 'antd';
import { useRequest } from 'ahooks';
import { useAuth } from '../../../../router/AuthProvider';
import Loading from '../../../../components/loading';
import { AUTHORIZE_CODE_PKCE_VERIFIER, oauth2Callback, QR_SCAN_PKCE_VERIFIER } from '../../../../services/Oauth2Service';
import { useRedirect } from '../../../../hooks/useRedirect';
import useFullParams from '../../../../hooks/useFullParams';

const OAuth2Callback = () => {
    const { code, state, error, error_description, error_uri, clientName, login_mode } = useFullParams()
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

        const handleAuth = async (code, state, clientName, loginMode) => {
            try {
                let verifier = sessionStorage.getItem(AUTHORIZE_CODE_PKCE_VERIFIER)
                // 根据你后端的接口调整参数
                const loginResponse = await runAsync(code, state, verifier, clientName)
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

                if (loginResponse.status === 'SUCCESS_BOUND') {
                    // 通过 postMessage 向原本的主窗口（opener）发送一个自定义事件，通知它绑定成功
                    if (window.opener) {
                        window.opener.postMessage('BIND_SUCCESS', window.location.origin)
                    }
                    window.close()
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
        handleAuth(code, state, clientName, login_mode)
    }, [code, state, clientName, login_mode])

    return (
        <Loading fullscreen tip="正在完成认证..." />
    )
}

export default OAuth2Callback;