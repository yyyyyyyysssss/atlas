import React, { useState, useEffect, useRef } from 'react';
import { Button, Typography, Flex, theme, Modal, Form, Input, Progress, Space, App, Steps, Dropdown } from 'antd';
import { CheckCircleFilled, CloseCircleOutlined, LockOutlined, MailOutlined, KeyOutlined, DownOutlined, LeftOutlined } from '@ant-design/icons';
import { changePassword, initPassword, verifyCaptcha, verifyPassword } from '../../../services/AccountService';
import { sendCaptcha } from '../../../services/LoginService';
import { useRequest } from 'ahooks';
import { motion, AnimatePresence } from 'framer-motion';
import UniversalPasswordVerifier from './verifiers/UniversalPasswordVerifier';
import UniversalCaptchaVerifier from './verifiers/UniversalCaptchaVerifier';
import VerifyDropdown from './verifiers';
import SecurityStepVerify from './verifiers/SecurityStepVerify';

const { Text, Title } = Typography;

const PasswordItem = ({ context, refresh }) => {
    const { token } = theme.useToken();
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [form] = Form.useForm();

    const { passwordSet, boundEmail } = context || {}

    const [passwordStrength, setPasswordStrength] = useState(0)

    const [password, setPassword] = useState('')

    const [verifyMethod, setVerifyMethod] = useState()

    const { message } = App.useApp();

    const { runAsync: changePasswordAsync, loading: changeLoading } = useRequest(changePassword, { manual: true });
    const { runAsync: initPasswordAsync, loading: initLoading } = useRequest(initPassword, { manual: true });


    const verifierRef = useRef(null);

    // 打开弹窗重置状态
    const handleOpenModal = () => {
        setIsModalOpen(true);
    };

    const handleCancel = () => {
        setIsModalOpen(false);
        form.resetFields();
        setPasswordStrength(0);
        setPassword('');
        setVerifyMethod(null)
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


    const handleConfirmPasswordSubmit = async (ticket) => {
        const pwdValues = await form.validateFields(['newPassword', 'confirmPassword']);

        if (passwordSet) {
            await changePasswordAsync({
                newPassword: pwdValues.newPassword,
                confirmPassword: pwdValues.confirmPassword,
                ticket: ticket
            })
        } else {
            await initPasswordAsync({
                password: pwdValues.newPassword,
                confirmPassword: pwdValues.confirmPassword,
            });
        }
        message.success(passwordSet ? '密码重置成功' : '初始密码设置成功');
        handleCancel();
        refresh?.();
    }

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
                    <div style={{ marginBottom: 16 }}>
                        <Title level={4} style={{ margin: 0, fontWeight: 600 }}>
                            {!passwordSet ? '初始化安全密码' : '修改登录密码'}
                        </Title>
                    </div>
                }
                open={isModalOpen}
                onCancel={handleCancel}
                width={460}
                footer={null}
                centered
                destroyOnHidden
            >
                <SecurityStepVerify
                    scene="RESET_PASSWORD"
                    captchaScene='RESET_PASSWORD'
                    context={context}
                    stepTitle={passwordSet ? "设置新密码" : "填写新密码"}
                    confirmText={passwordSet ? "确认修改" : "确认设置"}
                    confirmLoading={changeLoading || initLoading}
                    initialStep={passwordSet ? 0 : 1}
                    onCancel={handleCancel}
                    onConfirm={handleConfirmPasswordSubmit}
                >
                    <Form form={form} layout="vertical" requiredMark={false}>
                        <Form.Item
                            label={<Text style={{ fontWeight: 500, fontSize: 13 }}>新密码</Text>}
                            name="newPassword"
                            rules={[
                                { required: true, message: '请输入新密码' },
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
                </SecurityStepVerify>
            </Modal>
        </>
    );
};

export default PasswordItem;