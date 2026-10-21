import React, { useEffect, useRef, useState } from 'react';
import { Button, Typography, theme, List, Flex, Tag, App, Alert, Input, Tooltip, Space } from 'antd';
import {
    LockOutlined, MailOutlined, MobileOutlined,
    FileProtectOutlined, GoogleOutlined, GithubOutlined,
    UserOutlined,
    InfoCircleOutlined
} from '@ant-design/icons';
import { Fingerprint } from 'lucide-react';
import { useRequest } from 'ahooks';
import { fetchAccountSecurity } from '../../services/AccountService';
import Loading from '../../components/loading';
import UsernameItem from './components/UsernameItem';
import PasswordItem from './components/PasswordItem';
import EmailItem from './components/EmailItem';
import PasskeyItem from './components/PasskeyItem';
import UserProviderItem from './components/UserProviderItem';
import TotpSecurityItem from './components/TotpSecurityItem';
import BackupCodeSecurityItem from './components/BackupCodeSecurityItem';
import GestureItem from './components/GestureItem';
import WalletItem from './components/WalletItem';
import useFullParams from '../../hooks/useFullParams';

const { Title, Text, Paragraph } = Typography;

const SecurityTab = () => {
    const { token } = theme.useToken()

    const { section } = useFullParams()

    const { data, loading, refresh } = useRequest(fetchAccountSecurity)

    const sectionRefs = useRef({})

    const setSectionRef = (section) => (element) => {
        sectionRefs.current[section] = element
    }

    useEffect(() => {
        if (loading || !data || !section) {
            return;
        }
        const target = sectionRefs.current[section]
        target?.scrollIntoView({
            behavior: 'smooth',
            block: 'start',
        })
    }, [loading, data, section])

    return (
        <Loading spinning={loading || !data} tip="正在载入安全设置...">
            <div style={{ width: '100%' }}>
                <Title level={4} style={{ marginBottom: 8, color: token.colorTextHeading }}>登录与绑定</Title>
                <Paragraph type="secondary" style={{ marginBottom: 24 }}>
                    管理您可以用来登录 Atlas 账号的凭证和第三方服务。
                </Paragraph>
                {/* 系统账号组件 */}
                <div ref={setSectionRef('username')} style={{ scrollMarginTop: 80 }}>
                    <UsernameItem
                        context={data}
                        refresh={refresh}
                    />
                </div>

                {/* 登录密码组件 */}
                <div ref={setSectionRef('password')} style={{ scrollMarginTop: 80 }}>
                    <PasswordItem
                        context={data}
                        refresh={refresh}
                    />
                </div>

                {/* 电子邮箱组件 */}
                <div ref={setSectionRef('email')} style={{ scrollMarginTop: 80 }}>
                    <EmailItem
                        context={data}
                        refresh={refresh}
                    />
                </div>

                {/* 通行密钥 */}
                <div ref={setSectionRef('passkey')} style={{ scrollMarginTop: 80 }}>
                    <PasskeyItem
                        context={data}
                        refresh={refresh}
                    />
                </div>

                {/* 加密钱包 */}
                <div ref={setSectionRef('wallet')} style={{ scrollMarginTop: 80 }}>
                    <WalletItem
                        context={data}
                        refresh={refresh}
                    />
                </div>



                <Title level={4} style={{ marginBottom: 8, marginTop: 48, color: token.colorTextHeading }}>第三方账号</Title>
                <Paragraph type="secondary" style={{ marginBottom: 24 }}>
                    关联第三方账号，实现一键快捷登录，同时为账号提供多渠道的身份验证保护。
                </Paragraph>

                <div ref={setSectionRef('providers')} style={{ scrollMarginTop: 80 }}>
                    <UserProviderItem
                        context={data}
                        refresh={refresh}
                    />
                </div>





                <Title level={4} style={{ marginBottom: 8, marginTop: 48, color: token.colorTextHeading }}>多重身份验证 (MFA)</Title>
                <Paragraph type="secondary" style={{ marginBottom: 24 }}>
                    为您的账号增加一层额外的安全保护。首选身份验证器；若未绑定，将启用手势密码作为次选的二次校验手段。
                </Paragraph>

                <div ref={setSectionRef('totp')} style={{ scrollMarginTop: 80 }}>
                    <TotpSecurityItem
                        context={data}
                        refresh={refresh}
                    />
                </div>

                <div ref={setSectionRef('gesture')} style={{ scrollMarginTop: 80 }}>
                    <GestureItem
                        context={data}
                        refresh={refresh}
                    />
                </div>

                <div ref={setSectionRef('backupCode')} style={{ scrollMarginTop: 80 }}>
                    <BackupCodeSecurityItem
                        context={data}
                        refresh={refresh}
                    />
                </div>
            </div>
        </Loading>
    );
};

export default SecurityTab;