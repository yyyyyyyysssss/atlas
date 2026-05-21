import React, { useState } from 'react';
import { List, Button, App, theme, Typography } from 'antd';
import {
    LockOutlined, MailOutlined, MobileOutlined,
    FileProtectOutlined, GoogleOutlined, GithubOutlined,
    UserOutlined,
    InfoCircleOutlined
} from '@ant-design/icons';

const { Text } = Typography;


const PROVIDER_META = {
    google: {
        name: 'Google 账号',
        icon: GoogleOutlined,
        color: '#EA4335',
        canUnbind: true,
    },
    github: {
        name: 'GitHub 账号',
        icon: GithubOutlined,
        color: '#24292e',
        canUnbind: true,
    },
    atlas: {
        name: 'Atlas 核心账号',
        icon: '/logo128_eclipse.svg',
        canUnbind: false, // 核心主账号，不提供解绑按钮
        isSvg: true,
    },
};

const UserProviderItem = ({
    providers = [],
    refresh
}) => {
    const { token } = theme.useToken();
    const { modal, message } = App.useApp();
    const [loadingKey, setLoadingKey] = useState(null);


    const bind = async (item, meta) => {
        // 绑定流程
        try {
            setLoadingKey(item.provider);
            // 绑定流程
        } catch (error) {
            message.error(`发起 ${meta.name} 绑定失败`);
            setLoadingKey(null);
        }
    }

    const unbind = async (item, meta) => {
        // 解绑二次确认
        modal.confirm({
            title: `确定要解绑 ${meta.name} 吗？`,
            content: item.provider === 'google' || item.provider === 'github'
                ? '解绑后，您将无法再通过该社交账号一键登录。'
                : '确认解除此平台的绑定关联？',
            okText: '确定解绑',
            okType: 'danger',
            cancelText: '取消',
            onOk: async () => {
                try {
                    setLoadingKey(item.provider);
                    // 解绑
                    message.success(`${meta.name} 解绑成功`);
                } catch (error) {
                    message.error(`${meta.name} 解绑失败`);
                } finally {
                    setLoadingKey(null);
                }
            },
        });
    }

    // 处理按钮交互
    const handleAction = async (item) => {
        const meta = PROVIDER_META[item.provider] || { name: item.provider };
        if (item.isBound) {
            unbind(item, meta)
        } else {
            bind(item, meta)
        }
    };

    return (

        <List
            itemLayout="horizontal"
            dataSource={providers}
            renderItem={(item) => {
                // 获取当前渠道的元数据，如果没有匹配到则做防呆兜底
                const meta = PROVIDER_META[item.provider] || {
                    name: item.provider.toUpperCase(),
                    icon: UserOutlined,
                    color: token.colorTextDisabled,
                    canUnbind: true,
                    isSvg: false,
                };

                // 动态拼装描述文案
                let description = item.isBound ? `已绑定。可使用 ${meta.name} 一键登录。` : '未绑定。';
                if (item.isBound && item.boundName) {
                    description = `已绑定：${item.boundName}。可用于一键快捷登录。`;
                }

                const renderAvatar = () => {
                    if (meta.isSvg) {
                        return (
                            <img
                                src={meta.icon}
                                alt={meta.name}
                                style={{
                                    width: 20,
                                    height: 20,
                                    display: 'block',
                                    // 如果未绑定，做置灰和半透明处理
                                    filter: item.isBound ? 'none' : 'grayscale(100%)',
                                    opacity: item.isBound ? 1 : 0.4,
                                }}
                            />
                        );
                    } else {
                        const IconComponent = meta.icon;
                        return (
                            <IconComponent
                                style={{
                                    fontSize: 20,
                                    color: item.isBound ? meta.color : token.colorTextDisabled,
                                    opacity: item.isBound ? 1 : 0.4,
                                }}
                            />
                        );
                    }
                };

                return (
                    <List.Item
                        actions={[
                            // 如果平台允许解绑，或者当前处于未绑定状态，才渲染操作按钮
                            meta.canUnbind || !item.isBound ? (
                                <Button
                                    key="action-btn"
                                    type={item.isBound ? 'text' : 'default'}
                                    danger={item.isBound}
                                    loading={loadingKey === item.provider}
                                    onClick={() => handleAction(item)}
                                >
                                    {item.isBound ? '解绑' : '绑定'}
                                </Button>
                            ) : (
                                <Text type="secondary" key="lock-status">不可解绑</Text>
                            )
                        ]}
                    >
                        <List.Item.Meta
                            avatar={
                                <div style={{ padding: 12, background: token.colorFillAlter, borderRadius: '50%' }}>
                                    {renderAvatar()}
                                </div>
                            }
                            title={meta.name}
                            description={description}
                        />
                    </List.Item>
                );
            }}
        />

    );
};

export default UserProviderItem;