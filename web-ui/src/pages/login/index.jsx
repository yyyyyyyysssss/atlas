import React, { useEffect, useRef, useState } from 'react';
import { Form, Input, Button, Card, Flex, Tabs, Typography, App, Avatar, Divider, Dropdown, ConfigProvider } from 'antd';
import { UserOutlined, LockOutlined, MobileOutlined, MailOutlined, GithubOutlined, GoogleOutlined, KeyOutlined, ScanOutlined } from '@ant-design/icons';
import './index.css'
import { useRequest } from 'ahooks';
import { useAuth } from '../../router/AuthProvider';
import { login, sendEmailVerificationCode } from '../../services/LoginService';
import { useTranslation } from 'react-i18next'
import { useNavigate, useSearchParams } from 'react-router-dom';
import httpWrapper from '../../services/AxiosWrapper';
import { fetchAuthorizeUrl, fetchDeviceCode } from '../../services/Oauth2Service';
import { useRedirect } from '../../hooks/useRedirect';

const Login = () => {

    const { t } = useTranslation()

    const { signin } = useAuth()

    const [form] = Form.useForm()

    const { message } = App.useApp()

    const navigate = useNavigate()
    const [searchParams] = useSearchParams()

    const redirect = useRedirect()

    const { runAsync, loading } = useRequest(login, {
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

    useEffect(() => {
        // 组件卸载时清理定时器
        return () => {
            if (timerRef.current) clearInterval(timerRef.current)
        }
    }, [])

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

    return (
        <ConfigProvider
            theme={{
                token: {
                    colorBgContainer: '#ffffff',
                    colorText: '#1f2937',
                    colorTextSecondary: '#6b7280',
                    colorBorder: '#d9d9d9',
                    colorBgElevated: '#ffffff',
                    controlItemBgHover: '#f3f4f6',
                    colorPrimary: '#1677ff',
                    colorLink: '#1677ff',
                    colorLinkHover: '#4096ff',
                    colorLinkActive: '#0958d9',
                },
                components: {
                    Input: {
                        colorBgContainer: '#ffffff',
                        colorText: '#1f2937',
                        colorTextPlaceholder: '#9ca3af',
                    },
                    Tabs: {
                        itemColor: '#6b7280',
                        itemSelectedColor: '#1677ff',
                        itemHoverColor: '#1677ff',
                    }
                }
            }}
        >
            <div style={{
                minHeight: '100vh',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                background: 'linear-gradient(135deg, #f6f8fd 0%, #f1f5f9 100%)',
                padding: '20px',
                position: 'relative',
                overflow: 'hidden'
            }}>
                {/* 现代感背景装饰圆 */}
                <div style={{
                    position: 'absolute', top: '-10%', left: '-5%', width: '40vw', height: '40vw',
                    background: 'radial-gradient(circle, rgba(24,144,255,0.05) 0%, rgba(24,144,255,0) 70%)', borderRadius: '50%'
                }} />
                <div style={{
                    position: 'absolute', bottom: '-10%', right: '-5%', width: '35vw', height: '35vw',
                    background: 'radial-gradient(circle, rgba(114,46,209,0.05) 0%, rgba(114,46,209,0) 70%)', borderRadius: '50%'
                }} />

                <Card
                    style={{
                        width: '100%',
                        maxWidth: '420px',
                        borderRadius: '24px',
                        boxShadow: '0 20px 40px -10px rgba(0,0,0,0.08)',
                        border: '1px solid rgba(255,255,255,0.6)',
                        background: 'rgba(255,255,255,0.9)',
                        backdropFilter: 'blur(10px)',
                        padding: '8px'
                    }}
                >
                    <Flex vertical align="center" style={{ marginBottom: 32 }}>
                        <Avatar src={'/logo128.png'} size={64} style={{ boxShadow: '0 8px 16px rgba(0,0,0,0.08)' }} />
                        <Typography.Title level={3} style={{ margin: '20px 0 8px 0', fontWeight: 600, color: '#1f2937' }}>
                            {t('登录 Atlas')}
                        </Typography.Title>
                        <Typography.Text type="secondary" style={{ fontSize: 14, color: '#6b7280' }}>
                            {t('欢迎回来，请登录以继续访问系统')}
                        </Typography.Text>
                    </Flex>

                    <Form form={form} style={{ width: '100%' }} onFinish={onFinish}>
                        <Tabs
                            defaultActiveKey="1"
                            centered
                            onChange={(e) => switchLoginMethod(e)}
                            items={[
                                {
                                    key: '1',
                                    label: t('账号登录'),
                                    children: (
                                        <div style={{ marginTop: 12 }}>
                                            <Form.Item name="username" rules={[{ required: loginMethod === '1', message: '用户名不可为空' }]}>
                                                <Input allowClear size="large" placeholder="请输入用户名" prefix={<UserOutlined style={{ color: '#bfbfbf' }} />} style={{ borderRadius: 8, height: 44 }} />
                                            </Form.Item>
                                            <Form.Item name="password" rules={[{ required: loginMethod === '1', message: '密码不可为空' }]}>
                                                <Input.Password size="large" placeholder="请输入密码" prefix={<LockOutlined style={{ color: '#bfbfbf' }} />} style={{ borderRadius: 8, height: 44 }} />
                                            </Form.Item>
                                        </div>
                                    )
                                },
                                {
                                    key: '2',
                                    label: t('邮箱登录'),
                                    children: (
                                        <div style={{ marginTop: 12 }}>
                                            <Form.Item name="email" validateTrigger="onBlur" rules={[{ validator: emailVerification }]}>
                                                <Input allowClear size="large" placeholder="请输入邮箱" prefix={<MailOutlined style={{ color: '#bfbfbf' }} />} style={{ borderRadius: 8, height: 44 }} />
                                            </Form.Item>
                                            <Flex gap='small'>
                                                <Form.Item name="verificationCode" rules={[{ required: loginMethod === '2', message: '验证码不可为空' }]} style={{ flex: 1 }}>
                                                    <Input allowClear size="large" placeholder="验证码" prefix={<MailOutlined style={{ color: '#bfbfbf' }} />} style={{ borderRadius: 8, height: 44 }} />
                                                </Form.Item>
                                                <Button loading={sendEmailVerificationCodeLoading} disabled={verificationCode.disabled} size="large" onClick={handleWithVerificationCode} style={{ borderRadius: 8, height: 44 }}>
                                                    {verificationCode.disabled ? t('{{ti}} 秒后重试', { ti: verificationCode.seconds }) : t('获取验证码')}
                                                </Button>
                                            </Flex>
                                        </div>
                                    )
                                }
                            ]}
                        />

                        <Flex justify="end" align="center" style={{ marginBottom: 24 }}>
                            <Typography.Link onClick={handleForgetPassword} style={{ fontSize: 14 }}>
                                {t('忘记密码？')}
                            </Typography.Link>
                        </Flex>

                        <Form.Item style={{ marginBottom: 24 }}>
                            <Button type="primary" htmlType="submit" style={{ width: '100%', height: 44, borderRadius: 8, fontSize: 16, fontWeight: 500 }} loading={loading || getAuthorizeUrlLoading || getDeviceCodeLoading}>
                                {t('登 录')}
                            </Button>
                        </Form.Item>

                        <Divider plain>
                            <Typography.Text type="secondary" style={{ fontSize: 12, color: '#9ca3af' }}>
                                {t('其他登录方式')}
                            </Typography.Text>
                        </Divider>

                        <Flex justify="center" align="center" gap={32} style={{ marginBottom: 4 }}>
                            <GithubOutlined
                                style={{ fontSize: 24, color: '#374151', cursor: 'pointer', transition: 'transform 0.2s' }}
                                onMouseEnter={(e) => e.currentTarget.style.transform = 'scale(1.2)'}
                                onMouseLeave={(e) => e.currentTarget.style.transform = 'scale(1)'}
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
                                        transition: 'transform 0.2s',
                                        border: '1px solid #e5e7eb',
                                        background: '#fff'
                                    }}
                                    onMouseEnter={(e) => e.currentTarget.style.transform = 'scale(1.2)'}
                                    onMouseLeave={(e) => e.currentTarget.style.transform = 'scale(1)'}
                                />
                            </Dropdown>
                            <GoogleOutlined
                                style={{ fontSize: 24, color: '#EA4335', cursor: 'pointer', transition: 'transform 0.2s' }}
                                onMouseEnter={(e) => e.currentTarget.style.transform = 'scale(1.2)'}
                                onMouseLeave={(e) => e.currentTarget.style.transform = 'scale(1)'}
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