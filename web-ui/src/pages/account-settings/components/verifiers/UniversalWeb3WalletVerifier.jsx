import React, { useState } from 'react';
import { Typography, theme, Flex, App, Badge, Button } from 'antd';
import { useRequest } from 'ahooks';
import { Wallet, ShieldCheck, KeyRound, LogOut } from 'lucide-react';

// 🌟 引入 Wagmi & RainbowKit 核心 Hooks 和核心组件
import { useSignMessage, useDisconnect, useAccount } from 'wagmi';
import { ConnectButton } from '@rainbow-me/rainbowkit';

// 🌟 引入后端接口与你的胶囊组件
import { web3RegisterOptions } from '../../../../services/AccountService';
import { WalletConnectionCapsule } from '../WalletItem';

const { Text } = Typography;

/**
 * 全站通用的 Web3 加密钱包高级验证组件（无箭头、极致精简对齐版）
 */
const UniversalWeb3WalletVerifier = ({
    verifierRef,
    onVerifyAction,
    onSuccess,
    label = "加密钱包签名认证"
}) => {
    const { token } = theme.useToken();
    const { message } = App.useApp();

    // 🌟 Wagmi 核心控制
    const { signMessageAsync } = useSignMessage();
    const { disconnect } = useDisconnect()

    // 异步获取后端签名挑战项配置
    const { runAsync: web3RegisterOptionsAsync, loading: optionsLoading } = useRequest(web3RegisterOptions, {
        manual: true
    });

    const [hardwareLoading, setHardwareLoading] = useState(false); // 唤起钱包等待签名状态
    const [verifyLoading, setVerifyLoading] = useState(false);   // 后端验签状态

    const isGlobalLoading = optionsLoading || hardwareLoading || verifyLoading;

    /**
     * 核心身份鉴权确核逻辑
     */
    const doWalletAuthenticate = async (currentAddress, currentConnector) => {
        if (isGlobalLoading) return;

        try {
            const walletLabel = currentConnector?.name || 'Unknown';
            const registerOptionsRes = await web3RegisterOptionsAsync({
                address: currentAddress,
                walletType: 'EOA',
                label: walletLabel,
                source: 'web'
            });

            if (registerOptionsRes.boundAddress &&
                registerOptionsRes.boundAddress.toLowerCase() !== currentAddress.toLowerCase()) {
                disconnect();
                throw new Error('当前连接的钱包账户与系统要求不匹配，已自动断开');
            }

            setHardwareLoading(true);

            // 唤起私钥签名挑战
            const signature = await signMessageAsync({
                message: registerOptionsRes.message
            });

            setHardwareLoading(false);
            setVerifyLoading(true);

            const result = await onVerifyAction(signature, registerOptionsRes.web3Id);
            return result;

        } catch (error) {
            if (error?.name === 'UserRejectedRequestError' || error?.code === 4001) {
                throw new Error('用户取消了钱包签名');
            } else {
                throw new Error(error?.response?.data?.message || error?.message || '钱包鉴权验证失败');
            }
        } finally {
            setHardwareLoading(false);
            setVerifyLoading(false);
        }
    };

    if (verifierRef) {
        verifierRef.current = {
            getValue: () => 'WEB3_WALLET_MODE',
            reset: () => { }
        };
    }

    return (
        <Flex vertical gap={8} style={{ width: '100%' }}>
            {/* 上层表单 Label */}
            <Text style={{ fontWeight: 500, fontSize: 13, color: token.colorTextDescription }}>
                {label}
            </Text>

            <ConnectButton.Custom>
                {({ account, chain, openConnectModal,openAccountModal, mounted, authenticationStatus }) => {
                    const ready = mounted && authenticationStatus !== 'loading';
                    const connected = !!(ready && account && chain);

                    // 动态计算胶囊内要显示的文案
                    const getCapsuleDisplayName = () => {
                        if (optionsLoading) return '正在获取凭证...';
                        if (hardwareLoading) return '等待钱包签名...';
                        if (verifyLoading) return '正在确权核验...';
                        return `以 ${account?.displayName} 身份验证`;
                    };

                    // 动态获取状态描述文案
                    const getStatusMessage = () => {
                        if (optionsLoading || hardwareLoading) {
                            return '请在您的加密钱包扩展程序弹窗中允许“签名”请求以确权';
                        }
                        if (verifyLoading) {
                            return '正在进行链下签名密码学核验，请稍候...';
                        }
                        return '区块链身份已准备就绪，点击上方唤起私钥签名';
                    };

                    const handleActionTrigger = async () => {
                        if (!connected) {
                            openConnectModal?.();
                        } else {
                            try {
                                const result = await doWalletAuthenticate(account.address, account.connector);
                                if (result && onSuccess) {
                                    onSuccess(result);
                                }
                            } catch (error) {
                                message.error(error.message);
                            }
                        }
                    };

                    const handleKeyDown = (e) => {
                        if ((e.key === 'Enter' || e.key === ' ') && !connected) {
                            e.preventDefault();
                            handleActionTrigger();
                        }
                    };

                    return (
                        <>
                            {/* ==================== 状态一：未连接钱包 ==================== */}
                            {!connected && (
                                <Flex
                                    align="center"
                                    justify="center"
                                    vertical
                                    onClick={handleActionTrigger}
                                    tabIndex={0}
                                    onMouseEnter={(e) => e.currentTarget.focus()}
                                    onKeyDown={handleKeyDown}
                                    style={{
                                        padding: '28px 24px',
                                        borderRadius: token.borderRadiusLG,
                                        background: token.colorFillAlter,
                                        cursor: 'pointer',
                                        transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
                                        outline: 'none'
                                    }}
                                    className="web3-wallet-trigger"
                                >
                                    <div
                                        style={{
                                            padding: 16,
                                            background: optionsLoading ? token.colorPrimaryBg : token.colorBgContainer,
                                            borderRadius: '50%',
                                            display: 'flex',
                                            marginBottom: 12,
                                            boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)',
                                            transition: 'all 0.25s',
                                            animation: isGlobalLoading ? 'pulse 1.5s infinite' : 'none'
                                        }}
                                    >
                                        <Wallet
                                            style={{
                                                width: 28,
                                                height: 28,
                                                color: isGlobalLoading ? token.colorPrimary : token.colorTextDescription
                                            }}
                                        />
                                    </div>
                                    <Text strong style={{ color: token.colorText, fontSize: 14 }}>
                                        点击连接加密钱包
                                    </Text>
                                    <Text type="secondary" style={{ fontSize: 12, marginTop: 4 }}>
                                        支持 MetaMask、WalletConnect 及主流去中心化钱包
                                    </Text>
                                </Flex>
                            )}

                            {/* ==================== 状态二：已连接钱包（无外壳，居中扁平化排版） ==================== */}
                            {connected && (
                                <Flex
                                    align="center"
                                    justify="center"
                                    vertical
                                    onClick={handleActionTrigger}
                                    tabIndex={0}
                                    onMouseEnter={(e) => e.currentTarget.focus()}
                                    onKeyDown={handleKeyDown}
                                    style={{
                                        position: 'relative',
                                        padding: '28px 24px', // 🌟 与未连接态高度保持一致的呼吸间距
                                        borderRadius: token.borderRadiusLG,
                                        background: token.colorFillAlter,
                                        cursor: 'pointer',
                                        transition: 'all 0.2s cubic-bezier(0.4, 0, 0.2, 1)',
                                        outline: 'none'
                                    }}
                                    className="web3-wallet-trigger"
                                >
                                    <Button
                                        type="text"
                                        size="small"
                                        disabled={isGlobalLoading}
                                        icon={<LogOut size={12} />}
                                        onClick={(e) => {
                                            e.stopPropagation(); // 阻止冒泡，避免触发卡片签名
                                            openAccountModal?.(); // 唤起 RainbowKit 的切换账户/断开面板
                                        }}
                                        style={{
                                            position: 'absolute',
                                            right: 8,
                                            top: 8,
                                            zIndex: 10,
                                            fontSize: 12,
                                            color: token.colorTextDescription
                                        }}
                                    >
                                        切换账户
                                    </Button>

                                    {/* 激活态的主题色大图标容器，加载时自带柔和脉冲呼吸感 */}
                                    <div
                                        className="wallet-icon-wrapper-active"
                                        style={{
                                            padding: 16,
                                            background: token.colorPrimaryBg, // 🌟 使用主题色浅底
                                            borderRadius: '50%',
                                            display: 'flex',
                                            marginBottom: 12,
                                            boxShadow: `0 2px 10px ${token.colorPrimary}15`,
                                            transition: 'all 0.25s',
                                            animation: isGlobalLoading ? 'wallet-pulse 1.5s infinite' : 'none'
                                        }}
                                    >
                                        <Wallet
                                            style={{
                                                width: 28,
                                                height: 28,
                                                color: token.colorPrimary // 🌟 显式采用高亮主题色
                                            }}
                                        />
                                    </div>

                                    {/* 居中核心信息：直接展示格式化后的地址 */}
                                    <Text strong style={{ color: token.colorText, fontSize: 14 }}>
                                        {account?.displayName || account?.address}
                                    </Text>

                                    {/* 下方纯净状态提示词 */}
                                    <Text
                                        style={{
                                            fontSize: token.fontSizeSM,
                                            marginTop: 4,
                                            textAlign: 'center',
                                            color: isGlobalLoading ? token.colorPrimary : token.colorTextSecondary,
                                            transition: 'color 0.2s cubic-bezier(0.4, 0, 0.2, 1)'
                                        }}
                                    >
                                        {getStatusMessage()}
                                    </Text>
                                </Flex>
                            )}
                        </>
                    );
                }}
            </ConnectButton.Custom>

            {/* 注入与 Passkey 风格统一的 Hover 全量同步控制 */}
            <style>{`
                /* 未连接卡片的 Hover 纯色变换微调，对齐 Passkey 无位移风格 */
                .web3-wallet-trigger:hover {
                    background: ${token.colorFillSecondary} !important;
                    border-color: ${token.colorBorder} !important;
                    transform: translateY(-1px);
                }
                
                .web3-wallet-trigger:hover .wallet-icon-wrapper {
                    background: ${token.colorPrimaryBg} !important;
                    box-shadow: 0 4px 12px ${token.mode === 'dark' ? 'rgba(24, 144, 255, 0.12)' : 'rgba(24, 144, 255, 0.08)'} !important;
                }
                
                .web3-wallet-trigger:hover .wallet-icon-wrapper svg {
                    color: ${token.colorPrimary} !important;
                }
                
                .web3-wallet-trigger:active {
                    background: ${token.colorFill} !important;
                }
            `}</style>
        </Flex>
    );
};

export default UniversalWeb3WalletVerifier;