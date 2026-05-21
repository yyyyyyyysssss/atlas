import React, { useState, useEffect } from 'react';
import { Button, Typography, Flex, Input, Tooltip, Space, App, theme } from 'antd';
import { UserOutlined, InfoCircleOutlined } from '@ant-design/icons';
import { useRequest } from 'ahooks';
import { useDispatch } from 'react-redux';
import { changeUsername } from '../../../services/UserProfileService';
import { updateUserInfoPartial } from '../../../redux/slices/userSlice';

const { Text } = Typography;

const UsernameItem = ({ username, isUsernameModified, refresh }) => {
    const { token } = theme.useToken();
    const { modal, message } = App.useApp();
    const dispatch = useDispatch();

    const { runAsync: changeUsernameAsync, loading: changeUsernameLoading } = useRequest(changeUsername, { manual: true });

    const [isUsernameEditing, setIsUsernameEditing] = useState(false);
    const [editingUsername, setEditingUsername] = useState(username);

    useEffect(() => {
        if (username) setEditingUsername(username);
    }, [username]);

    const handleSave = () => {
        if (!editingUsername) {
            message.warning('账号名不能为空');
            return;
        }

        modal.confirm({
            title: '确认修改账号名？',
            content: `您即将把账号修改为 "${editingUsername}"。提交后将固定，无法再次更改！`,
            okText: '确认提交',
            cancelText: '取消',
            onOk: async () => {
                try {
                    await changeUsernameAsync({ newUsername: editingUsername });
                    dispatch(updateUserInfoPartial({ username: editingUsername }));
                    message.success('账号修改成功');
                    setIsUsernameEditing(false);
                    refresh();
                } catch (error) {
                    console.error('修改账号失败:', error);
                }
            }
        });
    };

    return (
        <Flex 
            justify="space-between" 
            align="center" 
            style={{ 
                padding: '20px 0', 
                borderBottom: `1px solid ${token.colorBorderSecondary}` 
            }}
        >
            {/* 左侧：图标 + 标题与描述 */}
            <Flex gap={16} align="flex-start" style={{ flex: 1 }}>
                <div style={{ padding: 12, background: token.colorFillAlter, borderRadius: '50%', display: 'flex' }}>
                    <UserOutlined style={{ color: token.colorPrimary, fontSize: 20 }} />
                </div>
                <Flex vertical gap={4}>
                    <Text strong style={{ fontSize: 16 }}>系统账号</Text>
                    <Text style={{ color: token.colorTextDescription, fontSize: 14, lineHeight: '22px' }}>
                        主要用于系统内唯一识别与基础登录。
                    </Text>
                    <Text
                        type="secondary"
                        style={{
                            fontSize: 13,
                            color: !isUsernameModified ? token.colorWarningActive : token.colorTextDisabled
                        }}
                    >
                        {!isUsernameModified
                            ? "⚠️ 账号仅支持修改一次，请谨慎填写。"
                            : "您已修改过账号，如需再次更改请联系系统管理员。"}
                    </Text>
                </Flex>
            </Flex>

            {/* 右侧：输入框 + 操作按钮 */}
            <Flex align="center" gap={12} style={{ justifyContent: 'flex-end'}}>
                <Input
                    size="middle"
                    value={editingUsername}
                    disabled={!isUsernameEditing || changeUsernameLoading}
                    onChange={(e) => setEditingUsername(e.target.value.trim())}
                    style={{
                        width: 180,
                        border: isUsernameEditing ? undefined : '1px solid transparent',
                        background: isUsernameEditing ? undefined : 'transparent',
                        textOverflow: 'ellipsis'
                    }}
                    suffix={!isUsernameModified && !isUsernameEditing && (
                        <Tooltip title="账号仅支持修改一次">
                            <InfoCircleOutlined style={{ color: token.colorWarning }} />
                        </Tooltip>
                    )}
                />

                {!isUsernameModified && !isUsernameEditing && (
                    <Button
                        type="text"
                        size="small"
                        onClick={() => setIsUsernameEditing(true)}
                    >
                        修改
                    </Button>
                )}

                {isUsernameModified && !isUsernameEditing && (
                    <Text type="secondary" style={{ fontSize: 13 }}>不可修改</Text>
                )}

                {isUsernameEditing && (
                    <Space size={4}>
                        <Button type="primary" size="middle" loading={changeUsernameLoading} onClick={handleSave}>
                            保存
                        </Button>
                        <Button type="default" size="middle" disabled={changeUsernameLoading} onClick={() => { setEditingUsername(username); setIsUsernameEditing(false); }}>
                            取消
                        </Button>
                    </Space>
                )}
            </Flex>
        </Flex>
    );
};

export default UsernameItem;