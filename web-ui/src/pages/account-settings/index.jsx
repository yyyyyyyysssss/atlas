import React, { useState } from 'react';
import {
    Tabs, Form, Input, Button, Upload, Avatar, Typography,
    theme, List, Switch, Flex, Divider, Segmented, Select,
    App,
    Alert,
    Tag,
    Tooltip
} from 'antd';
import {
    UserOutlined, SafetyCertificateOutlined, SettingOutlined,
    UploadOutlined, LockOutlined, MobileOutlined, MailOutlined,
    MonitorOutlined, ScanOutlined,
    KeyOutlined,
    IdcardOutlined,
    FileProtectOutlined,
    GoogleOutlined,
    GithubOutlined,
    InfoCircleOutlined
} from '@ant-design/icons';
import { Fingerprint, Pencil, ShieldCheck } from 'lucide-react';

const { Title, Text, Paragraph } = Typography;

const AccountSettings = () => {
    const { token } = theme.useToken();
    const [form] = Form.useForm();
    const [loading, setLoading] = useState(false);

    const { message } = App.useApp()

    const handleSaveProfile = async () => {
        setLoading(true);
        // 模拟保存请求
        setTimeout(() => {
            message.success('个人资料已更新');
            setLoading(false);
        }, 1000);
    };

    // --- 个人资料 (Profile) 面板 ---
    const ProfileTab = () => {

        const [isUsernameModified, setIsUsernameModified] = useState(false)

        return (
            <div style={{ width: '100%' }}>
                <Title level={4} style={{ marginBottom: 8, color: token.colorTextHeading }}>个人资料</Title>
                <Paragraph type="secondary" style={{ marginBottom: 24 }}>
                    更新您的头像和个人基本信息。这些信息可能会在团队或协作区公开显示。
                </Paragraph>
                <Divider style={{ borderColor: token.colorBorderSecondary }} />

                <Flex gap={32} align="flex-start" style={{ marginBottom: 32 }}>
                    <div style={{ flex: 1 }}>
                        <Form form={form} layout="vertical" initialValues={{ displayName: 'AtlasAdmin',username:'u_juqhs2k', email: 'admin@atlas.com' }}>
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
                                    // 如果已经修改过，则禁用输入框
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
                                extra="邮箱作为账号标识不可直接修改。如需更换，请前往“安全设置”。"
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
                            beforeUpload={() => { }}
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
                                        // 使用阴影代替边框，视觉上更现代
                                        boxShadow: `0 0 0 1px ${token.colorBorderSecondary}`,
                                    }}
                                    onClick={() => {

                                    }}
                                />
                                <div style={{
                                    position: 'absolute',
                                    bottom: 8,      // 稍微增加一点边距，视觉上更协调
                                    left: 8,        // 这里从 right 改为 left
                                    background: token.colorBgContainer,
                                    borderRadius: '50%',
                                    padding: '6px', // 稍微增加 padding 让图标居中更美观
                                    boxShadow: token.boxShadowSecondary, // 建议加上阴影，在浅色头像上更清晰
                                    display: 'flex',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    border: `1px solid ${token.colorBorderSecondary}` // 增加细边框提升精致感
                                }}>
                                    <Pencil color={token.colorPrimary} size={18} />
                                </div>
                            </div>
                        </Upload>
                    </div>
                </Flex>
            </div>
        )
    };

    // --- 安全设置 (Security) 面板 ---
    const SecurityTab = () => {
        const isWebAuthnSupported = window.PublicKeyCredential !== undefined && typeof window.PublicKeyCredential === 'function';

        const [isPasskeyBound, setIsPasskeyBound] = useState(false);
        const [isMfaEnabled, setIsMfaEnabled] = useState(false);
        const [isGoogleBound, setIsGoogleBound] = useState(true);
        const [isGithubBound, setIsGithubBound] = useState(false);

        const { modal } = App.useApp();

        // 登录方式数据
        const loginMethodsData = [
            {
                key: 'password',
                title: '登录密码',
                description: '已设置。建议您定期更改密码以提升账号安全性。',
                icon: <LockOutlined style={{ color: token.colorPrimary, fontSize: 20 }} />,
                action: <Button>修改密码</Button>,
            },
            {
                key: 'email',
                title: '电子邮件',
                description: '已设置。用于接收安全通知、重置密码及验证码登录。',
                icon: <MailOutlined style={{ color: token.colorPrimary, fontSize: 20 }} />,
                action: (
                    <Flex gap="middle" align="center">
                        <Tag color="success" bordered={false}>已验证</Tag>
                        <Button
                            type="text"
                            size="small"
                            onClick={() => {
                                modal.confirm({
                                    title: '更换绑定邮箱',
                                    content: '为了您的账号安全，更换邮箱需要进行身份验证。',
                                    okText: '开始验证',
                                    onOk: () => { /* 触发更换流程 */ }
                                });
                            }}
                        >
                            修改
                        </Button>
                    </Flex>
                ),
            },
            {
                key: 'passkey',
                title: '通行密钥',
                description: !isWebAuthnSupported
                    ? '您的当前设备不支持通行密钥。'
                    : (isPasskeyBound ? '已绑定设备。支持指纹、面容或 PIN 快速登录。' : '未绑定。绑定后可实现无密码安全登录。'),
                icon: <Fingerprint style={{ fontSize: 20, color: isPasskeyBound ? token.colorSuccess : (isWebAuthnSupported ? token.colorPrimary : token.colorTextDisabled) }} />,
                action: (
                    <Flex gap="small">
                        {!isPasskeyBound ? (
                            <Button
                                type="primary"
                                disabled={!isWebAuthnSupported}
                                onClick={() => {
                                    message.loading('正在调用设备凭证...', 1);
                                    setIsPasskeyBound(true);
                                }}
                            >
                                立即绑定
                            </Button>
                        ) : (
                            <>
                                <Button onClick={() => message.info('管理已绑定设备')}>管理设备</Button>
                                <Button danger type="text" onClick={() => {
                                    modal.confirm({ title: '确认解除绑定？', onOk: () => setIsPasskeyBound(false) });
                                }}>解绑</Button>
                            </>
                        )}
                    </Flex>
                ),
            },
            {
                key: 'google',
                title: 'Google 账号',
                description: isGoogleBound ? '已绑定：user@gmail.com。可使用 Google 一键登录。' : '未绑定。',
                icon: <GoogleOutlined style={{ color: isGoogleBound ? '#EA4335' : token.colorTextDisabled, fontSize: 20 }} />,
                action: (
                    isGoogleBound
                        ? <Button danger type="text" onClick={() => setIsGoogleBound(false)}>解绑</Button>
                        : <Button onClick={() => setIsGoogleBound(true)}>绑定</Button>
                ),
            },
            {
                key: 'github',
                title: 'GitHub 账号',
                description: isGithubBound ? '已绑定。可使用 GitHub 一键登录。' : '未绑定。',
                icon: <GithubOutlined style={{ color: isGithubBound ? token.colorTextHeading : token.colorTextDisabled, fontSize: 20 }} />,
                action: (
                    isGithubBound
                        ? <Button danger type="text" onClick={() => setIsGithubBound(false)}>解绑</Button>
                        : <Button onClick={() => setIsGithubBound(true)}>绑定</Button>
                ),
            }
        ];

        // 多因素认证数据
        const mfaData = [
            {
                key: 'totp',
                title: '身份验证器应用程序',
                description: isMfaEnabled
                    ? '已开启。登录时需要输入身份验证器 (如 Google Authenticator) 生成的 6 位动态码。'
                    : '未开启。极大地提高您的账号安全性，防止密码泄露。',
                icon: <MobileOutlined style={{ color: isMfaEnabled ? token.colorSuccess : token.colorWarning, fontSize: 20 }} />,
                action: isMfaEnabled ? (
                    <Button danger onClick={() => {
                        modal.confirm({ title: '关闭 MFA 认证？', onOk: () => setIsMfaEnabled(false) });
                    }}>关闭认证</Button>
                ) : (
                    <Button type="primary" onClick={() => setIsMfaEnabled(true)}>立即开启</Button>
                ),
            },
            {
                key: 'recovery',
                title: '恢复码',
                description: isMfaEnabled
                    ? '已生成。当您丢失移动设备或无法使用身份验证器时，这是您重新访问账号的唯一方式。'
                    : '开启 TOTP 认证后即可生成并查看恢复码。',
                icon: <FileProtectOutlined style={{ color: isMfaEnabled ? token.colorPrimary : token.colorTextDisabled, fontSize: 20 }} />,
                action: (
                    <Button disabled={!isMfaEnabled} onClick={() => {
                        modal.info({
                            title: '您的恢复码',
                            width: 500,
                            content: (
                                <div style={{ marginTop: 16 }}>
                                    <Alert message="请妥善保管" description="恢复码用于在无法进行 MFA 验证时登录。请下载或抄写并存放在安全位置。" type="warning" showIcon />
                                    <div style={{
                                        display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginTop: 20,
                                        padding: '16px', backgroundColor: token.colorFillAlter, borderRadius: token.borderRadius, fontFamily: 'monospace'
                                    }}>
                                        {['ABCD-1234', 'EFGH-5678', 'IJKL-9012', 'MNOP-3456'].map(code => (
                                            <Text key={code} strong copyable>{code}</Text>
                                        ))}
                                    </div>
                                </div>
                            ),
                        });
                    }}>查看恢复码</Button>
                ),
            }
        ];

        return (
            <div style={{ width: '100%' }}>
                {/* 登录方式区块 */}
                <Title level={4} style={{ marginBottom: 8, color: token.colorTextHeading }}>登录与绑定</Title>
                <Paragraph type="secondary" style={{ marginBottom: 24 }}>
                    管理您可以用来登录 Atlas 账号的凭证和第三方服务。
                </Paragraph>
                <List
                    itemLayout="horizontal"
                    dataSource={loginMethodsData}
                    renderItem={item => (
                        <List.Item actions={[item.action]} style={{ padding: '20px 0', borderBottomColor: token.colorBorderSecondary }}>
                            <List.Item.Meta
                                avatar={<div style={{ padding: 12, background: token.colorFillAlter, borderRadius: '50%' }}>{item.icon}</div>}
                                title={<Text strong style={{ fontSize: 16 }}>{item.title}</Text>}
                                description={<Text type="secondary">{item.description}</Text>}
                            />
                        </List.Item>
                    )}
                    style={{ marginBottom: 48 }}
                />

                {/* 多因素认证区块 */}
                <Title level={4} style={{ marginBottom: 8, color: token.colorTextHeading }}>两步验证 (2FA)</Title>
                <Paragraph type="secondary" style={{ marginBottom: 24 }}>
                    为您的账号增加一层额外的安全保护。
                </Paragraph>
                <List
                    itemLayout="horizontal"
                    dataSource={mfaData}
                    renderItem={item => (
                        <List.Item actions={[item.action]} style={{ padding: '20px 0', borderBottomColor: token.colorBorderSecondary }}>
                            <List.Item.Meta
                                avatar={<div style={{ padding: 12, background: token.colorFillAlter, borderRadius: '50%' }}>{item.icon}</div>}
                                title={<Text strong style={{ fontSize: 16 }}>{item.title}</Text>}
                                description={<Text type="secondary">{item.description}</Text>}
                            />
                        </List.Item>
                    )}
                />
            </div>
        );
    };

    // --- 偏好设置 (Preferences) 面板 ---
    const PreferencesTab = () => (
        <div style={{ width: '100%' }}>
            <Title level={4} style={{ marginBottom: 8, color: token.colorTextHeading }}>系统偏好</Title>
            <Paragraph type="secondary" style={{ marginBottom: 24 }}>
                自定义 Atlas 的外观、语言及通知接收方式。
            </Paragraph>
            <Divider style={{ borderColor: token.colorBorderSecondary }} />

            <Form layout="vertical">
                <Form.Item label="语言区域 (Language)">
                    <Select size="large" defaultValue="zh-CN" options={[
                        { value: 'zh-CN', label: '简体中文' },
                        { value: 'en-US', label: 'English (US)' },
                    ]} />
                </Form.Item>
                <Form.Item label="主题外观 (Theme)">
                    <Segmented
                        size="large"
                        options={[
                            { label: '跟随系统', value: 'auto', icon: <MonitorOutlined /> },
                            { label: '浅色模式', value: 'light' },
                            { label: '深色模式', value: 'dark' },
                        ]}
                    />
                </Form.Item>
                <Divider style={{ borderColor: token.colorBorderSecondary }} />
                <Flex justify="space-between" align="center" style={{ marginBottom: 24 }}>
                    <div>
                        <Text strong style={{ display: 'block', fontSize: 16 }}>系统通知</Text>
                        <Text type="secondary">接收关于系统更新、维护及关键事件的通知</Text>
                    </div>
                    <Switch defaultChecked />
                </Flex>
                <Flex justify="space-between" align="center">
                    <div>
                        <Text strong style={{ display: 'block', fontSize: 16 }}>营销与推广</Text>
                        <Text type="secondary">接收关于新功能和最佳实践的邮件</Text>
                    </div>
                    <Switch />
                </Flex>
            </Form>
        </div>
    );

    return (
        <Flex justify="center" style={{ width: '100%', minHeight: '100%' }}>
            <div style={{
                width: '100%',
                backgroundColor: token.colorBgContainer,
                borderRadius: token.borderRadiusLG,
            }}>
                <Tabs
                    tabPosition="left"
                    defaultActiveKey="profile"
                    style={{ height: '100%' }}
                    tabBarStyle={{
                        width: 240,
                        padding: '24px 0',
                        borderRight: `1px solid ${token.colorBorderSecondary}`
                    }}
                    items={[
                        {
                            key: 'profile',
                            label: (
                                <Flex align="center" gap={12} style={{ padding: '4px 8px' }}>
                                    <UserOutlined style={{ fontSize: 16 }} />
                                    <span style={{ fontSize: 15 }}>个人资料</span>
                                </Flex>
                            ),
                            children: <div style={{ padding: '32px 48px' }}><ProfileTab /></div>,
                        },
                        {
                            key: 'security',
                            label: (
                                <Flex align="center" gap={12} style={{ padding: '4px 8px' }}>
                                    <SafetyCertificateOutlined style={{ fontSize: 16 }} />
                                    <span style={{ fontSize: 15 }}>安全设置</span>
                                </Flex>
                            ),
                            children: <div style={{ padding: '32px 48px' }}><SecurityTab /></div>,
                        },
                        {
                            key: 'preferences',
                            label: (
                                <Flex align="center" gap={12} style={{ padding: '4px 8px' }}>
                                    <SettingOutlined style={{ fontSize: 16 }} />
                                    <span style={{ fontSize: 15 }}>偏好设置</span>
                                </Flex>
                            ),
                            children: <div style={{ padding: '32px 48px' }}><PreferencesTab /></div>,
                        }
                    ]}
                />
            </div>
        </Flex>
    );
};

export default AccountSettings;