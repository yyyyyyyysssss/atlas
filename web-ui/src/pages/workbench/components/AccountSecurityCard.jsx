
import React from 'react';
import { Card, Flex, Typography, Button, Space, Tag, theme, Skeleton } from 'antd';
import {
    ShieldCheck,
    ShieldAlert,
    Mail,
    Smartphone,
    Fingerprint,
    KeyRound,
    ChevronRight,
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useRequest } from 'ahooks';
import { fetchAccountSecurity } from '../../../services/AccountService';

const { Text } = Typography;

const AccountSecurityCard = () => {
    const { token } = theme.useToken()

    const navigate = useNavigate()

    const { data, loading } = useRequest(fetchAccountSecurity)

    const {
        passwordSet = false,
        passkeyEnabled = false,
        emailVerified = false,
        totpEnabled = false,
    } = data || {};

    const securityItems = [
        {
            key: 'totp',
            label: '多因素认证',
            description: '为账号增加额外的身份验证',
            enabled: totpEnabled,
            icon: Smartphone,
        },
        {
            key: 'passkey',
            label: 'Passkey',
            description: '使用设备凭据进行无密码登录',
            enabled: passkeyEnabled,
            icon: Fingerprint,
        },
        {
            key: 'password',
            label: '登录密码',
            description: '管理登录密码，保障账号安全',
            enabled: passwordSet,
            icon: KeyRound,
        },
        {
            key: 'email',
            label: '邮箱验证',
            description: '用于账号验证与安全通知',
            enabled: emailVerified,
            icon: Mail,
        }
    ]

    const enabledCount = securityItems.filter(item => item.enabled).length;
    const allEnabled = enabledCount === securityItems.length;

    const handleGoSecurity = (section) => {
        navigate(`/account/settings?panel=security&section=${section}`);
    };

    return (
        <Card
            title={
                <Space>
                    <ShieldCheck
                        size={18}
                        color={token.colorPrimary}
                    />
                    <span>账号安全</span>
                </Space>
            }
            extra={
                <Button
                    type="link"
                    size="small"
                    onClick={() => handleGoSecurity('')}
                    style={{ paddingInline: 0 }}
                >
                    安全设置
                </Button>
            }
            variant="borderless"
        >
            {loading ? (
                <Skeleton active paragraph={{ rows: 5 }} />
            ) : (
                <Flex vertical gap={16}>
                    {/* 安全状态概览 */}
                    <Flex
                        align="center"
                        justify="between"
                        gap={12}
                        style={{
                            padding: 16,
                            background: token.colorFillAlter,
                            borderRadius: token.borderRadiusLG,
                        }}
                    >
                        {allEnabled ? (
                            <ShieldCheck
                                size={28}
                                color={token.colorSuccess}
                            />
                        ) : (
                            <ShieldAlert
                                size={28}
                                color={token.colorWarning}
                            />
                        )}

                        <Flex flex={1} vertical gap={4}>
                            <Text strong>
                                {allEnabled
                                    ? '账号安全状态良好'
                                    : '完善账号安全设置'}
                            </Text>
                            <Text
                                type="secondary"
                                style={{ fontSize: token.fontSizeSM }}
                            >
                                已完成 {enabledCount}/{securityItems.length} 项安全配置
                            </Text>
                        </Flex>

                        <Tag color={allEnabled ? 'success' : 'warning'}>
                            {allEnabled ? '良好' : '待完善'}
                        </Tag>
                    </Flex>

                    {/* 安全配置列表 */}
                    <Flex vertical gap={8}>
                        {securityItems.map((item) => {
                            const Icon = item.icon;

                            return (
                                <Button
                                    key={item.key}
                                    block
                                    type="text"
                                    onClick={() => handleGoSecurity(item.key)}
                                    className="atlas-float-trigger"
                                    style={{
                                        height: 'auto',
                                        padding: '12px 8px',
                                        borderRadius: token.borderRadiusLG,
                                        background: token.colorFillAlter,
                                        textAlign: 'left',
                                    }}
                                >
                                    <Flex
                                        align="center"
                                        gap={12}
                                        style={{ width: '100%' }}
                                    >
                                        <Icon
                                            size={22}
                                            color={
                                                item.enabled
                                                    ? token.colorSuccess
                                                    : token.colorTextSecondary
                                            }
                                        />

                                        <Flex
                                            flex={1}
                                            vertical
                                            align="start"
                                            gap={4}
                                            style={{ minWidth: 0 }}
                                        >
                                            <Text
                                                strong
                                                style={{
                                                    fontSize: token.fontSize,
                                                }}
                                            >
                                                {item.label}
                                            </Text>

                                            <Text
                                                type="secondary"
                                                style={{
                                                    fontSize: token.fontSizeSM,
                                                }}
                                            >
                                                {item.description}
                                            </Text>
                                        </Flex>

                                        <Tag
                                            color={item.enabled ? 'success' : 'default'}
                                            style={{ marginInlineEnd: 0 }}
                                        >
                                            {item.enabled ? '已启用' : '未设置'}
                                        </Tag>

                                        <ChevronRight
                                            size={16}
                                            color={token.colorTextSecondary}
                                        />
                                    </Flex>
                                </Button>
                            );
                        })}
                    </Flex>
                </Flex>
            )}
        </Card>
    );
};

export default AccountSecurityCard;