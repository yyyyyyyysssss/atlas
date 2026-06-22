import React, { useState } from 'react';
import { List, Button, App, theme, Typography } from 'antd';
import {
    LockOutlined, MailOutlined, MobileOutlined,
    FileProtectOutlined, GoogleOutlined, GithubOutlined,
    UserOutlined,
    InfoCircleOutlined
} from '@ant-design/icons';
import { AUTHORIZE_CODE_PKCE_VERIFIER, fetchAuthorizeUrl } from '../../../services/Oauth2Service';
import { useRequest } from 'ahooks';
import { generateChallenge, generateVerifier } from '../../../utils/pkce';

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

    const { runAsync: getAuthorizeUrlAsync, loading: getAuthorizeUrlLoading } = useRequest(fetchAuthorizeUrl, {
        manual: true
    });

    const bind = async (item, meta) => {
        // 绑定流程
        try {
            const { authorizeUrl, isPKCERequired } = await getAuthorizeUrlAsync(item.provider, 'bind')
            let finalUrl = authorizeUrl
            if (isPKCERequired) {
                const verifier = generateVerifier()
                const challenge = await generateChallenge(verifier)
                sessionStorage.setItem(AUTHORIZE_CODE_PKCE_VERIFIER, verifier)
                finalUrl = finalUrl + `&code_challenge=${challenge}&code_challenge_method=S256`
            }
            const width = 600
            const height = 700
            const left = (window.screen.width - width) / 2
            const top = (window.screen.height - height) / 2
            const authWindow = window.open(
                finalUrl,
                `bind_${item.provider}`,
                `width=${width},height=${height},left=${left},top=${top},scrollbars=yes,resizable=yes`
            )
            if (!authWindow) {
                message.warning('弹窗被浏览器拦截，请允许弹出窗口后重试');
                return;
            }
            const handleMessage = (event) => {
                // 同源安全校验
                if (event.origin !== window.location.origin) return

                // 匹配子窗口通过 postMessage 发来的纯字符串状态
                if (event.data === 'BIND_SUCCESS') {
                    message.success(`${meta.name} 绑定成功！`);

                    // 刷新当前页面的三方绑定状态列表
                    refresh?.();

                    // 任务完成，解绑当前监听器，避免内存泄漏
                    window.removeEventListener('message', handleMessage)
                }
            }

            window.addEventListener('message', handleMessage)
        } catch (error) {
            console.error(error)
            message.error(`发起 ${meta.name} 绑定失败`);
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