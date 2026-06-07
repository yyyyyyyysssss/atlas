import React, { useRef, useState } from 'react';
import { Button, Typography, Flex, App, Alert, theme, Modal, QRCode, Input, Space } from 'antd';
import { MobileOutlined, KeyOutlined, EyeOutlined, EyeInvisibleOutlined } from '@ant-design/icons';
import { useRequest } from 'ahooks';
import { activateTotp, initTotp, unbindTotp } from '../../../services/AccountService';
import { BackupCodesDisplayModal } from './BackupCodeSecurityItem';
import SecurityStepVerify from './verifiers/SecurityStepVerify';

const { Text, Paragraph, Title } = Typography;

const TotpSecurityItem = ({ context, refresh }) => {
    const { token } = theme.useToken();
    const { message } = App.useApp();

    const { totpEnabled } = context || {};

    const [isBindModalOpen, setIsBindModalOpen] = useState(false);
    const [showSecret, setShowSecret] = useState(false);
    const [totpCode, setTotpCode] = useState('');
    
    // 🚀 新增：保存第一步校验通过后的凭证 ticket
    const [currentTicket, setCurrentTicket] = useState('');

    const [isBackupModalOpen, setIsBackupModalOpen] = useState(false);
    const [receivedBackupCodes, setReceivedBackupCodes] = useState([]);

    const [isUnbindModalOpen, setIsUnbindModalOpen] = useState(false);

    const otpRef = useRef(null);

    // 初始化 TOTP 密钥 (🚀 注意：入参需要带上 ticket)
    const {
        data: initData,
        loading: initTotpLoading,
        run: runInitTotp
    } = useRequest(initTotp, {
        manual: true,
        onSuccess: () => {
            setTotpCode('');
            setShowSecret(false);
            // 自动聚焦到第一个输入框
            requestAnimationFrame(() => {
                otpRef.current?.focus();
            });
        },
        onError: () => {
            message.error('获取双重认证密钥失败，请重试');
        }
    });

    // 激活 TOTP 接口
    const { loading: activateTotpLoading, runAsync: activateTotpAsync } = useRequest(activateTotp, { manual: true });

    // 解绑 TOTP 接口
    const { loading: unbindTotpLoading, runAsync: unbindTotpTotpAsync } = useRequest(unbindTotp, { manual: true });

    const { secret, otpAuthUrl } = initData || {};

    const handleToggleMfa = () => {
        if (totpEnabled) {
            setIsUnbindModalOpen(true);
        } else {
            // 🚀 开启时，不再直接调用 runInitTotp，而是先打开包含前置认证的 Modal
            setIsBindModalOpen(true);
        }
    };

    // 🚀 核心修改：第一步安全验证通关后的回调
    const handleBindVerifySuccess = (ticket) => {
        setCurrentTicket(ticket)
        runInitTotp({ ticket })
    };

    // 🚀 核心修改：第二步点击“激活并开启”时的终极确认回调
    const handleBindConfirm = async () => {
        if (totpCode.length !== 6) {
            message.warning('请输入完整的 6 位验证码');
            return;
        }
        try {
            // 激活时带上验证码和前置认证的 ticket
            const res = await activateTotpAsync({ 
                code: totpCode,
                ticket: currentTicket 
            });

            message.success('MFA 身份验证器绑定成功！');
            handleCancelBind();

            if (res && res.backupCodes && res.backupCodes.length > 0) {
                setReceivedBackupCodes(res.backupCodes);
                setIsBackupModalOpen(true);
            } else {
                refresh?.();
            }
        } catch (error) {
            if (error?.message) {
                message.error(error.message);
            }
            // 失败时回退一位并重新聚焦
            const nextValue = totpCode.slice(0, 5);
            setTotpCode(nextValue);
            requestAnimationFrame(() => {
                const inputs = otpRef.current?.nativeElement?.querySelectorAll('input');
                if (inputs?.[5]) {
                    inputs[5].focus();
                }
            });
        }
    };

    // 关闭绑定模态框，清理状态
    const handleCancelBind = () => {
        setIsBindModalOpen(false);
        setCurrentTicket('');
        setTotpCode('');
    };

    const handleBackupModalClose = () => {
        setIsBackupModalOpen(false);
        setReceivedBackupCodes([]);
        refresh?.();
    };

    const handleCancelUnbind = () => {
        setIsUnbindModalOpen(false);
    };

    const handleUnbindConfirmDestroy = async (ticket) => {
        await unbindTotpTotpAsync({ ticket: ticket });
        handleCancelUnbind();
        refresh?.();
    };

    // ==================== 样式计算 ====================
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
                        <MobileOutlined style={{ color: totpEnabled ? token.colorPrimary : token.colorWarning, fontSize: 20 }} />
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

            {/* 🚀 改造后的绑定 Modal */}
            <Modal
                title={
                    <div style={{ marginBottom: 16 }}>
                        <Title level={4} style={{ margin: 0, fontWeight: 600 }}>绑定双因子身份验证</Title>
                    </div>
                }
                open={isBindModalOpen}
                onCancel={handleCancelBind}
                width={480}
                centered
                destroyOnHidden
                footer={null} // 托管给内部的 SecurityStepVerify 驱动
            >
                <SecurityStepVerify
                    scene="BIND_TOTP"
                    captchaScene="BIND_TOTP"
                    context={context}
                    stepTitle="初始化验证令牌"
                    confirmText="激活并开启"
                    confirmLoading={activateTotpLoading}
                    onCancel={handleCancelBind}
                    onVerifySuccess={handleBindVerifySuccess} // 👈 第一步通关：拿 ticket 异步初始化二维码
                    onConfirm={handleBindConfirm}             // 👈 第二步确认：提交 code + ticket
                >
                    {/* 🚀 以下作为第二步的 Children 展示，此时 initData 应该已经由于 onVerifySuccess 触发而拉取成功 */}
                    <Flex vertical gap={16}>
                        <Alert message="请使用 Google Authenticator 或双重认证 App 扫描下方二维码" type="info" showIcon />

                        <Flex justify="center" align="center" style={{ padding: '16px 0', height: 212, width: '100%', position: 'relative' }}>
                            {(otpAuthUrl || initTotpLoading) ? (
                                <QRCode
                                    value={otpAuthUrl || 'loading'}
                                    size={180}
                                    status={initTotpLoading ? 'loading' : (otpAuthUrl ? 'active' : 'expired')}
                                    onRefresh={() => runInitTotp({ ticket: currentTicket })} // 刷新时记得带上 ticket
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
                                ref={otpRef}
                                length={6}
                                value={totpCode}
                                formatter={(str) => str.replace(/[^0-9]/g, '')}
                                onChange={(val) => {
                                    setTotpCode(val);
                                    if (val.length === 6) {
                                        // 当用户输满 6 位时，不直接提交请求，而是让用户感知到点击底部的“激活并开启”
                                        // 或者你也可以在此处直接触发 handleBindConfirm() 实现自动提交
                                    }
                                }}
                                style={{ justifyContent: 'space-between' }}
                                disabled={initTotpLoading || activateTotpLoading}
                            />
                        </Flex>
                    </Flex>
                </SecurityStepVerify>
            </Modal>

            {/* 解绑 Modal 保持原样... */}
            <Modal
                title={
                    <div style={{ marginBottom: 16 }}>
                        <Title level={4} style={{ margin: 0, fontWeight: 600 }}>关闭双因子身份验证</Title>
                    </div>
                }
                open={isUnbindModalOpen}
                onCancel={handleCancelUnbind}
                width={460}
                centered
                destroyOnHidden
                footer={null}
            >
                <SecurityStepVerify
                    scene="UNBIND_TOTP"
                    captchaScene='UNBIND_TOTP'
                    context={context}
                    stepTitle='风险确认'
                    confirmText="确定解绑并销毁凭证"
                    confirmDanger={true}
                    confirmLoading={unbindTotpLoading}
                    onCancel={handleCancelUnbind}
                    onConfirm={handleUnbindConfirmDestroy}
                >
                    <Alert
                        style={{ marginBottom: 16 }}
                        message="账户安全防护将减弱"
                        description={
                            <Paragraph style={{ margin: 0, fontSize: '14px', lineHeight: '1.6' }}>
                                关闭双因子认证后，登录将不再要求输入动态验证码。
                                <Text strong>
                                    您之前保存的所有离线备份码也会立即失效并被永久销毁。
                                </Text>
                                请确保您已启用通行密钥 (Passkey) 或安全的密码策略。
                            </Paragraph>
                        }
                        type="warning"
                        showIcon
                    />
                </SecurityStepVerify>
            </Modal>

            {isBackupModalOpen && (
                <BackupCodesDisplayModal
                    open={isBackupModalOpen}
                    codes={receivedBackupCodes}
                    onClose={handleBackupModalClose}
                />
            )}
        </>
    );
};

export default TotpSecurityItem;