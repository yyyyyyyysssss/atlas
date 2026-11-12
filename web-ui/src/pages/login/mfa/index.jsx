import React, { useState, useEffect, useRef, useMemo } from 'react';
import { Flex, Typography, ConfigProvider, App, Button } from 'antd';
import { ShieldAlert, ArrowLeft, KeyRound, Smartphone, Fingerprint, Grid3X3 } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { useRequest } from 'ahooks';
import { Navigate, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import useFullParams from '../../../hooks/useFullParams';
import { mfaLogin } from '../../../services/LoginService';
import './index.css';
import MfaVerify from './components/MfaVerify';

const { Title, Paragraph } = Typography;

const LoginMfa = () => {
    const { t } = useTranslation();
    const { ticket, preferredMfaType, availableMfaTypes = [] } = useFullParams()
    const navigate = useNavigate();

    if (!ticket) {
        return <Navigate to="/login" replace />;
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
                        <MfaVerify 
                            ticket={ticket}
                            preferredMfaType={preferredMfaType}
                            availableMfaTypes={availableMfaTypes}
                        />

                        {/* 交互操作底栏区 */}
                        <Flex vertical gap={12} align="center" style={{ marginTop: 24 }}>
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