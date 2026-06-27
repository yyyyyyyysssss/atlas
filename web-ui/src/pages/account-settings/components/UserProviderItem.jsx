import React, { useState } from 'react';
import { List, Button, App, theme, Typography, Flex, Modal, Alert, Space } from 'antd';
import {
    LockOutlined, MailOutlined, MobileOutlined,
    FileProtectOutlined, GoogleOutlined, GithubOutlined,
    UserOutlined,
    InfoCircleOutlined
} from '@ant-design/icons';
import { AUTHORIZE_CODE_PKCE_VERIFIER } from '../../../services/Oauth2Service';
import { useRequest } from 'ahooks';
import { generateChallenge, generateVerifier } from '../../../utils/pkce';
import { bindthirdPartyProvider, unbindthirdPartyProvider } from '../../../services/AccountService';
import SecurityStepVerify from './verifiers/SecurityStepVerify';
import { ShieldAlert } from 'lucide-react';

const { Text, Title } = Typography


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
    context,
    refresh
}) => {
    const { token } = theme.useToken();
    const { modal, message } = App.useApp();
    const [loadingKey, setLoadingKey] = useState(null)

    const { providers = [] } = context || {}

    const [modalMode, setModalMode] = useState(null) // 'bind' | 'unbind' | null
    const [targetItem, setTargetItem] = useState(null); // 当前准备解绑的三方账号

    const { runAsync: bindthirdPartyProviderAsync, loading: bindthirdPartyProviderLoading } = useRequest(bindthirdPartyProvider, {
        manual: true
    })

    const { runAsync: unbindthirdPartyProviderAsync, loading: unbindthirdPartyProviderLoading } = useRequest(unbindthirdPartyProvider, {
        manual: true
    })

    // 处理按钮交互
    const handleAction = async (item, e) => {
        const meta = PROVIDER_META[item.provider] || { name: item.provider };
        if (item.isBound) {
            handleOpenUnbindModal(item, e)
        } else {
            handleOpenBindModal(item, e)
        }
    };

    const handleBindSubmit = async (ticket) => {
        try {
            const { authorizeUrl, state, isPKCERequired } = await bindthirdPartyProviderAsync({
                ticket: ticket,
                provider: targetItem.provider
            })
            let finalUrl = authorizeUrl
            if (isPKCERequired) {
                const verifier = generateVerifier()
                const challenge = await generateChallenge(verifier)
                const verifierKey = AUTHORIZE_CODE_PKCE_VERIFIER + ":" + state
                sessionStorage.setItem(verifierKey, verifier)
                finalUrl = finalUrl + `&code_challenge=${challenge}&code_challenge_method=S256`
            }
            const width = 600
            const height = 700
            const left = (window.screen.width - width) / 2
            const top = (window.screen.height - height) / 2
            const authWindow = window.open(
                finalUrl,
                `bind_${targetItem.provider}`,
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
                    message.success(`绑定成功！`);

                    handleCloseModal()
                    // 刷新当前页面的三方绑定状态列表
                    refresh?.()

                    // 任务完成，解绑当前监听器，避免内存泄漏
                    window.removeEventListener('message', handleMessage)
                }
            }
            window.addEventListener('message', handleMessage)
        } catch (error) {
            console.error(error)
            message.error(`发起绑定失败`);
        }

    }

    const handleUnbindSubmit = async (ticket) => {
        if (!targetItem || !ticket) return;
        await unbindthirdPartyProviderAsync({
            providerId: targetItem.id,
            ticket: ticket
        });
        message.success(`已成功移除 [${targetItem.name}] 账号`)
        handleCloseModal()
        refresh?.()
    }

    const handleOpenBindModal = (item, e) => {
        if (e) e.stopPropagation()
        setTargetItem(item)
        setModalMode('bind')
    }

    const handleOpenUnbindModal = (item, e) => {
        if (e) e.stopPropagation()
        setTargetItem(item)
        setModalMode('unbind')
    }


    const handleCloseModal = () => {
        setModalMode(null)
        setTargetItem(null)
    }

    return (
        <>
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
                                        onClick={(e) => handleAction(item, e)}
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
            <Modal
                title={
                    <div style={{ marginBottom: 16 }}>
                        <Title level={4} style={{ margin: 0, fontWeight: 600 }}>
                            {modalMode === 'unbind' ? '移除三方账号' : '绑定三方账号'}
                        </Title>
                    </div>
                }
                open={modalMode !== null}
                onCancel={handleCloseModal}
                width={460}
                centered
                destroyOnHidden
                footer={null}
            >
                {modalMode === 'bind' && (
                    <SecurityStepVerify
                        scene="BIND_THIRD_PARTY_PROVIDER" // 后端风控定义对应的场景标识
                        captchaScene="default"
                        context={context}
                        stepTitle="绑定三方社交账号"
                        confirmText="前往平台授权"
                        confirmLoading={bindthirdPartyProviderLoading}
                        onCancel={handleCloseModal}
                        onConfirm={handleBindSubmit} // 🚀 验证通过后携带 ticket 去开启 WebAuthn 硬件
                    >
                        <Flex vertical gap={12} style={{ padding: '4px 0 16px 0' }}>
                            <Alert
                                message="身份验证成功"
                                description="您的身份已确认。接下来，系统将引导您跳转至第三方平台完成安全授权。"
                                type="success" // 💡 修正 2：既然通过了，状态应该改为成功（绿标）
                                showIcon
                            />
                            <Space direction="vertical" size={4} style={{ marginTop: 4 }}>
                                <Text strong style={{ fontSize: 14, color: token.colorPrimary }}>
                                    请点击下方按钮前往外部平台进行绑定
                                </Text>
                                <Text type="secondary" style={{ fontSize: 13, lineHeight: '20px' }}>
                                    点击后系统将打开授权页面。请在弹出的官方页面中确认授权，授权成功后将会自动返回并完成与当前 Atlas 账户的绑定绑定。
                                </Text>
                            </Space>
                        </Flex>
                    </SecurityStepVerify>
                )}

                {modalMode === 'unbind' && (
                    <SecurityStepVerify
                        scene="UNBIND_THIRD_PARTY_PROVIDER"
                        captchaScene="default"
                        context={context}
                        stepTitle="解除第三方账号绑定"
                        confirmText="确定解绑移除"
                        confirmDanger={true}
                        confirmLoading={unbindthirdPartyProviderLoading}
                        onCancel={handleCloseModal}
                        onConfirm={handleUnbindSubmit}
                    >
                        <Flex vertical gap={14}>
                            <Alert
                                message="解除社交账号绑定"
                                description="解绑后，您将无法再通过该第三方社交账号一键登录本系统。请确保您已记住本站的密码，或已绑定了其他可用的登录凭证（如 Passkey、钱包等），否则可能导致账户无法登录。"
                                type="warning"
                                showIcon
                                icon={<ShieldAlert style={{ color: token.colorWarning }} />}
                            />
                            <div style={{ padding: '4px 12px', background: token.colorFillAlter, borderRadius: 8, border: `1px solid ${token.colorBorderSecondary}` }}>
                                <Space direction="vertical" size={2} style={{ padding: '8px 0' }}>
                                    <Text type="secondary" style={{ fontSize: 13 }}>即将解绑的社交账户信息：</Text>
                                    <Text strong style={{ fontSize: 15, color: token.colorText }}>{targetItem?.label || '社交账号'}</Text>
                                    <Text type="secondary" style={{ fontSize: 12 }}>关联时间: {targetItem?.createTime || '-'}</Text>
                                </Space>
                            </div>
                            <Text type="danger" style={{ fontSize: 13, fontWeight: 500 }}>
                                💡 如果该账号是您唯一的登录方式，系统将强制拦截此解绑操作。
                            </Text>
                        </Flex>
                    </SecurityStepVerify>
                )}
            </Modal>
        </>
    );
};

export default UserProviderItem;