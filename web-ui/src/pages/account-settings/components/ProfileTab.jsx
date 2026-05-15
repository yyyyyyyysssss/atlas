import React, { useEffect, useState } from 'react';
import { Form, Input, Button, Typography, theme, Flex, Divider, App, Tooltip, Space } from 'antd';
import { InfoCircleOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useDispatch, useSelector } from 'react-redux';
import AvatarUpload from './AvatarUpload';
import { changeUsername, changeUserProfile } from '../../../services/UserProfileService';
import { useRequest } from 'ahooks';
import { updateUserInfoPartial } from '../../../redux/slices/userSlice';

const { Title, Paragraph } = Typography;

const ProfileTab = ({ onNavigateToSecurity }) => {
    const { t } = useTranslation()
    const { token } = theme.useToken()
    const [form] = Form.useForm()
    const userInfo = useSelector(state => state.user.userInfo)
    const dispatch = useDispatch()
    const { message } = App.useApp()

    const [isUsernameEditing, setIsUsernameEditing] = useState(false)

    const { isUsernameModified } = userInfo

    const { runAsync: changeUserProfileAsync, loading: changeUserProfileLoading } = useRequest(changeUserProfile, { manual: true });
    const { runAsync: changeUsernameAsync, loading: changeUsernameLoading } = useRequest(changeUsername, { manual: true });

    useEffect(() => {
        if (userInfo) {
            form.setFieldsValue({
                fullName: userInfo.fullName,
                username: userInfo.username,
                email: userInfo.email,
                motto: userInfo.motto
            })
        }
    }, [userInfo, form])

    const handleSaveProfile = async () => {
        const values = await form.validateFields();
        const newUserInfo = {
            fullName: values.fullName,
            motto: values.motto
        };
        await changeUserProfileAsync(newUserInfo)
        dispatch(updateUserInfoPartial({ ...newUserInfo }))
        message.success('个人资料已成功更新')
    }


    const handleConfirmUsername = async () => {
        const username = form.getFieldValue('username')
        await changeUsernameAsync({ username })
        dispatch(updateUserInfoPartial({
            username: username,
            isUsernameModified: true
        }))
        message.success('账号修改成功')
        setIsUsernameEditing(false)
    }

    return (
        <div style={{ width: '100%' }}>
            <Title level={4} style={{ marginBottom: 8, color: token.colorTextHeading }}>个人资料</Title>
            <Paragraph type="secondary" style={{ marginBottom: 24 }}>更新您的头像和个人基本信息。</Paragraph>
            <Divider style={{ borderColor: token.colorBorderSecondary }} />

            <Flex gap={32} align="flex-start" style={{ marginBottom: 32 }}>
                <div style={{ flex: 1 }}>
                    <Form form={form} layout="vertical">
                        <Form.Item label="显示名称" name="fullName" rules={[{ required: true }]}>
                            <Input size="large" />
                        </Form.Item>

                        <Form.Item
                            label="账号"
                            extra={
                                !isUsernameModified
                                    ? "账号仅支持修改一次，请谨慎填写。"
                                    : "您已修改过账号，如需再次更改请联系系统管理员。"
                            }
                        >
                            <Flex align="center" gap={12}>
                                <Form.Item name="username" noStyle>
                                    <Input
                                        size="large"
                                        disabled={!isUsernameEditing}
                                        style={{ flex: 1 }}
                                        suffix={!isUsernameModified && !isUsernameEditing && (
                                            <Tooltip title="账号仅支持修改一次">
                                                <InfoCircleOutlined style={{ color: token.colorWarning }} />
                                            </Tooltip>
                                        )}
                                    />
                                </Form.Item>
                                {!isUsernameModified && !isUsernameEditing && (
                                    <Button type="link" onClick={() => setIsUsernameEditing(true)} style={{ padding: 0 }}>
                                        修改账号
                                    </Button>
                                )}
                                {isUsernameEditing && (
                                    <Space>
                                        <Button type="primary" onClick={handleConfirmUsername} loading={changeUsernameLoading}>
                                            保存
                                        </Button>
                                        <Button type="default" onClick={() => setIsUsernameEditing(false)} loading={changeUsernameLoading}>
                                            取消
                                        </Button>
                                    </Space>
                                )}
                            </Flex>
                        </Form.Item>

                        <Form.Item
                            label="邮箱"
                            name="email"
                            extra={
                                <Space size={0}>
                                    邮箱作为账号标识不可直接修改。如需更换，请前往
                                    <Typography.Link onClick={onNavigateToSecurity} style={{ marginLeft: 4 }}>安全设置</Typography.Link>。
                                </Space>
                            }
                        >
                            <Input size="large" disabled />
                        </Form.Item>

                        <Form.Item label="个人签名" name="motto">
                            <Input.TextArea rows={4} />
                        </Form.Item>
                        {!isUsernameEditing && (
                            <Form.Item>
                                <Button type="primary" size="large" onClick={handleSaveProfile} loading={changeUserProfileLoading}>保存更改</Button>
                            </Form.Item>
                        )}

                    </Form>
                </div>

                <div style={{ padding: '24px 0' }}>
                    <AvatarUpload avatar={userInfo.avatar} />
                </div>
            </Flex>
        </div>
    );
};

export default ProfileTab;