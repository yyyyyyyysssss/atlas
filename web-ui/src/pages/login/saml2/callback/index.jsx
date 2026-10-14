import { App, Flex, Typography } from "antd"
import useFullParams from "../../../../hooks/useFullParams"
import { useNavigate } from "react-router-dom";
import { useEffect } from "react";
import Loading from "../../../../components/loading";
import { useRequest } from "ahooks";
import { restoreThirdPartyCallback } from "../../../../services/LoginService";
import useLoginHandler from "../../../../hooks/useLoginHandler";


const Saml2Callback = () => {

    const { providerName, ticket, error, error_description } = useFullParams()

    const navigate = useNavigate()

    const { message } = App.useApp()

    const { processLoginSuccess } = useLoginHandler()

    const { runAsync } = useRequest(restoreThirdPartyCallback, { manual: true })

    useEffect(() => {
        if (error) {
            navigate('/500', {
                state: {
                    title: error,
                    subTitle: error_description,
                }
            })
            return
        }
        if (!ticket) {
            message.error('认证凭证缺失')
            navigate('/login')
            return
        }

        const handleSignin = async (ticket) => {
            const res = await runAsync(ticket)
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
        }

        handleSignin(ticket)
    }, [ticket, error])

    return <Loading fullscreen tip="正在完成 SAML2 认证..." />
}

export default Saml2Callback