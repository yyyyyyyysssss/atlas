import { Form, Input, Button, Card, Flex, Typography, App, Avatar, Divider, Dropdown, ConfigProvider, QRCode, theme } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined, GithubOutlined, GoogleOutlined, KeyOutlined, ScanOutlined, CheckCircleFilled } from '@ant-design/icons';
import { useRequest } from 'ahooks';
import { AnimatePresence, motion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { useEffect, useRef, useState } from 'react';
import { web3WalletLogin } from '../../../../services/LoginService';
import UniversalWeb3WalletVerifier from '../../../account-settings/components/verifiers/UniversalWeb3WalletVerifier';

const Web3WalletLogin = ({ onSuccess }) => {

    const { t } = useTranslation()

    const { message } = App.useApp()

    const webWalletRef = useRef(null)

    const { runAsync: web3WalletLoginAsync, loading: web3WalletLoginLoading } = useRequest(web3WalletLogin, {
        manual: true
    })

    const web3Login = async (signature, web3Id) => {
        try {
            return await web3WalletLoginAsync({
                clientType: 'WEB',
                web3Id: web3Id,
                signature: signature
            })
        } catch (error) {
            if (error.response && error.response.status === 401) {
                if (error.response.data && error.response.data.code === 2201) {
                    message.error('账号已锁定，请联系系统管理员')
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
                const res = await webWalletRef.current.onVerify()
                if (res && onSuccess) {
                    onSuccess(res)
                }
            }}
        >
            <div style={{ marginBottom: 24 }}>
                <UniversalWeb3WalletVerifier
                    verifierRef={webWalletRef}
                    onVerifyAction={web3Login}
                    onConnected={async () => {
                        const res = await webWalletRef.current.onVerify()
                        if (res && onSuccess) {
                            onSuccess(res)
                        }
                    }}
                    onSuccess={(res) => {
                        if (res && onSuccess) {
                            onSuccess(res)
                        }
                    }}
                    label=""
                />
            </div>
        </motion.div>
    )
}

export default Web3WalletLogin