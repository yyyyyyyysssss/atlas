import React, { useState, useRef } from 'react';
import { Flex, Typography, ConfigProvider, App, Button } from 'antd';
import { ShieldAlert, ArrowLeft, KeyRound, Smartphone } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { useRequest } from 'ahooks';
import { useNavigate } from 'react-router-dom';
import useFullParams from '../../../hooks/useFullParams';
import { mfaLogin } from '../../../services/LoginService';

// 🔌 引入两个核心核验组件
import UniversalTotpVerifier from '../../account-settings/components/verifiers/UniversalTotpVerifier';
import './index.css';
import UniversalBackupCodeVerifier from '../../account-settings/components/verifiers/UniversalBackupCodeVerifier';
import { useAuth } from '../../../router/AuthProvider';
import { useRedirect } from '../../../hooks/useRedirect';

const { Title, Paragraph } = Typography;

const LoginMfa = () => {
    const { ticket, mfaType } = useFullParams()

    const { message } = App.useApp()

    const navigate = useNavigate()

    const { signin } = useAuth()

    const redirect = useRedirect()

    // 🔄 视图状态：'totp' (身份验证器) 或 'backup_code' (安全备份码)
    const [currentView, setCurrentView] = useState('totp');

    const { runAsync: mfaLoginAsync } = useRequest(mfaLogin, {
        manual: true
    });

    const verifierRef = useRef();

    // 🛡️ 统一的网络请求调用
    const doLogin = async (code) => {
        // 后端通常通过类型参数或备份码本身的特征（如带连字符）来区分业务
        // 如果后端需要明确的 mfaType，可以在切换视图时动态调整传参
        const finalMfaType = currentView === 'backup_code' ? 'BACKUP_CODE' : mfaType;

        return await mfaLoginAsync({
            ticket: ticket,
            mfaType: finalMfaType,
            code: code
        });
    };

    const loginSuccess = async (loginResponse) => {
        const { token } = loginResponse
        await signin(token)
        redirect('/', token.access.value)
    }

    const handleBackToLogin = () => {
        navigate('/login', { replace: true });
    };

    return (
        <ConfigProvider
            theme={{
                token: {
                    colorBgContainer: '#ffffff',
                    colorText: '#1f2937',
                    colorTextSecondary: '#6b7280',
                    colorBorder: '#e5e7eb',
                    colorBgElevated: '#ffffff',
                    controlItemBgHover: '#f3f4f6',
                    colorPrimary: '#4f46e5',
                    colorLink: '#4f46e5',
                    colorLinkHover: '#6366f1',
                    colorLinkActive: '#4338ca',
                    borderRadius: 12,
                    fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif",
                },
                components: {
                    Input: {
                        colorBgContainer: '#f9fafb',
                        colorText: '#1f2937',
                        colorTextPlaceholder: '#9ca3af',
                        controlHeightLG: 48,
                        colorBorder: 'transparent',
                        activeBorderColor: '#4f46e5',
                        hoverBorderColor: '#d1d5db',
                    },
                    Button: {
                        paddingInline: 0,
                    }
                }
            }}
        >
            <div style={{
                minHeight: '100vh',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                background: '#f3f4f6',
                padding: '20px',
                position: 'relative',
                overflow: 'hidden'
            }}>

                {/* 几何背景装饰 */}
                <div style={{
                    position: 'absolute', top: '-20%', left: '-10%', width: '60vw', height: '60vw',
                    background: 'linear-gradient(135deg, rgba(79,70,229,0.08) 0%, rgba(255,255,255,0) 60%)', borderRadius: '50%'
                }} />
                <div style={{
                    position: 'absolute', bottom: '-20%', right: '-10%', width: '50vw', height: '50vw',
                    background: 'linear-gradient(135deg, rgba(236,72,153,0.05) 0%, rgba(255,255,255,0) 60%)', borderRadius: '50%'
                }} />

                {/* 主卡片动画容器 */}
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.5, type: 'spring', stiffness: 200, damping: 22 }}
                    style={{
                        width: '100%',
                        maxWidth: '440px',
                        position: 'relative',
                        zIndex: 1
                    }}
                >
                    <div style={{
                        background: '#ffffff',
                        padding: '40px 32px 32px 32px',
                        borderRadius: '16px',
                        boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.05), 0 10px 10px -5px rgba(0, 0, 0, 0.01)',
                        border: '1px solid #f3f4f6'
                    }}>

                        {/* 头部区域：根据视图动态渲染 Icon 和文本 */}
                        <Flex vertical align="center" style={{ marginBottom: 32, textAlign: 'center' }}>
                            <div style={{
                                width: '56px',
                                height: '56px',
                                borderRadius: '14px',
                                background: 'rgba(79, 70, 229, 0.06)',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                color: '#4f46e5',
                                marginBottom: 16
                            }}>
                                {currentView === 'totp' ? (
                                    <ShieldAlert size={26} strokeWidth={2} />
                                ) : (
                                    <KeyRound size={26} strokeWidth={2} />
                                )}
                            </div>

                            <Title level={3} style={{
                                margin: 0,
                                fontSize: '24px',
                                fontWeight: 700,
                                color: '#1f2937',
                                letterSpacing: '-0.025em'
                            }}>
                                {currentView === 'totp' ? '安全两步验证' : '使用备份码验证'}
                            </Title>

                            <Paragraph style={{
                                color: '#6b7280',
                                fontSize: '14px',
                                marginTop: 8,
                                marginBottom: 0,
                                padding: '0 8px'
                            }}>
                                {currentView === 'totp'
                                    ? '您的账户已开启两步验证保护，请输入身份验证器生成的 6 位核验码。'
                                    : '请输入您保存的 10 位安全备份码。每个备份码只能使用一次。'
                                }
                            </Paragraph>
                        </Flex>

                        {/* 核心业务组件：利用 AnimatePresence 保持切换时的丝滑感 */}
                        <div style={{ position: 'relative', overflow: 'hidden', minHeight: '86px' }}>
                            <AnimatePresence mode="wait">
                                {currentView === 'totp' ? (
                                    <motion.div
                                        key="totp-view"
                                        initial={{ opacity: 0, x: -15 }}
                                        animate={{ opacity: 1, x: 0 }}
                                        exit={{ opacity: 0, x: 15 }}
                                        transition={{ duration: 0.2 }}
                                    >
                                        <UniversalTotpVerifier
                                            verifierRef={verifierRef}
                                            onVerifyAction={(code) => doLogin(code)}
                                            onSuccess={loginSuccess}
                                        />
                                    </motion.div>
                                ) : (
                                    <motion.div
                                        key="backup-view"
                                        initial={{ opacity: 0, x: 15 }}
                                        animate={{ opacity: 1, x: 0 }}
                                        exit={{ opacity: 0, x: -15 }}
                                        transition={{ duration: 0.2 }}
                                    >
                                        <UniversalBackupCodeVerifier
                                            verifierRef={verifierRef}
                                            codeLabel="安全备份码"
                                            onVerifyAction={(code) => doLogin(code)}
                                            onSuccess={loginSuccess}
                                        />
                                    </motion.div>
                                )}
                            </AnimatePresence>
                        </div>

                        {/* 交互操作区：动态切换入口 + 返回登录 */}
                        <Flex vertical gap={12} align="center" style={{ marginTop: 24 }}>
                            {currentView === 'totp' ? (
                                <Button
                                    type="link"
                                    size="small"
                                    onClick={() => setCurrentView('backup_code')}
                                    style={{ fontSize: '13px', fontWeight: 500, color: '#4f46e5' }}
                                >
                                    丢失手机？尝试使用安全备份码登录
                                </Button>
                            ) : (
                                <Button
                                    type="link"
                                    size="small"
                                    icon={<Smartphone size={14} style={{ marginRight: 4 }} />}
                                    onClick={() => setCurrentView('totp')}
                                    style={{ display: 'flex', alignItems: 'center', fontSize: '13px', fontWeight: 500, color: '#4f46e5' }}
                                >
                                    返回使用身份验证器码
                                </Button>
                            )}

                            <Button
                                type="link"
                                size="small"
                                onClick={handleBackToLogin}
                                style={{
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '6px',
                                    fontSize: '13px',
                                    fontWeight: 500,
                                    height: 'auto',
                                    color: '#6b7280',
                                }}
                                className="mfa-back-link"
                            >
                                <ArrowLeft size={14} />
                                返回登录页
                            </Button>
                        </Flex>
                    </div>
                </motion.div>
            </div>
        </ConfigProvider>
    );
};

export default LoginMfa;