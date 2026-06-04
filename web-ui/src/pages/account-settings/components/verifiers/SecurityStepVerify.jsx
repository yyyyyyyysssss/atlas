import React, { useState, useRef } from 'react';
import { Flex, Typography, Button, Steps, theme, App } from 'antd';
import { LeftOutlined } from '@ant-design/icons';
import { AnimatePresence, motion } from 'framer-motion';
import VerifyDropdown from '../verifiers'; // 请根据实际路径调整

const { Text } = Typography;

const SecurityStepVerify = ({
    scene = 'default',                         // 风控场景值 (例如: UNBIND_WEBAUTHN)
    captchaScene,
    context,                       // 业务上下文
    confirmLoading = false,        // 第二步终极提交时的加载状态
    onCancel,                      // 取消/关闭按钮回调
    onConfirm,                     // 终极确认回调: (ticket) => void
    confirmText = '确定',           // 确认按钮文本
    confirmDanger = false,         // 确认按钮是否呈现红色警告态
    stepTitle = '风险确认',         // 第二步的步骤标题
    initialStep = 0,
    children,                      // 业务自定义的第二步风险提示 UI
}) => {

    const isSingleStep = initialStep === 1

    const { message } = App.useApp();
    const { token } = theme.useToken();
    const [currentStep, setCurrentStep] = useState(initialStep);
    const [verifyMethod, setVerifyMethod] = useState();
    const [verifyLoading, setVerifyLoading] = useState(false);
    const [ticket, setTicket] = useState('');
    const verifierRef = useRef(null);

    const handleNextStep = async () => {
        if (!verifierRef.current) return
        try {
            const { verified, ticket: responseTicket } = await verifierRef.current.onVerify()
            if (verified && responseTicket) {
                setTicket(responseTicket);
                setCurrentStep(1)
            }
        } catch (error) {
            if (error?.message) {
                message.error(error.message);
            }
        }
    }

    // 返回上一步
    const handleBackStep = () => {
        setCurrentStep(0);
        setTicket('');
    };

    // 触发最终业务
    const handleFinalConfirm = () => {
        if (onConfirm) {
            onConfirm(ticket)
        }
    };

    return (
        <Flex vertical style={{ width: '100%' }}>
            {!isSingleStep && (
                <Steps
                    current={currentStep}
                    size="small"
                    items={[
                        { title: '安全验证' },
                        { title: stepTitle },
                    ]}
                />
            )}

            {/* 核心内容表单/内容视窗 */}
            <div style={{ minHeight: 120, marginTop: 24, marginBottom: 16, padding: '0 4px' }}>
                <AnimatePresence mode="wait">
                    {currentStep === 0 ? (
                        <motion.div
                            key="step-verify"
                            initial={{ opacity: 0, x: -10 }}
                            animate={{ opacity: 1, x: 0 }}
                            exit={{ opacity: 0, x: 10 }}
                            transition={{ duration: 0.2 }}
                        >
                            <VerifyDropdown
                                value={verifyMethod}
                                onChange={(value) => setVerifyMethod(value)}
                                verifierRef={verifierRef}
                                context={context}
                                scene={scene}
                                captchaScene={captchaScene}
                                onLoadingChange={(loading) => setVerifyLoading(loading)}
                                onSuccess={(res) => {
                                    setTicket(res.ticket);
                                    setCurrentStep(1);
                                }}
                            />
                        </motion.div>
                    ) : (
                        <motion.div
                            key="step-business"
                            initial={{ opacity: 0, x: 10 }}
                            animate={{ opacity: 1, x: 0 }}
                            exit={{ opacity: 0, x: -10 }}
                            transition={{ duration: 0.2 }}
                        >
                            {children}
                        </motion.div>
                    )}
                </AnimatePresence>
            </div>

            {/*  步骤驱动的动作按钮组 */}
            <Flex justify="space-between" align="center" style={{ width: '100%' }}>
                <div>
                    {currentStep === 1 && !isSingleStep && (
                        <Button
                            type="text"
                            icon={<LeftOutlined style={{ width: 14, height: 14 }} />}
                            onClick={handleBackStep}
                            style={{ paddingLeft: 0 }}
                        >
                            返回上一步
                        </Button>
                    )}
                </div>
                <Flex gap={8}>
                    <Button onClick={onCancel}>取消</Button>
                    {currentStep === 0 ? (
                        <Button
                            type="primary"
                            loading={verifyLoading}
                            onClick={handleNextStep}
                        >
                            下一步
                        </Button>
                    ) : (
                        <Button
                            type="primary"
                            danger={confirmDanger}
                            loading={confirmLoading}
                            onClick={handleFinalConfirm}
                        >
                            {confirmText}
                        </Button>
                    )}
                </Flex>
            </Flex>

        </Flex>
    )
}

export default SecurityStepVerify