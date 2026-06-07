import React, { useState } from 'react';
import { Button, Typography, Flex, theme, Modal, Alert, Space, App, Badge } from 'antd';
import { CopyOutlined, DownloadOutlined, FileProtectOutlined, SafetyCertificateOutlined } from '@ant-design/icons';
import SecurityStepVerify from './verifiers/SecurityStepVerify';
import { useRequest } from 'ahooks';
import { refreshBackupCode } from '../../../services/AccountService';

const { Text } = Typography;

const BackupCodeSecurityItem = ({ context, refresh }) => {
    const { token } = theme.useToken();

    const { message } = App.useApp()

    const { totpEnabled, backupCodeGenerated, remainingBackupCodeCount = 0 } = context || {};

    const [backupCode, setBackupCode] = useState([])

    const [isCodesDisplayModalOpen, setIsCodesDisplayModalOpen] = useState(false)

    const [isBackupCodeModalOpen, setIsBackupCodeModalOpen] = useState(false)

    const { runAsync: refreshBackupCodeAsync, loading: refreshBackupCodeLoading } = useRequest(refreshBackupCode, {
        manual: true
    });

    const handleRecoveryAction = async () => {
        setIsBackupCodeModalOpen(true)
    };

    // 图标外圈样式
    const iconWrapperStyle = {
        padding: 12,
        background: token.colorFillAlter,
        borderRadius: '50%',
        display: 'flex',
        opacity: totpEnabled ? 1 : 0.6
    };

    const handleConfirm = async (ticket) => {
        const res = await refreshBackupCodeAsync({
            ticket: ticket
        })
        setIsBackupCodeModalOpen(false)
        setBackupCode(res.backupCodes)
        setIsCodesDisplayModalOpen(true)
    }

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
                        <FileProtectOutlined style={{ color: totpEnabled ? token.colorPrimary : token.colorTextDisabled, fontSize: 20 }} />
                    </div>
                    <Flex vertical gap={4}>
                        <Space size={8}>
                            <Text strong style={{ fontSize: 16, color: totpEnabled ? token.colorText : token.colorTextDisabled }}>
                                备份码
                            </Text>
                            {totpEnabled && backupCodeGenerated && (
                                <Badge
                                    count={`剩余 ${remainingBackupCodeCount} 个`}
                                    style={{
                                        backgroundColor: remainingBackupCodeCount <= 2 ? token.colorError : token.colorInfoBg,
                                        color: remainingBackupCodeCount <= 2 ? token.colorTextLightSolid : token.colorPrimary,
                                    }}
                                />
                            )}
                        </Space>

                        <Text style={{ color: totpEnabled ? token.colorTextDescription : token.colorTextDisabled, fontSize: 14, lineHeight: '22px' }}>
                            {backupCodeGenerated
                                ? '已生成。当您丢失移动设备或无法使用身份验证器时，这是您重新访问账号的唯一方式。'
                                : (totpEnabled
                                    ? '未生成。为了防范身份验证器丢失风险，建议立即生成安全恢复码。'
                                    : '开启 TOTP 认证后即可生成并查看恢复码。')}
                        </Text>
                    </Flex>
                </Flex>

                <Flex align="center">
                    {backupCodeGenerated && (
                        <Button
                            disabled={!totpEnabled}
                            type={!backupCodeGenerated && totpEnabled ? "primary" : "default"}
                            onClick={handleRecoveryAction}
                        >
                            重置备份码
                        </Button>
                    )}
                </Flex>
            </Flex>

            {/* 声明式查看/生成恢复码 Modal */}
            <Modal
                title={
                    <Space>
                        <FileProtectOutlined style={{ color: token.colorSuccess }} />
                        <span>重置备份码</span>
                    </Space>
                }
                open={isBackupCodeModalOpen}
                onCancel={() => setIsBackupCodeModalOpen(false)}
                footer={null}
                width={460}
                centered
                destroyOnHidden
            >
                <SecurityStepVerify
                    scene="GENERATE_TOTP_BACKUP_CODE"
                    captchaScene='GENERATE_TOTP_BACKUP_CODE'
                    context={context}
                    stepTitle="确认生成"
                    confirmText="确认生成"
                    confirmDanger={true}
                    confirmLoading={refreshBackupCodeLoading}
                    onCancel={() => setIsBackupCodeModalOpen(false)}
                    onConfirm={handleConfirm}
                >
                    <Alert
                        style={{ marginBottom: 16 }}
                        message="重新生成备份码将导致您之前保存的旧备份码全部立即失效！请确保您能够妥善保管新生成的代码。"
                        type="warning"
                        showIcon
                    />
                </SecurityStepVerify>
            </Modal>

            <BackupCodesDisplayModal
                open={isCodesDisplayModalOpen}
                codes={backupCode}
                onClose={() => {
                    setIsCodesDisplayModalOpen(false)
                    refresh?.()
                }}
            />
        </>
    );
};

export default BackupCodeSecurityItem;



export const BackupCodesDisplayModal = ({
    open = false,
    codes = [],
    onClose,
}) => {

    const { token } = theme.useToken();
    const { message, modal } = App.useApp();

    const [hasCopied, setHasCopied] = useState(false);
    const [hasDownloaded, setHasDownloaded] = useState(false);

    const handleCopy = async () => {
        if (!codes?.length) return;
        const textToCopy = codes.join('\n');
        try {
            await navigator.clipboard.writeText(textToCopy);
            message.success('备份码已成功复制到剪贴板！');
            setHasCopied(true);
        } catch (err) {
            message.error('复制失败，请尝试手动截屏或选择下载文本。');
        }
    };

    // 2. 一键下载文本文件
    const handleDownload = () => {
        if (!codes?.length) return;
        try {
            const fileContent = `=== 安全备份码 (Backup Codes) ===\n生成时间: ${new Date().toLocaleString()}\n\n${codes.join('\n')}\n\n*请妥善保管，每个备份码仅可使用一次。`;
            const blob = new Blob([fileContent], { type: 'text/plain;charset=utf-8' });
            const url = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = `backup-codes-${new Date().toISOString().slice(0, 10)}.txt`;
            link.click();
            URL.revokeObjectURL(url);

            message.success('备份码文本下载成功！');
            setHasDownloaded(true);
        } catch (err) {
            message.error('文件下载失败，请尝试复制。');
        }
    };

    // 3. 通关安全确认
    const handleDoneClick = () => {
        // 如果用户极其粗心，既没复制也没下载，直接拦截
        if (!hasCopied && !hasDownloaded) {
            modal.confirm({
                title: '安全防丢确认',
                icon: <SafetyCertificateOutlined style={{ color: token.colorError }} />,
                content: '系统检测到您尚未复制或下载备份码。一旦关闭此窗口，这些备份码将【永远无法再次查看】！确定已经手动抄录好了吗？',
                okText: '确定关闭',
                cancelText: '去备份',
                okButtonProps: { danger: true },
                onOk: () => {
                    executeClose();
                }
            });
        } else {
            executeClose();
        }
    };

    // 彻底关闭并重置内部计数
    const executeClose = () => {
        setHasCopied(false);
        setHasDownloaded(false);
        if (onClose) onClose();
    };


    const codeGridStyle = {
        display: 'grid',
        gridTemplateColumns: '1fr 1fr',
        gap: '12px 24px',
        padding: '20px',
        background: token.colorFillAlter,
        borderRadius: token.borderRadiusLG,
        border: `1px solid ${token.colorBorderSecondary}`,
        fontFamily: 'SFMono-Regular, Consolas, "Liberation Mono", Menlo, monospace',
    };

    return (
        <Modal
            title={
                <Space>
                    <SafetyCertificateOutlined style={{ color: token.colorWarning }} />
                    <span>保管您的安全备份码</span>
                </Space>
            }
            open={open}
            closable={false}      // 🔒 禁右上角 X
            maskClosable={false}  // 🔒 禁阴影点击
            keyboard={false}      // 🔒 禁 Esc 键
            width={460}
            destroyOnHidden        // 💡 关闭时彻底销毁 dom，防止数据常驻内存泄漏
            footer={[
                <Button key="copy" icon={<CopyOutlined />} onClick={handleCopy}>
                    复制
                </Button>,
                <Button key="download" icon={<DownloadOutlined />} onClick={handleDownload}>
                    下载文本
                </Button>,
                <Button key="done" type="primary" onClick={handleDoneClick}>
                    我已妥善保存
                </Button>
            ]}
        >
            <Flex vertical gap={16} style={{ marginTop: 16 }}>
                <Alert
                    message="这是您的安全备份码，它们只会显示这一次。请立即复制或下载妥善保管。一旦丢失且无法访问身份验证器，您将永远失去账号访问权！"
                    type="warning"
                    showIcon
                />

                <div style={codeGridStyle}>
                    {codes?.map((code, index) => (
                        <Flex justify="space-between" align="center" key={index}>
                            <Text type="secondary" style={{ marginRight: 12, userSelect: 'none', fontSize: 13 }}>
                                {String(index + 1).padStart(2, '0')}.
                            </Text>
                            <Text strong style={{ flex: 1, letterSpacing: '0.5px', fontSize: 14 }}>
                                {code}
                            </Text>
                        </Flex>
                    ))}
                </div>
            </Flex>
        </Modal>
    );

}