import React, { useState } from 'react';
import { Form, Input, Button, Upload, Avatar, Typography, theme, Flex, Divider, App, Tooltip } from 'antd';
import { InfoCircleOutlined } from '@ant-design/icons';
import { Pencil } from 'lucide-react';

const { Title, Paragraph } = Typography;

const ProfileTab = ({ onNavigateToSecurity }) => {
    const { token } = theme.useToken();
    const [form] = Form.useForm();
    const [loading, setLoading] = useState(false);
    const { message } = App.useApp();
    const [isUsernameModified, setIsUsernameModified] = useState(false);

    const handleSaveProfile = async () => {
        setLoading(true);
        // 模拟保存请求
        setTimeout(() => {
            message.success('个人资料已更新');
            setLoading(false);
        }, 1000);
    };

    return (
        <div style={{ width: '100%' }}>
            <Title level={4} style={{ marginBottom: 8, color: token.colorTextHeading }}>个人资料</Title>
            <Paragraph type="secondary" style={{ marginBottom: 24 }}>
                更新您的头像和个人基本信息。这些信息可能会在团队或协作区公开显示。
            </Paragraph>
            <Divider style={{ borderColor: token.colorBorderSecondary }} />

            <Flex gap={32} align="flex-start" style={{ marginBottom: 32 }}>
                <div style={{ flex: 1 }}>
                    <Form form={form} layout="vertical" initialValues={{ displayName: 'AtlasAdmin', username: 'u_juqhs2k', email: 'admin@atlas.com' }}>
                        <Form.Item label="显示名称" name="displayName" rules={[{ required: true }]}>
                            <Input size="large" placeholder="请输入您的姓名或昵称" />
                        </Form.Item>
                        <Form.Item
                            label="账号"
                            name="username"
                            extra={
                                !isUsernameModified
                                    ? "账号仅支持修改一次，请谨慎填写。"
                                    : "您已修改过账号，如需再次更改请联系系统管理员。"
                            }
                        >
                            <Input
                                size="large"
                                placeholder="设置您的唯一账号"
                                disabled={isUsernameModified}
                                suffix={
                                    !isUsernameModified && (
                                        <Tooltip title="仅限修改一次">
                                            <InfoCircleOutlined style={{ color: token.colorWarning }} />
                                        </Tooltip>
                                    )
                                }
                            />
                        </Form.Item>
                        <Form.Item
                            label="邮箱"
                            name="email"
                            extra={
                                <span>
                                    邮箱作为账号标识不可直接修改。如需更换，请前往
                                    <a onClick={onNavigateToSecurity} style={{ cursor: 'pointer', marginLeft: 4 }}>安全设置</a>。
                                </span>
                            }
                        >
                            <Input size="large" disabled />
                        </Form.Item>
                        <Form.Item label="个人简介" name="bio">
                            <Input.TextArea rows={4} placeholder="简单介绍一下你自己..." />
                        </Form.Item>
                        <Form.Item>
                            <Button type="primary" size="large" onClick={handleSaveProfile} loading={loading}>
                                保存更改
                            </Button>
                        </Form.Item>
                    </Form>
                </div>
                <div style={{ textAlign: 'center' }}>
                    <Upload
                        showUploadList={false}
                        maxCount={1}
                        accept='image/*'
                        beforeUpload={() => { return false; }}
                    >
                        <div style={{
                            position: 'relative',
                            cursor: 'pointer',
                            padding: 4,
                            transition: 'all 0.3s',
                            display: 'inline-block'
                        }}>
                            <Avatar
                                size={150}
                                src="/logo128_eclipse.svg"
                                style={{
                                    cursor: 'pointer',
                                    backgroundColor: token.colorBgContainer,
                                    boxShadow: `0 0 0 1px ${token.colorBorderSecondary}`,
                                }}
                            />
                            <div style={{
                                position: 'absolute',
                                bottom: 8,
                                left: 8,
                                background: token.colorBgContainer,
                                borderRadius: '50%',
                                padding: '6px',
                                boxShadow: token.boxShadowSecondary,
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                border: `1px solid ${token.colorBorderSecondary}`
                            }}>
                                <Pencil color={token.colorPrimary} size={18} />
                            </div>
                        </div>
                    </Upload>
                </div>
            </Flex>
        </div>
    );
};

export default ProfileTab;