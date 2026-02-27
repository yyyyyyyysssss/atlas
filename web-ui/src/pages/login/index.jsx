import React, { useRef, useState } from 'react';
import { Form, Input, Button, Card, Flex, Tabs, Checkbox, Typography } from 'antd';
import { UserOutlined, LockOutlined, MobileOutlined, MailOutlined } from '@ant-design/icons';
import './index.css'
import { getMessageApi } from '../../utils/MessageUtil';
import { useRequest } from 'ahooks';
import { useAuth } from '../../router/AuthProvider';
import { login } from '../../services/LoginService';
import { useTranslation } from 'react-i18next'

const Login = () => {

    const { t } = useTranslation()

    const { signin } = useAuth()

    const [form] = Form.useForm()

    const { runAsync, loading } = useRequest(login, {
        manual: true
    })

    //登录方式
    const [loginMethod, setLoginMethod] = useState("1");

    //验证码设置
    const [verificationCode, setVerificationCode] = useState({
        disabled: false,
        tips: '获取验证码',
        time: 60,
        seconds: 0,
    })

    const timerRef = useRef()

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
        await form.validateFields(['email'])
        let ti = verificationCode.time;
        setVerificationCode({
            disabled: true,
            tips: `{{ti}} 秒后重新获取`,
            seconds: ti,
        });
        timerRef.current = setInterval(() => {
            if (--ti > 0) {
                setVerificationCode({
                    disabled: true,
                    tips: `{{ti}} 秒后重新获取`,
                    seconds: ti,
                });
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
            tips: '获取验证码',
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
                            getMessageApi().error('账号已锁定，请联系系统管理员')
                        } else {
                            getMessageApi().error('用户名或密码错误')
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
            className="min-h-screen p-4 bg-gradient-to-r from-gray-200 via-gray-300 to-gray-400"
        >
            <Flex
                style={{ marginRight: '10%' }}
                vertical
            >
                <Card variant="borderless" className="w-100 shadow-lg">
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
                                                <Input allowClear size="large" placeholder="邮箱" prefix={<MobileOutlined />} />
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
                                                <Button disabled={verificationCode.disabled} size="large" onClick={handleWithVerificationCode}>{t(verificationCode.tips,{ti : verificationCode.seconds})}</Button>
                                            </Flex>
                                        </>
                                    )
                                }
                            ]}
                        />
                        <Form.Item>
                            <Form.Item name="rememberMe" valuePropName="checked" initialValue={true} noStyle>
                                <Checkbox>{t('记住密码')}</Checkbox>
                            </Form.Item>
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
                </Card>
            </Flex>
        </Flex>
    )
}

export default Login