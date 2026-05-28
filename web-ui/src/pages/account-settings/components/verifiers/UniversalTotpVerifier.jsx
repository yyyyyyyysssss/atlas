import React, { useState, useEffect, useRef } from 'react';
import { Form, Typography, theme, App, Flex, Input } from 'antd';
import { LoadingOutlined, MobileOutlined } from '@ant-design/icons';
import { Shield } from 'lucide-react';

const { Text } = Typography;

/**
 * 现代极简主义 6 位 TOTP 动态令牌验证组件 (Input.OTP 极致重构版)
 */
const UniversalTotpVerifier = ({
    verifierRef,
    codeLabel = "请输入 6 位动态验证码",
    onVerifyAction,
    onSuccess,
    errorMsg = "动态验证码不正确或已过期"
}) => {
    const { token } = theme.useToken();
    const { message } = App.useApp();
    const [form] = Form.useForm();

    const [verifyLoading, setVerifyLoading] = useState(false);
    const [totpCode, setTotpCode] = useState('');

    const otpRef = useRef(null)

    useEffect(() => {
        if (otpRef.current) {
            otpRef.current.focus()
        }
    }, [])

    /**
     * 🛡️ 核心网络校验流
     */
    const executeVerifyWorkflow = async (rawCode) => {
        if (verifyLoading) return;

        const finalCode = rawCode || totpCode;
        if (finalCode.length !== 6) {
            throw new Error('请输入完整的 6 位动态码');
        }

        try {
            setVerifyLoading(true);

            // 触发外部传入的校验行为 (调用后端验证接口)
            const result = await onVerifyAction(finalCode);

            if (result === false || (result && result.verified === false)) {
                throw new Error(errorMsg);
            }

            return result;
        } catch (error) {
            const finalErrorMsg = error?.response?.data?.message || error?.message || errorMsg;
            throw new Error(finalErrorMsg);
        } finally {
            setVerifyLoading(false);
        }
    };

    /**
     * 🎯 当 6 位全部输完时，全自动回调
     */
    const handleCodeComplete = async (value) => {
        try {
            const result = await executeVerifyWorkflow(value);
            if (onSuccess) {
                onSuccess(result);
            }
        } catch (error) {
            message.error(error.message);
            const nextValue = value.slice(0, 5)
            setTotpCode(nextValue)
            requestAnimationFrame(() => {
                const inputs = otpRef.current?.nativeElement?.querySelectorAll('input')
                if (inputs?.[5]) {
                    inputs[5].focus();
                }
            })
        }
    };

    /**
     * 穿透命令给父组件
     */
    if (verifierRef) {
        verifierRef.current = {
            getValue: () => totpCode,
            validate: async () => {
                if (totpCode.length !== 6) throw new Error('验证码必须为 6 位');
                return true;
            },
            onVerify: async () => {
                return await executeVerifyWorkflow()
            },
            reset: () => {
                setTotpCode('')
            }
        };
    }

    return (
        <Form form={form} layout="vertical" style={{ width: '100%' }} component={false}>
            <Flex vertical gap={12} style={{ marginBottom: 16 }}>
                <Text style={{ fontWeight: 600, fontSize: 13, color: token.colorTextDescription }}>
                    {codeLabel}
                </Text>

                <Input.OTP
                    ref={otpRef}
                    length={6}
                    value={totpCode}
                    disabled={verifyLoading}
                    formatter={(str) => str.replace(/[^0-9]/g, '')}
                    onChange={(val) => {
                        setTotpCode(val)
                        if (val.length === 6) {
                            handleCodeComplete(val)
                        }
                    }}
                    style={{ width: '100%', justifyContent: 'space-between' }}
                />
            </Flex>

            {/* 2. 底部轻量化状态引导 */}
            <Flex justify="space-between" align="center" style={{ padding: '0 2px', minHeight: 22 }}>
                <div>
                    {verifyLoading ? (
                        <Text type="secondary" style={{ fontSize: 12, fontWeight: 500 }}>
                            <LoadingOutlined style={{ marginRight: 6, color: token.colorPrimary }} />
                            正在核验双重认证...
                        </Text>
                    ) : (
                        <Flex align="center" gap={6}>
                            <MobileOutlined style={{ color: token.colorTextDisabled, fontSize: 12 }} />
                            <Text type="secondary" style={{ fontSize: 12 }}>
                                打开您手机上的身份验证器应用查看动态码
                            </Text>
                        </Flex>
                    )}
                </div>
            </Flex>
        </Form>
    );
};

export default UniversalTotpVerifier;