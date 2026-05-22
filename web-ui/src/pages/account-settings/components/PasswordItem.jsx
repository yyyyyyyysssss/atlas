import React, { useState } from 'react';
import { Button, Typography, Flex, theme, Modal, Form, Input, message, Progress, Row, Col } from 'antd';
import { LockOutlined } from '@ant-design/icons';
import { changePassword, initPassword } from '../../../services/UserProfileService';
import { useRequest } from 'ahooks';

const { Text } = Typography;

const PasswordItem = ({ passwordSet, refresh }) => {
    const { token } = theme.useToken();
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [form] = Form.useForm();
    const [passwordStrength, setPasswordStrength] = useState(0); // 0-4 分
    const [password, setPassword] = useState('');

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
                title={passwordSet ? '修改密码' : '设置初始密码'}
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
                destroyOnHidden
            >
                <Form form={form} layout="vertical" style={{ marginTop: 20 }}>
                    {passwordSet && (
                        <Form.Item label="当前密码" name="oldPassword" rules={[{ required: true, message: '请输入当前密码' }]}>
                            <Input.Password placeholder="输入旧密码" />
                        </Form.Item>
                    )}
                    <Form.Item
                        label="新密码"
                        name="newPassword"
                        rules={[
                            { required: true, message: '请输入新密码' },
                            {
                                validator: (_, value) => {
                                    // 获取当前输入的旧密码
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
                            placeholder="设置新密码"
                            onChange={(e) => {
                                const val = e.target.value;
                                setPassword(val); // 保存当前输入值
                                setPasswordStrength(checkStrength(val)); // 计算分数
                            }}
                        />
                    </Form.Item>

                    {/* 仅在用户输入密码时才显示强度条 */}
                    {password.length > 0 && (
                        <div style={{ marginBottom: 24 }}>
                            <Flex justify="space-between" align="center" style={{ marginBottom: 4 }}>
                                <Text type="secondary" style={{ fontSize: 12 }}>密码强度</Text>
                                <Text style={{
                                    fontSize: 12,
                                    color: passwordStrength > 2 ? token.colorSuccess : (passwordStrength > 1 ? token.colorWarning : token.colorError)
                                }}>
                                    {['', '弱', '中等', '强', '极强'][passwordStrength]}
                                </Text>
                            </Flex>
                            <Progress
                                percent={passwordStrength === 0 ? 10 : passwordStrength * 25}
                                showInfo={false}
                                strokeColor={passwordStrength > 2 ? token.colorSuccess : (passwordStrength > 1 ? token.colorWarning : token.colorError)}
                                size="small"
                            />
                        </div>
                    )}
                    <Form.Item
                        label="确认密码"
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
                        <Input.Password placeholder="再次确认新密码" />
                    </Form.Item>
                </Form>
            </Modal>
        </>
    );
};

export default PasswordItem;

