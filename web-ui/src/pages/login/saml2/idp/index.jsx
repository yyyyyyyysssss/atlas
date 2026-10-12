import { App, Typography } from 'antd';
import './index.css'
import useFullParams from '../../../../hooks/useFullParams';
import Loading from '../../../../components/loading';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../../../router/AuthProvider';
import { useEffect } from 'react';
import { useRequest } from 'ahooks';
import { saml2IdpSso } from '../../../../services/Saml2Service';


function submitPostForm(data) {

    const form = document.createElement('form')

    form.method = 'POST'

    form.action = data.acsUrl

    const samlInput = document.createElement('input')

    samlInput.type = 'hidden'

    samlInput.name = 'SAMLResponse'

    samlInput.value = data.samlResponse

    form.appendChild(samlInput)

    if (data.relayState) {

        const relayInput = document.createElement('input')

        relayInput.type = 'hidden'

        relayInput.name = 'RelayState'

        relayInput.value = data.relayState

        form.appendChild(relayInput)
    }


    document.body.appendChild(form)

    form.submit()
}

const Saml2IdpSso = () => {

    const { samlIssuer, samlRequestId } = useFullParams()

    const { message } = App.useApp()

    const navigate = useNavigate()

    const { runAsync } = useRequest(saml2IdpSso, { manual: true })

    useEffect(() => {
        if (!samlIssuer || !samlRequestId) {
            navigate('/500', {
                state: {
                    title: 'SAML2 IDP认证异常',
                    subTitle: '缺少SAML认证参数',
                }
            })
            return
        }

        runAsync(samlIssuer, samlRequestId)
            .then(data => {
                if (data.binding === 'REDIRECT') {
                    window.location.href = data.redirectUrl
                    return
                }
                if (data.binding === 'POST') {
                    submitPostForm(data)
                    return
                }

                throw new Error(`不支持的SAML Binding: ${data.binding}`)

            })
            .catch(error => {
                navigate('/500', {
                    state: {
                        title: "SAML2 IDP认证异常",
                        subTitle: error.message,
                    }
                })
            })

    }, [samlIssuer, samlRequestId, navigate, runAsync])

    return <Loading fullscreen tip="正在完成 SAML2 认证..." />
}

export default Saml2IdpSso;