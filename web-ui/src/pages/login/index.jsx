import React, { useEffect, useRef, useState } from 'react';
import { Form, Input, Button, Card, Flex, Typography, App, Avatar, Divider, Dropdown, ConfigProvider } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined, GithubOutlined, GoogleOutlined, KeyOutlined, ScanOutlined } from '@ant-design/icons';
import './index.css'
import { useRequest } from 'ahooks';
import { useAuth } from '../../router/AuthProvider';
import { login, ottLogin, sendEmailVerificationCode, sendOttLink } from '../../services/LoginService';
import { useTranslation } from 'react-i18next'
import { useNavigate, useSearchParams } from 'react-router-dom';
import httpWrapper from '../../services/AxiosWrapper';
import { fetchAuthorizeUrl, fetchDeviceCode } from '../../services/Oauth2Service';
import { useRedirect } from '../../hooks/useRedirect';
import { AnimatePresence, motion } from 'framer-motion';
import useFullParams from '../../hooks/useFullParams';
import Loading from '../../components/loading';

const Login = () => {

    const { t } = useTranslation()

    const { signin } = useAuth()

    const [form] = Form.useForm()

    const { message } = App.useApp()

    const navigate = useNavigate()
    const { ottToken } = useFullParams()

    const redirect = useRedirect()

    const { runAsync, loading } = useRequest(login, {
        manual: true
    })

    const { runAsync: sendOttLinkAsync, loading: sendOttLinkLoading } = useRequest(sendOttLink, {
        manual: true
    })

    const { runAsync: ottLoginAsync, loading: ottLoginLoading } = useRequest(ottLogin, {
        manual: true
    })

    const { runAsync: sendEmailVerificationCodeAsync, loading: sendEmailVerificationCodeLoading } = useRequest(sendEmailVerificationCode, {
        manual: true
    })

    const { runAsync: getAuthorizeUrlAsync, loading: getAuthorizeUrlLoading } = useRequest(fetchAuthorizeUrl, {
        manual: true
    })

    const { runAsync: getDeviceCodeAsync, loading: getDeviceCodeLoading } = useRequest(fetchDeviceCode, {
        manual: true
    })

    //登录方式
    const [loginMethod, setLoginMethod] = useState("1");

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

    const handleSendMagicLink = async () => {
        try {
            // validateFields 返回的是通过校验的字段，如果没有通过会抛出异常中断执行
            const values = await form.validateFields(['magicUsername']);

            await sendOttLinkAsync(values.magicUsername);
            console.log('message',message)
            message.success('登录链接已发送到您的邮箱，请注意查收！');

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
        await sendEmailVerificationCodeAsync(values.email)
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

    const handleForgetPassword = () => {

    }

    const authorizeCodeLogin = async () => {
        const authorizeUrl = await getAuthorizeUrlAsync('atlas')
        // 授权码模式跳转逻辑
        window.location.href = authorizeUrl
    }

    const deviceCodeLogin = async () => {
        const deviceCodeResult = await getDeviceCodeAsync()
        window.location.href = deviceCodeResult.verification_uri_complete
    }

    const onFinish = (values) => {
        let loginReq;
        switch (loginMethod) {
            case '1':
                loginReq = {
                    username: values.username,
                    credential: values.password,
                    loginType: 'NORMAL',
                    clientType: 'WEB',
                    rememberMe: values.rememberMe ? 1 : null
                }
                break
            case '2':
                loginReq = {
                    username: values.email,
                    credential: values.verificationCode,
                    loginType: 'EMAIL',
                    clientType: 'WEB',
                    rememberMe: values.rememberMe ? 1 : null
                }
                break
        }
        runAsync(loginReq)
            .then(
                (data) => {
                    loginSuccessHandler(data)
                },
                (error) => {
                    if (error.response && error.response.status === 401) {
                        if (error.response.data && error.response.data.code === 2201) {
                            message.error('账号已锁定，请联系系统管理员')
                        } else {
                            message.error('用户名或密码错误')
                        }

                    }
                }
            )
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

                <Card
                    style={{
                        width: '100%',
                        maxWidth: '440px',
                        borderRadius: '24px',
                        boxShadow: '0 25px 50px -12px rgba(0,0,0,0.05)',
                        border: 'none',
                        background: '#ffffff',
                        padding: '16px'
                    }}
                >
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
                                        <Input allowClear size="large" placeholder="用户名或邮箱" prefix={<UserOutlined style={{ color: '#9ca3af', marginRight: 8 }} />} />
                                    </Form.Item>
                                    <Form.Item name="password" rules={[{ required: loginMethod === '1', message: '密码不可为空' }]} style={{ marginBottom: 20 }}>
                                        <Input.Password size="large" placeholder="密码" prefix={<LockOutlined style={{ color: '#9ca3af', marginRight: 8 }} />} />
                                    </Form.Item>
                                    <Form.Item style={{ marginBottom: 32, marginTop: 12 }}>
                                        <Button type="primary" htmlType="submit" size="large" block loading={loading || getAuthorizeUrlLoading || getDeviceCodeLoading} style={{ boxShadow: '0 4px 14px 0 rgba(79, 70, 229, 0.39)' }}>
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
                                        <Button loading={sendEmailVerificationCodeLoading} disabled={verificationCode.disabled} size="large" onClick={handleWithVerificationCode}>
                                            {verificationCode.disabled ? t('{{ti}}s', { ti: verificationCode.seconds }) : t('发送')}
                                        </Button>
                                    </Flex>
                                    <Form.Item style={{ marginBottom: 32, marginTop: 12 }}>
                                        <Button type="primary" htmlType="submit" size="large" block loading={loading || getAuthorizeUrlLoading || getDeviceCodeLoading} style={{ boxShadow: '0 4px 14px 0 rgba(79, 70, 229, 0.39)' }}>
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
                                        输入您的账号或邮箱，我们将向您发送一条免密登录的快捷链接。
                                    </Typography.Paragraph>
                                    <Form.Item name="magicUsername" rules={[{ required: loginMethod === '3', message: '账号不可为空' }]} style={{ marginBottom: 20 }}>
                                        <Input allowClear size="large" placeholder="输入用户名或邮箱" prefix={<UserOutlined style={{ color: '#9ca3af', marginRight: 8 }} />} />
                                    </Form.Item>
                                    <Form.Item style={{ marginBottom: 32, marginTop: 12 }}>
                                        <Button
                                            type="primary"
                                            size="large"
                                            block
                                            onClick={handleSendMagicLink}
                                            loading={sendOttLinkLoading}
                                            disabled={verificationCode.disabled}
                                            style={{ boxShadow: verificationCode.disabled === true ? '' : '0 4px 14px 0 rgba(79, 70, 229, 0.39)' }}
                                        >
                                            {verificationCode.disabled ? t('链接已发送 ({{ti}}s)', { ti: verificationCode.seconds }) : t('发送快捷登录链接')}
                                        </Button>
                                    </Form.Item>
                                </motion.div>
                            )}
                        </AnimatePresence>

                        <Flex justify="space-between" align="center" style={{ marginBottom: 32, marginTop: 8 }}>
                            {loginMethod === '1' ? (
                                <Dropdown
                                    menu={{
                                        items: [
                                            {
                                                key: '2',
                                                label: '邮箱登录',
                                                align: 'center',
                                                onClick: () => switchLoginMethod('2')
                                            },
                                            {
                                                key: '3',
                                                label: '免密登录',
                                                align: 'center',
                                                onClick: () => switchLoginMethod('3')
                                            }
                                        ]
                                    }}
                                    placement="bottomLeft"
                                    trigger={['click']}
                                >
                                    <Typography.Link style={{ fontSize: 14, color: '#6b7280' }}>
                                        {t('其他登录方式 ▾')}
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

                            {loginMethod === '1' && (
                                <Typography.Link onClick={handleForgetPassword} style={{ fontSize: 14, fontWeight: 500 }}>
                                    {t('忘记密码？')}
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
                                title="GitHub"
                            />
                            <Dropdown
                                menu={{
                                    items: [
                                        {
                                            key: 'auth_code',
                                            label: '授权码登录',
                                            icon: <KeyOutlined />,
                                            onClick: authorizeCodeLogin
                                        },
                                        {
                                            key: 'device_code',
                                            label: '设备码登录',
                                            icon: <ScanOutlined />,
                                            onClick: deviceCodeLogin
                                        }
                                    ]
                                }}
                                trigger={['hover']}
                                placement="bottom"
                            >
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
                                />
                            </Dropdown>
                            <GoogleOutlined
                                style={{ fontSize: 24, color: '#6b7280', cursor: 'pointer', transition: 'all 0.2s ease' }}
                                onMouseEnter={(e) => { e.currentTarget.style.transform = 'translateY(-3px)'; e.currentTarget.style.color = '#EA4335'; }}
                                onMouseLeave={(e) => { e.currentTarget.style.transform = 'translateY(0)'; e.currentTarget.style.color = '#6b7280'; }}
                                title="Google"
                            />
                        </Flex>
                    </Form>
                </Card>
            </div>
        </ConfigProvider>
    )
}

export default Login