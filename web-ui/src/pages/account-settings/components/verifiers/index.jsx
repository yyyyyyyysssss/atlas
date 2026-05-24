import React, { useEffect, useState } from 'react';
import { Dropdown, Button, Space, Flex, theme, Empty } from 'antd';
import { KeyOutlined, MailOutlined, DownOutlined } from '@ant-design/icons';
import { motion, AnimatePresence } from 'framer-motion';

import UniversalPasswordVerifier from './UniversalPasswordVerifier';
import UniversalCaptchaVerifier from './UniversalCaptchaVerifier';
import { sendCaptcha } from '../../../../services/LoginService';
import { useRequest } from 'ahooks';
import { verifyCaptcha, verifyPassword } from '../../../../services/UserProfileService';


const VerifyDropdown = ({
    verifierRef,
    context,
    scene,
    value,
    onChange,
    onLoadingChange
}) => {
    const { token } = theme.useToken();

    const { passwordSet, boundEmail } = context || {}

    const { runAsync: sendCaptchaAsync, cancel: cancelSend } = useRequest(sendCaptcha, { manual: true });

    const { runAsync: verifyCaptchaAsync, loading: verifyCaptchaLoading, cancel: cancelVerifyCaptcha } = useRequest(verifyCaptcha, {
        manual: true
    })

    const { runAsync: verifyPasswordAsync, loading: verifyPasswordLoading, cancel: cancelVerifyPassword } = useRequest(verifyPassword, { manual: true })

    // 构建下拉菜单的项
    const availableMethods = []
    if (passwordSet) {
        availableMethods.push({
            key: 'password',
            label: '密码验证',
            icon: <KeyOutlined />,
            // 直接把渲染函数挂在这里
            render: () => (
                <UniversalPasswordVerifier
                    verifierRef={verifierRef}
                    onVerifyAction={(pwd) => verifyPasswordAsync({ password: pwd, securityScene: scene })}
                />
            )
        });
    }
    if (boundEmail) {
        availableMethods.push({
            key: 'captcha',
            label: '邮箱验证',
            icon: <MailOutlined />,
            render: () => (
                <UniversalCaptchaVerifier
                    verifierRef={verifierRef}
                    target={boundEmail}
                    targetLabel="安全验证接收邮箱"
                    codeLabel="安全验证码"
                    onSendAction={() => sendCaptchaAsync({ target: boundEmail, captchaType: 'email', securityScene: scene })}
                    onVerifyAction={(code) => verifyCaptchaAsync({ target: boundEmail, captchaType: 'email', securityScene: scene, code })}
                />
            )
        });
    }

    const [internalMethod, setInternalMethod] = useState(availableMethods[0]?.key);

    const verifyMethod = value !== undefined ? value : internalMethod;


    useEffect(() => {
        return () => {
            cancelSend()
            cancelVerifyCaptcha()
            cancelVerifyPassword()
        }
    }, [verifyMethod])

    const isComponentLoading = verifyCaptchaLoading || verifyPasswordLoading;

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
        return <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="暂无可用身份验证方式" />;
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