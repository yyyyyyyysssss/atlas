import React, { useEffect, useState } from 'react';
import { Dropdown, Button, Space, Flex, theme } from 'antd';
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
    onLoadingChange
}) => {
    const { token } = theme.useToken();

    const { passwordSet, boundEmail } = context || {}

    const { runAsync: sendCaptchaAsync } = useRequest(sendCaptcha, { manual: true });

    const { runAsync: verifyCaptchaAsync, loading: verifyCaptchaLoading } = useRequest(verifyCaptcha, {
        manual: true
    })

    const { runAsync: verifyPasswordAsync, loading: verifyPasswordLoading } = useRequest(verifyPassword, { manual: true })

    // 1. 初始化默认的验证方式：优先密码，没密码用验证码
    const [verifyMethod, setVerifyMethod] = useState(passwordSet ? 'password' : 'captcha');

    const isComponentLoading = verifyCaptchaLoading || verifyPasswordLoading;

    // 🎯 实时将加载状态吐给父组件
    useEffect(() => {
        if (onLoadingChange) {
            onLoadingChange(isComponentLoading)
        }
    }, [isComponentLoading, onLoadingChange])

    // 2. 构建下拉菜单的项
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

    const currentMethod = availableMethods.find(m => m.key === verifyMethod);

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
                                onClick: ({ key }) => setVerifyMethod(key)
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