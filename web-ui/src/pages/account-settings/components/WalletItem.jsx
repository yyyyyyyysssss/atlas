import React, { useState } from 'react';
import { Button, Typography, Flex, App, theme, Space, List, Modal, Avatar, Alert } from 'antd';
import { ChevronDown, ShieldAlert, Trash2, Wallet } from 'lucide-react';
import { useRequest } from 'ahooks';
import {
    web3Bind,
    web3RegisterOptions,
    web3Unbind,         // 后端接口：核验签名并最终落库绑定
} from '../../../services/AccountService';
import { AnimatePresence, motion } from 'framer-motion';

// 🌟 Wagmi & RainbowKit 核心
import { useAccount, useSignMessage, useSignTypedData, useDisconnect } from 'wagmi';
import { useConnectModal, ConnectButton } from '@rainbow-me/rainbowkit';
import { WalletAvatar } from '../../../components/WalletAvatar';
import SecurityStepVerify from './verifiers/SecurityStepVerify';

const { Text, Title } = Typography;

const WalletItem = ({ context, refresh }) => {
    const { token } = theme.useToken();
    const { message } = App.useApp();

    // 状态控制
    const [isExpanded, setIsExpanded] = useState(false);

    const [modalMode, setModalMode] = useState(null)

    const [targetItem, setTargetItem] = useState(null)

    // 解构绑定的钱包数据
    const { web3Enabled = false, web3Wallets = [] } = context || {};
    const isBound = web3Enabled

    // Web3 连接状态与弹窗控制
    const { address, isConnected, chain, connector } = useAccount()
    const { openConnectModal } = useConnectModal();

    // TanStack 核心异步属性变体
    const { disconnectAsync } = useDisconnect()

    const { signMessageAsync } = useSignMessage()

    const { signTypedDataAsync } = useSignTypedData()

    const { runAsync: web3RegisterOptionsAsync, loading: web3RegisterOptionsLoading } = useRequest(web3RegisterOptions, { manual: true });

    const { runAsync: web3BindAsync, loading: web3BindLoading } = useRequest(web3Bind, { manual: true })

    const { runAsync: web3UnbindAsync, loading: web3UnbindLoading } = useRequest(web3Unbind, { manual: true });


    const getIconColor = () => {
        return isBound ? token.colorPrimary : token.colorTextDescription;
    };


    const handleBindSubmit = async (ticket, isConnected, address) => {
        try {
            if (!isConnected) {
                if (openConnectModal) {
                    openConnectModal()
                } else {
                    message.error({ content: '钱包插件未就绪，请刷新重试', key: 'wallet_action' })
                }
                return;
            }
            if (!address) {
                message.error({ content: '未检测到连接的钱包地址，请重新连接', key: 'wallet_action' })
                return
            }
            message.loading({ content: '正在向钱包发起持有权签名...', key: 'wallet_action' })
            const walletType = 'EOA'
            const walletLabel = connector?.name || 'Unknown'
            const registerOptionsRes = await web3RegisterOptionsAsync({
                address: address,
                walletType: walletType,
                label: walletLabel,
                source: 'web'
            })
            // 拉起钱包签名
            let signature
            if (walletType === 'EOA') {
                signature = await signMessageAsync({ message: registerOptionsRes.message })
            } else if (walletType === 'EIP712') {
                const typedData = JSON.parse(registerOptionsRes.message);
                signature = await signTypedDataAsync(typedData)
            } else {
                message.error({ content: `不支持的签名类型: ${walletType}`, key: 'wallet_action' })
                return
            }

            message.loading({ content: '正在提交服务器验证...', key: 'wallet_action' });

            // 最终提交后端核验
            await web3BindAsync({
                ticket: ticket,
                web3Id: registerOptionsRes.web3Id,
                signature: signature,
            });

            message.success({ content: 'Web3.0 加密钱包绑定成功！', key: 'wallet_action' })
            handleCloseModal()
            refresh?.();
            setIsExpanded(true)

        } catch (error) {
            console.error('Wallet 绑定失败:', error);
            message.error({
                content: error?.response?.data?.message || error?.message || '签名失败或用户取消',
                key: 'wallet_action'
            });
        }
    }

    const handleOpenUnbindModal = (item, e) => {
        if (e) e.stopPropagation();
        setTargetItem(item);
        setModalMode('unbind')
    }

    const handleOpenBindModal = (e) => {
        if (e) e.stopPropagation()
        setModalMode('bind')
    }

    const unbindConfirm = async (ticket) => {
        if (!targetItem || !ticket) return;
        await web3UnbindAsync({
            credentialId: targetItem.id,
            ticket: ticket
        });
        const walletIdentifier = targetItem.address
            ? `${targetItem.address.replace(/(.{6}).*(.{4})/, '$1...$2')}${targetItem.label ? ` (${targetItem.label})` : ''}`
            : (targetItem.label || '加密钱包');

        message.success({
            content: `已成功解除 Web3 钱包 [${walletIdentifier}] 的绑定关联`,
            key: 'wallet_action'
        });
        handleCloseModal()
        refresh?.()
    }

    const handleCloseModal = () => {
        setModalMode(null)
        setTargetItem(null)
    }

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
                        <Wallet style={{ width: 20, height: 20, color: getIconColor() }} />
                    </div>
                    <Flex vertical gap={4}>
                        <Text strong style={{ fontSize: 16 }}>Web3.0 加密钱包</Text>
                        <Text style={{ color: token.colorTextDescription, fontSize: 14, lineHeight: '22px' }}>
                            {isBound
                                ? `已启用。当前已关联 ${web3Wallets.length} 个加密数字钱包。`
                                : '未绑定。点击按钮将直接拉起钱包连接与链下签名。'
                            }
                        </Text>
                    </Flex>
                </Flex>

                <Flex align="center" gap={12} onClick={(e) => e.stopPropagation()}>
                    <ConnectButton.Custom>
                        {({ account, chain, openAccountModal, openChainModal, openConnectModal, authenticationStatus, mounted }) => {
                            const ready = mounted && authenticationStatus !== 'loading';
                            const connected = ready && account && chain;
                            const isCurrentAddressBound = web3Wallets.some(
                                (wallet) => wallet.address?.toLowerCase() === account?.address?.toLowerCase()
                            );
                            return (
                                <Flex gap={8} align="center">

                                    {connected && (
                                        <WalletConnectionCapsule
                                            isConnected={connected}
                                            address={account.address}
                                            displayName={account.displayName}
                                            onClick={openAccountModal}
                                        />
                                    )}

                                    {!isCurrentAddressBound && (
                                        <Button
                                            type={connected ? "primary" : "default"}
                                            loading={web3RegisterOptionsLoading || web3BindLoading}
                                            onClick={() => {
                                                if (!connected) {
                                                    if (openConnectModal) {
                                                        openConnectModal()
                                                    } else {
                                                        message.error({ content: '钱包插件未就绪，请刷新重试', key: 'wallet_action' })
                                                    }
                                                    return
                                                }
                                                handleOpenBindModal()
                                            }}
                                        >
                                            立即绑定
                                        </Button>
                                    )}
                                </Flex>
                            );
                        }}
                    </ConnectButton.Custom>

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

            {/* 已绑定钱包列表区域 */}
            <AnimatePresence>
                {isBound && isExpanded && (
                    <motion.div
                        initial={{ height: 0, opacity: 0 }}
                        animate={{ height: 'auto', opacity: 1 }}
                        exit={{ height: 0, opacity: 0 }}
                        transition={{ duration: 0.2, ease: 'easeInOut' }}
                        style={{ position: 'relative', paddingLeft: '22px', overflow: 'hidden' }}
                    >
                        <div style={{
                            position: 'absolute', left: '21px', top: '-10px', bottom: '24px',
                            width: '1.5px', backgroundColor: token.colorBorderSecondary, zIndex: 1
                        }} />

                        <List
                            itemLayout="horizontal"
                            dataSource={web3Wallets}
                            split={false}
                            style={{ marginTop: 8 }}
                            renderItem={(item) => (
                                <List.Item
                                    style={{
                                        padding: '12px 0 12px 24px',
                                        position: 'relative'
                                    }}
                                    actions={[
                                        <Button
                                            type="text"
                                            danger
                                            size="small"
                                            onClick={(e) => handleOpenUnbindModal(item, e)}
                                            icon={<Trash2 style={{ width: 14, height: 14 }} />}
                                            style={{ display: 'flex', alignItems: 'center', gap: 4 }}
                                        >
                                            移除
                                        </Button>
                                    ]}
                                >
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
                                                <Wallet style={{ width: 13, height: 13, color: token.colorTextDescription }} />
                                            </div>
                                        }
                                        title={
                                            <Text
                                                style={{
                                                    fontSize: 14,
                                                    fontWeight: 500,
                                                    cursor: 'pointer'
                                                }}
                                                onClick={async (e) => {
                                                    e.stopPropagation()
                                                    await navigator.clipboard.writeText(item.address);
                                                    message.success('地址已复制');
                                                }}
                                                title="点击复制完整地址"
                                            >
                                                {item.address ? item.address.replace(/(.{6}).*(.{4})/, '$1...$2') : '未知凭证'}
                                                {item.label && <Text type="secondary" style={{ marginLeft: 8, fontSize: 12 }}>({item.label})</Text>}
                                            </Text>
                                        }
                                        description={<Text type="secondary" style={{ fontSize: 12 }}>绑定于: {item.createTime || '-'}</Text>}
                                    />
                                </List.Item>
                            )}
                        />
                    </motion.div>
                )}
            </AnimatePresence>

            <Modal
                title={
                    <div style={{ marginBottom: 16 }}>
                        <Title level={4} style={{ margin: 0, fontWeight: 600 }}>
                            {modalMode === 'unbind' ? '解绑 Web3.0 加密钱包' : '绑定 Web3.0 加密钱包'}
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
                        scene="BIND_WEB3_WALLET" // 后端风控定义对应的场景标识
                        captchaScene="default"
                        context={context}
                        stepTitle="绑定 Web3.0 加密钱包"
                        confirmText="立即唤起钱包签名"
                        confirmLoading={web3RegisterOptionsLoading || web3BindLoading}
                        onCancel={handleCloseModal}
                        onConfirm={(ticket) => handleBindSubmit(ticket, isConnected, address)}
                    >
                        <Flex vertical gap={12} style={{ padding: '4px 0 16px 0' }}>
                            <Alert
                                message="身份验证成功"
                                description="安全风控校验已通过，您的操作凭证已生成。"
                                type="success"
                                showIcon
                            />
                            <Space direction="vertical" size={4} style={{ marginTop: 4 }}>
                                <Text strong style={{ fontSize: 14, color: token.colorPrimary }}>
                                    请确认当前钱包地址，并准备拉起签名
                                </Text>
                                <Text type="secondary" style={{ fontSize: 13, lineHeight: '22px' }}>
                                    点击下方按钮后，系统将通过浏览器插件或 WalletConnect 唤起您的加密钱包。请在钱包弹窗中核对签名信息并点击
                                    <Text strong style={{ color: token.colorText, margin: '0 4px' }}>“签名 (Sign)”</Text>
                                    以完成最终的链上地址归属权确权。
                                </Text>
                            </Space>
                        </Flex>
                    </SecurityStepVerify>
                )}

                {modalMode === 'unbind' && (
                    <SecurityStepVerify
                        scene="UNBIND_WEB3_WALLET"
                        captchaScene="default"
                        context={context}
                        stepTitle="风险确认"
                        confirmText="确定解绑并移除"
                        confirmDanger={true}
                        confirmLoading={web3UnbindLoading}
                        onCancel={handleCloseModal}
                        onConfirm={unbindConfirm}
                    >
                        <Flex vertical gap={14}>
                            <Alert
                                message="解绑加密钱包风险提示"
                                description="解绑后，该链上地址将立即失效，您将无法再使用该钱包拉起签名快捷登录此系统。若此账号未绑定其他登录凭证（如密码、MFA 或社交账号），可能会导致您无法登录。"
                                type="warning"
                                showIcon
                                icon={<ShieldAlert style={{ color: token.colorWarning }} />}
                            />
                            <div style={{ padding: '4px 12px', background: token.colorFillAlter, borderRadius: 8, border: `1px solid ${token.colorBorderSecondary}` }}>
                                <Space direction="vertical" size={2} style={{ padding: '8px 0' }}>
                                    <Text type="secondary" style={{ fontSize: 13 }}>即将解绑的钱包地址：</Text>
                                    <Text strong style={{ fontSize: 15, color: token.colorText }}>
                                        {targetItem?.address ? targetItem.address.replace(/(.{6}).*(.{4})/, '$1...$2') : '未知钱包'}
                                        {targetItem?.label && <span style={{ fontSize: 13, fontWeight: 'normal', color: token.colorTextDescription }}> ({targetItem.label})</span>}
                                    </Text>
                                    <Text type="secondary" style={{ fontSize: 12 }}>绑定时间: {targetItem?.createTime || '-'}</Text>
                                </Space>
                            </div>
                            <Text type="danger" style={{ fontSize: 13, fontWeight: 500 }}>
                                💡 移除解绑属于高危安全操作。确认无误并在上方完成安全风控验密后，点击下方的“确定解绑并移除”。
                            </Text>
                        </Flex>
                    </SecurityStepVerify>
                )}
            </Modal>
        </Flex>
    );
};

export default WalletItem;


/**
 * @param {boolean} isConnected - 是否已连接
 * @param {string} address - 钱包地址
 * @param {string} displayName - 显示名称
 * @param {Function} onClick - 点击回调
 */
export const WalletConnectionCapsule = ({ isConnected, address, displayName, onClick, size = 'middle' }) => {
    const { token } = theme.useToken()

    const sizeMap = {
        small: {
            height: '28px',
            padding: '3px 10px 3px 3px',
            avatarSize: 20,
            fontSize: '12px',
            dotSize: '6px',
            gap: '6px'
        },
        middle: {
            height: '36px',
            padding: '4px 12px 4px 4px',
            avatarSize: 24,
            fontSize: '14px',
            dotSize: '8px',
            gap: '8px'
        },
        large: {
            height: '46px',
            padding: '6px 18px 6px 6px',
            avatarSize: 32,
            fontSize: '16px',
            dotSize: '10px',
            gap: '10px'
        }
    }

    const currentSize = sizeMap[size] || sizeMap.middle

    const baseShadow = token.mode === 'dark'
        ? '0 2px 8px rgba(0, 0, 0, 0.4)'
        : '0 2px 8px rgba(0, 0, 0, 0.04)'

    const hoverShadow = token.mode === 'dark'
        ? '0 4px 16px rgba(0, 0, 0, 0.55)'
        : `0 4px 14px ${token.colorPrimary}15`

    return (
        <div
            onClick={onClick}
            style={{
                display: 'flex',
                alignItems: 'center',
                gap: currentSize.gap,
                padding: currentSize.padding,
                borderRadius: '999px',
                backgroundColor: isConnected ? token.colorFillAlter : token.colorBgContainer,
                cursor: 'pointer',
                boxShadow: baseShadow,
                transition: 'all 0.3s ease',
                height: currentSize.height,
                opacity: 0.9,
            }}
            onMouseEnter={(e) => {
                e.currentTarget.style.opacity = '1';
                e.currentTarget.style.backgroundColor = token.colorFillSecondary;
                e.currentTarget.style.boxShadow = hoverShadow;
            }}
            onMouseLeave={(e) => {
                e.currentTarget.style.opacity = '0.9';
                e.currentTarget.style.backgroundColor = isConnected ? token.colorFillAlter : token.colorBgContainer;
                e.currentTarget.style.boxShadow = baseShadow;
            }}
        >
            {/* 头像容器 */}
            <div style={{
                padding: '2px',
                backgroundColor: 'white',
                borderRadius: '50%',
                display: 'flex',
                filter: isConnected ? 'none' : 'grayscale(1)' // 未连接时头像灰度化
            }}>
                <WalletAvatar address={address || '0x0'} size={currentSize.avatarSize} />
            </div>

            {/* 名称 */}
            <span style={{
                fontSize: currentSize.fontSize,
                ffontWeight: size === 'large' ? 600 : 500,
                color: isConnected ? token.colorText : token.colorTextTertiary
            }}>
                {displayName || '未连接'}
            </span>

            {/* 状态圆点：已连接呼吸，未连接静止且灰色 */}
            <div style={{
                width: currentSize.dotSize,
                height: currentSize.dotSize,
                borderRadius: '50%',
                backgroundColor: isConnected ? token.colorSuccess : token.colorTextQuaternary,
                boxShadow: isConnected ? `0 0 6px ${token.colorSuccess}66` : 'none',
                animation: isConnected ? 'pulse 2s infinite' : 'none',
                marginLeft: '2px'
            }} />
            <style>{`
                @keyframes pulse {
                    0% { transform: scale(0.95); opacity: 0.7; }
                    50% { transform: scale(1.1); opacity: 1; }
                    100% { transform: scale(0.95); opacity: 0.7; }
                }
            `}</style>
        </div>
    );
};