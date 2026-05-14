import React, { useState } from 'react';
import { Form, Input, Button, Typography, theme, Flex, Divider, App, Tooltip } from 'antd';
import { InfoCircleOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useSelector } from 'react-redux';
import AvatarUpload from './AvatarUpload';
const { Title, Paragraph } = Typography;

const ProfileTab = ({ onNavigateToSecurity }) => {

    const { t } = useTranslation()

    const { token } = theme.useToken()

    const [form] = Form.useForm()

    const { fullName, avatar, email, username } = useSelector(state => state.user.userInfo)

    const [loading, setLoading] = useState(false)

    const { message } = App.useApp()

    const [isUsernameModified, setIsUsernameModified] = useState(false)

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
                    <Form form={form} layout="vertical" initialValues={{ displayName: fullName, username: username, email: email }}>
                        <Form.Item label="显示名称" name="displayName" rules={[{ required: true }]}>
                            <Input size="large" />
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
                            <Input.TextArea rows={4} />
                        </Form.Item>

                        <Form.Item>
                            <Button type="primary" size="large" onClick={handleSaveProfile} loading={loading}>保存更改</Button>
                        </Form.Item>
                    </Form>
                </div>

                {/* 头像区域 - 保持在右侧 */}
                <div style={{ padding: '24px 0' }}>
                    <AvatarUpload avatar={avatar} onAvatarChange={() => { }} />
                </div>
            </Flex>
        </div>
    );
};

export default ProfileTab;