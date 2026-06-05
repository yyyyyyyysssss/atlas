import { Form, Input, Button, Card, Flex, Typography, App, Avatar, Divider, Dropdown, ConfigProvider, QRCode, theme } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined, GithubOutlined, GoogleOutlined, KeyOutlined, ScanOutlined, CheckCircleFilled } from '@ant-design/icons';
import { useRequest } from 'ahooks';
import { AnimatePresence, motion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { useEffect, useRef, useState } from 'react';
import UniversalPasskeyVerifier from '../../../account-settings/components/verifiers/UniversalPasskeyVerifier';
import { webauthnLogin } from '../../../../services/LoginService';



const PasskeyLogin = ({ onSuccess }) => {

    const { t } = useTranslation()

    const [form] = Form.useForm()

    const { message } = App.useApp()

    const quickPasskeyRef = useRef(null)

    const { runAsync: webauthnLoginAsync, loading: webauthnLoginLoading } = useRequest(webauthnLogin, {
        manual: true
    })

    const passkeyLogin = async (webauthnId, credentialJson) => {
        try {
            return await webauthnLoginAsync(webauthnId, {
                clientType: 'WEB',
                webauthnAuthenticationRequest: credentialJson
            })
        } catch (error) {
            if (error.response && error.response.status === 401) {
                if (error.response.data && error.response.data.code === 2201) {
                    message.error('账号已锁定，请联系系统管理员')
                } else {
                    message.warning('验证失败。该设备密钥可能尚未绑定，或不属于当前系统。请先使用密码或验证码登录，成功后前往[安全设置]完成绑定。', 5)
                }

            }
        }

    }

    return (
        <motion.div
            key="passkey-login"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            transition={{ duration: 0.2 }}
            onAnimationComplete={async () => {
                const res = await quickPasskeyRef.current.onVerify()
                onSuccess(res)
            }}
        >
            <div style={{ marginBottom: 24 }}>
                <UniversalPasskeyVerifier
                    verifierRef={quickPasskeyRef}
                    onVerifyAction={passkeyLogin} // 👈 绑定你后端的 Passkey 登录服务
                    onSuccess={onSuccess} // 👈 成了就走统一成功处理器
                    label=""
                />
            </div>
        </motion.div>
    )
}

export default PasskeyLogin