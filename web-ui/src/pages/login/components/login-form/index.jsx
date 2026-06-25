import { Form, Input, Button, Card, Flex, Typography, App, Avatar, Divider, Dropdown, ConfigProvider, QRCode, theme, Tooltip } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined, GithubOutlined, GoogleOutlined, KeyOutlined, ScanOutlined, CheckCircleFilled, ArrowLeftOutlined, NodeIndexOutlined, BorderInnerOutlined, SlidersOutlined, AlipayCircleOutlined } from '@ant-design/icons';
import { QrCode, Monitor, Fingerprint, Grid3X3, Grid3x3, Wallet } from 'lucide-react';
import { useAuth } from '../../../../router/AuthProvider';
import { useRequest } from 'ahooks';
import { AUTHORIZE_CODE_PKCE_VERIFIER, fetchAuthorizeUrl } from '../../../../services/Oauth2Service';
import { useNavigate } from 'react-router-dom';
import { useRedirect } from '../../../../hooks/useRedirect';
import { useTranslation } from 'react-i18next';
import useFullParams from '../../../../hooks/useFullParams';
import { useEffect, useRef, useState } from 'react';
import { AnimatePresence, color, motion } from 'framer-motion';
import { generateChallenge, generateVerifier } from '../../../../utils/pkce';
import UniversalPasskeyVerifier from '../../../account-settings/components/verifiers/UniversalPasskeyVerifier';
import PasswordLogin from './PasswordLogin';
import CaptchaLogin from './CaptchaLogin';
import MagicLinkLogin from './MagicLinkLogin';
import PasskeyLogin from './PasskeyLogin';
import Web3WalletLogin from './Web3WalletLogin';
import { AtlasLogo, Auth0Icon } from '../../../../components/icons';
import ThirdPartyLogin from './ThirdPartyLogin';

const LoginForm = ({ loginPanel, setLoginPanel, loginSuccessHandler }) => {
    const { t } = useTranslation();
    const { token } = theme.useToken();

    // 检查浏览器是否支持 WebAuthn (Passkey)
    const isWebAuthnSupported = window.PublicKeyCredential !== undefined && typeof window.PublicKeyCredential === 'function';

    // 登录方法状态: "1"-密码, "2"-验证码, "3"-魔链, "4"-Passkey
    const [loginMethod, setLoginMethod] = useState("1");

    return (
        <Card
            style={{
                width: '100%',
                borderRadius: '24px',
                boxShadow: '0 25px 50px -12px rgba(0,0,0,0.05)',
                border: 'none',
                background: '#ffffff',
                padding: '32px 24px 24px 24px', // 优化四周内边距，使视觉更聚拢
                position: 'relative'
            }}
        >
            {/* 右上角快捷切换扫码 (带微小旋转的高级触感动效) */}
            <div
                style={{
                    position: 'absolute',
                    top: 20,
                    right: 20,
                    cursor: 'pointer',
                    zIndex: 10,
                    transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                }}
                onClick={() => setLoginPanel('qr')}
                title={t('扫码登录')}
                onMouseEnter={(e) => {
                    e.currentTarget.style.transform = 'scale(1.1) rotate(90deg)';
                    e.currentTarget.style.opacity = '0.8';
                }}
                onMouseLeave={(e) => {
                    e.currentTarget.style.transform = 'scale(1) rotate(0deg)';
                    e.currentTarget.style.opacity = '1';
                }}
            >
                <QrCode
                    size={28}
                    color="#9ca3af"
                    strokeWidth={1.5}
                />
            </div>

            {/* 头部 Logo 与 欢迎语 */}
            <Flex vertical align="center" style={{ marginBottom: 24 }}>
                <AtlasLogo
                    style={{
                        width: '72px',
                        height: '72px'
                    }}
                />
                <Typography.Title level={3} style={{ margin: '16px 0 4px 0', fontWeight: 700, color: '#111827', letterSpacing: '-0.02em' }}>
                    {t('登录 Atlas')}
                </Typography.Title>
                <Typography.Text style={{ fontSize: 14, color: '#6b7280' }}>
                    {t('欢迎回来，请登录以继续')}
                </Typography.Text>
            </Flex>

            {/* 表单动态切换核心区（预留最小高度，缓解不同表单高度不一带来的卡片剧烈抖动） */}
            <div style={{ minHeight: '150px' }}>
                <AnimatePresence mode="wait">
                    {loginMethod === '1' && <PasswordLogin onSuccess={loginSuccessHandler} />}
                    {loginMethod === '2' && <CaptchaLogin onSuccess={loginSuccessHandler} />}
                    {loginMethod === '3' && <MagicLinkLogin onSuccess={loginSuccessHandler} />}
                    {loginMethod === '4' && <PasskeyLogin onSuccess={loginSuccessHandler} />}
                    {loginMethod === '5' && <Web3WalletLogin onSuccess={loginSuccessHandler} />}
                </AnimatePresence>
            </div>

            {/* 系统内置登录方式的二次切换区 */}
            <Flex justify="space-between" align="center" style={{ marginTop: 16, marginBottom: 24 }}>
                {loginMethod === '1' ? (
                    <>
                        {/* 如果设备支持，将现代且安全的 Passkey 快捷方式提至此核心区域 */}
                        {isWebAuthnSupported ? (
                            <Typography.Link
                                onClick={() => setLoginMethod('4')}
                                style={{
                                    fontSize: 13,
                                    display: 'flex',
                                    alignItems: 'center',
                                    gap: '4px',
                                    fontWeight: 500,
                                    color: token.colorPrimary,
                                }}
                            >
                                <Fingerprint
                                    size={14}
                                    strokeWidth={2.5}
                                    style={{ animation: 'pulse 2s infinite' }}
                                />
                                <span>{t('通行密钥')}</span>
                            </Typography.Link>
                        ) : (
                            <div /> /* 优雅占位以维持 space-between 布局 */
                        )}

                        {/* 更多其他表单登录手段 */}
                        <Dropdown
                            menu={{
                                items: [
                                    {
                                        key: '2',
                                        label: t('验证码登录'),
                                        onClick: () => setLoginMethod('2')
                                    },
                                    {
                                        key: '3',
                                        label: t('免密链登录'),
                                        onClick: () => setLoginMethod('3')
                                    }
                                ]
                            }}
                            placement="bottomRight"
                            trigger={['click']}
                        >
                            <Typography.Link style={{ fontSize: 13, color: '#6b7280' }}>
                                {t('更多方式 ▾')}
                            </Typography.Link>
                        </Dropdown>
                    </>
                ) : (
                    /* 非密码登录状态下统一返回主密码登录的快捷入口 */
                    <Typography.Link
                        onClick={() => setLoginMethod('1')}
                        style={{
                            fontSize: 13,
                            color: token.colorPrimary,
                            fontWeight: 500,
                            display: 'inline-flex',
                            alignItems: 'center',
                            gap: '4px',
                            transition: 'all 0.2s'
                        }}
                        onMouseEnter={(e) => {
                            const icon = e.currentTarget.querySelector('.back-icon');
                            if (icon) icon.style.transform = 'translateX(-3px)';
                        }}
                        onMouseLeave={(e) => {
                            const icon = e.currentTarget.querySelector('.back-icon');
                            if (icon) icon.style.transform = 'translateX(0)';
                        }}
                    >
                        <ArrowLeftOutlined
                            className="back-icon"
                            style={{
                                fontSize: 12,
                                transition: 'transform 0.2s ease'
                            }}
                        />
                        <span>{t('返回密码登录')}</span>
                    </Typography.Link>
                )}
            </Flex>

            {/* 第三方社会化登录分割线 */}
            <Divider plain style={{ margin: '16px 0' }}>
                <span style={{ color: '#9ca3af', fontSize: 11, fontWeight: 500, textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                    {t('第三方账号登录')}
                </span>
            </Divider>

            {/* 第三方登录渠道按钮组 */}
            <Flex justify="center" align="center" gap={28} style={{ marginTop: 16 }}>
                <ThirdPartyLogin/>
            </Flex>

            {loginMethod === '1' && (
                <div style={{ textAlign: 'center', marginTop: 20, marginBottom: 0 }}>
                    <Typography.Link
                        onClick={() => setLoginMethod('5')}
                        style={{
                            fontSize: 12,
                            fontWeight: 400,
                            color: '#9ca3af',
                            transition: 'all 0.25s cubic-bezier(0.4, 0, 0.2, 1)',
                            display: 'inline-block',
                            letterSpacing: '0.02em'
                        }}
                        onMouseEnter={(e) => {
                            e.currentTarget.style.color = token.colorPrimary; // 悬停时才温和地亮起主色
                        }}
                        onMouseLeave={(e) => {
                            e.currentTarget.style.color = '#9ca3af';
                        }}
                    >
                        <span>{t('已经是 Web3 用户？一键连接钱包登录')}</span>
                    </Typography.Link>
                </div>
            )}
        </Card>
    );
};

export default LoginForm;