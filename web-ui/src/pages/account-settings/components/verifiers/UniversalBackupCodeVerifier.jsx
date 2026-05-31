import React, { useState, useEffect, useRef } from 'react';
import { Form, Typography, theme, App, Flex, Input, Button } from 'antd';
import { LoadingOutlined, KeyOutlined, ArrowRightOutlined } from '@ant-design/icons';

const { Text } = Typography;

/**
 * 现代极简主义 安全备份码验证组件 (动效滑出兜底按钮版)
 */
const UniversalBackupCodeVerifier = ({
    verifierRef,
    codeLabel = "请输入安全备份码",
    onVerifyAction,
    onSuccess,
    errorMsg = "备份码不正确或已被使用"
}) => {
    const { token } = theme.useToken();
    const { message } = App.useApp();
    const [form] = Form.useForm();

    const [verifyLoading, setVerifyLoading] = useState(false);
    
    const [part1, setPart1] = useState('');
    const [part2, setPart2] = useState('');

    const inputRef1 = useRef(null);
    const inputRef2 = useRef(null);

    const getFullCode = () => {
        if (!part1 || !part2) return '';
        return `${part1.trim()}-${part2.trim()}`.toLowerCase();
    };

    useEffect(() => {
        inputRef1.current?.focus();
    }, []);

    /**
     * 🛡️ 核心网络校验流
     */
    const executeVerifyWorkflow = async (forcedCode) => {
        if (verifyLoading) return;

        const finalCode = forcedCode || getFullCode();
        if (finalCode.length !== 11) {
            throw new Error('请输入完整的 10 位安全备份码');
        }

        try {
            setVerifyLoading(true);
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

    const handleVerifyFailure = (error) => {
        message.error(error.message);
        setPart2(prev => {
            const nextVal = prev.slice(0, -1);
            setTimeout(() => {
                inputRef2.current?.focus();
            }, 10);
            return nextVal;
        });
    };

    const handleVerifySubmit = async () => {
        try {
            const result = await executeVerifyWorkflow();
            if (onSuccess) onSuccess(result);
        } catch (error) {
            handleVerifyFailure(error);
        }
    };

    const handleCodeComplete = async (code) => {
        try {
            const result = await executeVerifyWorkflow(code);
            if (onSuccess) onSuccess(result);
        } catch (error) {
            handleVerifyFailure(error);
        }
    };

    const handlePaste = (e) => {
        e.preventDefault();
        const pasteData = e.clipboardData.getData('text');
        const pureText = pasteData.replace(/[^a-zA-Z0-9]/g, '').slice(0, 10).toLowerCase();
        
        if (pureText.length > 0) {
            const p1 = pureText.slice(0, 5);
            const p2 = pureText.slice(5, 10);
            setPart1(p1);
            setPart2(p2);

            if (pureText.length === 10) {
                setTimeout(() => handleCodeComplete(`${p1}-${p2}`), 50);
            } else if (p1.length === 5) {
                inputRef2.current?.focus();
            }
        }
    };

    if (verifierRef) {
        verifierRef.current = {
            getValue: getFullCode,
            validate: async () => {
                if (getFullCode().length !== 11) throw new Error('备份码格式不正确');
                return true;
            },
            onVerify: async () => {
                return await executeVerifyWorkflow();
            },
            reset: () => {
                setPart1('');
                setPart2('');
            }
        };
    }

    const isReadyToVerify = part1.length === 5 && part2.length === 5;
    // 决定右侧悬浮动效区是否需要展开
    const showActionButton = isReadyToVerify || verifyLoading;

    return (
        <Form form={form} layout="vertical" style={{ width: '100%' }} component={false}>
            <Flex vertical gap={12} style={{ marginBottom: 16 }}>
                <Text style={{ fontWeight: 600, fontSize: 13, color: token.colorTextDescription }}>
                    {codeLabel}
                </Text>

                <Flex align="center" style={{ width: '100%', overflow: 'hidden' }}>
                    
                    {/* 双输入框核心区域（自带平滑宽度过渡） */}
                    <Flex 
                        gap={8} 
                        align="center" 
                        style={{ 
                            flex: 1,
                            transition: 'all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1)'
                        }}
                    >
                        <Input
                            ref={inputRef1}
                            size="large"
                            placeholder="pemag"
                            maxLength={5}
                            value={part1}
                            disabled={verifyLoading}
                            onPaste={handlePaste}
                            onChange={(e) => {
                                const val = e.target.value.replace(/[^a-zA-Z0-9]/g, '').toLowerCase();
                                setPart1(val);
                                if (val.length === 5 && part2.length === 5) {
                                    handleCodeComplete(`${val}-${part2}`);
                                } else if (val.length === 5) {
                                    inputRef2.current?.focus();
                                }
                            }}
                            onPressEnter={handleVerifySubmit}
                            style={{
                                flex: 1,
                                height: 48,
                                textAlign: 'center',
                                fontFamily: "'Courier New', monospace",
                                fontSize: '16px',
                                fontWeight: 600,
                                letterSpacing: '1px'
                            }}
                        />

                        <span style={{ 
                            color: token.colorTextDisabled, 
                            fontWeight: 700, 
                            fontSize: '16px',
                            userSelect: 'none'
                        }}>-</span>

                        <Input
                            ref={inputRef2}
                            size="large"
                            placeholder="rgkqe"
                            maxLength={5}
                            value={part2}
                            disabled={verifyLoading}
                            onChange={(e) => {
                                const val = e.target.value.replace(/[^a-zA-Z0-9]/g, '').toLowerCase();
                                setPart2(val);
                                if (val.length === 5 && part1.length === 5) {
                                    handleCodeComplete(`${part1}-${val}`);
                                } else if (val.length === 5) {
                                    inputRef1.current?.focus();
                                }
                            }}
                            onKeyDown={(e) => {
                                if (e.key === 'Backspace' && !part2) {
                                    inputRef1.current?.focus();
                                }
                            }}
                            onPressEnter={handleVerifySubmit}
                            style={{
                                flex: 1,
                                height: 48,
                                textAlign: 'center',
                                fontFamily: "'Courier New', monospace",
                                fontSize: '16px',
                                fontWeight: 600,
                                letterSpacing: '1px'
                            }}
                        />
                    </Flex>

                    {/* 🌟 动效裁剪区：未满10位时完全不占空间 */}
                    <div style={{ 
                        width: showActionButton ? 48 : 0, 
                        opacity: showActionButton ? 1 : 0,
                        transform: showActionButton ? 'translateX(0)' : 'translateX(20px)',
                        transition: 'all 0.3s cubic-bezier(0.25, 0.8, 0.25, 1)',
                        display: 'flex', 
                        alignItems: 'center', 
                        justifyContent: 'flex-end',
                        height: 48
                    }}>
                        {verifyLoading ? (
                            <LoadingOutlined style={{ color: token.colorPrimary, fontSize: 18, marginRight: 12 }} />
                        ) : (
                            <Button
                                type="primary"
                                onClick={handleVerifySubmit}
                                icon={<ArrowRightOutlined />}
                                style={{ 
                                    width: 36, 
                                    height: 36, 
                                    display: 'flex', 
                                    alignItems: 'center', 
                                    justifyContent: 'center',
                                    borderRadius: '8px',
                                    boxShadow: 'none'
                                }}
                            />
                        )}
                    </div>

                </Flex>
            </Flex>

            {/* 底部提示 */}
            <Flex justify="space-between" align="center" style={{ padding: '0 2px', minHeight: 22 }}>
                <div>
                    {verifyLoading ? (
                        <Text type="secondary" style={{ fontSize: 12, fontWeight: 500 }}>
                            正在核验备份码...
                        </Text>
                    ) : (
                        <Flex align="center" gap={6}>
                            <Text type="secondary" style={{ fontSize: 12 }}>
                                请输入 10 位安全码（支持在一号框直接复制粘贴整段）
                            </Text>
                        </Flex>
                    )}
                </div>
            </Flex>
        </Form>
    );
};

export default UniversalBackupCodeVerifier;