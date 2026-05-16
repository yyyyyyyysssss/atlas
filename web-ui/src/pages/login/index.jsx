import React, { useEffect, useRef, useState } from 'react';
import { Form, Input, Button, Card, Flex, Typography, App, Avatar, Divider, Dropdown, ConfigProvider, QRCode } from 'antd';
import './index.css'
import { useRequest } from 'ahooks';
import { useAuth } from '../../router/AuthProvider';
import { ottLogin } from '../../services/LoginService';
import { useTranslation } from 'react-i18next'
import { useNavigate, useSearchParams } from 'react-router-dom';
import httpWrapper from '../../services/AxiosWrapper';
import { useRedirect } from '../../hooks/useRedirect';
import { AnimatePresence, motion } from 'framer-motion';
import useFullParams from '../../hooks/useFullParams';
import Loading from '../../components/loading';
import LoginFrom from './components/LoginForm';
import QrLoginCard from './components/QrLoginCard';

const Login = () => {

    const { t } = useTranslation()

    const { signin } = useAuth()

    const [form] = Form.useForm()

    const { message } = App.useApp()

    const navigate = useNavigate()
    
    const { ottToken, targetUrl } = useFullParams()

    const redirect = useRedirect()


    const { runAsync: ottLoginAsync, loading: ottLoginLoading } = useRequest(ottLogin, {
        manual: true
    })

    // 是否显示扫码登录 (翻转状态)
    const [isQrLogin, setIsQrLogin] = useState(false);

    const hasHandledOttRef = useRef(false) // 用于防止 StrictMode 下执行两次

    useEffect(() => {
        if (ottToken && !hasHandledOttRef.current) {
            hasHandledOttRef.current = true
            handleOttLogin(ottToken)
        }
    }, [ottToken])

    const handleOttLogin = async (token) => {
        try {
            const data = await ottLoginAsync(token)
            loginSuccessHandler(data)
        } catch (error) {
            message.error('快捷登录链接已失效或无效，请重新登录')
            // 登录失败后清理 URL 中的 token，防止刷新再次触发
            navigate('/login', { replace: true })
            hasHandledOttRef.current = false
        }
    }

    const loginSuccessHandler = async (data) => {
        await signin(data)
        redirect('/', data?.access?.token)
    }

    if (ottLoginLoading) {
        return <Loading fullscreen tip="正在通过快捷链接登录..." />
    }

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
                        controlHeightLG: 48,
                        fontWeight: 600,
                    },
                    Tabs: {
                        itemColor: '#9ca3af',
                        itemSelectedColor: '#1f2937',
                        itemHoverColor: '#4f46e5',
                        titleFontSize: 16,
                        horizontalMargin: '0 0 24px 0',
                    }
                }
            }}
        >
            <div style={{
                minHeight: '100vh',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                background: '#f3f4f6', // 非常干净的浅灰底色
                padding: '20px',
                position: 'relative',
                overflow: 'hidden'
            }}>
                {/* 极简现代的几何背景装饰 */}
                <div style={{
                    position: 'absolute', top: '-20%', left: '-10%', width: '60vw', height: '60vw',
                    background: 'linear-gradient(135deg, rgba(79,70,229,0.08) 0%, rgba(255,255,255,0) 60%)', borderRadius: '50%'
                }} />
                <div style={{
                    position: 'absolute', bottom: '-20%', right: '-10%', width: '50vw', height: '50vw',
                    background: 'linear-gradient(135deg, rgba(236,72,153,0.05) 0%, rgba(255,255,255,0) 60%)', borderRadius: '50%'
                }} />

                <motion.div
                    animate={{ rotateY: isQrLogin ? 180 : 0 }}
                    transition={{ duration: 0.6, type: 'spring', stiffness: 260, damping: 20 }}
                    style={{
                        width: '100%',
                        maxWidth: '440px',
                        transformStyle: 'preserve-3d',
                        perspective: '1000px',
                        position: 'relative'
                    }}
                >
                    {/* 密码/表单登录面 (Front) */}
                    <LoginFrom
                        setIsQrLogin={setIsQrLogin}
                        loginSuccessHandler={loginSuccessHandler}
                    />

                    {/* 扫码登录面 (Back) */}
                    <QrLoginCard
                        isQrLogin={isQrLogin}
                        setIsQrLogin={setIsQrLogin}
                        loginSuccessHandler={loginSuccessHandler}
                    />
                </motion.div>
            </div>
        </ConfigProvider>
    )
}

export default Login