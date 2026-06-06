import React, { useEffect, useRef, useState } from 'react';
import { useRequest } from "ahooks"
import { Form, Input, Button, Card, Flex, Typography, App, Avatar, Divider, Dropdown, ConfigProvider, QRCode } from 'antd';
import { UserOutlined, LockOutlined, MailOutlined, GithubOutlined, GoogleOutlined, KeyOutlined, ScanOutlined, CheckCircleFilled } from '@ant-design/icons';
import { QrCode, Monitor } from 'lucide-react';
import { fetchQrScanUrl, QR_SCAN_PKCE_VERIFIER, qrStatus, qrTicket } from "../../../services/Oauth2Service"
import { AnimatePresence, motion } from 'framer-motion';
import { useTranslation } from 'react-i18next';
import { generateChallenge, generateVerifier } from '../../../utils/pkce';
import { useNavigate } from 'react-router-dom';


const QrLoginCard = ({ loginPanel, setLoginPanel, loginSuccessHandler }) => {

    const { t } = useTranslation()

    const navigate = useNavigate()

    const { runAsync: getQrScanUrlAsync, loading: getQrScanUrlLoading } = useRequest(fetchQrScanUrl, {
        manual: true
    })


    const { runAsync: qrTicketAsync, loading: qrTicketLoading } = useRequest(qrTicket, {
        manual: true
    })

    const { runAsync: qrStatusAsync, loading: qrStatusLoading } = useRequest(qrStatus, {
        manual: true
    })

    const [qrCodeData, setQrCodeData] = useState({
        url: ''
    })

    const qrScanUrlRef = useRef(null)

    const pollingTimerRef = useRef(null)

    // 停止轮询
    const stopPolling = () => {
        if (pollingTimerRef.current) {
            clearInterval(pollingTimerRef.current)
            pollingTimerRef.current = null
        }
    }

    // 开始轮询二维码状态
    const startPolling = (sceneId) => {
        stopPolling() // 确保之前的轮询被清除
        pollingTimerRef.current = setInterval(async () => {
            try {
                const statusRes = await qrStatusAsync(sceneId)
                const currentStatus = statusRes.status

                setQrCodeData(prev => ({ ...prev, status: currentStatus }))

                if (currentStatus === 'CONFIRMED') {
                    stopPolling()
                    const { code, clientName } = statusRes
                    navigate(`/oauth2/callback/${clientName}?code=${code}&login_mode=qr`)
                } else if (currentStatus === 'EXPIRED') {
                    stopPolling()
                }
            } catch (error) {
                console.error('查询二维码状态失败', error)
                setQrCodeData(prev => ({ ...prev, status: 'EXPIRED' }))
                stopPolling()
            }
        }, 2000) // 每 2 秒轮询一次
    }

    const refreshQrCode = async () => {
        stopPolling()

        if (!qrScanUrlRef.current) {
            qrScanUrlRef.current = await getQrScanUrlAsync('atlas')
        }
        // 生成新的 Verifier
        const verifier = generateVerifier()
        sessionStorage.setItem(QR_SCAN_PKCE_VERIFIER, verifier)
        // 生成 Challenge
        const challenge = await generateChallenge(verifier)
        const qrTicketUrl = qrScanUrlRef.current + `&code_challenge=${challenge}&code_challenge_method=S256`
        qrTicketAsync(qrTicketUrl)
            .then((data) => {
                setQrCodeData({
                    url: data?.qrUrl || '',
                    sceneId: data?.sceneId || '',
                    status: 'PENDING'
                })
                if (data?.sceneId) {
                    startPolling(data.sceneId)
                }
            })
            .catch(() => {
                message.error('获取二维码失败，请重试')
                setQrCodeData(prev => ({ ...prev, status: 'EXPIRED' }))
            })
    }

    useEffect(() => {
        if (loginPanel === 'qr') {
            refreshQrCode()
        } else {
            stopPolling()
        }
    }, [loginPanel])

    useEffect(() => {
        // 组件卸载时清理定时器
        return () => {
            stopPolling()
        }
    }, [])

    return (
        <Card
            style={{
                width: '100%',
                height: '100%',
                borderRadius: '24px',
                boxShadow: '0 25px 50px -12px rgba(0,0,0,0.05)',
                border: 'none',
                background: '#ffffff',
                padding: '16px',
            }}
            styles={{
                body: {
                    height: '100%',
                }
            }}
        >
            {/* 右上角切回电脑图标 (折角图片) */}
            <div
                style={{
                    position: 'absolute',
                    top: 16,
                    right: 16,
                    cursor: 'pointer',
                    zIndex: 10,
                    transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                }}
                onClick={() => setLoginPanel('main')}
                title="密码登录"
                onMouseEnter={(e) => {
                    e.currentTarget.style.transform = 'scale(1.1)';
                    e.currentTarget.style.opacity = '0.8';
                }}
                onMouseLeave={(e) => {
                    e.currentTarget.style.transform = 'scale(1)';
                    e.currentTarget.style.opacity = '1';
                }}
            >
                <Monitor
                    size={32}
                    color="#9ca3af"
                    strokeWidth={1.5}
                />
            </div>

            <Flex vertical align="center" justify="center" style={{ height: '100%' }}>
                <Typography.Title level={3} style={{ margin: '0 0 8px 0', fontWeight: 600, color: '#111827' }}>
                    {t('扫码安全登录')}
                </Typography.Title>
                <Typography.Text style={{ fontSize: 14, color: '#6b7280', marginBottom: 32 }}>
                    {t('请使用 Atlas 移动端扫描二维码')}
                </Typography.Text>

                <div style={{ position: 'relative', width: 200, height: 200, marginBottom: 32 }}>
                    <QRCode
                        onClick={refreshQrCode}
                        value={qrCodeData.url || 'https://atlas.ys0921.sbs'}
                        status={
                            qrTicketLoading || getQrScanUrlLoading ? 'loading' :
                                (qrCodeData.status === 'EXPIRED' || !qrCodeData.url ? 'expired' : 'active')
                        }
                        onRefresh={refreshQrCode}
                        icon="/logo128_eclipse.svg"
                        size={200}
                        bordered={false}
                        color={qrCodeData.status === 'SCANNED' ? "rgba(17, 24, 39, 0.2)" : "#111827"} // 扫描后变灰
                        bgColor="#ffffff"
                    />

                    {/* 扫码成功后的半透明覆盖层 */}
                    <AnimatePresence>
                        {qrCodeData.status === 'SCANNED' && (
                            <motion.div
                                initial={{ opacity: 0, scale: 0.9 }}
                                animate={{ opacity: 1, scale: 1 }}
                                exit={{ opacity: 0 }}
                                style={{
                                    position: 'absolute',
                                    top: 0, left: 0, right: 0, bottom: 0,
                                    display: 'flex',
                                    flexDirection: 'column',
                                    alignItems: 'center',
                                    justifyContent: 'center',
                                    background: 'rgba(255, 255, 255, 0.85)',
                                    backdropFilter: 'blur(2px)',
                                    zIndex: 2
                                }}
                            >
                                <CheckCircleFilled style={{ fontSize: 32, color: '#10b981', marginBottom: 12 }} />
                                <Typography.Text strong style={{ fontSize: 16, color: '#111827' }}>
                                    扫描成功
                                </Typography.Text>
                                <Typography.Text type="secondary" style={{ fontSize: 13, marginTop: 4 }}>
                                    请在手机上点击确认
                                </Typography.Text>
                            </motion.div>
                        )}
                    </AnimatePresence>
                </div>
                <Typography.Text type="secondary" style={{ fontSize: 12, textAlign: 'center', maxWidth: 280 }}>
                    尚未安装 App？您可以尝试使用浏览器自带的扫码功能，或安装兼容的
                    <Typography.Link
                        href="https://chromewebstore.google.com/search/qr%20code%20scanner"
                        target="_blank"
                        style={{ color: '#4f46e5', margin: '0 4px' }}
                    >
                        扫码扩展
                    </Typography.Link>
                    进行验证。
                </Typography.Text>
            </Flex>
        </Card>
    )
}

export default QrLoginCard