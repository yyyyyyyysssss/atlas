import React, { useState } from 'react';
import { Button, Typography, Flex, theme, Modal, Form, Input, Progress, Row, Col, Space, App } from 'antd';
import { CheckCircleFilled, CloseCircleOutlined, LockOutlined } from '@ant-design/icons';
import { changePassword, initPassword } from '../../../services/UserProfileService';
import { useRequest } from 'ahooks';
import { motion, AnimatePresence } from 'framer-motion';

const { Text, Title } = Typography;

const PasswordItem = ({ passwordSet, refresh }) => {
    const { token } = theme.useToken();
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [form] = Form.useForm();
    const [passwordStrength, setPasswordStrength] = useState(0); // 0-4 分
    const [password, setPassword] = useState('');

    const { modal, message } = App.useApp();

    const { runAsync: changePasswordAsync, loading: changeLoading } = useRequest(changePassword, { manual: true });
    const { runAsync: initPasswordAsync, loading: initLoading } = useRequest(initPassword, { manual: true });

    const checkStrength = (value) => {
        if (!value) return 0;
        let strength = 0;
        if (value.length >= 8) strength += 1;
        if (/[A-Z]/.test(value)) strength += 1;
        if (/[0-9]/.test(value)) strength += 1;
        if (/[^A-Za-z0-9]/.test(value)) strength += 1;
        return strength;
    };

    // 辅助计算强度颜色与文案映射
    const getStrengthConfig = (score) => {
        if (score <= 1) return { color: token.colorError, label: '弱安全性' };
        if (score === 2) return { color: token.colorWarning, label: '中等强度' };
        if (score === 3) return { color: token.colorSuccess, label: '高安全性' };
        return { color: token.colorLink, label: '极强密码' };
    };

    const currentStrength = getStrengthConfig(passwordStrength);

    // 实时规则符合状态
    const rulesCheck = {
        length: password.length >= 8,
        mixed: /[A-Z]/.test(password),
        digit: /[0-9]/.test(password),
        special: /[^A-Za-z0-9]/.test(password)
    };

    const handleOk = async () => {
        try {
            const values = await form.validateFields();
            if (passwordSet) {
                await changePasswordAsync(values);
                message.success('密码修改成功');
            } else {
                await initPasswordAsync({ password: values.newPassword });
                message.success('初始密码设置成功');
            }
            setIsModalOpen(false);
            setPasswordStrength(0);
            setPassword('');
            refresh?.();
        } catch (error) {
            // 表单校验失败不处理
        }
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
                <Button type={passwordSet ? "default" : "primary"} onClick={() => setIsModalOpen(true)}>
                    {passwordSet ? '修改密码' : '设置密码'}
                </Button>
            </Flex>

            <Modal
                title={
                    <div style={{ marginBottom: 4 }}>
                        <Title level={4} style={{ margin: 0, fontWeight: 600 }}>
                            {passwordSet ? '变更安全密码' : '初始化安全密码'}
                        </Title>
                        <Text type="secondary" style={{ fontSize: 13, fontWeight: 400 }}>
                            {passwordSet ? '为了您的账户安全，定期更换密码是个好习惯' : '请为您的账户设置一个符合安全合规要求的高强度密码'}
                        </Text>
                    </div>
                }
                open={isModalOpen}
                onOk={handleOk}
                onCancel={() => {
                    setIsModalOpen(false);
                    form.resetFields();
                    setPasswordStrength(0);
                    setPassword('');
                }}
                confirmLoading={changeLoading || initLoading}
                afterClose={() => form.resetFields()}
                styles={{
                    body: { paddingTop: 20 }
                }}
                destroyOnHidden
            >
                <Form form={form} layout="vertical" requiredMark={false}>
                    {passwordSet && (
                        <Form.Item 
                            label={<Text style={{ fontWeight: 500, fontSize: 13 }}>当前密码</Text>}
                            name="oldPassword" 
                            rules={[{ required: true, message: '请输入当前密码' }]}
                        >
                            <Input.Password 
                                placeholder="输入旧密码验证身份" 
                                size="large"
                                style={{ borderRadius: 8 }}
                            />
                        </Form.Item>
                    )}
                    
                    <Form.Item
                        label={<Text style={{ fontWeight: 500, fontSize: 13 }}>新密码</Text>}
                        name="newPassword"
                        rules={[
                            { required: true, message: '请输入新密码' },
                            {
                                validator: (_, value) => {
                                    const oldPassword = form.getFieldValue('oldPassword');
                                    if (passwordSet && oldPassword && value === oldPassword) {
                                        return Promise.reject(new Error('新密码不能与原密码相同'));
                                    }
                                    return Promise.resolve();
                                }
                            }
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

                    {/* 🌟 动态舒展的精细化密码强度指示面板 */}
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

                                {/* 密码复杂度实时审计指引 */}
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
            </Modal>
        </>
    );
};

export default PasswordItem;

