import React, { useRef, useState } from 'react';
import { Button, Typography, Flex, App, theme, Space, List, Modal, Alert } from 'antd';
import { Fingerprint, Trash2, Plus, ChevronDown, ShieldAlert } from 'lucide-react';
import { useRequest } from 'ahooks';
import {
    webauthnRegisterOptions,
    webauthnRegister,
    unbindWebauthn,
    verifyWebauthn,
} from '../../../services/AccountService';
import { AnimatePresence, motion } from 'framer-motion';
import SecurityStepVerify from './verifiers/SecurityStepVerify';

const { Text, Title } = Typography;

// 精准识别当前设备信息
const getDeviceLabel = () => {
    const ua = navigator.userAgent;
    let os = "未知设备";
    let browser = "浏览器";

    if (ua.indexOf("Win") !== -1) os = "Windows";
    else if (ua.indexOf("Mac") !== -1) os = "Mac";
    else if (ua.indexOf("X11") !== -1) os = "Linux";
    else if (ua.indexOf("Android") !== -1) os = "Android";
    else if (ua.indexOf("iPhone") !== -1) os = "iPhone/iPad";

    if (ua.indexOf("Edg") !== -1) browser = "Edge";
    else if (ua.indexOf("Chrome") !== -1 && ua.indexOf("Safari") !== -1) browser = "Chrome";
    else if (ua.indexOf("Firefox") !== -1) browser = "Firefox";
    else if (ua.indexOf("Safari") !== -1 && ua.indexOf("Chrome") === -1) browser = "Safari";

    return `${os} (${browser})`;
};

const base64urlToArrayBuffer = (base64url) => {
    const padding = '='.repeat((4 - base64url.length % 4) % 4);
    const base64 = (base64url + padding).replace(/-/g, '+').replace(/_/g, '/');
    const rawData = window.atob(base64);
    const buffer = new Uint8Array(rawData.length);
    for (let i = 0; i < rawData.length; i++) {
        buffer[i] = rawData.charCodeAt(i);
    }
    return buffer.buffer;
};

const PasskeyItem = ({ context, refresh }) => {
    const { token } = theme.useToken();
    const { message } = App.useApp();

    // 状态控制
    const [isExpanded, setIsExpanded] = useState(false);
    const [modalMode, setModalMode] = useState(null); // 'bind' | 'unbind' | null
    const [targetItem, setTargetItem] = useState(null); // 当前准备解绑的设备

    const { passkeys = [] } = context || {};
    const isBound = passkeys && passkeys.length > 0;

    // 检测当前环境是否支持 WebAuthn
    const isWebAuthnSupported =
        window.PublicKeyCredential !== undefined &&
        typeof window.PublicKeyCredential === 'function' &&
        navigator.credentials !== undefined;

    // API 请求控制
    const { runAsync: getOptionsAsync, loading: getOptionsLoading } = useRequest(webauthnRegisterOptions, { manual: true });
    const { runAsync: webauthnRegisterAsync, loading: webauthnRegisterLoading } = useRequest(webauthnRegister, { manual: true });
    const { runAsync: unbindWebauthnAsync, loading: unbindLoading } = useRequest(unbindWebauthn, { manual: true });

    const getIconColor = () => {
        if (isBound) return token.colorPrimary;
        if (isWebAuthnSupported) return token.colorPrimary;
        return token.colorTextDisabled;
    };

    // --- 🚀 核心：风控验证成功后，触发的真正 WebAuthn 硬件绑定流程 ---
    const handleBindSubmit = async (ticket) => {
        try {
            // 1. 将前置身份认证产生的 ticket 传给后端，换取初始化创建凭证参数
            const { webauthnId, publicKey } = await getOptionsAsync({ ticket });

            message.loading({ content: '安全校验通过，正在唤起设备凭证...', key: 'passkey_action' });

            // 2. 跨浏览器兼容处理：转换 Options 
            let nativeOptions;
            if (typeof PublicKeyCredential.parseCreationOptionsFromJSON === 'function') {
                nativeOptions = PublicKeyCredential.parseCreationOptionsFromJSON(publicKey);
            } else {
                nativeOptions = {
                    ...publicKey,
                    challenge: base64urlToArrayBuffer(publicKey.challenge),
                    user: { ...publicKey.user, id: base64urlToArrayBuffer(publicKey.user.id) },
                    excludeCredentials: publicKey.excludeCredentials?.map(cred => ({
                        ...cred, id: base64urlToArrayBuffer(cred.id)
                    }))
                };
            }

            // 3. 唤起生物识别（指纹、面容或安全密钥）
            const credential = await navigator.credentials.create({ publicKey: nativeOptions });

            message.loading({ content: '正在提交服务器验证...', key: 'passkey_action' });
            const credentialJson = credential.toJSON();
            const label = getDeviceLabel();

            // 4. 将硬件签名凭证连同 ticket 一起注册到后端
            await webauthnRegisterAsync(webauthnId, {
                publicKey: {
                    label: label,
                    credential: credentialJson
                },
                ticket: ticket // 保持事务完整性
            });

            message.success({ content: '通行密钥绑定成功！', key: 'passkey_action' });
            handleCloseModal();
            refresh?.();
            setIsExpanded(true); // 绑定成功后自动展开列表

        } catch (error) {
            console.error('Passkey 绑定失败:', error);
            if (error instanceof DOMException) {
                if (error.name === 'NotAllowedError') {
                    message.warning({ content: '操作已取消或验证超时', key: 'passkey_action' });
                } else if (error.name === 'InvalidStateError' || error.name === 'ConstraintError') {
                    message.info({ content: '当前设备已绑定过该账号，无需重复绑定', key: 'passkey_action', duration: 4 });
                } else {
                    message.error({ content: `设备交互失败: ${error.message}`, key: 'passkey_action' });
                }
            } else {
                message.error({
                    content: error?.response?.data?.message || error?.message || '服务器验证失败，请稍后重试',
                    key: 'passkey_action'
                });
            }
            // 如果在唤起硬件或提交时失败，让 Modal 保持打开，用户可重试
        }
    };

    // --- 💡 解绑核心流 ---
    const handleUnbindSubmit = async (ticket) => {
        if (!targetItem || !ticket) return;
        await unbindWebauthnAsync({
            credentialId: targetItem.credentialId,
            ticket: ticket
        });
        message.success(`已成功移除密钥凭证 [${targetItem.label || '未知设备'}]`);
        handleCloseModal();
        refresh?.();
    };

    // --- 弹窗显隐控制 ---
    const handleOpenBindModal = (e) => {
        if (e) e.stopPropagation();
        setModalMode('bind');
    };

    const handleOpenUnbindModal = (item, e) => {
        if (e) e.stopPropagation();
        setTargetItem(item);
        setModalMode('unbind');
    };

    const handleCloseModal = () => {
        setModalMode(null);
        setTargetItem(null);
    };

    const isBindLoading = getOptionsLoading || webauthnRegisterLoading;

    return (
        <Flex
            vertical
            style={{
                borderBottom: `1px solid ${token.colorBorderSecondary}`,
                padding: '20px 0'
            }}
        >
            {/* 主控制行 */}
            <Flex
                justify="space-between"
                align="center"
                onClick={() => isBound && setIsExpanded(!isExpanded)}
                style={{
                    cursor: isBound ? 'pointer' : 'default',
                    position: 'relative'
                }}
            >
                <Flex gap={16} align="flex-start" style={{ flex: 1 }}>
                    <div style={{
                        padding: 12,
                        background: token.colorFillAlter,
                        borderRadius: '50%',
                        display: 'flex',
                        zIndex: 2
                    }}>
                        <Fingerprint style={{ width: 20, height: 20, color: getIconColor() }} />
                    </div>
                    <Flex vertical gap={4}>
                        <Text strong style={{ fontSize: 16 }}>通行密钥 (Passkey)</Text>
                        <Text style={{ color: token.colorTextDescription, fontSize: 14, lineHeight: '22px' }}>
                            {!isWebAuthnSupported
                                ? '您的当前设备或浏览器版本过低，不支持通行密钥功能。'
                                : (isBound
                                    ? `已启用。当前已绑定 ${passkeys.length} 个密钥凭证。`
                                    : '未绑定。绑定后可实现指纹、面容等无密码安全快速登录。'
                                )}
                        </Text>
                    </Flex>
                </Flex>

                <Flex align="center" gap={12} onClick={(e) => e.stopPropagation()}>
                    <Button
                        type={isBound ? "default" : "primary"}
                        disabled={!isWebAuthnSupported}
                        onClick={handleOpenBindModal}
                    >
                        {isBound ? '添加设备' : '立即绑定'}
                    </Button>

                    {isBound && (
                        <div
                            onClick={() => setIsExpanded(!isExpanded)}
                            style={{ display: 'flex', alignItems: 'center', padding: '4px', cursor: 'pointer' }}
                        >
                            <ChevronDown
                                style={{
                                    width: 18,
                                    height: 18,
                                    color: token.colorTextDescription,
                                    transform: isExpanded ? 'rotate(180deg)' : 'rotate(0deg)',
                                    transition: 'transform 0.2s cubic-bezier(0.645, 0.045, 0.355, 1)'
                                }}
                            />
                        </div>
                    )}
                </Flex>
            </Flex>

            {/* 子资产设备列表展示 */}
            <AnimatePresence>
                {isBound && isExpanded && (
                    <motion.div
                        initial={{ height: 0, opacity: 0 }}
                        animate={{ height: 'auto', opacity: 1 }}
                        exit={{ height: 0, opacity: 0 }}
                        transition={{ duration: 0.2, ease: 'easeInOut' }}
                        style={{ position: 'relative', paddingLeft: '22px', overflow: 'hidden' }}
                    >
                        {/* 垂直连接引导线 */}
                        <div style={{
                            position: 'absolute', left: '21px', top: '-10px', bottom: '24px',
                            width: '1.5px', backgroundColor: token.colorBorderSecondary, zIndex: 1
                        }} />

                        <List
                            itemLayout="horizontal"
                            dataSource={passkeys}
                            split={false}
                            style={{ marginTop: 8 }}
                            renderItem={(item) => (
                                <List.Item style={{ padding: '12px 0 12px 24px', position: 'relative' }}>
                                    <div style={{
                                        position: 'absolute', left: 0, top: '28px', width: '12px',
                                        height: '1.5px', backgroundColor: token.colorBorderSecondary
                                    }} />

                                    <List.Item.Meta
                                        avatar={
                                            <div style={{
                                                padding: 6, background: token.colorFillAlter, borderRadius: '50%',
                                                display: 'flex', border: `1px solid ${token.colorBorderSecondary}`, zIndex: 2
                                            }}>
                                                <Fingerprint style={{ width: 13, height: 13, color: token.colorTextDescription }} />
                                            </div>
                                        }
                                        title={<Text style={{ fontSize: 14, fontWeight: 500 }}>{item.label || '未命名设备'}</Text>}
                                        description={<Text type="secondary" style={{ fontSize: 12 }}>绑定于: {item.createTime || '-'}</Text>}
                                    />
                                    <Button
                                        type="text" danger size="small"
                                        icon={<Trash2 style={{ width: 14, height: 14 }} />}
                                        onClick={(e) => handleOpenUnbindModal(item, e)}
                                        style={{ display: 'flex', alignItems: 'center', gap: 4 }}
                                    >
                                        移除
                                    </Button>
                                </List.Item>
                            )}
                        />
                    </motion.div>
                )}
            </AnimatePresence>

            {/* 🚀 统一复用的多步风控安全弹窗 */}
            <Modal
                title={
                    <div style={{ marginBottom: 16 }}>
                        <Title level={4} style={{ margin: 0, fontWeight: 600 }}>
                            {modalMode === 'unbind' ? '移除安全凭证' : '绑定通行密钥'}
                        </Title>
                    </div>
                }
                open={modalMode !== null}
                onCancel={handleCloseModal}
                width={460}
                centered
                destroyOnHidden
                footer={null}
            >
                {modalMode === 'bind' && (
                    <SecurityStepVerify
                        scene="BIND_WEBAUTHN" // 后端风控定义对应的场景标识
                        captchaScene="default"
                        context={context}
                        stepTitle="请录入硬件指纹/面容"
                        confirmText="唤起硬件验证"
                        confirmLoading={isBindLoading}
                        onCancel={handleCloseModal}
                        onConfirm={handleBindSubmit} // 🚀 验证通过后携带 ticket 去开启 WebAuthn 硬件
                    >
                        <Flex vertical gap={12} style={{ padding: '4px 0 16px 0' }}>
                            <Alert
                                message="身份验证成功"
                                description="您的身份已确认。接下来，系统将唤起浏览器原生的 Passkey 凭据管理器。"
                                type="success" // 💡 修正 2：既然通过了，状态应该改为成功（绿标）
                                showIcon
                            />
                            <Space direction="vertical" size={4} style={{ marginTop: 4 }}>
                                <Text strong style={{ fontSize: 14, color: token.colorPrimary }}>
                                    请点击下方按钮录入指纹、面容或安全密钥
                                </Text>
                                <Text type="secondary" style={{ fontSize: 13, lineHeight: '20px' }}>
                                    点击后，请根据操作系统弹出的系统提示（如 Windows Hello 或 Touch ID）完成触控验证。该密钥将安全地保存在您当前设备中。
                                </Text>
                            </Space>
                        </Flex>
                    </SecurityStepVerify>
                )}

                {modalMode === 'unbind' && (
                    <SecurityStepVerify
                        scene="UNBIND_WEBAUTHN"
                        captchaScene="default"
                        context={context}
                        stepTitle="风险确认"
                        confirmText="确定解绑移除"
                        confirmDanger={true}
                        confirmLoading={unbindLoading}
                        onCancel={handleCloseModal}
                        onConfirm={handleUnbindSubmit}
                    >
                        <Flex vertical gap={14}>
                            <Alert
                                message="不可逆的凭证移除警告"
                                description="移除后，您将无法再通过该硬件设备的指纹或面容快速登录该系统。如需重新启用，您必须在对应设备上重新进行物理绑定流程。"
                                type="warning"
                                showIcon
                                icon={<ShieldAlert style={{ color: token.colorWarning }} />}
                            />
                            <div style={{ padding: '4px 12px', background: token.colorFillAlter, borderRadius: 8, border: `1px solid ${token.colorBorderSecondary}` }}>
                                <Space direction="vertical" size={2} style={{ padding: '8px 0' }}>
                                    <Text type="secondary" style={{ fontSize: 13 }}>即将断开的资产信息：</Text>
                                    <Text strong style={{ fontSize: 15, color: token.colorText }}>{targetItem?.label || '未命名密钥'}</Text>
                                    <Text type="secondary" style={{ fontSize: 12 }}>创建时间: {targetItem?.createTime || '-'}</Text>
                                </Space>
                            </div>
                            <Text type="danger" style={{ fontSize: 13, fontWeight: 500 }}>
                                💡 确认无误请在完成上方风控后，点击下方的 “确定解绑移除”。
                            </Text>
                        </Flex>
                    </SecurityStepVerify>
                )}
            </Modal>
        </Flex>
    );
};

export default PasskeyItem;