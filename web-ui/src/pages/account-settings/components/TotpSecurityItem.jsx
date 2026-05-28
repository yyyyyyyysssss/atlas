import React, { useState } from 'react';
import { Button, Typography, Flex, App, Alert, theme, Modal, QRCode, Input, Space, message } from 'antd';
import { MobileOutlined, KeyOutlined, EyeOutlined, EyeInvisibleOutlined, CopyOutlined, DownloadOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import { useRequest } from 'ahooks';
import { activateTotp, initTotp } from '../../../services/AccountService';

const { Text, Paragraph } = Typography;

const TotpSecurityItem = ({ context, refresh }) => {
    const { token } = theme.useToken();
    const { message: antdMessage } = App.useApp();

    const { totpEnabled } = context || {};

    // ==================== 状态控制 ====================
    const [isBindModalOpen, setIsBindModalOpen] = useState(false);
    const [showSecret, setShowSecret] = useState(false);
    const [totpCode, setTotpCode] = useState('');
    
    // 控制 Modal 内部的步骤阶段: 'BIND' (扫描绑定) | 'BACKUP' (强制保存备份码)
    const [modalStage, setModalStage] = useState('BIND');
    // 暂存后端激活成功后返回的明文备份码
    const [receivedBackupCodes, setReceivedBackupCodes] = useState([]);

    // 初始化 TOTP 密钥
    const {
        data: initData,
        loading: initTotpLoading,
        run: runInitTotp
    } = useRequest(initTotp, {
        manual: true,
        onSuccess: () => {
            setTotpCode('');
            setShowSecret(false);
            setModalStage('BIND'); // 每次打开/重试确保是绑定阶段
        },
        onError: () => {
            antdMessage.error('获取双重认证密钥失败，请重试');
        }
    });

    // 激活 TOTP 接口
    const {
        loading: activateTotpLoading,
        runAsync: activateTotpAsync
    } = useRequest(activateTotp, { manual: true });

    const { secret, otpAuthUrl } = initData || {};

    // ==================== 事件处理 ====================
    const handleToggleMfa = () => {
        if (totpEnabled) {
            // TODO: 解绑流程逻辑
        } else {
            setIsBindModalOpen(true);
            runInitTotp();
        }
    };

    const handleBindSubmit = async () => {
        if (totpCode.length !== 6) {
            antdMessage.warning('请输入完整的 6 位验证码');
            return;
        }
        try {
            // 提交激活，后端返回：{ backupCodes: ['xxxx-xxxx', ...] }
            const res = await activateTotpAsync({ code: totpCode });
            
            antdMessage.success('MFA 身份验证器绑定成功！');
            
            if (res && res.backupCodes && res.backupCodes.length > 0) {
                // 原位切入第二个阶段：展示备份码
                setReceivedBackupCodes(res.backupCodes);
                setModalStage('BACKUP');
            } else {
                // 极端防御：如果后端没吐备份码，直接关闭
                setIsBindModalOpen(false);
                refresh?.();
            }
        } catch (err) {
            // 验证失败时保持打开状态，错误由全局/ahooks处理
        }
    };

    // 备份码下载与复制逻辑
    const handleCopyBackupCodes = () => {
        const textToCopy = receivedBackupCodes.join('\n');
        navigator.clipboard.writeText(textToCopy).then(() => {
            antdMessage.success('已复制到剪贴板');
        });
    };

    const handleDownloadBackupCodes = () => {
        const textContent = `Atlas 安全备份码\n生成时间: ${new Date().toLocaleString()}\n\n${receivedBackupCodes.join('\n')}\n\n注意：每个备份码只能使用一次。`;
        const blob = new Blob([textContent], { type: 'text/plain;charset=utf-8' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = 'atlas-backup-codes.txt';
        link.click();
        URL.revokeObjectURL(url);
    };

    const handleAllSavedDone = () => {
        setIsBindModalOpen(false);
        setReceivedBackupCodes([]);
        setModalStage('BIND');
        refresh?.(); // 彻底完成后刷新外层用户凭证上下文
    };

    // ==================== 样式计算 ====================
    const iconWrapperStyle = {
        padding: 12,
        background: token.colorFillAlter,
        borderRadius: '50%',
        display: 'flex'
    };

    const codeGridStyle = {
        display: 'grid',
        gridTemplateColumns: '1fr 1fr', 
        gap: '8px 24px',
        padding: '16px',
        background: token.colorBgContainer,
        border: `1px solid ${token.colorBorder}`,
        borderRadius: token.borderRadiusLG,
        fontFamily: 'monospace, Consolas, "SF Mono"',
        fontSize: '15px',
        letterSpacing: '0.5px'
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
                        <MobileOutlined style={{ color: totpEnabled ? token.colorSuccess : token.colorWarning, fontSize: 20 }} />
                    </div>
                    <Flex vertical gap={4}>
                        <Text strong style={{ fontSize: 16 }}>身份验证器应用程序</Text>
                        <Text style={{ color: token.colorTextDescription, fontSize: 14, lineHeight: '22px' }}>
                            {totpEnabled
                                ? '已开启。登录时需要输入身份验证器 (如 Google Authenticator) 生成的 6 位动态码。'
                                : '未开启。极大地提高您的账号安全性，防止密码泄露。'}
                        </Text>
                    </Flex>
                </Flex>

                <Flex align="center">
                    <Button
                        danger={totpEnabled}
                        type={totpEnabled ? 'default' : 'primary'}
                        onClick={handleToggleMfa}
                    >
                        {totpEnabled ? '关闭认证' : '立即开启'}
                    </Button>
                </Flex>
            </Flex>
            <Modal
                title={
                    <Space>
                        {modalStage === 'BIND' ? (
                            <>
                                <MobileOutlined style={{ color: token.colorPrimary }} />
                                <span>绑定身份验证器</span>
                            </>
                        ) : (
                            <>
                                <SafetyCertificateOutlined style={{ color: token.colorWarning }} />
                                <span>保管您的安全备份码</span>
                            </>
                        )}
                    </Space>
                }
                open={isBindModalOpen}
                closable={modalStage === 'BIND'} 
                maskClosable={modalStage === 'BIND'}
                onCancel={() => setIsBindModalOpen(false)}
                width={460}
                destroyOnHidden
                footer={
                    modalStage === 'BIND' ? [
                        <Button key="cancel" onClick={() => setIsBindModalOpen(false)}>取消</Button>,
                        <Button key="submit" type="primary" loading={activateTotpLoading} onClick={handleBindSubmit}>激活并开启</Button>
                    ] : [
                        <Button key="copy" icon={<CopyOutlined />} onClick={handleCopyBackupCodes}>复制</Button>,
                        <Button key="download" icon={<DownloadOutlined />} onClick={handleDownloadBackupCodes}>下载文本</Button>,
                        <Button key="done" type="primary" onClick={handleAllSavedDone}>我已妥善保存</Button>
                    ]
                }
            >
                {modalStage === 'BIND' ? (
                    /* ==================== 阶段一：扫码绑定 UI ==================== */
                    <Flex vertical gap={16} style={{ marginTop: 16 }}>
                        <Alert message="请使用 Google Authenticator 或双重认证 App 扫描下方二维码" type="info" showIcon />

                        <Flex justify="center" align="center" style={{ padding: '16px 0', height: 212, width: '100%', position: 'relative' }}>
                            {(otpAuthUrl || initTotpLoading) ? (
                                <QRCode
                                    value={otpAuthUrl || 'loading'}
                                    size={180}
                                    status={initTotpLoading ? 'loading' : (otpAuthUrl ? 'active' : 'expired')}
                                    onRefresh={runInitTotp}
                                />
                            ) : (
                                <div style={{ width: 180, height: 180, background: token.colorFillAlter, borderRadius: token.borderRadiusLG }} />
                            )}
                        </Flex>

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
                                            marginBottom: 0, fontSize: 13, background: token.colorBgContainer,
                                            padding: '6px 10px', borderRadius: token.borderRadiusSM, display: 'flex',
                                            justifyContent: 'space-between', alignItems: 'center', width: '100%',
                                            fontFamily: 'SFMono-Regular, Consolas, monospace',
                                            border: `1px solid ${token.colorBorderSecondary}`
                                        }}
                                    >
                                        <span style={{ letterSpacing: '1.5px', userSelect: 'all' }}>
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
                                formatter={(str) => str.replace(/[^0-9]/g, '')}
                                onChange={setTotpCode}
                                style={{ justifyContent: 'space-between' }}
                                disabled={initTotpLoading || activateTotpLoading}
                            />
                        </Flex>
                    </Flex>
                ) : (
                    /* ==================== 阶段二：新生成的备份码展示 UI（GitHub 风格） ==================== */
                    <Flex vertical gap={16} style={{ marginTop: 16 }}>
                        <Alert 
                            message="双重认证开启成功！这是您的安全备份码，它们只会显示这一次。请立即复制或下载妥善保管。一旦丢失且无法访问身份验证器，您将永远失去账号访问权！" 
                            type="warning" 
                            showIcon 
                        />
                        
                        {/* 2x8 紧凑型等宽字符矩阵 */}
                        <div style={codeGridStyle}>
                            {receivedBackupCodes.map((code, index) => (
                                <Flex justify="space-between" key={index}>
                                    <Text type="secondary" style={{ marginRight: 8, userSelect: 'none' }}>
                                        {String(index + 1).padStart(2, '0')}.
                                    </Text>
                                    <Text strong style={{ flex: 1 }}>{code}</Text>
                                </Flex>
                            ))}
                        </div>
                    </Flex>
                )}
            </Modal>
        </>
    );
};

export default TotpSecurityItem;