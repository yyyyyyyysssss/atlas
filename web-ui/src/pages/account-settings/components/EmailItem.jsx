import React, { useState, useEffect, useRef } from 'react';
import { Button, Typography, Flex, Tag, theme, Modal, Form, Input, Steps, Row, Col, App, Radio } from 'antd';
import { MailOutlined, SafetyCertificateOutlined, LeftOutlined, LockOutlined, KeyOutlined } from '@ant-design/icons';
import { motion, AnimatePresence } from 'framer-motion';
import { sendCaptcha } from '../../../services/LoginService';
import { useRequest } from 'ahooks';
import { changeEmail, initEmail, verifyCaptcha, verifyPassword } from '../../../services/UserProfileService';
import UniversalCaptchaVerifier from './verifiers/UniversalCaptchaVerifier';
import UniversalPasswordVerifier from './verifiers/UniversalPasswordVerifier';
import VerifyDropdown from './verifiers';

const { Text, Title } = Typography;

const EmailItem = ({ context, refresh }) => {
    const { token } = theme.useToken()
    const [form] = Form.useForm()
    const [isModalOpen, setIsModalOpen] = useState(false)
    const [currentStep, setCurrentStep] = useState(0)

    const { passwordSet, boundEmail, emailVerified } = context || {}

    const { modal, message } = App.useApp()

    // 倒计时状态
    const [countdown, setCountdown] = useState(0)

    const [sendLoading, setSendLoading] = useState(false)

    const [verifyLoading, setVerifyLoading] = useState(false)

    const [ticket, setTicket] = useState('')

    const verifierRef = useRef(null);

    const { runAsync: sendCaptchaAsync, loading: sendCaptchaLoading } = useRequest(sendCaptcha, {
        manual: true
    })

    const { runAsync: initEmailAsync, loading: initEmailLoading } = useRequest(initEmail, {
        manual: true
    })

    const { runAsync: changeEmailAsync, loading: changeEmailLoading } = useRequest(changeEmail, {
        manual: true
    })

    // 验证码倒计时逻辑
    useEffect(() => {
        let timer;
        if (countdown > 0) {
            timer = setTimeout(() => setCountdown(countdown - 1), 1000);
        }
        return () => clearTimeout(timer);
    }, [countdown]);

    const handleEmailAction = () => {
        setCurrentStep(boundEmail ? 0 : 1);
        setIsModalOpen(true);
    }

    // 发送验证码接口
    const handleSendCode = async () => {
        try {
            await form.validateFields(['newEmail']);
            const targetEmail = form.getFieldValue('newEmail');
            setSendLoading(true);
            // 💡 这里对接你的后台发送接口
            await sendCaptchaAsync({
                target: targetEmail,
                captchaType: 'email',
                securityScene: 'MODIFY_EMAIL'
            })

            message.success(`验证码已成功发送至 ${targetEmail}`);
            setCountdown(60);
        } catch (error) {
            // 格式校验失败
        } finally {
            setSendLoading(false);
        }
    };

    // 下一步按钮的处理（需要验证第 0 步的表单项）
    const handleNextStep = async () => {
        if (!verifierRef.current) return;
        try {
            const { verified, ticket } = await verifierRef.current.onVerify();
            if (verified) {
                setCurrentStep(1);
                setCountdown(0)
                setTicket(ticket)
            }
        } catch (error) {
            if (error?.message) {
                message.error(error.message);
            }
        }
    };

    // 最终提交
    const handleSubmit = async () => {
        try {
            // 校验新邮箱和新验证码
            const values = await form.validateFields(['newEmail', 'newCode']);
            if (boundEmail) {
                await changeEmailAsync({
                    newEmail: values.newEmail,
                    code: values.newCode,
                    ticket: ticket
                })
            } else {
                await initEmailAsync({
                    email: values.newEmail,
                    code: values.newCode
                })
            }
            message.success(boundEmail ? '邮箱变更成功' : '邮箱绑定成功');
            handleCancel();
            refresh?.();
        } catch (error) {
            // 校验未通过
        }
    };

    const handleCancel = () => {
        setIsModalOpen(false)
        setCurrentStep(0)
        setCountdown(0)
        setTicket('')
        form.resetFields()
    };

    return (
        <>
            <Flex
                justify="space-between"
                align="center"
                style={{
                    padding: '20px 0',
                    borderBottom: `1px solid ${token.colorBorderSecondary}`
                }}
            >
                {/* 左侧：图标 + 标题与描述 */}
                <Flex gap={16} align="flex-start" style={{ flex: 1 }}>
                    <div style={{ padding: 12, background: token.colorFillAlter, borderRadius: '50%', display: 'flex' }}>
                        <MailOutlined style={{ color: token.colorPrimary, fontSize: 20 }} />
                    </div>
                    <Flex vertical gap={4}>
                        <Text strong style={{ fontSize: 16 }}>电子邮箱</Text>
                        <Text style={{ color: token.colorTextDescription, fontSize: 14, lineHeight: '22px' }}>
                            {boundEmail
                                ? `已绑定：${boundEmail}。用于接收安全通知、重置密码及验证码登录。`
                                : '未绑定电子邮箱。绑定后可用于接收通知与找回密码。'}
                        </Text>
                    </Flex>
                </Flex>

                {/* 右侧：标签 + 操作按钮 */}
                <Flex align="center" gap={12} style={{ justifyContent: 'flex-end' }}>
                    {boundEmail && (
                        <Tag color={emailVerified ? "success" : "warning"} bordered={false}>
                            {emailVerified ? "已验证" : "未验证"}
                        </Tag>
                    )}
                    <Button
                        type={boundEmail ? "default" : "primary"}
                        onClick={handleEmailAction}
                    >
                        {boundEmail ? "修改" : "绑定"}
                    </Button>
                </Flex>
            </Flex>

            <Modal
                title={
                    <div style={{ marginBottom: 4 }}>
                        <Title level={4} style={{ margin: 0, fontWeight: 600 }}>
                            {!boundEmail ? '绑定电子邮箱' : currentStep === 0 ? '第一步：安全身份验证' : '第二步：绑定新邮箱'}
                        </Title>
                        <Text type="secondary" style={{ fontSize: 13, fontWeight: 400 }}>
                            {!boundEmail
                                ? '请为您的账号绑定一个安全的电子邮箱'
                                : currentStep === 0
                                    ? '为了您的账户安全，修改前请先验证当前绑定的旧邮箱'
                                    : '旧邮箱验证已通过，请输入您的新邮箱地址'}
                        </Text>
                    </div>
                }
                open={isModalOpen}
                onCancel={handleCancel}
                footer={null}
                width={440}
                centered
                destroyOnHidden
            >
                {/* 仅在“修改/换绑”且有旧邮箱时，才展示步骤条 */}
                {boundEmail && (
                    <Steps
                        current={currentStep}
                        size="small"
                        items={[{ title: '验证身份' }, { title: '绑定新邮箱' }]}
                        style={{ marginBottom: 24, marginTop: 12 }}
                    />
                )}

                {/* 统一外层 Form，取消原带的红星标识 */}

                <AnimatePresence mode="wait">
                    {currentStep === 0 ? (
                        // 💻 步骤 0：验证旧邮箱
                        <motion.div
                            key="step0"
                            initial={{ opacity: 0, x: -10 }}
                            animate={{ opacity: 1, x: 0 }}
                            exit={{ opacity: 0, x: 10 }}
                            transition={{ duration: 0.2 }}
                        >
                            <VerifyDropdown
                                verifierRef={verifierRef}
                                context={context}
                                scene="MODIFY_EMAIL"
                                onLoadingChange={(loading) => setVerifyLoading(loading)}
                            />
                        </motion.div>
                    ) : (
                        // 💻 步骤 1：填写新邮箱 (换绑或纯绑定公用此界面)
                        <motion.div
                            key="step1"
                            initial={{ opacity: 0, x: 10 }}
                            animate={{ opacity: 1, x: 0 }}
                            exit={{ opacity: 0, x: -10 }}
                            transition={{ duration: 0.2 }}
                        >
                            <Form form={form} layout="vertical" requiredMark={false} style={{ minHeight: 160, marginTop: 16 }}>
                                <Form.Item
                                    name="newEmail"
                                    label={<Text style={{ fontWeight: 500, fontSize: 13 }}>新邮箱地址</Text>}
                                    rules={[
                                        { required: true, message: '请输入新邮箱地址' },
                                        { type: 'email', message: '请输入合法的邮箱格式' },
                                        {
                                            validator: (_, value) => {
                                                if (value && value === boundEmail) {
                                                    return Promise.reject(new Error('新邮箱不能与当前绑定的旧邮箱相同'));
                                                }
                                                return Promise.resolve();
                                            }
                                        }
                                    ]}
                                >
                                    <Input size="large" placeholder="example@domain.com" style={{ borderRadius: 8 }} />
                                </Form.Item>

                                <Form.Item
                                    name="newCode"
                                    label={<Text style={{ fontWeight: 500, fontSize: 13 }}>验证码</Text>}
                                    rules={[{ required: true, message: '请输入新邮箱验证码' }, { len: 6, message: '验证码为 6 位数字和字母' }]}
                                >
                                    <Row gap={8} wrap={false}>
                                        <Col flex="auto">
                                            <Input size="large" placeholder="输入新邮箱验证码" style={{ borderRadius: 8 }} prefix={<SafetyCertificateOutlined style={{ color: token.colorTextPlaceholder }} />} />
                                        </Col>
                                        <Col flex="none" style={{ marginLeft: 8 }}>
                                            <Button size="large" disabled={countdown > 0} loading={sendLoading} onClick={handleSendCode} style={{ width: 110, borderRadius: 8 }}>
                                                {countdown > 0 ? `${countdown}s` : '获取验证码'}
                                            </Button>
                                        </Col>
                                    </Row>
                                </Form.Item>
                            </Form>
                        </motion.div>
                    )}
                </AnimatePresence>


                {/* 底部操作按钮区域 */}
                <Flex justify="space-between" align="center" style={{ marginTop: 24 }}>
                    {/* 如果在第二步，且当前是换绑流程，允许点左下角按钮返回上一步 */}
                    {currentStep === 1 && boundEmail ? (
                        <Button type="text" icon={<LeftOutlined />} onClick={() => { setCurrentStep(0); setCountdown(0); }}>
                            返回上一步
                        </Button>
                    ) : <div />}

                    <Flex gap={12}>
                        <Button onClick={handleCancel}>取消</Button>
                        {currentStep === 0 ? (
                            <Button type="primary" onClick={handleNextStep} loading={verifyLoading}>下一步</Button>
                        ) : (
                            <Button type="primary" onClick={handleSubmit} loading={initEmailLoading || changeEmailLoading}>完成修改</Button>
                        )}
                    </Flex>
                </Flex>
            </Modal>
        </>
    );
};

export default EmailItem;