import React, { useEffect, useState } from 'react';
import { Button, Typography, theme, List, Flex, Tag, App, Alert, Input, Tooltip, Space } from 'antd';
import {
    LockOutlined, MailOutlined, MobileOutlined,
    FileProtectOutlined, GoogleOutlined, GithubOutlined,
    UserOutlined,
    InfoCircleOutlined
} from '@ant-design/icons';
import { Fingerprint } from 'lucide-react';
import { useRequest } from 'ahooks';
import { changeUsername, fetchAccountSecurity } from '../../../services/UserProfileService';
import Loading from '../../../components/loading';
import { updateUserInfoPartial } from '../../../redux/slices/userSlice';
import { useDispatch } from 'react-redux';

const { Title, Text, Paragraph } = Typography;

const SecurityTab = () => {
    const { token } = theme.useToken();
    const { modal, message } = App.useApp();

    const dispatch = useDispatch()

    const { data, loading, refresh } = useRequest(fetchAccountSecurity)

    const { runAsync: changeUsernameAsync, loading: changeUsernameLoading } = useRequest(changeUsername, { manual: true });

    const isWebAuthnSupported = window.PublicKeyCredential !== undefined && typeof window.PublicKeyCredential === 'function';

    const {
        username,
        isUsernameModified,
        passwordSet,
        passkeyBound,
        boundEmail,
        emailVerified,
        boundPhone,
        phoneVerified,
        googleBound,
        githubBound,
        mfaEnabled,
        recoveryCodeGenerated
    } = data || {}

    useEffect(() => {
        if (username) {
            setEditingUsername(username)
        }
    }, [username])

    const [isUsernameEditing, setIsUsernameEditing] = useState(false)

    const [editingUsername, setEditingUsername] = useState(username)

    if (loading || !data) {
        return (
            <Loading tip="正在载入安全设置..." />
        );
    }

    const loginMethodsData = [
        {
            key: 'username',
            title: '系统账号',
            description: (
                <Flex vertical gap={4} style={{ color: token.colorTextDescription, fontSize: 14, lineHeight: '22px' }}>
                    <div>主要用于系统内唯一识别与基础登录。</div>
                    <Text
                        type="secondary"
                        style={{
                            fontSize: 13, // 提示性文字比主描述稍微收敛一点点（13px），更有层次感
                            color: !isUsernameModified ? token.colorWarningActive : token.colorTextDisabled
                        }}
                    >
                        {!isUsernameModified
                            ? "⚠️ 账号仅支持修改一次，请谨慎填写。"
                            : "您已修改过账号，如需再次更改请联系系统管理员。"}
                    </Text>
                </Flex>
            ),
            icon: <UserOutlined style={{ color: token.colorPrimary, fontSize: 20 }} />,
            action: (
                <Flex align="center" gap={12} style={{ minWidth: 280, justifyContent: 'flex-end' }}>
                    {/* 输入框：没有 Form 后，直接用 value 和 onChange 绑定 */}
                    <Input
                        size="middle"
                        value={editingUsername}
                        disabled={!isUsernameEditing || changeUsernameLoading}
                        onChange={(e) => setEditingUsername(e.target.value.trim())}
                        style={{
                            width: 180,
                            // 没在编辑时，把外框去掉，伪装成普通文本展示
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

                    {/* 状态 A：没改过，且不在编辑状态 -> 显示“修改”字样 */}
                    {!isUsernameModified && !isUsernameEditing && (
                        <Button
                            type="text"
                            size="small"
                            onClick={() => {
                                setEditingUsername(username); // 确保打开时是最新值
                                setIsUsernameEditing(true);
                            }}
                        >
                            修改
                        </Button>
                    )}

                    {/* 状态 B：已经改过了，且不在编辑状态 -> 优雅展示“不可修改” */}
                    {isUsernameModified && !isUsernameEditing && (
                        <Text type="secondary" style={{ fontSize: 13 }}>不可修改</Text>
                    )}

                    {/* 状态 C：正在编辑中 -> 展示“保存”与“取消” */}
                    {isUsernameEditing && (
                        <Space size={4}>
                            <Button
                                type="primary"
                                size="middle"
                                loading={changeUsernameLoading}
                                onClick={() => {
                                    // 简单的非空校验
                                    if (!editingUsername) {
                                        message.warning('账号名不能为空');
                                        return;
                                    }
                                    // 弹出防呆确认二次弹窗
                                    modal.confirm({
                                        title: '确认修改账号名？',
                                        content: `您即将把账号修改为 "${editingUsername}"。提交后将固定，无法再次更改！`,
                                        okText: '确认提交',
                                        onOk: async () => {
                                            await changeUsernameAsync({ newUsername: editingUsername })
                                            dispatch(updateUserInfoPartial({
                                                username: editingUsername,
                                            }))
                                            message.success('账号修改成功')
                                            setIsUsernameEditing(false)
                                            refresh()
                                        }
                                    });
                                }}
                            >
                                保存
                            </Button>
                            <Button
                                type="default"
                                size="middle"
                                disabled={changeUsernameLoading}
                                onClick={() => {
                                    setEditingUsername(username); // 还原数据
                                    setIsUsernameEditing(false);
                                }}
                            >
                                取消
                            </Button>
                        </Space>
                    )}
                </Flex>
            ),
        },
        {
            key: 'password',
            title: '登录密码',
            description: passwordSet
                ? '已设置。建议您定期更改密码以提升账号安全性。'
                : '未设置。请尽快设置密码以确保基础登录安全。',
            icon: <LockOutlined style={{ color: token.colorPrimary, fontSize: 20 }} />,
            action: <Button>{passwordSet ? '修改密码' : '设置密码'}</Button>,
        },
        {
            key: 'email',
            title: '电子邮箱',
            description: boundEmail
                ? `已绑定：${boundEmail}。用于接收安全通知、重置密码及验证码登录。`
                : '未绑定电子邮箱。绑定后可用于接收通知与找回密码。',
            icon: <MailOutlined style={{ color: token.colorPrimary, fontSize: 20 }} />,
            action: (
                <Flex gap="middle" align="center">
                    {boundEmail && (
                        <Tag color={emailVerified ? "success" : "warning"} bordered={false}>
                            {emailVerified ? "已验证" : "未验证"}
                        </Tag>
                    )}
                    <Button
                        type={boundEmail ? "text" : "primary"}
                        size={boundEmail ? "small" : "middle"}
                        onClick={() => {
                            modal.confirm({
                                title: boundEmail ? '更换绑定邮箱' : '绑定电子邮箱',
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
                : (passkeyBound ? '已绑定设备。支持指纹、面容或 PIN 快速登录。' : '未绑定。绑定后可实现无密码安全登录。'),
            icon: <Fingerprint style={{ fontSize: 20, color: passkeyBound ? token.colorSuccess : (isWebAuthnSupported ? token.colorPrimary : token.colorTextDisabled) }} />,
            action: (
                <Flex gap="small">
                    {!passkeyBound ? (
                        <Button
                            type="primary"
                            disabled={!isWebAuthnSupported}
                            onClick={() => {
                                message.loading('正在调用设备凭证...', 1);
                            }}
                        >
                            立即绑定
                        </Button>
                    ) : (
                        <>
                            <Button onClick={() => message.info('管理已绑定设备')}>管理设备</Button>
                            <Button danger type="text" onClick={() => {
                                modal.confirm({ title: '确认解除绑定？', onOk: () => { } });
                            }}>解绑</Button>
                        </>
                    )}
                </Flex>
            ),
        },
        {
            key: 'google',
            title: 'Google 账号',
            description: googleBound ? '已绑定。可使用 Google 一键登录。' : '未绑定。',
            icon: <GoogleOutlined style={{ color: googleBound ? '#EA4335' : token.colorTextDisabled, fontSize: 20 }} />,
            action: (
                googleBound
                    ? <Button danger type="text" onClick={() => { /* 联动后端解绑，成功后 refresh() */ }}>解绑</Button>
                    : <Button onClick={() => { /* 引导 OAuth2 绑定 */ }}>绑定</Button>
            ),
        },
        {
            key: 'github',
            title: 'GitHub 账号',
            description: githubBound ? '已绑定。可使用 GitHub 一键登录。' : '未绑定。',
            icon: <GithubOutlined style={{ color: githubBound ? token.colorTextHeading : token.colorTextDisabled, fontSize: 20 }} />,
            action: (
                githubBound
                    ? <Button danger type="text" onClick={() => { /* 联动后端解绑，成功后 refresh() */ }}>解绑</Button>
                    : <Button onClick={() => { /* 引导 OAuth2 绑定 */ }}>绑定</Button>
            ),
        }
    ];

    const mfaData = [
        {
            key: 'totp',
            title: '身份验证器应用程序',
            description: mfaEnabled
                ? '已开启。登录时需要输入身份验证器 (如 Google Authenticator) 生成的 6 位动态码。'
                : '未开启。极大地提高您的账号安全性，防止密码泄露。',
            icon: <MobileOutlined style={{ color: mfaEnabled ? token.colorSuccess : token.colorWarning, fontSize: 20 }} />,
            action: mfaEnabled ? (
                <Button danger onClick={() => {
                    modal.confirm({ title: '关闭 MFA 认证？', onOk: () => { /* 联动后端解绑 */ } });
                }}>关闭认证</Button>
            ) : (
                <Button type="primary" onClick={() => { /* 开启 TOTP 流程 */ }}>立即开启</Button>
            ),
        },
        {
            key: 'recovery',
            title: '恢复码',
            description: recoveryCodeGenerated
                ? '已生成。当您丢失移动设备或无法使用身份验证器时，这是您重新访问账号的唯一方式。'
                : (mfaEnabled
                    ? '未生成。为了防范身份验证器丢失风险，建议立即生成安全恢复码。'
                    : '开启 TOTP 认证后即可生成并查看恢复码。'),
            icon: <FileProtectOutlined style={{ color: mfaEnabled ? token.colorPrimary : token.colorTextDisabled, fontSize: 20 }} />,
            action: (
                <Button
                    disabled={!mfaEnabled}
                    type={!recoveryCodeGenerated ? "primary" : "default"}
                    onClick={() => {
                        if (!recoveryCodeGenerated) {
                            // 💡 3. 业务防呆：如果没生成过，点击应该是去请求后端“生成”
                            modal.confirm({
                                title: '生成安全恢复码',
                                content: '生成新的恢复码后，请务必妥善保存。',
                                onOk: () => {
                                    // TODO: 调用后端 generateRecoveryCodes 接口，成功后执行 refresh()
                                    message.success('恢复码已成功创建');
                                }
                            });
                        } else {
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
                        }

                    }}>
                    查看恢复码
                </Button>
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