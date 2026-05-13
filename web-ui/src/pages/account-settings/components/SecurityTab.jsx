import React, { useState } from 'react';
import { Button, Typography, theme, List, Flex, Tag, App, Alert } from 'antd';
import {
    LockOutlined, MailOutlined, MobileOutlined,
    FileProtectOutlined, GoogleOutlined, GithubOutlined
} from '@ant-design/icons';
import { Fingerprint } from 'lucide-react';

const { Title, Text, Paragraph } = Typography;

const SecurityTab = () => {
    const { token } = theme.useToken();
    const { modal, message } = App.useApp();

    const isWebAuthnSupported = window.PublicKeyCredential !== undefined && typeof window.PublicKeyCredential === 'function';

    const [isPasskeyBound, setIsPasskeyBound] = useState(false);
    const [isMfaEnabled, setIsMfaEnabled] = useState(false);
    const [isGoogleBound, setIsGoogleBound] = useState(true);
    const [isGithubBound, setIsGithubBound] = useState(false);

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
                                onOk: () => { }
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

export default SecurityTab;