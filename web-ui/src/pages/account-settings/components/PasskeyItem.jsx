import React, { useRef, useState } from 'react';
import { Button, Typography, Flex, App, theme, Space, List, Modal, Steps, Alert } from 'antd';
import { Fingerprint, Trash2, Plus, ChevronDown, ShieldAlert } from 'lucide-react';
import { useRequest } from 'ahooks';
import {
    webauthnRegisterOptions,
    webauthnRegister,
    unbindWebauthn,
    webauthnAuthenticateOptions,
    verifyWebauthn,
} from '../../../services/AccountService';
import { LeftOutlined } from '@ant-design/icons';
import VerifyDropdown from './verifiers';
import { AnimatePresence, motion } from 'framer-motion';
import SecurityStepVerify from './verifiers/SecurityStepVerify';

const { Text, Title } = Typography

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
}

const PasskeyItem = ({ context, refresh }) => {
    const { token } = theme.useToken();
    const { message, modal } = App.useApp();

    // 状态控制：控制下方已绑定设备列表的就地展示/收起
    const [isExpanded, setIsExpanded] = useState(false);

    const [isModalOpen, setIsModalOpen] = useState(false)

    const { passkeys = [] } = context || {};
    const isBound = passkeys && passkeys.length > 0

    // 💡 移除解绑流的核心状态
    const [targetItem, setTargetItem] = useState(null); // 当前准备移除的设备凭证
    const [verifyMethod, setVerifyMethod] = useState();


    const verifierRef = useRef(null)

    // 检测当前环境是否支持 WebAuthn
    const isWebAuthnSupported =
        window.PublicKeyCredential !== undefined &&
        typeof window.PublicKeyCredential === 'function' &&
        typeof PublicKeyCredential.parseCreationOptionsFromJSON === 'function';

    // 获取注册配置的请求
    const { runAsync: getOptionsAsync, loading: getOptionsLoading } = useRequest(webauthnRegisterOptions, {
        manual: true
    });

    // 确认绑定的请求
    const { runAsync: webauthnRegisterAsync, loading: webauthnRegisterLoading } = useRequest(webauthnRegister, {
        manual: true
    });

    const { runAsync: verifyWebauthnAsync, loading: verifyWebauthnLoading } = useRequest(verifyWebauthn, {
        manual: true
    });

    //  解绑请求
    const { runAsync: unbindWebauthnAsync, loading: unbindLoading } = useRequest(unbindWebauthn, {
        manual: true
    });

    const getIconColor = () => {
        if (isBound) return token.colorSuccess;
        if (isWebAuthnSupported) return token.colorPrimary;
        return token.colorTextDisabled;
    };

    // 执行绑定流程
    const handleBind = async (e) => {
        // 阻止点击事件冒泡到整行的展开动作
        if (e) e.stopPropagation();

        try {
            const { webauthnId, publicKey } = await getOptionsAsync();
            message.loading({ content: '正在唤起设备凭证...', key: 'passkey_action' });

            const nativeOptions = PublicKeyCredential.parseCreationOptionsFromJSON(publicKey);
            const credential = await navigator.credentials.create({ publicKey: nativeOptions });

            message.loading({ content: '正在向服务器验证...', key: 'passkey_action' });
            const credentialJson = credential.toJSON();
            const label = getDeviceLabel();

            await webauthnRegisterAsync(webauthnId,
                {
                    publicKey: {
                        label: label,
                        credential: credentialJson
                    }
                }
            )

            message.success({ content: '通行密钥绑定成功！', key: 'passkey_action' });
            if (refresh) refresh();
            setIsExpanded(true); // 绑定成功后自动展开看到新设备

        } catch (error) {
            console.error('Passkey 绑定失败:', error);
            if (error instanceof DOMException || error.name) {
                if (error.name === 'NotAllowedError') {
                    message.warning({ content: '操作已取消或验证超时', key: 'passkey_action' });
                } else if (error.name === 'InvalidStateError' || error.name === 'ConstraintError') {
                    message.info({ content: '当前设备已绑定过该账号，无需重复绑定', key: 'passkey_action', duration: 4 });
                } else {
                    message.error({ content: `设备交互失败: ${error.message || '未知错误'}`, key: 'passkey_action' });
                }
            } else {
                message.error({
                    content: error?.response?.data?.message || error?.message || '服务器验证失败，请稍后重试',
                    key: 'passkey_action'
                });
            }
        }
    }


    // --- 💡 移除解绑流程控制 ---
    const handleOpenUnbindModal = (item, e) => {
        if (e) e.stopPropagation();
        setTargetItem(item);
        setVerifyMethod(null);
        setIsModalOpen(true);
    }

    const handleCancelUnbind = () => {
        setIsModalOpen(false);
        setTargetItem(null);
        setVerifyMethod(null);
    }

    const handleConfirmDestroy = async (ticket) => {
        if (!targetItem || !ticket) return
        await unbindWebauthnAsync({
            credentialId: targetItem.credentialId,
            ticket: ticket
        })
        message.success(`已成功移除密钥凭证 [${targetItem.label || '未知设备'}]`);
        handleCancelUnbind()
        refresh?.()
    }


    const isGlobalLoading = getOptionsLoading || webauthnRegisterLoading;

    return (
        <Flex
            vertical
            style={{
                borderBottom: `1px solid ${token.colorBorderSecondary}`,
                padding: '20px 0'
            }}
        >
            {/* 上半部分：主控制行（有设备时整行可点击切换展开） */}
            <Flex
                justify="space-between"
                align="center"
                onClick={() => isBound && setIsExpanded(!isExpanded)}
                style={{
                    cursor: isBound ? 'pointer' : 'default',
                    position: 'relative'
                }}
            >
                {/* 左侧信息展示 */}
                <Flex gap={16} align="flex-start" style={{ flex: 1 }}>
                    <div style={{
                        padding: 12,
                        background: token.colorFillAlter,
                        borderRadius: '50%',
                        display: 'flex',
                        zIndex: 2 // 确保图标压在连接线上方
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

                {/* 右侧操作控制区 */}
                <Flex align="center" gap={12} style={{ justifyContent: 'flex-end' }} onClick={(e) => e.stopPropagation()}>
                    {/* 追加绑定 / 立即绑定 按钮 */}
                    <Button
                        type={isBound ? "default" : "primary"}
                        disabled={!isWebAuthnSupported || isGlobalLoading}
                        loading={isGlobalLoading}
                        onClick={handleBind}
                    >
                        {isBound ? '添加设备' : '立即绑定'}
                    </Button>

                    {/* 💡 右侧仅保留纯粹的旋转箭头 */}
                    {isBound && (
                        <div
                            onClick={() => setIsExpanded(!isExpanded)}
                            style={{
                                display: 'flex',
                                alignItems: 'center',
                                padding: '4px',
                                cursor: 'pointer',
                                borderRadius: token.borderRadiusSM,
                                transition: 'background-color 0.2s'
                            }}
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

            {/* 下半部分：树状树枝关系的子项设备列表 */}
            {isBound && isExpanded && (
                <div style={{ position: 'relative', paddingLeft: '22px' }}>
                    {/* 💡 垂直资产树级引导连接线 (Context Line) 
                        起点位于父级指纹图标正中心下方，向下延伸 */}
                    <div
                        style={{
                            position: 'absolute',
                            left: '21px', // 精准居中对齐父级圆圈图标 (12px padding + 20px width -> 中心点在 22px 附近)
                            top: '-10px',
                            bottom: '24px',
                            width: '1.5px',
                            backgroundColor: token.colorBorderSecondary,
                            zIndex: 1
                        }}
                    />

                    <List
                        itemLayout="horizontal"
                        dataSource={passkeys}
                        split={false} // 去掉原生的通栏分割线，用树状间距代替
                        style={{ marginTop: 8 }}
                        renderItem={(item) => (
                            <List.Item
                                style={{
                                    padding: '12px 0 12px 24px', // 💡 核心：往右缩进 24px，让子项图标与父项的标题文本精准左对齐
                                    position: 'relative'
                                }}
                            >
                                {/* 💡 水平树枝短连接线 */}
                                <div style={{
                                    position: 'absolute',
                                    left: 0,
                                    top: '28px',
                                    width: '12px',
                                    height: '1.5px',
                                    backgroundColor: token.colorBorderSecondary,
                                }} />

                                <List.Item.Meta
                                    avatar={
                                        <div style={{
                                            padding: 6,
                                            background: token.colorFillAlter,
                                            borderRadius: '50%',
                                            display: 'flex',
                                            border: `1px solid ${token.colorBorderSecondary}`,
                                            zIndex: 2
                                        }}>
                                            <Fingerprint style={{ width: 13, height: 13, color: token.colorTextDescription }} />
                                        </div>
                                    }
                                    title={<Text style={{ fontSize: 14, fontWeight: 500 }}>{item.label || '未命名设备'}</Text>}
                                    description={<Text type="secondary" style={{ fontSize: 12 }}>绑定于: {item.createTime || '-'}</Text>}
                                />
                                <Button
                                    type="text"
                                    danger
                                    size="small"
                                    icon={<Trash2 style={{ width: 14, height: 14 }} />}
                                    onClick={(e) => handleOpenUnbindModal(item, e)}
                                    style={{ display: 'flex', alignItems: 'center', gap: 4 }}
                                >
                                    移除
                                </Button>
                            </List.Item>
                        )}
                    />
                </div>
            )}

            {/* 💡 多步解绑风控安全弹窗 */}
            <Modal
                title={
                    <div style={{ marginBottom: 16 }}>
                        <Title level={4} style={{ margin: 0, fontWeight: 600 }}>移除安全凭证</Title>
                    </div>
                }
                open={isModalOpen}
                onCancel={handleCancelUnbind}
                width={460}
                centered
                destroyOnHidden
                footer={null}
            >
                <SecurityStepVerify
                    scene="UNBIND_WEBAUTHN"
                    context={context}
                    stepTitle='风险确认'
                    confirmText="确定解绑移除"
                    confirmDanger={true}
                    confirmLoading={unbindLoading}
                    onCancel={handleCancelUnbind}
                    onConfirm={handleConfirmDestroy}
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
                            💡 确认无误请点击下方的 “确定解绑移除”。
                        </Text>
                    </Flex>
                </SecurityStepVerify>
            </Modal>
        </Flex>
    );
};

export default PasskeyItem;