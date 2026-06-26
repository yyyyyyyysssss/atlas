import { useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { message, Flex, App } from 'antd';
import { useRequest } from 'ahooks';
import Loading from '../../../../components/loading';
import { AUTHORIZE_CODE_PKCE_VERIFIER, oauth2Callback, QR_SCAN_PKCE_VERIFIER } from '../../../../services/Oauth2Service';
import useFullParams from '../../../../hooks/useFullParams';
import useLoginHandler from '../../../../hooks/useLoginHandler';

const OAuth2Callback = () => {
    const { code: rawCode, auth_code, state, error, error_description, error_uri, clientName } = useFullParams()
    const navigate = useNavigate()

    const { message } = App.useApp()

    const { processLoginSuccess } = useLoginHandler()

    const { runAsync } = useRequest(oauth2Callback, { manual: true })

    const code = rawCode || auth_code

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
            const verifierKey = AUTHORIZE_CODE_PKCE_VERIFIER + ":" + state
            try {
                let verifier = sessionStorage.getItem(verifierKey)
                // 根据你后端的接口调整参数
                const res = await runAsync(code, state, verifier, clientName)
                if (res.callbackStatus === 'LOGIN') {
                    const tokenResponse = res.tokenResponse
                    processLoginSuccess(tokenResponse, res.targetUrl)
                    return
                }

                if (res.callbackStatus === 'BIND') {
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
            } finally {
                sessionStorage.removeItem(verifierKey)
            }
        }
        handleAuth(code, state, clientName)
    }, [code, state, clientName])

    return (
        <Loading fullscreen tip="正在完成认证..." />
    )
}

export default OAuth2Callback;