import React, { useState } from 'react';
import { Button, Typography, Flex, theme, Modal, Alert, Space } from 'antd';
import { FileProtectOutlined } from '@ant-design/icons';

const { Text } = Typography;

const RecoverySecurityItem = ({ context, refresh }) => {
    const { token } = theme.useToken();
    
    const { mfaEnabled, recoveryCodeGenerated } = context || {};

    // ==================== 状态控制 ====================
    const [isRecoveryModalOpen, setIsRecoveryModalOpen] = useState(false);
    const [recoveryLoading, setRecoveryLoading] = useState(false);

    // ==================== 事件处理 ====================
    const handleRecoveryAction = async () => {
        if (recoveryCodeGenerated) {
            // TODO: 调用查看恢复码接口
            setIsRecoveryModalOpen(true);
        } else {
            // TODO: 调用生成恢复码接口
            setIsRecoveryModalOpen(true);
        }
    };

    // 图标外圈样式
    const iconWrapperStyle = {
        padding: 12,
        background: token.colorFillAlter,
        borderRadius: '50%',
        display: 'flex',
        opacity: mfaEnabled ? 1 : 0.6
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

            {/* 声明式查看/生成恢复码 Modal */}
            <Modal
                title={
                    <Space>
                        <FileProtectOutlined style={{ color: token.colorSuccess }} />
                        <span>安全恢复码</span>
                    </Space>
                }
                open={isRecoveryModalOpen}
                onCancel={() => setIsRecoveryModalOpen(false)}
                footer={[
                    <Button key="close" type="primary" onClick={() => setIsRecoveryModalOpen(false)}>已妥善保存</Button>
                ]}
                width={400}
                destroyOnHidden
            >
                <Flex vertical gap={16} style={{ marginTop: 16 }}>
                    <Alert message="请将恢复码保存在安全的地方（如密码管理器）。每个恢复码只能使用一次。" type="warning" showIcon />
                    {/* TODO: 展示具体恢复码的 UI */}
                </Flex>
            </Modal>
        </>
    );
};

export default RecoverySecurityItem;