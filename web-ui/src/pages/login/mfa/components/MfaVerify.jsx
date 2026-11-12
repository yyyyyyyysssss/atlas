import React, { useState, useEffect, useRef, useMemo } from 'react';
import { Flex, Typography, ConfigProvider, App, Button } from 'antd';
import { ShieldAlert, ArrowLeft, KeyRound, Smartphone, Fingerprint, Grid3X3 } from 'lucide-react';
import { motion, AnimatePresence } from 'framer-motion';
import { useRequest } from 'ahooks';
import { Navigate, useNavigate } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { mfaLogin } from '../../../../services/LoginService';
import { useAuth } from '../../../../router/AuthProvider';
import { useRedirect } from '../../../../hooks/useRedirect';

// 🔌 引入具体策略核验组件
import UniversalTotpVerifier from '../../../account-settings/components/verifiers/UniversalTotpVerifier';
import UniversalBackupCodeVerifier from '../../../account-settings/components/verifiers/UniversalBackupCodeVerifier';
import UniversalGestureVerifier from '../../../account-settings/components/verifiers/UniversalGestureVerifier';

const { Title, Paragraph } = Typography;


const MfaVerify = ({ ticket, preferredMfaType, availableMfaTypes = [] }) => {

    const { t } = useTranslation();
    const navigate = useNavigate();
    const { signin } = useAuth();
    const redirect = useRedirect();

    // 🔄 当前激活的 MFA 核验类型（支持动态降级切换，如切换到备份码）
    const [activeMfaType, setActiveMfaType] = useState(preferredMfaType || availableMfaTypes[0])

    useEffect(() => {
        if (preferredMfaType) {
            setActiveMfaType(preferredMfaType);
        }
    }, [preferredMfaType]);

    const { runAsync: mfaLoginAsync } = useRequest(mfaLogin, { manual: true });

    // 🛡️ 统一的网络请求调用
    const doLogin = async (code) => {
        const credential = {
            code: code
        }
        return await mfaLoginAsync({
            ticket: ticket,
            mfaType: activeMfaType,
            credential: credential
        });
    };

    const loginSuccess = async (loginResponse) => {
        await signin()
        redirect('/')
    }

    const mfaStrategies = useMemo(() => {

        const hasBackupCode = availableMfaTypes.includes('BACKUP_CODE')

        const allPool = {
            TOTP: {
                icon: <ShieldAlert size={26} strokeWidth={2} />,
                title: t('安全两步验证'),
                description: t('您的账户已开启两步验证保护，请输入身份验证器生成的 6 位核验码。'),
                component: (
                    <UniversalTotpVerifier
                        onVerifyAction={doLogin}
                        onSuccess={loginSuccess}
                    />
                ),
                // 该策略下的辅助切换按钮
                actionButton: hasBackupCode && (
                    <Button
                        type="link"
                        size="small"
                        onClick={() => setActiveMfaType('BACKUP_CODE')}
                        style={{ fontSize: '13px', fontWeight: 500, color: '#4f46e5' }}
                    >
                        {t('丢失手机？尝试使用安全备份码登录')}
                    </Button>
                )
            },
            BACKUP_CODE: {
                icon: <KeyRound size={26} strokeWidth={2} />,
                title: t('使用备份码验证'),
                description: t('请输入您保存的 10 位安全备份码。每个备份码只能使用一次。'),
                component: (
                    <UniversalBackupCodeVerifier
                        codeLabel={t('安全备份码')}
                        onVerifyAction={doLogin}
                        onSuccess={loginSuccess}
                    />
                ),
                // 如果初始不是备份码，允许用户切回去
                actionButton: preferredMfaType  !== 'BACKUP_CODE' && (
                    <Button
                        type="link"
                        size="small"
                        icon={<Smartphone size={14} style={{ marginRight: 4 }} />}
                        onClick={() => setActiveMfaType(preferredMfaType )}
                        style={{ display: 'flex', alignItems: 'center', fontSize: '13px', fontWeight: 500, color: '#4f46e5' }}
                    >
                        {preferredMfaType === 'TOTP' ? t('返回常规两步验证') : t('返回手势轨迹核验')}
                    </Button>
                )
            },
            GESTURE: {
                icon: <Grid3X3 size={26} strokeWidth={2} />,
                title: t('手势轨迹核验'),
                description: t('请输入您的九宫格手势快捷密码完成安全二次身份验证。'),
                component: (
                    <UniversalGestureVerifier
                        label=''
                        onVerifyAction={doLogin}
                        onSuccess={loginSuccess}
                    />
                ),
                actionButton: hasBackupCode && (
                    <Button
                        type="link"
                        size="small"
                        onClick={() => setActiveMfaType('BACKUP_CODE')}
                        style={{ fontSize: '13px', fontWeight: 500, color: '#4f46e5' }}
                    >
                        {t('忘记手势？尝试使用安全备份码登录')}
                    </Button>
                )
            }
        }
        // 根据后端返回的可用列表，对策略表进行动态裁剪
        const filteredStrategies = {}
        availableMfaTypes.forEach(key => {
            if (allPool[key]) {
                filteredStrategies[key] = allPool[key]
            }
        })
        return filteredStrategies
    }, [availableMfaTypes, preferredMfaType, t])

    // 获取当前处于激活状态的策略元数据，提供兜底保护
    const currentStrategy = mfaStrategies[activeMfaType] || mfaStrategies['TOTP'];

    return (
        <AnimatePresence mode="wait">
            <motion.div
                key={activeMfaType}
                initial={{ opacity: 0, x: 15 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: -15 }}
                transition={{ duration: 0.2 }}
            >
                {/* 统一的头部 UI 结构 */}
                <Flex vertical align="center" style={{ marginBottom: 32, textAlign: 'center' }}>
                    <div style={{ width: '56px', height: '56px', borderRadius: '14px', background: 'rgba(79, 70, 229, 0.06)', display: 'flex', alignItems: 'center', justifyContent: 'center', color: '#4f46e5', marginBottom: 16 }}>
                        {currentStrategy.icon}
                    </div>
                    <Title level={3} style={{ margin: 0, fontSize: '24px', fontWeight: 700, color: '#1f2937', letterSpacing: '-0.025em' }}>
                        {currentStrategy.title}
                    </Title>
                    <Paragraph style={{ color: '#6b7280', fontSize: '14px', marginTop: 8, marginBottom: 0, padding: '0 8px' }}>
                        {currentStrategy.description}
                    </Paragraph>
                </Flex>

                {/* 核心表单组件 */}
                <div style={{ minHeight: '86px' }}>
                    {currentStrategy.component}
                </div>
                <Flex justify='center' align='center' style={{ marginTop: '20px' }}>
                    {currentStrategy.actionButton}
                </Flex>
            </motion.div>
        </AnimatePresence>
    )
}

export default MfaVerify