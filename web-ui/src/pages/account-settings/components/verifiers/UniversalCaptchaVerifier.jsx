import React, { useState, useEffect, useRef } from 'react';
import { Form, Typography, theme, Card, App, Flex } from 'antd';
import { MailOutlined, LoadingOutlined } from '@ant-design/icons';

const { Text, Link } = Typography;

/**
 * 现代极简主义 6 位数字验证码验证组件 (纯粹线条美学版)
 */
const UniversalCaptchaVerifier = ({
    verifierRef,
    target,
    targetLabel = "安全验证接收账号",
    codeLabel = "验证码",
    onSendAction,
    onVerifyAction,
    onSuccess,
    errorMsg = "验证码错误或已过期"
}) => {
    const { token } = theme.useToken();
    const { message } = App.useApp();
    const [form] = Form.useForm();

    const [countdown, setCountdown] = useState(0);
    const [sendLoading, setSendLoading] = useState(false);
    const [verifyLoading, setVerifyLoading] = useState(false);

    const [codeArray, setCodeArray] = useState(['', '', '', '', '', '']);
    const inputRefs = [useRef(), useRef(), useRef(), useRef(), useRef(), useRef()];

    useEffect(() => {
        let timer;
        if (countdown > 0) {
            timer = setTimeout(() => setCountdown(countdown - 1), 1000);
        }
        return () => clearTimeout(timer);
    }, [countdown]);

    /**
     * 🛡️ 1. 纯净的网络验证流
     */
    const executeVerifyWorkflow = async (rawCode) => {
        if (verifyLoading) return;

        const finalCode = rawCode || codeArray.join('');
        if (finalCode.length !== 6) {
            throw new Error('请输入完整的6位验证码');
        }

        try {
            setVerifyLoading(true);
            form.setFieldsValue({ captchaCode: finalCode });
            await form.validateFields(['captchaCode']);

            const result = await onVerifyAction(finalCode);
            if (!result || !result.verified) {
                throw new Error(errorMsg);
            }

            if (onSuccess) {
                onSuccess(result.ticket);
            }
            return { verified: result.verified, ticket: result.ticket };

        } catch (error) {
            const finalErrorMsg = error?.response?.data?.message || error?.message || errorMsg;
            throw new Error(finalErrorMsg);
        } finally {
            setVerifyLoading(false);
        }
    };

    /**
     * 🎯 2. 内部自动触发器
     */
    const handleInternalTrigger = async (currentFullCode) => {
        try {
            await executeVerifyWorkflow(currentFullCode);
        } catch (error) {
            message.error(error.message);
            focusAndClearLast();
        }
    };

    const focusAndClearLast = () => {
        const newArray = [...codeArray];
        newArray[5] = '';
        setCodeArray(newArray);
        setTimeout(() => inputRefs[5].current?.focus(), 50);
    };

    /**
     * 🚀 3. 发送验证码
     */
    const handleSendCode = async () => {
        if (!onSendAction || countdown > 0 || sendLoading || !target) return;
        try {
            setSendLoading(true);
            await onSendAction(target);
            setCountdown(60);
            message.success('验证码已成功发送');
            setTimeout(() => inputRefs[0].current?.focus(), 100);
        } catch (error) {
            const apiErrorMsg = error?.response?.data?.message || error?.message || '验证码发送失败，请稍后重试';
            message.error(apiErrorMsg);
        } finally {
            setSendLoading(false);
        }
    };

    /**
     * ⌨️ 4. 输入控制与联动
     */
    const handleInputChange = (value, index) => {
        const cleanValue = value.replace(/[^0-9]/g, '').slice(-1);
        const newArray = [...codeArray];
        newArray[index] = cleanValue;
        setCodeArray(newArray);

        if (cleanValue && index < 5) {
            inputRefs[index + 1].current?.focus();
        }

        if (cleanValue && index === 5) {
            const fullCode = newArray.join('');
            handleInternalTrigger(fullCode);
        }
    };

    const handleKeyDown = (e, index) => {
        if (e.key === 'Backspace') {
            if (!codeArray[index] && index > 0) {
                const newArray = [...codeArray];
                newArray[index - 1] = '';
                setCodeArray(newArray);
                inputRefs[index - 1].current?.focus();
                e.preventDefault();
            } else if (codeArray[index]) {
                const newArray = [...codeArray];
                newArray[index] = '';
                setCodeArray(newArray);
            }
        }
    };

    const handlePaste = (e) => {
        e.preventDefault();
        const pasteData = e.clipboardData.getData('text').replace(/[^0-9]/g, '').slice(0, 6);
        if (pasteData.length > 0) {
            const newArray = [...codeArray];
            for (let i = 0; i < pasteData.length; i++) {
                newArray[i] = pasteData[i];
            }
            setCodeArray(newArray);
            
            const nextFocusIndex = Math.min(pasteData.length, 5);
            inputRefs[nextFocusIndex].current?.focus();

            if (pasteData.length === 6) {
                handleInternalTrigger(pasteData);
            }
        }
    };

    if (verifierRef) {
        verifierRef.current = {
            getValue: () => codeArray.join(''),
            validate: async () => {
                if (codeArray.join('').length !== 6) throw new Error('验证码必须为6位');
                return true;
            },
            onVerify: async () => {
                return await executeVerifyWorkflow();
            },
            reset: () => {
                setCodeArray(['', '', '', '', '', '']);
                form.resetFields();
            }
        };
    }

    return (
        <Form form={form} layout="vertical" requiredMark={false} style={{ width: '100%' }} component={false}>
            
            {/* 1. 账号看板 */}
            <Card
                variant="outlined"
                size="small"
                style={{
                    marginBottom: 24,
                    borderRadius: 12,
                    backgroundColor: token.colorFillAlter,
                    borderColor: token.colorBorderSecondary,
                }}
                styles={{
                    body: { padding: '12px 16px', display: 'flex', flexDirection: 'column', gap: 2 }
                }}
            >
                <Text type="secondary" style={{ fontSize: 11, letterSpacing: '0.06em' }}>
                    {targetLabel.toUpperCase()}
                </Text>
                <Flex align="center" gap={8}>
                    <MailOutlined style={{ color: token.colorTextDescription, fontSize: 13 }} />
                    <Text strong style={{ fontSize: 14, color: token.colorTextHeading }}>
                        {target || '--'}
                    </Text>
                </Flex>
            </Card>

            {/* 2. 核心 6 矩阵线条输入 UI */}
            <Flex vertical gap={12} style={{ marginBottom: 20 }}>
                <Text style={{ fontWeight: 600, fontSize: 13, color: token.colorTextDescription }}>
                    {codeLabel}
                </Text>
                
                <Flex gap={4} justify="space-between" align="center" style={{ width: '100%' }}>
                    {codeArray.map((digit, index) => (
                        <div key={index} style={{ width: '14%' }}>
                            <input
                                ref={inputRefs[index]}
                                type="text"
                                inputMode="numeric"
                                pattern="[0-9]*"
                                maxLength={1}
                                value={digit}
                                disabled={verifyLoading}
                                onChange={(e) => handleInputChange(e.target.value, index)}
                                onKeyDown={(e) => handleKeyDown(e, index)}
                                onPaste={index === 0 ? handlePaste : undefined}
                                className={`captcha-minimal-line ${digit ? 'has-value' : ''}`}
                                style={{
                                    width: '100%',
                                    height: 52,
                                    textAlign: 'center',
                                    fontSize: 26, 
                                    fontWeight: 700,
                                    color: token.colorTextHeading,
                                    background: 'transparent', 
                                    border: 'none',
                                    // 默认状态下：一条非常低调、淡雅的次级边框底线
                                    borderBottom: `2px solid ${token.colorBorder}`,
                                    outline: 'none',
                                    boxSizing: 'border-box',
                                    transition: 'all 0.18s cubic-bezier(0.4, 0, 0.2, 1)',
                                    cursor: verifyLoading ? 'not-allowed' : 'text',
                                    padding: 0
                                }}
                            />
                        </div>
                    ))}
                </Flex>
            </Flex>

            {/* 3. 底部轻量优雅倒计时 UI */}
            <Flex justify="space-between" align="center" style={{ padding: '0 2px' }}>
                <div>
                    {verifyLoading && (
                        <Text type="secondary" style={{ fontSize: 13, fontWeight: 500 }}>
                            <LoadingOutlined style={{ marginRight: 6, color: token.colorPrimary }} />
                            安全校验中
                        </Text>
                    )}
                </div>
                
                <div style={{ height: 22 }}>
                    {countdown > 0 ? (
                        <Flex align="center" gap={6}>
                            <span className="countdown-pulse-dot" style={{ background: token.colorTextDisabled }} />
                            <Text type="secondary" style={{ fontSize: 12, letterSpacing: '0.02em' }}>
                                {countdown}s 后可重发
                            </Text>
                        </Flex>
                    ) : (
                        <Link 
                            disabled={!target || sendLoading || verifyLoading} 
                            onClick={handleSendCode}
                            style={{ fontSize: 12, fontWeight: 600, color: token.colorPrimary, letterSpacing: '0.02em' }}
                        >
                            {sendLoading ? '正在发送...' : '重新获取验证码'}
                        </Link>
                    )}
                </div>
            </Flex>

            {/* 🛠️ 纯线条动效 CSS 样式表 */}
            <style>{`
                /* 🎯 当格子聚焦时：线条变成宽阔厚实的品牌色，并带有一丝优雅的向上微提，引导视觉焦点 */
                .captcha-minimal-line:focus {
                    border-bottom: 2.5px solid ${token.colorPrimary} !important;
                    transform: translateY(-2px);
                }
                
                /* 🎯 当格子里已经有数字时：线条颜色自动沉淀为暗色，暗示“已填入” */
                .captcha-minimal-line.has-value {
                    border-bottom-color: ${token.colorTextHeading};
                }
                
                /* 🎯 底部倒计时脉冲点 */
                .countdown-pulse-dot {
                    width: 5px;
                    height: 5px;
                    border-radius: 50%;
                    display: inline-block;
                    animation: circleShrink 1.2s infinite ease-in-out;
                }
                @keyframes circleShrink {
                    0% { transform: scale(1); opacity: 0.4; }
                    50% { transform: scale(1.3); opacity: 1; }
                    100% { transform: scale(1); opacity: 0.4; }
                }
            `}</style>
        </Form>
    );
};

export default UniversalCaptchaVerifier;