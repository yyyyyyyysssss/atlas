import React, { useState, useEffect, useRef } from 'react';
import { Button, Typography, Flex, Tag, theme, Modal, Form, Input, Steps, Row, Col, App, Radio } from 'antd';
import { MailOutlined, SafetyCertificateOutlined, LeftOutlined, LockOutlined, KeyOutlined } from '@ant-design/icons';
import { motion, AnimatePresence } from 'framer-motion';
import { sendCaptcha } from '../../../services/LoginService';
import { useRequest } from 'ahooks';
import { changeEmail, initEmail, verifyCaptcha, verifyPassword } from '../../../services/AccountService';
import UniversalCaptchaVerifier from './verifiers/UniversalCaptchaVerifier';
import UniversalPasswordVerifier from './verifiers/UniversalPasswordVerifier';
import VerifyDropdown from './verifiers';
import SecurityStepVerify from './verifiers/SecurityStepVerify';

const { Text, Title } = Typography;

const EmailItem = ({ context, refresh }) => {
    const { token } = theme.useToken()
    const [form] = Form.useForm()
    const [isModalOpen, setIsModalOpen] = useState(false)

    const { passwordSet, boundEmail, emailVerified } = context || {}

    const { modal, message } = App.useApp()

    // 倒计时状态
    const [countdown, setCountdown] = useState(0)

    const [sendLoading, setSendLoading] = useState(false)

    const [verifyMethod, setVerifyMethod] = useState()

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

    const handleConfirmSubmit = async (ticket) => {
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
    }

    const handleCancel = () => {
        setIsModalOpen(false)
        setCountdown(0)
        form.resetFields()
        setVerifyMethod(null)
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
                    <div style={{ marginBottom: 16 }}>
                        <Title level={4} style={{ margin: 0, fontWeight: 600 }}>
                            {!boundEmail ? '绑定电子邮箱' : '修改电子邮箱'}
                        </Title>
                    </div>
                }
                open={isModalOpen}
                onCancel={handleCancel}
                width={440}
                centered
                destroyOnHidden
                footer={null}
            >
                <SecurityStepVerify
                    scene="MODIFY_EMAIL"
                    context={context}
                    stepTitle={boundEmail ? "设置邮箱" : "绑定新邮箱"}
                    confirmText="确认修改"
                    confirmLoading={initEmailLoading || changeEmailLoading}
                    initialStep={boundEmail ? 0 : 1}
                    onCancel={handleCancel}
                    onConfirm={handleConfirmSubmit}
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
                </SecurityStepVerify>
            </Modal>
        </>
    );
};

export default EmailItem;