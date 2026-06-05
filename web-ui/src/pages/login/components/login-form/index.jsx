import { Form, Input, Button, Card, Flex, Typography, App, Avatar, Divider, Dropdown, ConfigProvider, QRCode, theme } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined, GithubOutlined, GoogleOutlined, KeyOutlined, ScanOutlined, CheckCircleFilled } from '@ant-design/icons';
import { QrCode, Monitor, Fingerprint } from 'lucide-react';
import { useAuth } from '../../../../router/AuthProvider';
import { useRequest } from 'ahooks';
import { AUTHORIZE_CODE_PKCE_VERIFIER, fetchAuthorizeUrl } from '../../../../services/Oauth2Service';
import { useNavigate } from 'react-router-dom';
import { useRedirect } from '../../../../hooks/useRedirect';
import { useTranslation } from 'react-i18next';
import useFullParams from '../../../../hooks/useFullParams';
import { useEffect, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { generateChallenge, generateVerifier } from '../../../../utils/pkce';
import UniversalPasskeyVerifier from '../../../account-settings/components/verifiers/UniversalPasskeyVerifier';
import PasswordLogin from './PasswordLogin';
import CaptchaLogin from './CaptchaLogin';
import MagicLinkLogin from './MagicLinkLogin';
import PasskeyLogin from './PasskeyLogin';

const LoginForm = ({ setIsQrLogin, loginSuccessHandler }) => {

    const { t } = useTranslation()

    const { token } = theme.useToken()

    const isWebAuthnSupported = window.PublicKeyCredential !== undefined && typeof window.PublicKeyCredential === 'function';

    const { runAsync: getAuthorizeUrlAsync, loading: getAuthorizeUrlLoading } = useRequest(fetchAuthorizeUrl, {
        manual: true
    })

    const [loginMethod, setLoginMethod] = useState("1")

    const authorizeCodeLogin = async (clientName) => {
        const authorizeUrl = await getAuthorizeUrlAsync(clientName)
        const verifier = generateVerifier()
        const challenge = await generateChallenge(verifier)
        sessionStorage.setItem(AUTHORIZE_CODE_PKCE_VERIFIER, verifier)
        const finalUrl = authorizeUrl + `&code_challenge=${challenge}&code_challenge_method=S256`
        // 授权码模式跳转逻辑
        window.location.href = finalUrl
    }

    return (
        <Card
            style={{
                width: '100%',
                borderRadius: '24px',
                boxShadow: '0 25px 50px -12px rgba(0,0,0,0.05)',
                border: 'none',
                background: '#ffffff',
                padding: '16px',
                backfaceVisibility: 'hidden', // 翻转时隐藏背面
                position: 'relative'
            }}
        >
            {/* 右上角切换扫码图标 (折角图片) */}
            <div
                style={{
                    position: 'absolute',
                    top: 16,
                    right: 16,
                    cursor: 'pointer',
                    zIndex: 10,
                    transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                }}
                className="qr-toggle-btn"
                onClick={() => setIsQrLogin(true)}
                title="扫码登录"
                onMouseEnter={(e) => {
                    e.currentTarget.style.transform = 'scale(1.1)';
                    e.currentTarget.style.opacity = '0.8';
                }}
                onMouseLeave={(e) => {
                    e.currentTarget.style.transform = 'scale(1)';
                    e.currentTarget.style.opacity = '1';
                }}
            >
                <QrCode
                    size={32}
                    color="#9ca3af"
                    strokeWidth={1.5}
                />
            </div>

            <Flex vertical align="center" style={{ marginBottom: 32 }}>
                <Avatar src={'/logo128_eclipse.svg'} size={80} style={{ background: 'transparent' }} />
                <Typography.Title level={2} style={{ margin: '24px 0 8px 0', fontWeight: 700, color: '#111827', letterSpacing: '-0.02em' }}>
                    {t('登录 Atlas')}
                </Typography.Title>
                <Typography.Text style={{ fontSize: 15, color: '#6b7280' }}>
                    {t('欢迎回来，请登录以继续')}
                </Typography.Text>
            </Flex>

            <AnimatePresence mode="wait">
                {loginMethod === '1' && <PasswordLogin onSuccess={loginSuccessHandler} />}
                {loginMethod === '2' && <CaptchaLogin onSuccess={loginSuccessHandler} />}
                {loginMethod === '3' && <MagicLinkLogin onSuccess={loginSuccessHandler} />}
                {loginMethod === '4' && <PasskeyLogin onSuccess={loginSuccessHandler} />}
            </AnimatePresence>

            <Flex justify="end" align="center" style={{ marginBottom: 32, marginTop: 8 }}>
                {loginMethod === '1' ? (
                    <Dropdown
                        menu={{
                            items: [
                                {
                                    key: '2',
                                    label: '验证码登录',
                                    align: 'center',
                                    onClick: () => setLoginMethod('2')
                                },
                                {
                                    key: '3',
                                    label: '免密链登录',
                                    align: 'center',
                                    onClick: () => setLoginMethod('3')
                                }
                            ]
                        }}
                        placement="bottomLeft"
                        trigger={['click']}
                    >
                        <Typography.Link style={{ fontSize: 14, color: '#6b7280' }}>
                            {t('更多登录方式 ▾')}
                        </Typography.Link>
                    </Dropdown>
                ) : (
                    <Typography.Link
                        onClick={() => setLoginMethod('1')}
                        style={{ fontSize: 14, color: '#6b7280' }}
                    >
                        {t('返回密码登录')}
                    </Typography.Link>
                )}


            </Flex>

            <Divider plain>
                <span style={{ color: '#9ca3af', fontSize: 12, fontWeight: 500, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                    {t('其他方式')}
                </span>
            </Divider>

            <Flex justify="center" align="center" gap={32} style={{ marginBottom: 8, marginTop: 24 }}>
                <GithubOutlined
                    style={{ fontSize: 24, color: '#374151', cursor: 'pointer', transition: 'all 0.2s ease' }}
                    onMouseEnter={(e) => { e.currentTarget.style.transform = 'translateY(-3px)'; e.currentTarget.style.color = '#111827'; }}
                    onMouseLeave={(e) => { e.currentTarget.style.transform = 'translateY(0)'; e.currentTarget.style.color = '#374151'; }}
                    onClick={() => authorizeCodeLogin('gitHub')}
                    title="GitHub"
                />
                <Avatar
                    src="/logo128.png"
                    size={28}
                    style={{
                        cursor: 'pointer',
                        transition: 'transform 0.2s ease',
                        background: 'transparent',
                        filter: 'grayscale(20%)'
                    }}
                    onMouseEnter={(e) => { e.currentTarget.style.transform = 'translateY(-3px)'; e.currentTarget.style.filter = 'grayscale(0%)'; }}
                    onMouseLeave={(e) => { e.currentTarget.style.transform = 'translateY(0)'; e.currentTarget.style.filter = 'grayscale(20%)'; }}
                    onClick={() => authorizeCodeLogin('atlas')}
                />
                <GoogleOutlined
                    style={{ fontSize: 24, color: '#6b7280', cursor: 'pointer', transition: 'all 0.2s ease' }}
                    onMouseEnter={(e) => { e.currentTarget.style.transform = 'translateY(-3px)'; e.currentTarget.style.color = '#EA4335'; }}
                    onMouseLeave={(e) => { e.currentTarget.style.transform = 'translateY(0)'; e.currentTarget.style.color = '#6b7280'; }}
                    onClick={() => authorizeCodeLogin('google')}
                    title="Google"
                />
            </Flex>
            {isWebAuthnSupported && (
                <Flex justify="center" style={{ marginTop: 24, marginBottom: 8 }}>
                    <Typography.Link
                        onClick={() => setLoginMethod('4')}
                        style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: '6px',
                            fontSize: 14,
                            fontWeight: 500,
                            padding: '6px 12px',
                            borderRadius: token.borderRadiusSM,
                            color: token.colorPrimary,                   // 🎯 字体和图标直接使用系统主色
                            transition: 'all 0.2s ease',

                        }}
                        onMouseEnter={(e) => {
                            e.currentTarget.style.transform = 'translateY(-1px)';
                        }}
                        onMouseLeave={(e) => {
                            e.currentTarget.style.transform = 'translateY(0)';
                        }}
                    >
                        <Fingerprint
                            size={16}
                            strokeWidth={2.5}
                            style={{
                                animation: 'pulse 2s infinite',
                                color: token.colorPrimary
                            }}
                        />
                        <span>{t('使用 指纹 / 面容 快捷登录')}</span>
                    </Typography.Link>
                </Flex>
            )}
        </Card>
    )
}

export default LoginForm