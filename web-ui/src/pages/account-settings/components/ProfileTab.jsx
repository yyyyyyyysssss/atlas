import React, { useEffect, useState } from 'react';
import { Form, Input, Button, Typography, theme, Flex, Divider, App, Tooltip, Space } from 'antd';
import { InfoCircleOutlined } from '@ant-design/icons';
import { useTranslation } from 'react-i18next';
import { useDispatch, useSelector } from 'react-redux';
import AvatarUpload from './AvatarUpload';
import { changeUserProfile } from '../../../services/UserProfileService';
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

    const { runAsync: changeUserProfileAsync, loading: changeUserProfileLoading } = useRequest(changeUserProfile, { manual: true });

    useEffect(() => {
        if (userInfo) {
            form.setFieldsValue({
                fullName: userInfo.fullName,
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
                        <Form.Item label="个人签名" name="motto">
                            <Input.TextArea rows={4} />
                        </Form.Item>
                        <Form.Item>
                            <Button type="primary" size="large" onClick={handleSaveProfile} loading={changeUserProfileLoading}>保存更改</Button>
                        </Form.Item>

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