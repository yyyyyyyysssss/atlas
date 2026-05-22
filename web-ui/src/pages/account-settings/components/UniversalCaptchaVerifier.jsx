import React, { useState, useEffect } from 'react';
import { Form, Input, Button, Typography, theme, Space, Card } from 'antd';
import { SafetyCertificateOutlined, MailOutlined } from '@ant-design/icons';

const { Text } = Typography;

/**
 * 经典高质感验证码验证组件（回归最初的纯正版本）
 */
const UniversalCaptchaVerifier = ({
    verifierRef,
    target,
    targetLabel = "安全验证接收账号",
    codeLabel = "验证码",
    placeholder = "请输入验证码",
    onSendAction,
    onVerifyAction,
    errorMsg = "验证码错误或已过期"
}) => {
    const { token } = theme.useToken();
    const [form] = Form.useForm();
    const [countdown, setCountdown] = useState(0);
    const [sendLoading, setSendLoading] = useState(false);

    // 订阅表单值变化，用于动态控制验证码的字间距
    const captchaCodeValue = Form.useWatch('captchaCode', form);

    useEffect(() => {
        let timer;
        if (countdown > 0) {
            timer = setTimeout(() => setCountdown(countdown - 1), 1000);
        }
        return () => clearTimeout(timer);
    }, [countdown]);

    const handleSendCode = async () => {
        if (!onSendAction) return;
        try {
            setSendLoading(true);
            await onSendAction(target);
            setCountdown(60);
        } catch (error) {
            console.error("发送验证码失败:", error);
        } finally {
            setSendLoading(false);
        }
    };

    if (verifierRef) {
        verifierRef.current = {
            getCaptchaCode: () => {
                return form.getFieldValue('captchaCode');
            },
            validate: async () => {
                return await form.validateFields(['captchaCode']);
            },
            onVerify: async () => {
                await form.validateFields(['captchaCode']);
                const code = form.getFieldValue('captchaCode');
                if (onVerifyAction) {
                    const verify = await onVerifyAction(code);
                    if (!verify) {
                        throw new Error(errorMsg);
                    }
                }
                return true;
            },
            reset: () => {
                form.resetFields();
            }
        };
    }

    return (
        <Form form={form} layout="vertical" requiredMark={false} style={{ width: '100%' }} component={false}>

            {/* 1. 使用 Antd Card 打造的经典账号看板 */}
            <Card
                variant="outlined"
                size="small"
                style={{
                    marginBottom: 24,
                    borderRadius: 12,
                    backgroundColor: token.colorFillAlter, // 选用比标准背景略深一层的背景色
                    borderColor: token.colorBorderSecondary, // 选用次级弱边框
                    boxShadow: 'none'
                }}
                styles={{
                    body: {
                        padding: '12px 16px', // ✨ 顺便优化：Antd 5.x 中 bodyStyle 建议用 styles.body 替代
                        display: 'flex',
                        flexDirection: 'column',
                        gap: 2
                    }
                }}
            >
                <Text type="secondary" style={{ fontSize: 11, letterSpacing: '0.06em' }}>
                    {targetLabel.toUpperCase()}
                </Text>
                <Space size={8} style={{ alignItems: 'center' }}>
                    <MailOutlined style={{ color: token.colorTextDescription, fontSize: 13 }} />
                    <Text strong style={{ fontSize: 15, color: token.colorTextHeading }}>
                        {target || '--'}
                    </Text>
                </Space>
            </Card>

            {/* 2. 验证码输入与动作栏 */}
            <Form.Item
                name="captchaCode"
                label={
                    <Text style={{
                        fontWeight: 600,
                        fontSize: 13,
                        color: token.colorText,
                        marginBottom: 4
                    }}>
                        {codeLabel}
                    </Text>
                }
                rules={[{ required: true, message: '验证码不能为空' }]}
            >
                <div style={{ display: 'flex', gap: 10, alignItems: 'flex-start' }}>
                    <div style={{ flex: 1 }}>
                        <Input
                            size="large"
                            placeholder={placeholder}
                            maxLength={6}
                            style={{
                                borderRadius: 10,
                                height: 48,
                                paddingLeft: 16,
                                fontSize: 15,
                                // 输入值后拉开大数字间距，方便核对
                                letterSpacing: captchaCodeValue ? '0.2em' : 'normal',
                                fontWeight: captchaCodeValue ? 600 : 'normal',
                            }}
                            prefix={
                                <SafetyCertificateOutlined
                                    style={{
                                        color: token.colorTextPlaceholder,
                                        marginRight: 4,
                                        fontSize: 16
                                    }}
                                />
                            }
                        />
                    </div>

                    <Button
                        size="large"
                        disabled={countdown > 0 || !target}
                        loading={sendLoading}
                        onClick={handleSendCode}
                        style={{
                            height: 48,
                            minWidth: 116,
                            borderRadius: 10,
                            fontWeight: 500,
                            fontSize: 14,
                            border: countdown > 0 ? `1px solid ${token.colorBorderSecondary}` : undefined,
                            background: countdown > 0 ? token.colorBgContainerDisabled : undefined,
                            color: countdown > 0 ? token.colorTextDisabled : token.colorPrimary,
                            transition: 'all 0.2s cubic-bezier(0.645, 0.045, 0.355, 1)'
                        }}
                    >
                        {countdown > 0 ? `${countdown} 秒后重发` : '获取验证码'}
                    </Button>
                </div>
            </Form.Item>
        </Form>
    );
};

export default UniversalCaptchaVerifier;