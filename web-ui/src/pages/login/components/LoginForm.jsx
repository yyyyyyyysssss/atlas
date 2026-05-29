import { Form, Input, Button, Card, Flex, Typography, App, Avatar, Divider, Dropdown, ConfigProvider, QRCode, theme } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined, GithubOutlined, GoogleOutlined, KeyOutlined, ScanOutlined, CheckCircleFilled } from '@ant-design/icons';
import { QrCode, Monitor, Fingerprint } from 'lucide-react';
import { useAuth } from '../../../router/AuthProvider';
import { useRequest } from 'ahooks';
import { captchaLogin, passwordLogin, sendCaptcha, sendOttLink, webauthnLogin } from '../../../services/LoginService';
import { AUTHORIZE_CODE_PKCE_VERIFIER, fetchAuthorizeUrl } from '../../../services/Oauth2Service';
import { useNavigate } from 'react-router-dom';
import { useRedirect } from '../../../hooks/useRedirect';
import { useTranslation } from 'react-i18next';
import useFullParams from '../../../hooks/useFullParams';
import { useEffect, useRef, useState } from 'react';
import { AnimatePresence, motion } from 'framer-motion';
import { generateChallenge, generateVerifier } from '../../../utils/pkce';
import UniversalPasskeyVerifier from '../../account-settings/components/verifiers/UniversalPasskeyVerifier';

const LoginFrom = ({ setIsQrLogin, loginSuccessHandler }) => {

    const { t } = useTranslation()

    const { token } = theme.useToken()

    const { signin } = useAuth()

    const [form] = Form.useForm()

    const { message } = App.useApp()

    const navigate = useNavigate()
    const { ottToken, targetUrl } = useFullParams()

    const isWebAuthnSupported = window.PublicKeyCredential !== undefined && typeof window.PublicKeyCredential === 'function';

    const redirect = useRedirect()

    const { runAsync: captchaLoginAsync, loading: captchaLoginLoading } = useRequest(captchaLogin, {
        manual: true
    })

    const { runAsync: passwordLoginAsync, loading: passwordLoginLoading } = useRequest(passwordLogin, {
        manual: true
    })

    const { runAsync: webauthnLoginAsync, loading: webauthnLoginLoading } = useRequest(webauthnLogin, {
        manual: true
    })

    const { runAsync: sendOttLinkAsync, loading: sendOttLinkLoading } = useRequest(sendOttLink, {
        manual: true
    })


    const { runAsync: sendCaptchaAsync, loading: sendCaptchaLoading } = useRequest(sendCaptcha, {
        manual: true
    })

    const { runAsync: getAuthorizeUrlAsync, loading: getAuthorizeUrlLoading } = useRequest(fetchAuthorizeUrl, {
        manual: true
    })


    const [loginMethod, setLoginMethod] = useState("1")

    const quickPasskeyRef = useRef(null)


    //验证码设置
    const [verificationCode, setVerificationCode] = useState({
        disabled: false,
        time: 60,
        seconds: 0,
    })

    const timerRef = useRef()
    const hasHandledOttRef = useRef(false) // 用于防止 StrictMode 下执行两次

    useEffect(() => {
        // 组件卸载时清理定时器
        return () => {
            if (timerRef.current) clearInterval(timerRef.current)
        }
    }, [])

    const handleSendMagicLink = async () => {
        try {
            // validateFields 返回的是通过校验的字段，如果没有通过会抛出异常中断执行
            const values = await form.validateFields(['magicUsername']);

            await sendOttLinkAsync(values.magicUsername, targetUrl);
            message.success({
                content: t('发送请求已提交。如果账号存在，您将在几分钟内收到登录邮件。'),
                duration: 5 // 增加停留时间，让用户有充足时间阅读安全提示
            });

            // 重置定时器状态为60秒倒计时
            if (timerRef.current) clearInterval(timerRef.current);
            let ti = 60;
            setVerificationCode(prev => ({
                ...prev,
                disabled: true,
                seconds: ti,
            }));

            timerRef.current = setInterval(() => {
                ti--;
                if (ti > 0) {
                    setVerificationCode(prev => ({
                        ...prev,
                        seconds: ti,
                    }));
                } else {
                    resetVerificationCode();
                }
            }, 1000);
        } catch (e) {
            // 这里捕获的是表单校验失败的错误或接口请求失败的错误
            // 如果是因为 validateFields 没过，e.errorFields 会存在
            if (!e.errorFields) {
                console.error('发送失败:', e);
            }
        }
    }

    const switchLoginMethod = (loginMethod) => {
        form.resetFields()
        resetVerificationCode()
        setLoginMethod(loginMethod)
    }

    const emailVerification = (_, val) => {
        if (loginMethod !== "2") {
            return Promise.resolve();
        }
        if (!val) {
            return Promise.reject("邮箱不能为空")
        }
        const emailReg = /^\w+(-+.\w+)*@\w+(-.\w+)*.\w+(-.\w+)*$/;
        const validateResult = emailReg.test(val)
        if (!validateResult) {
            return Promise.reject("邮箱不合法")
        }
        return Promise.resolve();
    }

    const phoneVerification = (_, val) => {
        if (loginMethod !== "2") {
            return Promise.resolve();
        }
        const phoneReg = /^(?:\+?86)?1(?:3\d{3}|5[^4\D]\d{2}|8\d{3}|7(?:[235-8]\d{2}|4(?:0\d|1[0-2]|9\d))|9[0-35-9]\d{2}|66\d{2})\d{6}$/;
        const validateResult = phoneReg.test(val)
        if (!validateResult) {
            return Promise.reject("手机号不合法");
        }
        return Promise.resolve();
    }

    // 发送验证码
    const handleWithVerificationCode = async () => {
        const values = await form.validateFields(['email'])
        // 调用接口
        await sendCaptchaAsync({
            target: values.email,
            captchaType: 'email',
            captchaScene: 'login'
        })
        // 清理旧定时器
        if (timerRef.current) clearInterval(timerRef.current)
        let ti = verificationCode.time
        setVerificationCode(prev => ({
            ...prev,
            disabled: true,
            seconds: ti,
        }))

        timerRef.current = setInterval(() => {
            ti--
            if (ti > 0) {
                setVerificationCode(prev => ({
                    ...prev,
                    seconds: ti,
                }))
            } else {
                resetVerificationCode()
            }
        }, 1000);
    }

    const resetVerificationCode = () => {
        if (timerRef.current) {
            clearInterval(timerRef.current);
        }
        setVerificationCode({
            disabled: false,
            time: 60
        })
    }

    const authorizeCodeLogin = async (clientName) => {
        const authorizeUrl = await getAuthorizeUrlAsync(clientName)
        const verifier = generateVerifier()
        const challenge = await generateChallenge(verifier)
        sessionStorage.setItem(AUTHORIZE_CODE_PKCE_VERIFIER, verifier)
        const finalUrl = authorizeUrl + `&code_challenge=${challenge}&code_challenge_method=S256`
        // 授权码模式跳转逻辑
        window.location.href = finalUrl
    }

    const deviceCodeLogin = async () => {
        const deviceCodeResult = await getDeviceCodeAsync()
        window.open(deviceCodeResult.verification_uri_complete, '_blank');
    }

    const passkeyLogin = async (webauthnId, credentialJson) => {
        try {
            return await webauthnLoginAsync(webauthnId, {
                clientType: 'WEB',
                webauthnAuthenticationRequest: credentialJson
            })
        } catch (error) {
            if (error.response && error.response.status === 401) {
                if (error.response.data && error.response.data.code === 2201) {
                    message.error('账号已锁定，请联系系统管理员')
                } else {
                    message.warning('验证失败。该设备密钥可能尚未绑定，或不属于当前系统。请先使用密码或验证码登录，成功后前往[安全设置]完成绑定。', 5)
                }

            }
        }

    }

    const onFinish = async (values) => {
        let loginReq;
        try {
            let loginResponse
            switch (loginMethod) {
                case '1':
                    loginResponse = await passwordLoginAsync({
                        username: values.username,
                        password: values.password,
                        clientType: 'WEB',
                    })
                    break
                case '2':
                    loginResponse = await captchaLoginAsync({
                        identity: values.email,
                        captcha: values.verificationCode,
                        captchaType: 'EMAIL',
                        clientType: 'WEB',
                    })
                    break
            }
            loginSuccessHandler(loginResponse)
        } catch (error) {
            if (error.response && error.response.status === 401) {
                if (error.response.data && error.response.data.code === 2201) {
                    message.error('账号已锁定，请联系系统管理员')
                } else {
                    message.error('用户名或密码错误')
                }

            }
        }
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

            <Form form={form} style={{ width: '100%' }} onFinish={onFinish}>
                <AnimatePresence mode="wait">
                    {loginMethod === '1' && (
                        <motion.div
                            key="password-login"
                            initial={{ opacity: 0, x: -10 }}
                            animate={{ opacity: 1, x: 0 }}
                            exit={{ opacity: 0, x: 10 }}
                            transition={{ duration: 0.2 }}
                        >
                            <Form.Item name="username" rules={[{ required: loginMethod === '1', message: '用户名不可为空' }]} style={{ marginBottom: 20 }}>
                                <Input
                                    allowClear
                                    size="large" placeholder="用户名或邮箱"
                                    prefix={
                                        <UserOutlined style={{ color: '#9ca3af', marginRight: 8 }} />
                                    }
                                />
                            </Form.Item>
                            <Form.Item name="password" rules={[{ required: loginMethod === '1', message: '密码不可为空' }]} style={{ marginBottom: 20 }}>
                                <Input.Password size="large" placeholder="密码" prefix={<LockOutlined style={{ color: '#9ca3af', marginRight: 8 }} />} />
                            </Form.Item>
                            <Form.Item style={{ marginBottom: 32, marginTop: 12 }}>
                                <Button type="primary" htmlType="submit" size="large" block loading={passwordLoginLoading || getAuthorizeUrlLoading} style={{ boxShadow: '0 4px 14px 0 rgba(79, 70, 229, 0.39)' }}>
                                    {t('登 录')}
                                </Button>
                            </Form.Item>
                        </motion.div>
                    )}

                    {loginMethod === '2' && (
                        <motion.div
                            key="code-login"
                            initial={{ opacity: 0, x: 10 }}
                            animate={{ opacity: 1, x: 0 }}
                            exit={{ opacity: 0, x: -10 }}
                            transition={{ duration: 0.2 }}
                        >
                            <Form.Item name="email" validateTrigger="onBlur" rules={[{ validator: emailVerification }]} style={{ marginBottom: 20 }}>
                                <Input allowClear size="large" placeholder="注册邮箱" prefix={<MailOutlined style={{ color: '#9ca3af', marginRight: 8 }} />} />
                            </Form.Item>
                            <Flex gap='small' style={{ marginBottom: 20 }}>
                                <Form.Item name="verificationCode" rules={[{ required: loginMethod === '2', message: '验证码不可为空' }]} style={{ flex: 1, marginBottom: 0 }}>
                                    <Input allowClear size="large" placeholder="6位验证码" prefix={<MailOutlined style={{ color: '#9ca3af', marginRight: 8 }} />} />
                                </Form.Item>
                                <Button loading={sendCaptchaLoading} disabled={verificationCode.disabled} size="large" onClick={handleWithVerificationCode}>
                                    {verificationCode.disabled ? t('{{ti}}s', { ti: verificationCode.seconds }) : t('发送')}
                                </Button>
                            </Flex>
                            <Form.Item style={{ marginBottom: 32, marginTop: 12 }}>
                                <Button type="primary" htmlType="submit" size="large" block loading={captchaLoginLoading || getAuthorizeUrlLoading} style={{ boxShadow: '0 4px 14px 0 rgba(79, 70, 229, 0.39)' }}>
                                    {t('登 录')}
                                </Button>
                            </Form.Item>
                        </motion.div>
                    )}

                    {loginMethod === '3' && (
                        <motion.div
                            key="magic-login"
                            initial={{ opacity: 0, y: 10 }}
                            animate={{ opacity: 1, y: 0 }}
                            exit={{ opacity: 0, y: -10 }}
                            transition={{ duration: 0.2 }}
                        >
                            <Typography.Paragraph type="secondary" style={{ textAlign: 'center', marginBottom: 20, fontSize: 13 }}>
                                输入您的账号或邮箱，我们将向该账号绑定的邮箱发送登录链接。
                            </Typography.Paragraph>
                            <Form.Item name="magicUsername" rules={[{ required: loginMethod === '3', message: '账号不可为空' }]} style={{ marginBottom: 32 }}>
                                <Input allowClear size="large" placeholder="输入用户名或邮箱" prefix={<UserOutlined style={{ color: '#9ca3af', marginRight: 8 }} />} />
                            </Form.Item>

                            <Form.Item style={{ marginBottom: 32 }}>
                                <Button
                                    type="primary"
                                    size="large"
                                    block
                                    onClick={handleSendMagicLink}
                                    loading={sendOttLinkLoading}
                                    disabled={verificationCode.disabled}
                                    style={{ boxShadow: verificationCode.disabled === true ? '' : '0 4px 14px 0 rgba(79, 70, 229, 0.39)' }}
                                >
                                    {verificationCode.disabled ? t('请求已发送 ({{ti}}s)', { ti: verificationCode.seconds }) : t('发送快捷登录链接')}
                                </Button>
                            </Form.Item>
                        </motion.div>
                    )}

                    {loginMethod === '4' && (
                        <motion.div
                            key="passkey-login"
                            initial={{ opacity: 0, y: 10 }}
                            animate={{ opacity: 1, y: 0 }}
                            exit={{ opacity: 0, y: -10 }}
                            transition={{ duration: 0.2 }}
                            onAnimationComplete={async () => {
                                const res = await quickPasskeyRef.current.onVerify()
                                loginSuccessHandler(res)
                            }}
                        >
                            <div style={{ marginBottom: 24 }}>
                                <UniversalPasskeyVerifier
                                    verifierRef={quickPasskeyRef}
                                    onVerifyAction={passkeyLogin} // 👈 绑定你后端的 Passkey 登录服务
                                    onSuccess={loginSuccessHandler} // 👈 成了就走统一成功处理器
                                    label=""
                                />
                            </div>
                        </motion.div>
                    )}
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
                                        onClick: () => switchLoginMethod('2')
                                    },
                                    {
                                        key: '3',
                                        label: '免密链登录',
                                        align: 'center',
                                        onClick: () => switchLoginMethod('3')
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
                            onClick={() => switchLoginMethod('1')}
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
                            onClick={() => switchLoginMethod('4')}
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

            </Form>
        </Card>
    )
}

export default LoginFrom