import React, { useState } from 'react';
import { Button, Typography, Flex, App, Alert, theme } from 'antd';
import { MobileOutlined, FileProtectOutlined } from '@ant-design/icons';

const { Text } = Typography;

const MfaSecurityItem = ({ mfaEnabled, recoveryCodeGenerated, refresh }) => {
    const { token } = theme.useToken();
    const { modal, message } = App.useApp();
    
    // 内部维护 Loading 状态，防止重复点击
    const [totpLoading, setTotpLoading] = useState(false);
    const [recoveryLoading, setRecoveryLoading] = useState(false);

    // 1. 处理 TOTP 开启/关闭
    const handleToggleMfa = () => {
        if (mfaEnabled) {
            modal.confirm({
                title: '关闭 MFA 认证？',
                content: '关闭两步验证后，您的账号安全性将会降低。确定要关闭吗？',
                okText: '确定关闭',
                okType: 'danger',
                cancelText: '取消',
                onOk: async () => {
                    try {
                        setTotpLoading(true);
                        // TODO: 替换为您真正的后端解绑接口，例如：await disableMfaApi();
                        await new Promise(resolve => setTimeout(resolve, 800)); 
                        
                        message.success('两步验证已成功关闭');
                        refresh?.(); // 触发外部刷新状态
                    } catch (error) {
                        message.error('关闭两步验证失败');
                    } finally {
                        setTotpLoading(false);
                    }
                }
            });
        } else {
            // 💡 开启流程：这里可以拉起您内部的开启 MFA / 扫码绑定的 Modal
            console.log('打开 MFA 绑定弹窗');
            // 绑定成功后记得调用 refresh()
        }
    };

    // 2. 处理恢复码的生成与查看
    const handleRecoveryAction = async () => {
        if (!recoveryCodeGenerated) {
            // 情况 A：未生成过，走生成逻辑
            modal.confirm({
                title: '生成安全恢复码',
                content: '生成新的恢复码后，请务必妥善保存。当您丢失设备时，这是唯一的自救方式。',
                okText: '生成',
                cancelText: '取消',
                onOk: async () => {
                    try {
                        setRecoveryLoading(true);
                        // TODO: 替换为您真正的生成恢复码接口，例如：await generateRecoveryCodesApi();
                        await new Promise(resolve => setTimeout(resolve, 800));
                        
                        message.success('恢复码已成功创建');
                        refresh?.(); // 刷新外部状态，让按钮变为“查看恢复码”
                    } catch (error) {
                        message.error('恢复码生成失败');
                    } finally {
                        setRecoveryLoading(false);
                    }
                }
            });
        } else {
            // 情况 B：已生成，直接在内部请求列表并弹窗展示
            try {
                setRecoveryLoading(true);
                // TODO: 替换为您真正的获取恢复码明文接口，例如：const res = await fetchRecoveryCodesApi();
                await new Promise(resolve => setTimeout(resolve, 600));
                const mockCodes = ['ABCD-1234', 'EFGH-5678', 'IJKL-9012', 'MNOP-3456'];

                modal.info({
                    title: '您的安全恢复码',
                    width: 500,
                    okText: '我已妥善保存',
                    content: (
                        <div style={{ marginTop: 16 }}>
                            <Alert 
                                message="请妥善保管" 
                                description="恢复码用于在无法进行 MFA 验证时紧急登录。请下载或抄写并存放在绝对安全的位置。" 
                                type="warning" 
                                showIcon 
                            />
                            <div style={{
                                display: 'grid', 
                                gridTemplateColumns: '1fr 1fr', 
                                gap: '12px', 
                                marginTop: 20,
                                padding: '16px', 
                                backgroundColor: token.colorFillAlter, 
                                borderRadius: token.borderRadius, 
                                fontFamily: 'monospace'
                            }}>
                                {mockCodes.map(code => (
                                    <Text key={code} strong copyable>{code}</Text>
                                ))}
                            </div>
                        </div>
                    ),
                });
            } catch (error) {
                message.error('获取恢复码失败，请重试');
            } finally {
                setRecoveryLoading(false);
            }
        }
    };

    // 圆圈图标样式定义
    const getIconWrapperStyle = (active) => ({
        padding: 12, 
        background: token.colorFillAlter, 
        borderRadius: '50%', 
        display: 'flex',
        opacity: active ? 1 : 0.6
    });

    return (
        <>
            {/* 1. 身份验证器项 */}
            <Flex 
                justify="space-between" 
                align="center" 
                style={{ 
                    padding: '20px 0', 
                    borderBottom: `1px solid ${token.colorBorderSecondary}` 
                }}
            >
                <Flex gap={16} align="flex-start" style={{ flex: 1 }}>
                    <div style={getIconWrapperStyle(true)}>
                        <MobileOutlined style={{ color: mfaEnabled ? token.colorSuccess : token.colorWarning, fontSize: 20 }} />
                    </div>
                    <Flex vertical gap={4}>
                        <Text strong style={{ fontSize: 16 }}>身份验证器应用程序</Text>
                        <Text style={{ color: token.colorTextDescription, fontSize: 14, lineHeight: '22px' }}>
                            {mfaEnabled
                                ? '已开启。登录时需要输入身份验证器 (如 Google Authenticator) 生成的 6 位动态码。'
                                : '未开启。极大地提高您的账号安全性，防止密码泄露。'}
                        </Text>
                    </Flex>
                </Flex>

                <Flex align="center" style={{ justifyContent: 'flex-end' }}>
                    <Button 
                        danger={mfaEnabled} 
                        type={mfaEnabled ? 'default' : 'primary'}
                        loading={totpLoading}
                        onClick={handleToggleMfa}
                    >
                        {mfaEnabled ? '关闭认证' : '立即开启'}
                    </Button>
                </Flex>
            </Flex>

            {/* 2. 恢复码项 */}
            <Flex 
                justify="space-between" 
                align="center" 
                style={{ 
                    padding: '20px 0', 
                    borderBottom: `1px solid ${token.colorBorderSecondary}` 
                }}
            >
                <Flex gap={16} align="flex-start" style={{ flex: 1 }}>
                    <div style={getIconWrapperStyle(mfaEnabled)}>
                        <FileProtectOutlined style={{ color: mfaEnabled ? token.colorPrimary : token.colorTextDisabled, fontSize: 20 }} />
                    </div>
                    <Flex vertical gap={4}>
                        <Text strong style={{ fontSize: 16, color: mfaEnabled ? token.colorText : token.colorTextDisabled }}>
                            恢复码
                        </Text>
                        <Text style={{ color: mfaEnabled ? token.colorTextDescription : token.colorTextDisabled, fontSize: 14, lineHeight: '22px' }}>
                            {recoveryCodeGenerated
                                ? '已生成。当您丢失移动设备或无法使用身份验证器时，这是您重新访问账号的唯一方式。'
                                : (mfaEnabled
                                    ? '未生成。为了防范身份验证器丢失风险，建议立即生成安全恢复码。'
                                    : '开启 TOTP 认证后即可生成并查看恢复码。')}
                        </Text>
                    </Flex>
                </Flex>

                <Flex align="center" style={{ justifyContent: 'flex-end' }}>
                    <Button
                        disabled={!mfaEnabled}
                        loading={recoveryLoading}
                        type={!recoveryCodeGenerated && mfaEnabled ? "primary" : "default"}
                        onClick={handleRecoveryAction}
                    >
                        {!recoveryCodeGenerated ? '生成恢复码' : '查看恢复码'}
                    </Button>
                </Flex>
            </Flex>
        </>
    );
};

export default MfaSecurityItem;