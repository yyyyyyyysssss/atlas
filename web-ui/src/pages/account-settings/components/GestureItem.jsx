import React, { useState, useRef, useEffect } from 'react';
import { Button, Typography, Flex, theme, Modal, Form, Space, App, Alert } from 'antd';
import { NodeIndexOutlined } from '@ant-design/icons';
import { bindGesture, unbindGesture } from '../../../services/AccountService';
import { useRequest } from 'ahooks';
import SecurityStepVerify from './verifiers/SecurityStepVerify';
import UniversalGestureVerifier from './verifiers/UniversalGestureVerifier';
import { Grid3x3 } from 'lucide-react';

const { Text, Title } = Typography;

const GestureItem = ({ context, refresh }) => {
    const { token } = theme.useToken();
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [modalMode, setModalMode] = useState('bind'); // 'bind' | 'unbind'
    const [form] = Form.useForm();
    const { message } = App.useApp();

    const { gestureEnabled } = context || {};

    // 🚀 核心控制 Ref：这一次我们直接让它指向适配器包装后的句柄
    const adaptorRef = useRef(null);

    const { runAsync: bindGestureAsync, loading: bindGestureLoading } = useRequest(bindGesture, { manual: true });
    const { runAsync: unbindGestureAsync, loading: unbindGestureLoading } = useRequest(unbindGesture, { manual: true });

    const handleOpenBindModal = () => {
        setModalMode('bind');
        setIsModalOpen(true);
    };

    const handleOpenUnbindModal = () => {
        setModalMode('unbind');
        setIsModalOpen(true);
    };

    const handleCancel = () => {
        setIsModalOpen(false);
        form.resetFields();
        // 🚀 呼叫适配器的重置：既能物理清空画布，又能把状态重置回第一步
        adaptorRef.current?.resetAll();
    };

    // 绑定/重置手势的核心提交逻辑
    const handleBindSubmit = async (ticket) => {
        try {
            const values = await form.validateFields(['gesture', 'confirmGesture']);

            await bindGestureAsync({
                gesture: values.gesture,
                confirmGesture: values.confirmGesture,
                ticket: ticket
            });

            message.success(gestureEnabled ? '手势密码修改成功' : '手势密码设置成功');
            handleCancel();
            refresh?.();
        } catch (error) {
            // 🚀 接口报错（如 Ticket 校验失败等），强制重置画布到录入第一步并清空
            adaptorRef.current?.resetAll();
        }
    };

    // 解绑逻辑
    const handleUnbindSubmit = async (ticket) => {
        await unbindGestureAsync({ ticket });
        message.success('手势密码已成功解绑');
        handleCancel();
        refresh?.();
    };

    return (
        <>
            <Flex justify="space-between" align="center" style={{ padding: '20px 0', borderBottom: `1px solid ${token.colorBorderSecondary}` }}>
                <Flex gap={16} align="flex-start" style={{ flex: 1 }}>
                    <div style={{ padding: 12, background: token.colorFillAlter, borderRadius: '50%' }}>
                        <Grid3x3 style={{ color: token.colorPrimary, fontSize: 20 }} />
                    </div>
                    <Flex vertical gap={4}>
                        <Text strong style={{ fontSize: 16 }}>手势密码</Text>
                        <Text type="secondary" style={{ fontSize: 14 }}>
                            {gestureEnabled ? '已开启。可通过绘制手势轨迹快速完成二次身份验证。' : '未开启。开启后可通过手势轨迹进行多因素安全校验。'}
                        </Text>
                    </Flex>
                </Flex>

                <Space size={12}>
                    {gestureEnabled && (
                        <Button type="text" danger onClick={handleOpenUnbindModal}>
                            关闭手势
                        </Button>
                    )}
                    <Button type={gestureEnabled ? "default" : "primary"} onClick={handleOpenBindModal}>
                        {gestureEnabled ? '修改手势' : '设置手势'}
                    </Button>
                </Space>
            </Flex>

            <Modal
                title={
                    <div style={{ marginBottom: 16 }}>
                        <Title level={4} style={{ margin: 0, fontWeight: 600 }}>
                            {modalMode === 'unbind' ? '解绑手势认证' : (!gestureEnabled ? '开启手势密码' : '修改手势密码')}
                        </Title>
                    </div>
                }
                open={isModalOpen}
                onCancel={handleCancel}
                width={480}
                footer={null}
                centered
                destroyOnHidden
            >
                {modalMode === 'bind' ? (
                    <SecurityStepVerify
                        scene="BIND_GESTURE"
                        captchaScene="default"
                        context={context}
                        stepTitle={gestureEnabled ? "设置新手势" : "验证成功，请绘制手势"}
                        confirmText={gestureEnabled ? "确认修改" : "确认开启"}
                        confirmLoading={bindGestureLoading}
                        onCancel={handleCancel}
                        onConfirm={handleBindSubmit}
                    >
                        <Form form={form} layout="vertical" requiredMark={false}>
                            <Form.Item name="gesture" noStyle rules={[{ required: true, message: '请绘制新手势' }]} />
                            <Form.Item name="confirmGesture" noStyle rules={[{ required: true, message: '请绘制确认手势' }]} />

                            {/* 单画布复合适配器 */}
                            <FormItemSingleCanvasAdaptor
                                form={form}
                                adaptorRef={adaptorRef}
                            />
                        </Form>
                    </SecurityStepVerify>
                ) : (
                    <SecurityStepVerify
                        scene="UNBIND_GESTURE"
                        captchaScene="default"
                        context={context}
                        stepTitle="确认关闭手势密码"
                        confirmText="确认解绑并关闭"
                        confirmLoading={unbindGestureLoading}
                        initialStep={0}
                        onCancel={handleCancel}
                        onConfirm={handleUnbindSubmit}
                    >
                        <Flex vertical gap={16} style={{ padding: '8px 0 16px 0' }}>
                            <Alert
                                message="手势密码解绑警告"
                                description="关闭手势密码凭证后，您在移动端设备上将无法再通过快捷滑动解锁。此操作不可逆，后续如需使用必须重新在移动端进行物理录入。"
                                type="warning" // 💡 橙色警示，或者用 error 变红
                                showIcon
                            />

                            <div style={{ padding: '0 4px' }}>
                                <Text type="secondary" style={{ fontSize: 13, lineHeight: '20px' }}>
                                    💡 您的账户身份已通过前置安全审计。如果您确认不再需要此快捷通道，请点击下方的 <Text strong>“确认解绑并关闭”</Text> 按钮完成最终移除。
                                </Text>
                            </div>
                        </Flex>
                    </SecurityStepVerify>
                )}
            </Modal>
        </>
    );
};

/**
 * 🚀 单画布双阶段受控桥接适配器
 */
const FormItemSingleCanvasAdaptor = ({ form, adaptorRef }) => {
    const { token } = theme.useToken();
    const [stage, setStage] = useState('INIT'); // 'INIT' | 'CONFIRM'
    const [firstSequence, setFirstSequence] = useState('');

    // 控制传递给底层的状态 'default' | 'error'
    const [verifyStatus, setVerifyStatus] = useState('default');

    // 原生画布的内部真正引用
    const canvasRef = useRef(null);

    // 封装公共的“一键回退到第一步”重置逻辑
    const handleReturnToFirstStep = () => {
        setStage('INIT');
        setFirstSequence('');
        setVerifyStatus('default');
        form.setFieldsValue({ gesture: undefined, confirmGesture: undefined });
        form.setFields([{ name: 'confirmGesture', errors: [] }]); // 清空可能残留的红字错误
        canvasRef.current?.reset?.();
    };

    // 通过专有句柄向父组件暴露一个“复合重置”方法
    useEffect(() => {
        if (adaptorRef) {
            adaptorRef.current = {
                resetAll: handleReturnToFirstStep // 统一复用重置逻辑
            };
        }
    }, [adaptorRef, form]);

    const handleLocalVerifyAction = async (sequence) => {
        return { verified: true, sequence };
    };

    const handleCanvasSuccess = (result) => {
        // 如果正处于错误挂起阶段，锁定画布，不接受新的绘制
        if (verifyStatus === 'error') return;

        const sequence = result.sequence;

        if (stage === 'INIT') {
            setFirstSequence(sequence);
            form.setFieldsValue({ gesture: sequence });

            setTimeout(() => {
                if (canvasRef.current) {
                    canvasRef.current.reset?.();
                }
                setStage('CONFIRM');
            }, 250);

        } else if (stage === 'CONFIRM') {
            form.setFieldsValue({ confirmGesture: sequence });

            if (sequence !== firstSequence) {
                setVerifyStatus('error');
                form.setFields([
                    { name: 'confirmGesture', errors: ['两次绘制的手势图案不一致，请重新绘制'] }
                ]);

                setTimeout(() => {
                    canvasRef.current?.reset?.();
                    setVerifyStatus('default');
                    form.setFieldsValue({ confirmGesture: undefined });
                }, 1200);
            } else {
                form.setFields([{ name: 'confirmGesture', errors: [] }]);
                setVerifyStatus('default');
            }
        }
    };

    // 核心文案突出：根据当前实际状态输出最直观的引导
    const getHighlightedLabel = () => {
        if (verifyStatus === 'error') {
            return <Text strong style={{ fontSize: 15, color: token.colorError }}>验证失败：两次绘制的手势不一致</Text>;
        }
        if (stage === 'CONFIRM') {
            return <Text strong style={{ fontSize: 15, color: token.colorPrimary }}>第二步：请再次绘制相同图形以确认</Text>;
        }
        return <Text strong style={{ fontSize: 15, color: token.colorText }}>第一步：请绘制您的新手势图形</Text>;
    };

    return (
        <Flex vertical gap={12} style={{ position: 'relative', marginTop: 8 }}>

            {/* 顶栏排版层 */}
            <Flex justify="space-between" align="center" style={{ padding: '0 2px', height: 24 }}>
                {getHighlightedLabel()}

                {/* 右侧动作与状态槽 */}
                <div>
                    {stage === 'INIT' ? (
                        <Text style={{ fontSize: 12, fontWeight: 500, color: token.colorTextDescription }}>
                            ● 录入中
                        </Text>
                    ) : (
                        /* 🚀 核心改良：当处于确认阶段或错误阶段时，右侧提供可点击的文本“重新录入” */
                        <span
                            onClick={verifyStatus === 'error' ? undefined : handleReturnToFirstStep}
                            style={{
                                fontSize: 12,
                                fontWeight: 500,
                                cursor: verifyStatus === 'error' ? 'not-allowed' : 'pointer',
                                color: verifyStatus === 'error' ? token.colorError : token.colorPrimary,
                                opacity: verifyStatus === 'error' ? 0.6 : 1,
                                textDecoration: verifyStatus === 'error' ? 'none' : 'underline',
                                transition: 'all 0.2s ease',
                                display: 'inline-flex',
                                alignItems: 'center'
                            }}
                        >
                            {verifyStatus === 'error' ? '● 校验失败' : '返回重设'}
                        </span>
                    )}
                </div>
            </Flex>

            {/* 画布容器 */}
            <UniversalGestureVerifier
                verifierRef={canvasRef}
                label={null}
                status={verifyStatus}
                onVerifyAction={handleLocalVerifyAction}
                onSuccess={handleCanvasSuccess}
            />
        </Flex>
    );
};

export default GestureItem;