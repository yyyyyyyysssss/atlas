import React, { useRef, useState } from 'react';
import { Typography, theme, Flex, App } from 'antd';
import { Fingerprint } from 'lucide-react';
import { useRequest } from 'ahooks';
import { webauthnAuthenticateOptions } from '../../../../services/AccountService';
import Loading from '../../../../components/loading';

const { Text } = Typography;

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

/**
 * 全站通用的通行密钥 (Passkey) 硬件鉴权组件
 * @param {Object} verifierRef - 用于穿透绑定的 ref
 * @param {Function} onVerifyAction - 外部传入的最终服务器验证请求，需返回 Promise<{verified, ticket}>
 * @param {string} label - 上方的标签文案
 */
const UniversalPasskeyVerifier = ({
    verifierRef,
    onVerifyAction,
    onSuccess,
    label = "通行密钥验证"
}) => {
    const { token } = theme.useToken()

    const { message } = App.useApp()

    const isWebAuthnSupported =
        window.PublicKeyCredential !== undefined &&
        typeof window.PublicKeyCredential === 'function';

    const { runAsync: getAuthOptionsAsync, loading: optionsLoading } = useRequest(webauthnAuthenticateOptions, {
        manual: true
    })

    const [hardwareLoading, setHardwareLoading] = useState(false)

    const [verifyLoading, setVerifyLoading] = useState(false)

    const isGlobalLoading = optionsLoading || hardwareLoading || verifyLoading

    const doHardwareAuthenticate = async () => {
        if (isGlobalLoading) {
            return
        }
        if (!isWebAuthnSupported) {
            throw new Error('当前设备或浏览器版本过低，不支持通行密钥功能。');
        }

        try {
            // 1. 获取挑战配置
            const authenticateOptions = await getAuthOptionsAsync();

            // 2. 数据格式转换
            const nativePublicKeyOptions = {
                ...authenticateOptions,
                challenge: base64urlToArrayBuffer(authenticateOptions.challenge),
                allowCredentials: authenticateOptions.allowCredentials?.map(cred => ({
                    ...cred,
                    id: base64urlToArrayBuffer(cred.id)
                }))
            };

            setHardwareLoading(true)
            

            // 3. 唤起硬件
            const credential = await navigator.credentials.get({
                publicKey: nativePublicKeyOptions
            });

            setHardwareLoading(false)
            setVerifyLoading(true)

            // 4. 执行外部传入的验证 API
            const credentialJson = credential.toJSON();
            const result = await onVerifyAction(credentialJson);

            if (!result || !result.verified) {
                throw new Error('通行密钥验证失败，请重试');
            }

            const successResult = { verified: result.verified, ticket: result.ticket }

            if (onSuccess) {
                onSuccess(result.ticket)
            }

            return successResult

        } catch (error) {
            // 精准识别错误并转化为具体的文案向外抛出
            if (error instanceof DOMException || error.name) {
                if (error.name === 'NotAllowedError') {
                    throw new Error('操作已取消或验证超时');
                } else if (error.name === 'InvalidStateError' || error.name === 'ConstraintError') {
                    throw new Error('当前设备已绑定过该账号，无需重复绑定');
                } else {
                    throw new Error(`设备交互失败: ${error.message || '未知错误'}`);
                }
            } else {
                // 抛出后端接口返回的、或者自定义的错误具体信息
                const apiErrorMsg = error?.response?.data?.message || error?.message || '服务器验证失败，请稍后重试';
                throw new Error(apiErrorMsg);
            }
        } finally {
            setHardwareLoading(false)
            setVerifyLoading(false)
        }
    };

    const handleInternalTrigger = async () => {
        try {
            await doHardwareAuthenticate()
        } catch (error) {
            message.error(error.message);
        }
    };

    const handleKeyDown = (e) => {
        // 支持 Enter 键或者 空格键
        if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault(); // 阻止空格键默认向下滚动页面的行为
            handleInternalTrigger();
        }
    };

    const getTipText = () => {
        if (optionsLoading) return '正在读取认证配置...'
        if (hardwareLoading) return '请轻触指纹或面容识别...'
        if (verifyLoading) return '正在完成认证...'
        return '点击触发通行密钥 (Passkey) 认证'
    }

    if (verifierRef) {
        verifierRef.current = {
            getValue: () => 'PASSKEY_MODE',
            validate: async () => {
                if (!isWebAuthnSupported) throw new Error('当前设备或浏览器版本过低，不支持通行密钥功能。');
                return true;
            },
            onVerify: async () => {
                return await doHardwareAuthenticate();
            },
            reset: () => { }
        };
    }

    return (
        <Flex vertical gap={8} style={{ width: '100%' }}>
            <Text style={{ fontWeight: 500, fontSize: 13 }}>{label}</Text>
            <Flex
                align="center"
                justify="center"
                vertical
                onClick={handleInternalTrigger}
                tabIndex={isGlobalLoading ? -1 : 0}
                onMouseEnter={(e) => !isGlobalLoading && e.currentTarget.focus()}
                onKeyDown={handleKeyDown}
                style={{
                    padding: '24px',
                    borderRadius: token.borderRadiusLG,
                    background: isGlobalLoading ? token.colorFillTertiary : token.colorFillAlter,
                    cursor: isGlobalLoading ? 'not-allowed' : 'pointer',
                    transition: 'all 0.25s cubic-bezier(0.4, 0, 0.2, 1)',
                    outline: 'none'
                }}
                className="passkey-verifier-trigger"
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
                    <Loading spinning={verifyLoading}>
                        <Fingerprint style={{
                            width: 32,
                            height: 32,
                            color: isGlobalLoading ? token.colorPrimary : token.colorTextDescription
                        }} />
                    </Loading>
                </div>
                <Text strong style={{ color: isGlobalLoading ? token.colorPrimary : token.colorText }}>
                    {getTipText()}
                </Text>
                <Text type="secondary" style={{ fontSize: 12, marginTop: 4 }}>
                    {!isWebAuthnSupported ? '当前浏览器不支持' : '通过您设备的指纹、面容或锁屏密码快速验证'}
                </Text>
            </Flex>

            <style>{`
                @keyframes pulse {
                    0% { transform: scale(1); opacity: 1; }
                    50% { transform: scale(1.04); opacity: 0.85; }
                    100% { transform: scale(1); opacity: 1; }
                }
                
                /* 🎯 悬浮态：大卡片背景优雅过渡，并在暗色下激活微弱的质感微边框 */
                .passkey-verifier-trigger:hover {
                    background: ${token.colorFillSecondary} !important;
                    border-color: ${token.colorBorderSecondary} !important;
                    transform: translateY(-1px);
                }
                
                /* 中间指纹图标外圈在悬浮时的连动 */
                .passkey-verifier-trigger:hover .fingerprint-icon-wrapper {
                    background: ${token.colorPrimaryBg} !important;
                    /* 💡 暗色下使用低饱和度的主色扩散作为柔和微发光，极其高级 */
                    box-shadow: 0 4px 12px ${token.mode === 'dark' ? 'rgba(24, 144, 255, 0.15)' : token.colorPrimaryBgHover} !important;
                }
                
                .passkey-verifier-trigger:hover .fingerprint-icon-wrapper svg {
                    color: ${token.colorPrimary} !important;
                }
                
                /* 🎯 按下态 */
                .passkey-verifier-trigger:active {
                    transform: translateY(0px);
                    background: ${token.colorFill} !important;
                }

            `}</style>
        </Flex>
    );
};

export default UniversalPasskeyVerifier;