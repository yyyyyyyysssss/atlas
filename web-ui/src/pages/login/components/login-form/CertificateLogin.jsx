import { App, Typography } from "antd"
import { useTranslation } from "react-i18next"
import { AnimatePresence, motion } from 'framer-motion';
import { useCallback, useRef, useState } from "react";
import UniversalCertificateVerifier from "../../../account-settings/components/verifiers/UniversalCertificateVerifier";
import { useRequest } from "ahooks";

const CertificateLogin = ({ onSuccess }) => {

    const { t } = useTranslation()

    const { message } = App.useApp()

    const quickCertificateRef = useRef(null)

    const [verified, setVerified] = useState(false)

    const {
        runAsync: doCertificateAuthenticate,
        loading: verifyLoading
    } = useRequest(
        async () => {
            const response = await fetch(`${import.meta.env.VITE_API_CERT_URL}/api/auth/login/certificate?clientType=WEB`,
                {
                    method: 'POST',
                    credentials: 'include',
                    headers: {
                        'Accept': 'application/json'
                    },
                    cache: 'no-store',
                    redirect: 'error'
                }
            );
            let result;
            try {
                result = await response.json()
            } catch {
                throw new Error('服务器返回了无效的认证响应。')
            }

            if (!response.ok) {
                throw new Error(result?.message || `证书认证失败（HTTP ${response.status}）`)
            }

            if (result?.success === false) {
                throw new Error(result?.message || '证书认证失败，请重试。')
            }

            return result?.data ?? result;
        },
        {
            manual: true,

            onSuccess: (result) => {
                setVerified(true)
                onSuccess?.(result)
            },

            onError: (error) => {
                setVerified(false);
                if (error instanceof TypeError) {
                    message.error('证书认证请求失败，请检查网络或客户端证书配置。')
                    return
                }
                message.error(error?.message || '证书认证失败，请稍后重试。')
            }
        }
    );

    const doLogin = useCallback(() => {
        if (verifyLoading) {
            return
        }

        setVerified(false)

        return doCertificateAuthenticate()
    }, [verifyLoading, doCertificateAuthenticate])

    return (
        <motion.div
            key="certificate-login"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            transition={{ duration: 0.2 }}
            onAnimationComplete={async () => {
                await quickCertificateRef.current.onVerify()
            }}
        >
            <div style={{ marginBottom: 24 }}>
                <UniversalCertificateVerifier
                    verifierRef={quickCertificateRef}
                    onVerifyAction={doLogin}
                    verifyLoading={verifyLoading}
                    verified={verified}
                    label=""
                />
            </div>
        </motion.div>
    )
}

export default CertificateLogin