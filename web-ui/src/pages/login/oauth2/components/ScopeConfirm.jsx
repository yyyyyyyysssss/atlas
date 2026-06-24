import React from 'react';
import { Flex, Card, Button, Typography, Avatar, theme, Checkbox, Divider } from "antd";
import { UserOutlined, MailOutlined, PhoneOutlined, ProfileOutlined, CheckCircleFilled } from '@ant-design/icons';
import logo from '/favicon.ico';

const ScopeItem = ({ icon, title, description }) => {
    const { token } = theme.useToken();
    return (
        <Flex gap={12} align="start" style={{ marginBottom: 16 }}>
            <div style={{ 
                padding: '8px', 
                backgroundColor: token.colorFillAlter, 
                borderRadius: '8px',
                color: token.colorPrimary,
                fontSize: '18px'
            }}>
                {icon}
            </div>
            <Flex vertical flex={1}>
                <Typography.Text strong>{title}</Typography.Text>
                <Typography.Text type="secondary" style={{ fontSize: 12 }}>{description}</Typography.Text>
            </Flex>
            <CheckCircleFilled style={{ color: token.colorSuccess, marginTop: 4 }} />
        </Flex>
    );
};

const ScopeConfirm = ({ params, onConfirm, onCancel, loading }) => {
    const { token } = theme.useToken();
    const scopes = params.scope ? params.scope.split(' ') : [];

    const scopeMap = {
        'openid': {
            icon: <UserOutlined />,
            title: '公开身份信息',
            description: '查看您的 Atlas 账号唯一标识符'
        },
        'profile': {
            icon: <ProfileOutlined />,
            title: '个人基本信息',
            description: '查看您的头像、姓名等个人资料'
        },
        'email': {
            icon: <MailOutlined />,
            title: '电子邮箱地址',
            description: '查看您的主要电子邮箱地址'
        },
        'phone': {
            icon: <PhoneOutlined />,
            title: '手机号码',
            description: '查看您账号绑定的手机号码'
        }
    };

    return (
        <Card
            title={
                <Flex gap={10} align='center'>
                    <Avatar src={logo} />
                    <Typography.Title level={4} style={{ margin: 0 }}>授权确认</Typography.Title>
                </Flex>
            }
            style={{ 
                width: '100%', 
                height: '100%',
                borderRadius: "20px", 
                boxShadow: token.boxShadowTertiary,
                overflow: 'hidden'
            }}
        >
            <Flex vertical gap="middle">
                <Flex vertical align="center" gap={12} style={{ marginBottom: 8 }}>
                    <Avatar src={params.logo_uri} size={64} />
                    <Typography.Title level={5} style={{ margin: 0 }}>
                        {params.client_name} 想要访问您的账号：
                    </Typography.Title>
                </Flex>

                <div style={{ maxHeight: '300px', overflowY: 'auto', paddingRight: 8 }}>
                    {scopes.map(scope => {
                        const info = scopeMap[scope] || {
                            icon: <ProfileOutlined />,
                            title: scope,
                            description: `访问您的 ${scope} 权限`
                        };
                        return (
                            <ScopeItem 
                                key={scope}
                                icon={info.icon}
                                title={info.title}
                                description={info.description}
                            />
                        );
                    })}
                </div>

                <Divider style={{ margin: '8px 0' }} />

                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                    确认后，您将允许该应用访问上述选中的信息。您可以随时在 Atlas 账号设置中撤销授权。
                </Typography.Text>

                <Flex gap="small" justify="end">
                    <Button onClick={onCancel} disabled={loading}>取消</Button>
                    <Button type="primary" onClick={onConfirm} loading={loading}>
                        允许
                    </Button>
                </Flex>
            </Flex>
        </Card>
    );
};

export default ScopeConfirm;
