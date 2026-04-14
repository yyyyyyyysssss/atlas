import React, { useEffect, useRef, useState } from 'react';
import { Form, Input, Button, Card, Flex, Tabs, Checkbox, Typography, App, Avatar } from 'antd';
import { UserOutlined, LockOutlined, MobileOutlined, MailOutlined } from '@ant-design/icons';
import './index.css'
import { useRequest } from 'ahooks';
import { useAuth } from '../../router/AuthProvider';
import { login, sendEmailVerificationCode } from '../../services/LoginService';
import { useTranslation } from 'react-i18next'

const Login = () => {

    const { t } = useTranslation()

    const { signin } = useAuth()

    const [form] = Form.useForm()

    const { message } = App.useApp()

    const { runAsync, loading } = useRequest(login, {
        manual: true
    })

    const { runAsync: sendEmailVerificationCodeAsync, loading: sendEmailVerificationCodeLoading } = useRequest(sendEmailVerificationCode, {
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

    const loginSuccessHandler = (data) => {
        signin(data)
    }

    return (
        <Flex
            justify='end'
            align='center'
            className="min-h-screen p-4"
        >
            <Flex
                style={{ marginRight: '10%' }}
                vertical
            >
                <Card variant="borderless" className="w-100 shadow-lg">
                    <Flex
                        justify='center'
                        align='center'
                        gap={30}
                        vertical
                    >
                        <Flex justify='center' align='center' gap={15} vertical>
                            <Avatar
                                src={'/logo128.png'}
                                size={48}
                            />
                            <Typography.Text style={{ fontSize: '25px' }} strong>登录Atlas</Typography.Text>
                        </Flex>
                        <Form form={form} style={{ width: '100%' }} onFinish={onFinish}>
                            <Tabs
                                defaultActiveKey="1"
                                centered
                                onChange={(e) => switchLoginMethod(e)}
                                items={[
                                    {
                                        key: '1',
                                        label: t('账号密码登录'),
                                        children: (
                                            <>
                                                <Form.Item name="username" rules={[
                                                    {
                                                        required: loginMethod === '1',
                                                        message: '用户名不可为空'
                                                    }
                                                ]}>
                                                    <Input allowClear size="large" placeholder="用户名" prefix={<UserOutlined />} />
                                                </Form.Item>
                                                <Form.Item name="password" rules={[
                                                    {
                                                        required: loginMethod === '1',
                                                        message: '密码不可为空'
                                                    }
                                                ]}>
                                                    <Input.Password size="large" placeholder="密码" prefix={<LockOutlined />} />
                                                </Form.Item>
                                            </>
                                        )
                                    },
                                    {
                                        key: '2',
                                        label: t('邮箱登录'),
                                        children: (
                                            <>
                                                <Form.Item name="email" validateTrigger="onBlur" rules={[
                                                    {
                                                        validator: emailVerification
                                                    }
                                                ]}>
                                                    <Input allowClear size="large" placeholder="邮箱" prefix={<MailOutlined />} />
                                                </Form.Item>
                                                <Flex gap='small'>
                                                    <Form.Item name="verificationCode" rules={[
                                                        {
                                                            required: loginMethod === '2',
                                                            message: '验证码不可为空'
                                                        }
                                                    ]}>
                                                        <Input allowClear size="large" placeholder="请输入验证码!" prefix={<MailOutlined />} />
                                                    </Form.Item>
                                                    <Button loading={sendEmailVerificationCodeLoading} disabled={verificationCode.disabled} size="large" onClick={handleWithVerificationCode}>
                                                        {verificationCode.disabled
                                                            ? t('{{ti}} 秒后重新获取', { ti: verificationCode.seconds })
                                                            : t('获取验证码')}
                                                    </Button>
                                                </Flex>
                                            </>
                                        )
                                    }
                                ]}
                            />
                            <Form.Item>
                                <Typography.Link onClick={handleForgetPassword} style={{ float: 'right' }}>
                                    {t('忘记密码')}
                                </Typography.Link>
                            </Form.Item>
                            <Form.Item>
                                <Button type="primary" htmlType="submit" style={{ width: '100%' }} size="large" loading={loading}>
                                    {t('登录')}
                                </Button>
                            </Form.Item>
                        </Form>
                    </Flex>
                </Card>
            </Flex>
        </Flex>
    )
}

export default Login