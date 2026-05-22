import React, { useState, useEffect, useRef } from 'react';
import { Button, Typography, Flex, theme, Modal, Form, Input, Progress, Row, Col, Space, App } from 'antd';
import { CheckCircleFilled, CloseCircleOutlined, LockOutlined, MailOutlined, KeyOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { changePassword, initPassword } from '../../../services/UserProfileService';
import { sendCaptcha } from '../../../services/LoginService';
import { useRequest } from 'ahooks';
import { motion, AnimatePresence } from 'framer-motion';
import UniversalCaptchaVerifier from './UniversalCaptchaVerifier';

const { Text, Title } = Typography;

// 💡 增加了 boundEmail 属性传入，用于忘记密码时给原邮箱发验证码
const PasswordItem = ({ passwordSet, boundEmail, refresh }) => {
    const { token } = theme.useToken();
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [form] = Form.useForm();
    const [passwordStrength, setPasswordStrength] = useState(0);
    const [password, setPassword] = useState('');

    // 💡 新增：验证模式 'change'(密码变动) | 'reset'(邮箱重置)
    const [mode, setMode] = useState('change');
    const [countdown, setCountdown] = useState(0);
    const [sendLoading, setSendLoading] = useState(false);

    const { message } = App.useApp();

    const { runAsync: changePasswordAsync, loading: changeLoading } = useRequest(changePassword, { manual: true });
    const { runAsync: initPasswordAsync, loading: initLoading } = useRequest(initPassword, { manual: true });
    const { runAsync: sendCaptchaAsync } = useRequest(sendCaptcha, { manual: true });

    const verifierRef = useRef(null);

    // 监听倒计时
    useEffect(() => {
        let timer;
        if (countdown > 0) {
            timer = setTimeout(() => setCountdown(countdown - 1), 1000);
        }
        return () => clearTimeout(timer);
    }, [countdown]);

    // 打开弹窗重置状态
    const handleOpenModal = () => {
        setMode('change');
        setIsModalOpen(true);
    };

    const handleCancel = () => {
        setIsModalOpen(false);
        form.resetFields();
        setPasswordStrength(0);
        setPassword('');
        setCountdown(0);
    };

    // 发送旧邮箱验证码逻辑
    const handleSendAction = async (targetMail) => {
        await sendCaptchaAsync({
            target: targetMail,
            captchaType: 'email',
            captchaScene: 'RESET_PASSWORD'
        });
        message.success(`验证码已成功发送至安全邮箱`)
    };

    const checkStrength = (value) => {
        if (!value) return 0;
        let strength = 0;
        if (value.length >= 8) strength += 1;
        if (/[A-Z]/.test(value)) strength += 1;
        if (/[0-9]/.test(value)) strength += 1;
        if (/[^A-Za-z0-9]/.test(value)) strength += 1;
        return strength;
    };

    const getStrengthConfig = (score) => {
        if (score <= 1) return { color: token.colorError, label: '弱安全性' };
        if (score === 2) return { color: token.colorWarning, label: '中等强度' };
        if (score === 3) return { color: token.colorSuccess, label: '高安全性' };
        return { color: token.colorLink, label: '极强密码' };
    };

    const currentStrength = getStrengthConfig(passwordStrength);

    const rulesCheck = {
        length: password.length >= 8,
        mixed: /[A-Z]/.test(password),
        digit: /[0-9]/.test(password),
        special: /[^A-Za-z0-9]/.test(password)
    };

    const handleOk = async () => {
        try {
            // 公共新密码校验
            const pwdValues = await form.validateFields(['newPassword', 'confirmPassword'])
            if (!passwordSet) {
                await initPasswordAsync({
                    password: pwdValues.newPassword,
                    confirmPassword: pwdValues.confirmPassword,
                });
                message.success('初始密码设置成功');
                handleCancel();
                refresh?.();
                return
            }
            let captchaCode = null
            // 旧密码模式：只校验外层 Form 的旧密码字段
            if (mode === 'change') {
                await form.validateFields(['oldPassword']);
            } else {
                // 邮箱验证码模式：必须调用子组件暴露的句柄进行校验与取值
                await verifierRef.current?.validate()
                captchaCode = verifierRef.current?.getCaptchaCode()
            }
            await changePasswordAsync({
                verifyMethod: mode, // 'change' 或 'reset'
                oldPassword: form.getFieldValue('oldPassword'),
                code: captchaCode,   // 完美注入从子组件拿到的验证码
                newPassword: pwdValues.newPassword,
                confirmPassword: pwdValues.confirmPassword,
            });
            message.success('密码重置成功')

            handleCancel();
            refresh?.();
        } catch (error) {
            console.error(error)
        }
    };

    return (
        <>
            <Flex justify="space-between" align="center" style={{ padding: '20px 0', borderBottom: `1px solid ${token.colorBorderSecondary}` }}>
                <Flex gap={16} align="flex-start" style={{ flex: 1 }}>
                    <div style={{ padding: 12, background: token.colorFillAlter, borderRadius: '50%' }}>
                        <LockOutlined style={{ color: token.colorPrimary, fontSize: 20 }} />
                    </div>
                    <Flex vertical gap={4}>
                        <Text strong style={{ fontSize: 16 }}>登录密码</Text>
                        <Text type="secondary" style={{ fontSize: 14 }}>{passwordSet ? '已设置。建议定期更改以提升安全性。' : '未设置。请尽快设置以保护账号。'}</Text>
                    </Flex>
                </Flex>
                <Button type={passwordSet ? "default" : "primary"} onClick={handleOpenModal}>
                    {passwordSet ? '修改密码' : '设置密码'}
                </Button>
            </Flex>

            <Modal
                title={
                    <div style={{ marginBottom: 4 }}>
                        <Title level={4} style={{ margin: 0, fontWeight: 600 }}>
                            {!passwordSet ? '初始化安全密码' : mode === 'change' ? '变更安全密码' : '通过安全邮箱重置密码'}
                        </Title>
                        <Text type="secondary" style={{ fontSize: 13, fontWeight: 400 }}>
                            {!passwordSet
                                ? '请为您的账户设置一个符合安全合规要求的高强度密码'
                                : mode === 'change'
                                    ? '为了您的账户安全，变更密码前请先验证当前旧密码'
                                    : `验证码将发送至您的安全邮箱`}
                        </Text>
                    </div>
                }
                open={isModalOpen}
                onOk={handleOk}
                onCancel={handleCancel}
                confirmLoading={changeLoading || initLoading}
                styles={{ body: { paddingTop: 20 } }}
                afterClose={() => handleCancel()}
                width={440}
                centered
                destroyOnHidden
            >

                <Form form={form} layout="vertical" requiredMark={false}>
                    {/* 使用 AnimatePresence 管理旧密码框与邮箱验证码框的平滑轮替 */}
                    <AnimatePresence mode="wait">
                        {passwordSet && (
                            mode === 'change' ? (
                                <motion.div
                                    key="old-pwd-field"
                                    initial={{ opacity: 0, x: -10 }}
                                    animate={{ opacity: 1, x: 0 }}
                                    exit={{ opacity: 0, x: 10 }}
                                    transition={{ duration: 0.15 }}
                                >
                                    <Form.Item
                                        label={<Text style={{ fontWeight: 500, fontSize: 13 }}>当前密码</Text>}
                                        name="oldPassword"
                                        rules={[{ required: true, message: '请输入当前密码' }]}
                                        style={{ marginBottom: 4 }}
                                    >
                                        <Input.Password
                                            placeholder="输入旧密码验证身份"
                                            size="large"
                                            style={{ borderRadius: 8 }}
                                        />
                                    </Form.Item>

                                    {/* 💡 引导切入点：忘记密码 */}
                                    <Flex justify="flex-end" style={{ marginBottom: 20 }}>
                                        <Button
                                            type="link"
                                            size="small"
                                            icon={<MailOutlined />}
                                            disabled={!boundEmail} // 如果没绑定邮箱则无法使用此通道
                                            onClick={() => { setMode('reset'); form.setFieldValue('oldPassword', ''); }}
                                            style={{ fontSize: 12, color: token.colorTextDescription, padding: '4px 0' }}
                                        >
                                            {boundEmail ? '忘记密码？改用安全邮箱验证' : '未绑定安全邮箱，不可通过邮箱找回'}
                                        </Button>
                                    </Flex>
                                </motion.div>
                            ) : (
                                <motion.div
                                    key="email-code-field"
                                    initial={{ opacity: 0, x: 10 }}
                                    animate={{ opacity: 1, x: 0 }}
                                    exit={{ opacity: 0, x: -10 }}
                                    transition={{ duration: 0.15 }}
                                >
                                    <UniversalCaptchaVerifier
                                        verifierRef={verifierRef}
                                        target={boundEmail}
                                        targetLabel="安全验证接收邮箱"
                                        codeLabel="安全验证码"
                                        onSendAction={handleSendAction}
                                    />

                                    {/* 💡 引导切入点：返回密码验证 */}
                                    <Flex justify="flex-end" style={{ marginBottom: 20 }}>
                                        <Button
                                            type="link"
                                            size="small"
                                            icon={<KeyOutlined />}
                                            onClick={() => { setMode('change'); form.setFieldValue('emailCode', ''); }}
                                            style={{ fontSize: 12, color: token.colorTextDescription, padding: '4px 0' }}
                                        >
                                            想起来了？返回使用旧密码验证
                                        </Button>
                                    </Flex>
                                </motion.div>
                            )
                        )}
                    </AnimatePresence>

                    {/* 以下为公共部分：新密码输入与强度检测 */}
                    <Form.Item
                        label={<Text style={{ fontWeight: 500, fontSize: 13 }}>新密码</Text>}
                        name="newPassword"
                        rules={[
                            { required: true, message: '请输入新密码' },
                            {
                                validator: (_, value) => {
                                    const oldPassword = form.getFieldValue('oldPassword');
                                    if (passwordSet && mode === 'change' && oldPassword && value === oldPassword) {
                                        return Promise.reject(new Error('新密码不能与原密码相同'));
                                    }
                                    return Promise.resolve();
                                }
                            }
                        ]}
                    >
                        <Input.Password
                            placeholder="输入新密码"
                            size="large"
                            style={{ borderRadius: 8 }}
                            onChange={(e) => {
                                const val = e.target.value;
                                setPassword(val);
                                setPasswordStrength(checkStrength(val));
                            }}
                        />
                    </Form.Item>

                    {/* 密码强度动态面板 */}
                    <AnimatePresence>
                        {password.length > 0 && (
                            <motion.div
                                initial={{ opacity: 0, height: 0, y: -10 }}
                                animate={{ opacity: 1, height: 'auto', y: 0 }}
                                exit={{ opacity: 0, height: 0, y: -10 }}
                                transition={{ duration: 0.2 }}
                                style={{
                                    background: token.colorFillAlter,
                                    padding: '12px 16px',
                                    borderRadius: 8,
                                    marginBottom: 24,
                                    border: `1px solid ${token.colorBorderSecondary}`,
                                    overflow: 'hidden'
                                }}
                            >
                                <Flex justify="space-between" align="center" style={{ marginBottom: 8 }}>
                                    <Text style={{ fontSize: 12, color: token.colorTextSecondary }}>安全评级</Text>
                                    <Text style={{ fontSize: 12, fontWeight: 600, color: currentStrength.color }}>
                                        {currentStrength.label}
                                    </Text>
                                </Flex>

                                <Progress
                                    percent={passwordStrength === 0 ? 8 : passwordStrength * 25}
                                    showInfo={false}
                                    strokeColor={currentStrength.color}
                                    size="small"
                                    style={{ margin: '0 0 12px 0' }}
                                />

                                <Space direction="vertical" size={4} style={{ width: '100%' }}>
                                    <Flex align="center" gap={6}>
                                        {rulesCheck.length ? <CheckCircleFilled style={{ color: token.colorSuccess, fontSize: 12 }} /> : <CloseCircleOutlined style={{ color: token.colorTextDescription, fontSize: 12 }} />}
                                        <Text style={{ fontSize: 12 }} type={rulesCheck.length ? 'success' : 'secondary'}>长度达到或超过 8 位</Text>
                                    </Flex>
                                    <Flex align="center" gap={6}>
                                        {rulesCheck.mixed ? <CheckCircleFilled style={{ color: token.colorSuccess, fontSize: 12 }} /> : <CloseCircleOutlined style={{ color: token.colorTextDescription, fontSize: 12 }} />}
                                        <Text style={{ fontSize: 12 }} type={rulesCheck.mixed ? 'success' : 'secondary'}>包含大写英文字母</Text>
                                    </Flex>
                                    <Flex align="center" gap={6}>
                                        {rulesCheck.digit ? <CheckCircleFilled style={{ color: token.colorSuccess, fontSize: 12 }} /> : <CloseCircleOutlined style={{ color: token.colorTextDescription, fontSize: 12 }} />}
                                        <Text style={{ fontSize: 12 }} type={rulesCheck.digit ? 'success' : 'secondary'}>包含数字字符 (0-9)</Text>
                                    </Flex>
                                    <Flex align="center" gap={6}>
                                        {rulesCheck.special ? <CheckCircleFilled style={{ color: token.colorSuccess, fontSize: 12 }} /> : <CloseCircleOutlined style={{ color: token.colorTextDescription, fontSize: 12 }} />}
                                        <Text style={{ fontSize: 12 }} type={rulesCheck.special ? 'success' : 'secondary'}>包含特殊符号或标点</Text>
                                    </Flex>
                                </Space>
                            </motion.div>
                        )}
                    </AnimatePresence>

                    <Form.Item
                        label={<Text style={{ fontWeight: 500, fontSize: 13 }}>确认新密码</Text>}
                        name="confirmPassword"
                        dependencies={['newPassword']}
                        rules={[
                            { required: true, message: '请确认密码' },
                            ({ getFieldValue }) => ({
                                validator(_, value) {
                                    if (!value || getFieldValue('newPassword') === value) return Promise.resolve();
                                    return Promise.reject(new Error('两次输入的密码不一致'));
                                },
                            }),
                        ]}
                    >
                        <Input.Password
                            placeholder="请再次输入新密码"
                            size="large"
                            style={{ borderRadius: 8 }}
                        />
                    </Form.Item>
                </Form>
            </Modal>
        </>
    );
};

export default PasswordItem;