import React from 'react';
import { Button, Typography, Flex, App, theme, Space } from 'antd';
import { Fingerprint } from 'lucide-react';

const { Text } = Typography;

const PasskeyItem = ({ passkeyBound, refresh }) => {
    const { token } = theme.useToken();
    const { message, modal } = App.useApp();

    const isWebAuthnSupported = window.PublicKeyCredential !== undefined && typeof window.PublicKeyCredential === 'function';

    // 💡 动态计算图标颜色
    const getIconColor = () => {
        if (passkeyBound) return token.colorSuccess;
        if (isWebAuthnSupported) return token.colorPrimary;
        return token.colorTextDisabled;
    };

    const handleBind = () => {
        message.loading('正在调用设备凭证...', 1);
        // 后续在这里对接 WebAuthn 注册凭证逻辑
    };

    const handleUnbind = () => {
        modal.confirm({
            title: '确认解除绑定？',
            content: '解绑后您将无法使用指纹、面容或 PIN 码快速登录此设备。',
            okText: '确认解绑',
            cancelText: '取消',
            okButtonProps: { danger: true },
            onOk: async () => {
                try {
                    console.log('触发解绑流程');
                    // refresh();
                } catch (error) {
                    console.error(error);
                }
            }
        });
    };

    return (
        <Flex
            justify="space-between"
            align="center" // 💡 保持两端垂直居中
            style={{
                padding: '20px 0',
                borderBottom: `1px solid ${token.colorBorderSecondary}`
            }}
        >
            {/* 左侧：图标 + 标题与描述 */}
            <Flex gap={16} align="flex-start" style={{ flex: 1 }}>
                <div style={{ padding: 12, background: token.colorFillAlter, borderRadius: '50%', display: 'flex' }}>
                    <Fingerprint style={{ fontSize: 20, color: getIconColor() }} />
                </div>
                <Flex vertical gap={4}>
                    <Text strong style={{ fontSize: 16 }}>通行密钥</Text>
                    <Text style={{ color: token.colorTextDescription, fontSize: 14, lineHeight: '22px' }}>
                        {!isWebAuthnSupported
                            ? '您的当前设备或浏览器不支持通行密钥。'
                            : (passkeyBound ? '已绑定设备。支持指纹、面容或 PIN 快速登录。' : '未绑定。绑定后可实现无密码安全登录。')}
                    </Text>
                </Flex>
            </Flex>

            {/* 右侧：操作区域 */}
            <Flex align="center" style={{ justifyContent: 'flex-end' }}>
                {!passkeyBound ? (
                    <Button
                        type="primary"
                        disabled={!isWebAuthnSupported}
                        onClick={handleBind}
                    >
                        立即绑定
                    </Button>
                ) : (
                    // 💡 使用 Space 包裹多个按钮，间距更规范
                    <Space size="small">
                        <Button onClick={() => message.info('管理已绑定设备')}>
                            管理设备
                        </Button>
                        <Button danger type="text" onClick={handleUnbind}>
                            解绑
                        </Button>
                    </Space>
                )}
            </Flex>
        </Flex>
    );
};

export default PasskeyItem;