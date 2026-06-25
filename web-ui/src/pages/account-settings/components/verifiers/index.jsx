import React, { useEffect, useState } from 'react';
import { Dropdown, Button, Space, Flex, theme, Empty } from 'antd';
import { KeyOutlined, MailOutlined, DownOutlined, MobileOutlined, FileProtectOutlined, NodeIndexOutlined } from '@ant-design/icons';
import { motion, AnimatePresence } from 'framer-motion';

import UniversalPasswordVerifier from './UniversalPasswordVerifier';
import UniversalCaptchaVerifier from './UniversalCaptchaVerifier';
import { sendCaptcha } from '../../../../services/LoginService';
import { useRequest } from 'ahooks';
import { verifyCaptcha, verifyGesture, verifyPassword, verifyTotp, verifyWeb3Wallet, verifyWebauthn } from '../../../../services/AccountService';
import UniversalPasskeyVerifier from './UniversalPasskeyVerifier';
import { Fingerprint, Grid3x3, Grip, Wallet } from 'lucide-react';
import UniversalTotpVerifier from './UniversalTotpVerifier';
import UniversalGestureVerifier from './UniversalGestureVerifier';
import UniversalWeb3WalletVerifier from './UniversalWeb3WalletVerifier';
import { useAccount } from 'wagmi';


const VerifyDropdown = ({
    verifierRef,
    context,
    scene,
    captchaScene = 'default',
    value,
    onChange,
    onLoadingChange,
    onSuccess
}) => {
    const { token } = theme.useToken();

    const { passwordSet, boundEmail, passkeyEnabled, passkeys, totpEnabled, web3Enabled, gestureEnabled } = context || {}

    const isWebAuthnSupported = window.PublicKeyCredential !== undefined && typeof window.PublicKeyCredential === 'function';

    const hasPasskey = isWebAuthnSupported && (passkeyEnabled || (passkeys && passkeys.length > 0))

    const { runAsync: sendCaptchaAsync, cancel: cancelSend } = useRequest(sendCaptcha, { manual: true });

    const { runAsync: verifyCaptchaAsync, loading: verifyCaptchaLoading, cancel: cancelVerifyCaptcha } = useRequest(verifyCaptcha, {
        manual: true
    })

    const { runAsync: verifyPasswordAsync, loading: verifyPasswordLoading, cancel: cancelVerifyPassword } = useRequest(verifyPassword, { manual: true })


    const { runAsync: verifyWebauthnAsync, loading: verifyWebauthnLoading, cancel: cancelVerifyWebauthn } = useRequest(verifyWebauthn, {
        manual: true
    });

    const { runAsync: verifyTotpAsync, loading: verifyTotpLoading, cancel: cancelVerifyTotp } = useRequest(verifyTotp, {
        manual: true
    });

    const { runAsync: verifyGestureAsync, loading: verifyGestureLoading, cancel: cancelVerifyGesture } = useRequest(verifyGesture, {
        manual: true
    })

    const { runAsync: verifyWeb3WalletAsync, loading: verifyWeb3WalletLoading, cancel: cancelVerifyWeb3Wallet } = useRequest(verifyWeb3Wallet, {
        manual: true
    });

    // 构建下拉菜单的项
    const availableMethods = []

    // 通行密钥验证选项
    if (hasPasskey) {
        availableMethods.push({
            key: 'passkey',
            label: '密钥认证',
            icon: <Fingerprint style={{ width: 14, height: 14 }} />,
            render: () => (
                <UniversalPasskeyVerifier
                    verifierRef={verifierRef}
                    // 触发挥手硬件后，回调后端的验证接口
                    onVerifyAction={(webauthnId, credentialJson) => verifyWebauthnAsync(webauthnId, credentialJson, scene)}
                    onSuccess={onSuccess}
                />
            )
        });
    }

    // web3钱包
    if (web3Enabled) {
        availableMethods.push({
            key: 'web3-wallet',
            label: 'Web3钱包',
            icon: <Wallet style={{ width: 14, height: 14 }} />,
            render: () => (
                <UniversalWeb3WalletVerifier
                    verifierRef={verifierRef}
                    // 触发挥手硬件后，回调后端的验证接口
                    onVerifyAction={(signature, web3Id) => verifyWeb3WalletAsync({ signature: signature, web3Id: web3Id, securityScene: scene })}
                    onSuccess={onSuccess}
                />
            )
        })
    }

    // totp
    if (totpEnabled) {
        availableMethods.push({
            key: 'totp',
            label: 'TOTP认证',
            icon: <MobileOutlined />,
            render: () => (
                <UniversalTotpVerifier
                    verifierRef={verifierRef}
                    onVerifyAction={(code) => verifyTotpAsync({ code: code, securityScene: scene })}
                    onSuccess={onSuccess}
                />
            )
        });
    }

    // 手势验证选项
    if (gestureEnabled) {
        availableMethods.push({
            key: 'gesture',
            label: '手势认证',
            icon: <Grid3x3 style={{ width: 14, height: 14 }} />,
            render: () => (
                <UniversalGestureVerifier
                    verifierRef={verifierRef}
                    // 触发挥手硬件后，回调后端的验证接口
                    onVerifyAction={(gestureSequence) => verifyGestureAsync({ gesture: gestureSequence, securityScene: scene })}
                    onSuccess={onSuccess}
                />
            )
        });
    }


    // 密码验证选项
    if (passwordSet) {
        availableMethods.push({
            key: 'password',
            label: '密码认证',
            icon: <KeyOutlined />,
            // 直接把渲染函数挂在这里
            render: () => (
                <UniversalPasswordVerifier
                    verifierRef={verifierRef}
                    onVerifyAction={(pwd) => verifyPasswordAsync({ password: pwd, securityScene: scene })}
                    onSuccess={onSuccess}
                />
            )
        });
    }


    // 邮箱验证选项
    if (boundEmail) {
        availableMethods.push({
            key: 'captcha',
            label: '邮箱认证',
            icon: <MailOutlined />,
            render: () => (
                <UniversalCaptchaVerifier
                    verifierRef={verifierRef}
                    target={boundEmail}
                    targetLabel="安全验证接收邮箱"
                    codeLabel="安全验证码"
                    onSendAction={() => sendCaptchaAsync({ target: boundEmail, captchaType: 'email', captchaScene: captchaScene })}
                    onVerifyAction={(code) => verifyCaptchaAsync({ target: boundEmail, captchaType: 'email', captchaScene: captchaScene, securityScene: scene, code })}
                    onSuccess={onSuccess}
                />
            )
        });
    }

    const [internalMethod, setInternalMethod] = useState(availableMethods[0]?.key);

    const verifyMethod = value || internalMethod

    useEffect(() => {
        return () => {
            cancelSend()
            cancelVerifyCaptcha()
            cancelVerifyPassword()
            cancelVerifyWebauthn()
            cancelVerifyTotp()
            cancelVerifyGesture()
            cancelVerifyWeb3Wallet()
        }
    }, [verifyMethod])

    const isComponentLoading = verifyCaptchaLoading || verifyPasswordLoading || verifyWebauthnLoading || verifyTotpLoading || verifyGestureLoading || verifyWeb3WalletLoading;

    // 🎯 实时将加载状态吐给父组件
    useEffect(() => {
        if (onLoadingChange) {
            onLoadingChange(isComponentLoading)
        }
    }, [isComponentLoading, onLoadingChange])

    const handleMethodChange = (key) => {
        if (onChange) {
            onChange(key)
        } else {
            setInternalMethod(key)
        }
    }

    const currentMethod = availableMethods.find(m => m.key === verifyMethod);

    if (availableMethods.length === 0) {
        return <Empty
            image={Empty.PRESENTED_IMAGE_SIMPLE}
            description={
                <div style={{ color: token.colorTextDescription }}>
                    <p style={{ fontSize: 12, margin: 0 }}>为了您的账号安全，请先前往安全中心绑定邮箱或设置密码。</p>
                </div>
            }
        >
        </Empty>
    }

    return (
        <AnimatePresence mode="wait">
            <motion.div
                key={verifyMethod} // 切换验证方式时触发平滑动画
                initial={{ opacity: 0, y: -5 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, y: 5 }}
                transition={{ duration: 0.15 }}
            >
                {currentMethod?.render()}

                {availableMethods.length > 1 && (
                    <Flex justify="flex-end" style={{ marginTop: 16 }}>
                        <Dropdown
                            menu={{
                                items: availableMethods.map(m => ({
                                    key: m.key,
                                    label: m.label,
                                    icon: m.icon,
                                    disabled: m.key === verifyMethod,
                                })),
                                onClick: ({ key }) => handleMethodChange(key)
                            }}
                            trigger={['click']}
                        >
                            <Button
                                type="link"
                                size="small"
                                style={{
                                    color: token.colorTextDescription,
                                    fontSize: 13,
                                    padding: '0 4px'
                                }}
                            >
                                <Space size={4}>
                                    {currentMethod?.icon}
                                    {currentMethod?.label}
                                    <DownOutlined style={{ fontSize: 10 }} />
                                </Space>
                            </Button>
                        </Dropdown>
                    </Flex>
                )}
            </motion.div>
        </AnimatePresence>
    );
};

export default VerifyDropdown;