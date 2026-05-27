import React, { useState } from 'react';
import { Button, Typography, Flex, App, Alert, theme, Modal, QRCode, Input, Space } from 'antd';
import { MobileOutlined, KeyOutlined, EyeOutlined, EyeInvisibleOutlined } from '@ant-design/icons';
import { useRequest } from 'ahooks';
import { activateTotp, initTotp } from '../../../services/AccountService';
import Loading from '../../../components/loading';

const { Text, Paragraph } = Typography;

const TotpSecurityItem = ({ context, refresh }) => {
    const { token } = theme.useToken();
    const { message } = App.useApp();

    const { mfaEnabled } = context || {};

    const [isBindModalOpen, setIsBindModalOpen] = useState(false);
    const [showSecret, setShowSecret] = useState(false);
    const [totpCode, setTotpCode] = useState('');

    const {
        data: initData,
        loading: initTotpLoading,
        run: runInitTotp
    } = useRequest(initTotp, {
        manual: true,
        onSuccess: () => {
            setTotpCode('');
            setShowSecret(false);
        },
        onError: () => {
            message.error('获取双重认证密钥失败，请重试');
        }
    });

    // 激活 TOTP
    const {
        loading: activateTotpLoading,
        runAsync: activateTotpAsync
    } = useRequest(activateTotp, { manual: true });

    const { secret, otpAuthUrl } = initData || {};

    // ==================== 事件处理 ====================
    const handleToggleMfa = () => {
        if (mfaEnabled) {
            // TODO: 解绑流程逻辑
        } else {
            setIsBindModalOpen(true);
            runInitTotp();
        }
    };

    const handleBindSubmit = async () => {
        if (totpCode.length !== 6) {
            message.warning('请输入完整的 6 位验证码');
            return;
        }
        try {
            await activateTotpAsync({ code: totpCode });
            message.success('MFA 身份验证器绑定成功！');
            setIsBindModalOpen(false);
            refresh?.();
        } catch (err) {
            // 验证失败时保持打开状态
        }
    };

    // 图标外圈样式
    const iconWrapperStyle = {
        padding: 12,
        background: token.colorFillAlter,
        borderRadius: '50%',
        display: 'flex'
    };

    return (
        <>
            <Flex
                justify="space-between"
                align="center"
                style={{
                    padding: '20px 0',
                    borderBottom: `1px solid ${token.colorBorderSecondary}`
                }}
            >
                <Flex gap={16} align="flex-start" style={{ flex: 1 }}>
                    <div style={iconWrapperStyle}>
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
                        onClick={handleToggleMfa}
                    >
                        {mfaEnabled ? '关闭认证' : '立即开启'}
                    </Button>
                </Flex>
            </Flex>

            {/* 声明式绑定 Modal */}
            <Modal
                title={
                    <Space>
                        <MobileOutlined style={{ color: token.colorPrimary }} />
                        <span>绑定身份验证器</span>
                    </Space>
                }
                open={isBindModalOpen}
                onCancel={() => setIsBindModalOpen(false)}
                onOk={handleBindSubmit}
                confirmLoading={activateTotpLoading}
                okText="激活并开启"
                cancelText="取消"
                width={420}
                destroyOnHidden
            >
                <Flex vertical gap={16} style={{ marginTop: 16 }}>
                    <Alert message="请使用 Google Authenticator 或双重认证 App 扫描下方二维码" type="info" showIcon />

                    <Flex
                        justify="center"
                        align="center"
                        style={{
                            padding: '16px 0',
                            height: 212, // 180(图片尺寸) + 32(上下padding的总和)，锁死高度防止抖动
                            width: '100%',
                            position: 'relative'
                        }}
                    >
                        {/* 只有当接口响应过（无论成功失败），或者处于 loading 状态时才渲染 QRCode，保证初始进入有据可依 */}
                        {(otpAuthUrl || initTotpLoading) ? (
                            <QRCode
                                value={otpAuthUrl || 'loading'} // 确保 loading 时也有非空占位值，防止组件内部崩塌
                                size={180}
                                status={initTotpLoading ? 'loading' : (otpAuthUrl ? 'active' : 'expired')}
                                onRefresh={runInitTotp}
                            />
                        ) : (
                            // 极端防御：如果既没加载也没数据（比如接口挂了），渲染一个和二维码等大的骨架屏或占位块
                            <div style={{ width: 180, height: 180, background: token.colorFillAlter, borderRadius: token.borderRadiusLG }} />
                        )}
                    </Flex>

                    {/* 可折叠密钥区 */}
                    <Flex vertical style={{ background: token.colorFillAlter, padding: '10px 14px', borderRadius: token.borderRadius }}>
                        <Flex justify="space-between" align="center">
                            <Space size={6}>
                                <KeyOutlined style={{ color: token.colorTextDescription }} />
                                <Text style={{ color: token.colorTextDescription, fontSize: 13 }}>无法扫描二维码？</Text>
                            </Space>
                            <Button
                                type="link"
                                size="small"
                                style={{ padding: 0, height: 'auto' }}
                                icon={showSecret ? <EyeInvisibleOutlined /> : <EyeOutlined />}
                                onClick={() => setShowSecret(!showSecret)}
                            >
                                {showSecret ? '隐藏密钥' : '显示密钥'}
                            </Button>
                        </Flex>
                        {showSecret && (
                            <div style={{ marginTop: 8, width: '100%' }}>
                                <Paragraph
                                    copyable={{ text: secret }}
                                    style={{
                                        marginBottom: 0,
                                        fontSize: 13,
                                        background: token.colorBgContainer,
                                        padding: '6px 10px',
                                        borderRadius: token.borderRadiusSM,
                                        display: 'flex',
                                        justifyContent: 'space-between',
                                        alignItems: 'center',
                                        width: '100%',
                                        fontFamily: 'SFMono-Regular, Consolas, "Liberation Mono", Menlo, Courier, monospace',
                                        border: `1px solid ${token.colorBorderSecondary}`
                                    }}
                                >
                                    {/* 👇 核心修改：在 span 上增加 letterSpacing 控制字母间距 */}
                                    <span
                                        style={{
                                            letterSpacing: '1.5px',
                                            userSelect: 'all'
                                        }}
                                    >
                                        {secret || '正在获取密钥...'}
                                    </span>
                                </Paragraph>
                            </div>
                        )}
                    </Flex>

                    <Flex vertical gap={8}>
                        <Text strong>请输入 6 位动态验证码：</Text>
                        <Input.OTP
                            length={6}
                            value={totpCode}
                            onChange={setTotpCode}
                            style={{ justifyContent: 'space-between' }}
                            disabled={initTotpLoading || activateTotpLoading}
                        />
                    </Flex>
                </Flex>
            </Modal>
        </>
    );
};

export default TotpSecurityItem;