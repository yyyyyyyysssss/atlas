import { Form, Input, Button, Card, Flex, Typography, App, Avatar, Divider, Dropdown, ConfigProvider, QRCode, theme } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined, GithubOutlined, GoogleOutlined, KeyOutlined, ScanOutlined, CheckCircleFilled } from '@ant-design/icons';
import { useRequest } from 'ahooks';
import { AnimatePresence, motion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { captchaLogin, sendCaptcha } from '../../../../services/LoginService';
import { useEffect, useRef, useState } from 'react';

const CaptchaLogin = ({ onSuccess }) => {

    const { t } = useTranslation()

    const [form] = Form.useForm()

    const { message } = App.useApp()

    const timerRef = useRef()

    //验证码设置
    const [verificationCode, setVerificationCode] = useState({
        disabled: false,
        time: 60,
        seconds: 0,
    })

    useEffect(() => {
        return () => { if (timerRef.current) clearInterval(timerRef.current); };
    }, [])

    const { runAsync: captchaLoginAsync, loading: loginLoading } = useRequest(captchaLogin, { manual: true })

    const { runAsync: sendCaptchaAsync, loading: sendLoading } = useRequest(sendCaptcha, { manual: true })


    const emailVerification = (_, val) => {
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

    const onFinish = async (values) => {
        try {
            const loginResponse = await captchaLoginAsync({
                identity: values.email,
                captcha: values.verificationCode,
                captchaType: 'EMAIL',
                clientType: 'WEB',
            })
            onSuccess(loginResponse)
        } catch (error) {
            if (error.response && error.response.status === 401) {
                if (error.response.data && error.response.data.code === 2201) {
                    message.error('账号已锁定，请联系系统管理员')
                } else {
                    message.error('验证码错误或已过期')
                }
            }
        }
    }

    return (
        <motion.div
            key="code-login"
            initial={{ opacity: 0, x: 10 }}
            animate={{ opacity: 1, x: 0 }}
            exit={{ opacity: 0, x: -10 }}
            transition={{ duration: 0.2 }}
        >
            <Form form={form} onFinish={onFinish} layout="vertical">
                <Form.Item name="email" validateTrigger="onBlur" rules={[{ validator: emailVerification }]} style={{ marginBottom: 20 }}>
                    <Input allowClear size="large" placeholder="注册邮箱" prefix={<MailOutlined style={{ color: '#9ca3af', marginRight: 8 }} />} />
                </Form.Item>
                <Flex gap='small' style={{ marginBottom: 20 }}>
                    <Form.Item name="verificationCode" rules={[{ required: true, message: '验证码不可为空' }]} style={{ flex: 1, marginBottom: 0 }}>
                        <Input allowClear size="large" placeholder="6位验证码" prefix={<MailOutlined style={{ color: '#9ca3af', marginRight: 8 }} />} />
                    </Form.Item>
                    <Button loading={sendLoading} disabled={verificationCode.disabled} size="large" onClick={handleWithVerificationCode}>
                        {verificationCode.disabled ? t('{{ti}}s', { ti: verificationCode.seconds }) : t('发送')}
                    </Button>
                </Flex>
                <Form.Item style={{ marginBottom: 32, marginTop: 12 }}>
                    <Button type="primary" htmlType="submit" size="large" block loading={loginLoading} style={{ boxShadow: '0 4px 14px 0 rgba(79, 70, 229, 0.39)' }}>
                        {t('登 录')}
                    </Button>
                </Form.Item>
            </Form>
        </motion.div>
    )
}

export default CaptchaLogin