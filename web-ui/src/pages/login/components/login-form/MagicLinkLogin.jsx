import { Form, Input, Button, Card, Flex, Typography, App, Avatar, Divider, Dropdown, ConfigProvider, QRCode, theme } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined, GithubOutlined, GoogleOutlined, KeyOutlined, ScanOutlined, CheckCircleFilled } from '@ant-design/icons';
import { useRequest } from 'ahooks';
import { AnimatePresence, motion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { captchaLogin, sendCaptcha, sendOttLink } from '../../../../services/LoginService';
import { useEffect, useRef, useState } from 'react';
import useFullParams from '../../../../hooks/useFullParams';

const MagicLinkLogin = ({ onSuccess }) => {


    const { t } = useTranslation()

    const [form] = Form.useForm()

    const { message } = App.useApp()

    const timerRef = useRef()

    const { targetUrl } = useFullParams()

    const [verificationCode, setVerificationCode] = useState({
        disabled: false,
        time: 60,
        seconds: 0,
    })

    const { runAsync: sendOttLinkAsync, loading: sendOttLinkLoading } = useRequest(sendOttLink, {
        manual: true
    })

    useEffect(() => {
        return () => { if (timerRef.current) clearInterval(timerRef.current); };
    }, [])

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
            if (!e.errorFields) {
                console.error('发送失败:', e);
            }
        }
    }

    return (
        <motion.div
            key="magic-login"
            initial={{ opacity: 0, y: 10 }}
            animate={{ opacity: 1, y: 0 }}
            exit={{ opacity: 0, y: -10 }}
            transition={{ duration: 0.2 }}
        >
            <Form form={form} onFinish={onFinish} layout="vertical">
                <Typography.Paragraph type="secondary" style={{ textAlign: 'center', marginBottom: 20, fontSize: 13 }}>
                    输入您的账号或邮箱，我们将向该账号绑定的邮箱发送登录链接。
                </Typography.Paragraph>
                <Form.Item name="magicUsername" rules={[{ required: true, message: '账号不可为空' }]} style={{ marginBottom: 32 }}>
                    <Input allowClear size="large" placeholder="输入用户名或邮箱" prefix={<UserOutlined style={{ color: '#9ca3af', marginRight: 8 }} />} />
                </Form.Item>

                <Form.Item style={{ marginBottom: 32 }}>
                    <Button
                        type="primary"
                        size="large"
                        block
                        htmlType="submit"
                        loading={sendOttLinkLoading}
                        disabled={verificationCode.disabled}
                        style={{ boxShadow: verificationCode.disabled === true ? '' : '0 4px 14px 0 rgba(79, 70, 229, 0.39)' }}
                    >
                        {verificationCode.disabled ? t('请求已发送 ({{ti}}s)', { ti: verificationCode.seconds }) : t('发送快捷登录链接')}
                    </Button>
                </Form.Item>
            </Form>
        </motion.div>
    )
}

export default MagicLinkLogin;