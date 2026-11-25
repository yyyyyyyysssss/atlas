import React, { useCallback, useImperativeHandle, useRef, useState } from 'react';
import { Typography, theme, Flex, App } from 'antd';
import { FileKey, Key, ShieldCheck } from 'lucide-react';
import Loading from '../../../../components/loading';

const { Text } = Typography;

const UniversalCertificateVerifier = ({
    verifierRef,
    verifyLoading = false,
    verified = false,
    onVerifyAction,
    label = '证书验证'
}) => {

    const { token } = theme.useToken()

    const { message } = App.useApp()

    const handleInternalTrigger = useCallback(() => {
        if (verifyLoading || !onVerifyAction) {
            return;
        }

        return onVerifyAction();
    }, [verifyLoading, onVerifyAction]);

    // 暴露统一验证器接口
    useImperativeHandle(verifierRef, () => ({
        getValue: () => 'CERTIFICATE_MODE',

        validate: () => true,

        onVerify: () => handleInternalTrigger(),

        reset: () => {

        }
    }), [handleInternalTrigger])

    const handleKeyDown = (e) => {
        if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            handleInternalTrigger()
        }
    }

    const getTipText = () => {
        if (verifyLoading) return '正在验证客户端证书...'

        if (verified) return '证书验证成功'

        return '点击触发客户端证书认证'
    }

    return (
        <Flex vertical gap={8} style={{ width: '100%' }}>
            <Text style={{ fontWeight: 500, fontSize: 13 }}>
                {label}
            </Text>
            <Flex
                align="center"
                justify="center"
                vertical
                onClick={handleInternalTrigger}
                tabIndex={verifyLoading ? -1 : 0}
                onMouseEnter={(e) => !verifyLoading && e.currentTarget.focus()}
                onKeyDown={handleKeyDown}
                style={{
                    padding: '24px',
                    borderRadius: token.borderRadiusLG,
                    background: verifyLoading
                        ? token.colorFillTertiary
                        : token.colorFillAlter,
                    cursor: verifyLoading ? 'not-allowed' : 'pointer',
                    transition: 'all 0.25s cubic-bezier(0.4, 0, 0.2, 1)',
                    outline: 'none'
                }}
                className="certificate-verifier-trigger"
            >
                <div
                    className="certificate-icon-wrapper"
                    style={{
                        padding: 16,
                        background: verifyLoading ? token.colorPrimaryBg : token.colorBgContainer,
                        borderRadius: '50%',
                        display: 'flex',
                        marginBottom: 12,
                        boxShadow: '0 2px 8px rgba(0, 0, 0, 0.04)',
                        transition: 'all 0.25s',
                        animation: verifyLoading ? 'pulse 1.5s infinite' : 'none'
                    }}
                >
                    <Loading spinning={verifyLoading}>
                        <Key
                            style={{
                                width: 32,
                                height: 32,
                                color: verifyLoading
                                    ? token.colorPrimary
                                    : token.colorTextDescription
                            }}
                        />
                    </Loading>
                </div>

                <Text
                    strong
                    style={{
                        color: verifyLoading
                            ? token.colorPrimary
                            : token.colorText
                    }}
                >
                    {getTipText()}
                </Text>

                <Text
                    type="secondary"
                    style={{
                        fontSize: 12,
                        marginTop: 4,
                        textAlign: 'center'
                    }}
                >
                    {verifyLoading
                        ? '请在浏览器弹出的窗口中选择客户端证书'
                        : '通过您设备中安装的 X.509 客户端证书验证身份'}
                </Text>
            </Flex>

            <style>{`
                @keyframes pulse {
                    0% {
                        transform: scale(1);
                        opacity: 1;
                    }
                    50% {
                        transform: scale(1.04);
                        opacity: 0.85;
                    }
                    100% {
                        transform: scale(1);
                        opacity: 1;
                    }
                }

                /* 悬浮态 */
                .certificate-verifier-trigger:hover {
                    background: ${token.colorFillSecondary} !important;
                    border-color: ${token.colorBorderSecondary} !important;
                    transform: translateY(-1px);
                }

                .certificate-verifier-trigger:hover .certificate-icon-wrapper svg {
                    color: ${token.colorPrimary} !important;
                }

                /* 按下态 */
                .certificate-verifier-trigger:active {
                    transform: translateY(0px);
                    background: ${token.colorFill} !important;
                }
            `}</style>
        </Flex>
    )
}

export default UniversalCertificateVerifier