import React, { useState, useEffect, useRef, useMemo } from 'react';
import { Flex, Typography, ConfigProvider, App, Button } from 'antd';
import { ShieldAlert, ArrowLeft, KeyRound, Smartphone, Fingerprint, Grid3X3 } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { useRequest } from 'ahooks';
import { useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';

import useFullParams from '../../../hooks/useFullParams';
import { mfaLogin } from '../../../services/LoginService';
import { useAuth } from '../../../router/AuthProvider';
import { useRedirect } from '../../../hooks/useRedirect';

// 🔌 引入具体策略核验组件
import UniversalTotpVerifier from '../../account-settings/components/verifiers/UniversalTotpVerifier';
import UniversalBackupCodeVerifier from '../../account-settings/components/verifiers/UniversalBackupCodeVerifier';
import './index.css';
import UniversalGestureVerifier from '../../account-settings/components/verifiers/UniversalGestureVerifier';

const { Title, Paragraph } = Typography;

const LoginMfa = () => {
    const { t } = useTranslation();
    const { ticket, mfaType: initialMfaType, activeMfaStrategies = [] } = useFullParams()
    const navigate = useNavigate();
    const { signin } = useAuth();
    const redirect = useRedirect();

    // 🔄 当前激活的 MFA 核验类型（支持动态降级切换，如切换到备份码）
    const [activeMfaType, setActiveMfaType] = useState(initialMfaType || 'TOTP')

    // 当 URL 参数变化时，同步更新状态
    useEffect(() => {
        if (initialMfaType) {
            setActiveMfaType(initialMfaType);
        }
    }, [initialMfaType]);

    const { runAsync: mfaLoginAsync } = useRequest(mfaLogin, { manual: true });
    const verifierRef = useRef();

    // 🛡️ 统一的网络请求调用
    const doLogin = async (code) => {
        return await mfaLoginAsync({
            ticket: ticket,
            mfaType: activeMfaType,
            code: code
        });
    };

    const loginSuccess = async (loginResponse) => {
        const { token } = loginResponse;
        await signin(token);
        redirect('/', token.access.value);
    };

    const handleBackToLogin = () => {
        navigate('/login', { replace: true });
    };

    const mfaStrategies = useMemo(() => {

        const hasBackupCode = activeMfaStrategies.includes('BACKUP_CODE')

        const allPool = {
            TOTP: {
                icon: <ShieldAlert size={26} strokeWidth={2} />,
                title: t('安全两步验证'),
                description: t('您的账户已开启两步验证保护，请输入身份验证器生成的 6 位核验码。'),
                component: (
                    <UniversalTotpVerifier
                        verifierRef={verifierRef}
                        onVerifyAction={doLogin}
                        onSuccess={loginSuccess}
                    />
                ),
                // 该策略下的辅助切换按钮
                actionButton: hasBackupCode && (
                    <Button
                        type="link"
                        size="small"
                        onClick={() => setActiveMfaType('BACKUP_CODE')}
                        style={{ fontSize: '13px', fontWeight: 500, color: '#4f46e5' }}
                    >
                        {t('丢失手机？尝试使用安全备份码登录')}
                    </Button>
                )
            },
            BACKUP_CODE: {
                icon: <KeyRound size={26} strokeWidth={2} />,
                title: t('使用备份码验证'),
                description: t('请输入您保存的 10 位安全备份码。每个备份码只能使用一次。'),
                component: (
                    <UniversalBackupCodeVerifier
                        verifierRef={verifierRef}
                        codeLabel={t('安全备份码')}
                        onVerifyAction={doLogin}
                        onSuccess={loginSuccess}
                    />
                ),
                // 如果初始不是备份码，允许用户切回去
                actionButton: initialMfaType !== 'BACKUP_CODE' && (
                    <Button
                        type="link"
                        size="small"
                        icon={<Smartphone size={14} style={{ marginRight: 4 }} />}
                        onClick={() => setActiveMfaType(initialMfaType)}
                        style={{ display: 'flex', alignItems: 'center', fontSize: '13px', fontWeight: 500, color: '#4f46e5' }}
                    >
                        {initialMfaType === 'TOTP' ? t('返回常规两步验证') : t('返回手势轨迹核验')}
                    </Button>
                )
            },
            GESTURE: {
                icon: <Grid3X3 size={26} strokeWidth={2} />,
                title: t('手势轨迹核验'),
                description: t('请输入您的九宫格手势快捷密码完成安全二次身份验证。'),
                component: (
                    <UniversalGestureVerifier
                        label=''
                        verifierRef={verifierRef}
                        onVerifyAction={doLogin}
                        onSuccess={loginSuccess}
                    />
                ),
                actionButton: hasBackupCode && (
                    <Button
                        type="link"
                        size="small"
                        onClick={() => setActiveMfaType('BACKUP_CODE')}
                        style={{ fontSize: '13px', fontWeight: 500, color: '#4f46e5' }}
                    >
                        {t('忘记手势？尝试使用安全备份码登录')}
                    </Button>
                )
            }
        }
        // 根据后端返回的可用列表，对策略表进行动态裁剪
        const filteredStrategies = {}
        activeMfaStrategies.forEach(key => {
            if (allPool[key]) {
                filteredStrategies[key] = allPool[key]
            }
        })
        return filteredStrategies
    }, [activeMfaStrategies, initialMfaType, t])

    // 获取当前处于激活状态的策略元数据，提供兜底保护
    const currentStrategy = mfaStrategies[activeMfaType] || mfaStrategies['TOTP'];

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
                    Button: { paddingInline: 0 }
                }
            }}
        >
            <div style={{
                minHeight: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center',
                background: '#f3f4f6', padding: '20px', position: 'relative', overflow: 'hidden'
            }}>
                {/* 几何背景装饰 */}
                <div style={{ position: 'absolute', top: '-20%', left: '-10%', width: '60vw', height: '60vw', background: 'linear-gradient(135deg, rgba(79,70,229,0.08) 0%, rgba(255,255,255,0) 60%)', borderRadius: '50%' }} />
                <div style={{ position: 'absolute', bottom: '-20%', right: '-10%', width: '50vw', height: '50vw', background: 'linear-gradient(135deg, rgba(236,72,153,0.05) 0%, rgba(255,255,255,0) 60%)', borderRadius: '50%' }} />

                {/* 主卡片动画容器 */}
                <motion.div
                    initial={{ opacity: 0, y: 20 }}
                    animate={{ opacity: 1, y: 0 }}
                    transition={{ duration: 0.5, type: 'spring', stiffness: 200, damping: 22 }}
                    style={{ width: '100%', maxWidth: '440px', position: 'relative', zIndex: 1 }}
                >
                    <div style={{ background: '#ffffff', padding: '40px 32px 32px 32px', borderRadius: '16px', boxShadow: '0 20px 25px -5px rgba(0, 0, 0, 0.05), 0 10px 10px -5px rgba(0, 0, 0, 0.01)', border: '1px solid #f3f4f6' }}>

                        {/* 🌟 丝滑切换外壳：只要 activeMfaType 变了，头部和内容整体联动切换动画 */}
                        <AnimatePresence mode="wait">
                            <motion.div
                                key={activeMfaType}
                                initial={{ opacity: 0, x: 15 }}
                                animate={{ opacity: 1, x: 0 }}
                                exit={{ opacity: 0, x: -15 }}
                                transition={{ duration: 0.2 }}
                            >
                                {/* 统一的头部 UI 结构 */}
                                <Flex vertical align="center" style={{ marginBottom: 32, textAlign: 'center' }}>
                                    <div style={{ width: '56px', height: '56px', borderRadius: '14px', background: 'rgba(79, 70, 229, 0.06)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#4f46e5', marginBottom: 16 }}>
                                        {currentStrategy.icon}
                                    </div>
                                    <Title level={3} style={{ margin: 0, fontSize: '24px', fontWeight: 700, color: '#1f2937', letterSpacing: '-0.025em' }}>
                                        {currentStrategy.title}
                                    </Title>
                                    <Paragraph style={{ color: '#6b7280', fontSize: '14px', marginTop: 8, marginBottom: 0, padding: '0 8px' }}>
                                        {currentStrategy.description}
                                    </Paragraph>
                                </Flex>

                                {/* 核心表单组件 */}
                                <div style={{ minHeight: '86px' }}>
                                    {currentStrategy.component}
                                </div>
                            </motion.div>
                        </AnimatePresence>

                        {/* 交互操作底栏区 */}
                        <Flex vertical gap={12} align="center" style={{ marginTop: 24 }}>
                            {/* 动态渲染当前策略的兜底切换按钮（如丢失手机） */}
                            {currentStrategy.actionButton}

                            {/* 绝对统一的底栏：返回登录页 */}
                            <Button
                                type="link"
                                size="small"
                                onClick={handleBackToLogin}
                                style={{ display: 'inline-flex', alignItems: 'center', gap: '4px', fontSize: '13px', fontWeight: 500, height: 'auto', padding: 0, color: '#6b7280', transition: 'all 0.2s ease' }}
                                onMouseEnter={(e) => {
                                    e.currentTarget.style.color = '#4f46e5';
                                    const icon = e.currentTarget.querySelector('.mfa-back-icon');
                                    if (icon) icon.style.transform = 'translateX(-3px)';
                                }}
                                onMouseLeave={(e) => {
                                    e.currentTarget.style.color = '#6b7280';
                                    const icon = e.currentTarget.querySelector('.mfa-back-icon');
                                    if (icon) icon.style.transform = 'translateX(0)';
                                }}
                            >
                                <div className="mfa-back-icon" style={{ display: 'flex', alignItems: 'center', transition: 'transform 0.2s ease' }}>
                                    <ArrowLeft size={14} strokeWidth={2.5} />
                                </div>
                                <span>{t('返回登录页')}</span>
                            </Button>
                        </Flex>

                    </div>
                </motion.div>
            </div>
        </ConfigProvider>
    );
};

export default LoginMfa;