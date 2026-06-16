import { App, Flex, Typography } from "antd"
import useFullParams from "../../../../hooks/useFullParams"
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../../../router/AuthProvider";
import { useRedirect } from "../../../../hooks/useRedirect";
import { useEffect } from "react";
import Loading from "../../../../components/loading";


const Saml2Callback = () => {

    const { providerName, accessToken, refreshToken, error, error_description } = useFullParams()

    const navigate = useNavigate()
    const { signin } = useAuth()
    const redirect = useRedirect()
    const { message } = App.useApp()

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
        if (!accessToken) {
            message.error('认证凭证缺失')
            navigate('/login')
            return
        }

        const handleSignin = async () => {
            const token = {
                access: { value: accessToken },
                refresh: { value: refreshToken }
            }
            await signin(token)
            redirect('/', accessToken)
        }

        handleSignin()
    }, [accessToken, refreshToken, error])

    return <Loading fullscreen tip="正在完成 SAML2 认证..." />
}

export default Saml2Callback