import React, { useState, useEffect, useRef } from 'react';
import { Button, Typography, Flex, theme, Modal, Form, Input, Progress, Space, App, Steps, Dropdown } from 'antd';
import { CheckCircleFilled, CloseCircleOutlined, LockOutlined, MailOutlined, KeyOutlined, DownOutlined, LeftOutlined } from '@ant-design/icons';
import { changePassword, initPassword, verifyCaptcha, verifyPassword } from '../../../services/UserProfileService';
import { sendCaptcha } from '../../../services/LoginService';
import { useRequest } from 'ahooks';
import { motion, AnimatePresence } from 'framer-motion';
import UniversalPasswordVerifier from './verifiers/UniversalPasswordVerifier';
import UniversalCaptchaVerifier from './verifiers/UniversalCaptchaVerifier';
import VerifyDropdown from './verifiers';

const { Text, Title } = Typography;

const PasswordItem = ({ context, refresh }) => {
    const { token } = theme.useToken();
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [form] = Form.useForm();


    const { passwordSet, boundEmail } = context || {}

    // 💡 分步核心状态：0 - 验证身份 / 1 - 设置新密码
    const [currentStep, setCurrentStep] = useState(0);

    const [passwordStrength, setPasswordStrength] = useState(0)

    const [password, setPassword] = useState('')

    const [ticket, setTicket] = useState('')

    const [verifyLoading, setVerifyLoading] = useState(false)

    const [verifyMethod, setVerifyMethod] = useState()

    const { message } = App.useApp();

    const { runAsync: changePasswordAsync, loading: changeLoading } = useRequest(changePassword, { manual: true });
    const { runAsync: initPasswordAsync, loading: initLoading } = useRequest(initPassword, { manual: true });


    const verifierRef = useRef(null);

    // 打开弹窗重置状态
    const handleOpenModal = () => {
        // 如果原本就没有设置过密码，直接进入第二步（设置新密码）
        setCurrentStep(passwordSet ? 0 : 1);
        setIsModalOpen(true);
    };

    const handleCancel = () => {
        setIsModalOpen(false);
        form.resetFields();
        setPasswordStrength(0);
        setPassword('');
        setCurrentStep(0);
        setTicket('');
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

    const handlePrev = () => {
        setCurrentStep(0);
        setPassword('');
        setPasswordStrength(0)
    };

    // 💡 下一步（第一步通往第二步）
    const handleNext = async () => {
        try {
            if (!verifierRef.current) return;
            const { verified, ticket } = await verifierRef.current.onVerify()
            if (verified) {
                setCurrentStep(1)
                setTicket(ticket)
            }
        } catch (error) {
            if (error?.message) {
                message.error(error.message);
            }
        }
    };

    // 💡 最终提交（第二步的确定）
    const handleOk = async () => {
        // 校验第二步的密码字段
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
                    <div style={{ marginBottom: 16 }}>
                        <Title level={4} style={{ margin: 0, fontWeight: 600 }}>
                            {!passwordSet ? '初始化安全密码' : '修改登录密码'}
                        </Title>
                    </div>
                }
                open={isModalOpen}
                onCancel={handleCancel}
                width={460}
                centered
                destroyOnHidden
                footer={
                    <Flex justify="space-between" align="center" style={{ width: '100%' }}>
                        {/* 左侧区域：仅在第二步且原先有密码（即有第一步验证流程）时显示带箭头的文本按钮 */}
                        <div>
                            {currentStep === 1 && passwordSet ? (
                                <Button
                                    type="text"
                                    icon={<LeftOutlined />}
                                    onClick={handlePrev}
                                    style={{ paddingLeft: 0 }} // 消除内边距保证图标贴齐左边界
                                >
                                    返回上一步
                                </Button>
                            ) : null}
                        </div>

                        {/* 右侧区域：固定右侧对齐的通用控制按钮 */}
                        <Flex gap={8}>
                            <Button onClick={handleCancel}>取消</Button>
                            {currentStep === 0 ? (
                                <Button type="primary" loading={verifyLoading} onClick={handleNext}>
                                    下一步
                                </Button>
                            ) : (
                                <Button type="primary" loading={changeLoading || initLoading} onClick={handleOk}>
                                    确定修改
                                </Button>
                            )}
                        </Flex>
                    </Flex>
                }
            >
                {/* 💡 增加了 Steps 进度条组件，让分步感知更明确 */}
                {passwordSet && (
                    <Steps
                        current={currentStep}
                        size="small"
                        style={{ marginBottom: 24 }}
                        items={[
                            { title: '验证身份' },
                            { title: '设置新密码' },
                        ]}
                    />
                )}

                <Form form={form} layout="vertical" requiredMark={false}>
                    <AnimatePresence mode="wait">
                        {/* 💡 第一步：身份验证面板 */}
                        {currentStep === 0 && passwordSet && (
                            <motion.div
                                key="step-verify"
                                initial={{ opacity: 0, x: -10 }}
                                animate={{ opacity: 1, x: 0 }}
                                exit={{ opacity: 0, x: 10 }}
                                transition={{ duration: 0.2 }}
                            >
                                <AnimatePresence mode="wait">

                                    <motion.div
                                        key="old-pwd-field"
                                        initial={{ opacity: 0, y: -5 }}
                                        animate={{ opacity: 1, y: 0 }}
                                        exit={{ opacity: 0, y: 5 }}
                                        transition={{ duration: 0.15 }}
                                    >
                                        <VerifyDropdown
                                            value={verifyMethod}
                                            onChange={(value) => setVerifyMethod(value)}
                                            verifierRef={verifierRef}
                                            context={context}
                                            scene="RESET_PASSWORD"
                                            onLoadingChange={(loading) => setVerifyLoading(loading)}
                                        />
                                    </motion.div>
                                </AnimatePresence>
                            </motion.div>
                        )}

                        {/* 💡 第二步：新密码设置面板 */}
                        {currentStep === 1 && (
                            <motion.div
                                key="step-password"
                                initial={{ opacity: 0, x: 10 }}
                                animate={{ opacity: 1, x: 0 }}
                                exit={{ opacity: 0, x: -10 }}
                                transition={{ duration: 0.2 }}
                            >
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
                            </motion.div>
                        )}
                    </AnimatePresence>
                </Form>
            </Modal>
        </>
    );
};

export default PasswordItem;