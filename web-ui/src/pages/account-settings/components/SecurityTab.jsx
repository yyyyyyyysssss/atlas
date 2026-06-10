import React, { useEffect, useState } from 'react';
import { Button, Typography, theme, List, Flex, Tag, App, Alert, Input, Tooltip, Space } from 'antd';
import {
    LockOutlined, MailOutlined, MobileOutlined,
    FileProtectOutlined, GoogleOutlined, GithubOutlined,
    UserOutlined,
    InfoCircleOutlined
} from '@ant-design/icons';
import { Fingerprint } from 'lucide-react';
import { useRequest } from 'ahooks';
import { fetchAccountSecurity } from '../../../services/AccountService';
import Loading from '../../../components/loading';
import UsernameItem from './UsernameItem';
import PasswordItem from './PasswordItem';
import EmailItem from './EmailItem';
import PasskeyItem from './PasskeyItem';
import UserProviderItem from './UserProviderItem';
import TotpSecurityItem from './TotpSecurityItem';
import BackupCodeSecurityItem from './BackupCodeSecurityItem';
import GestureItem from './GestureItem';
import WalletItem from './WalletItem';

const { Title, Text, Paragraph } = Typography;

const SecurityTab = () => {
    const { token } = theme.useToken()

    const { data, loading, refresh } = useRequest(fetchAccountSecurity)

    const {
        username,
        isUsernameModified,
        passwordSet,
        passkeyBound,
        boundEmail,
        emailVerified,
        boundPhone,
        phoneVerified,
        providers,
        totpEnabled,
        recoveryCodeGenerated
    } = data || {}

    return (
        <Loading spinning={loading || !data} tip="正在载入安全设置...">
            <div style={{ width: '100%' }}>
                <Title level={4} style={{ marginBottom: 8, color: token.colorTextHeading }}>登录与绑定</Title>
                <Paragraph type="secondary" style={{ marginBottom: 24 }}>
                    管理您可以用来登录 Atlas 账号的凭证和第三方服务。
                </Paragraph>
                {/* 系统账号组件 */}
                <UsernameItem
                    username={username}
                    isUsernameModified={isUsernameModified}
                    refresh={refresh}
                />
                {/* 登录密码组件 */}
                <PasswordItem
                    context={data}
                    refresh={refresh}
                />
                {/* 电子邮箱组件 */}
                <EmailItem
                    context={data}
                    refresh={refresh}
                />
                {/* 通行密钥 */}
                <PasskeyItem
                    context={data}
                    refresh={refresh}
                />

                {/* 加密钱包 */}
                <WalletItem
                    context={data}
                    refresh={refresh}
                />



                <Title level={4} style={{ marginBottom: 8, marginTop: 48, color: token.colorTextHeading }}>第三方账号</Title>
                <Paragraph type="secondary" style={{ marginBottom: 24 }}>
                    关联第三方账号，实现一键快捷登录，同时为账号提供多渠道的身份验证保护。
                </Paragraph>
                <UserProviderItem
                    providers={providers}
                    refresh={refresh}
                />





                <Title level={4} style={{ marginBottom: 8, marginTop: 48, color: token.colorTextHeading }}>多重身份验证 (MFA)</Title>
                <Paragraph type="secondary" style={{ marginBottom: 24 }}>
                    为您的账号增加一层额外的安全保护。首选身份验证器；若未绑定，将启用手势密码作为次选的二次校验手段。
                </Paragraph>

                <TotpSecurityItem
                    context={data}
                    refresh={refresh}
                />

                <GestureItem
                    context={data}
                    refresh={refresh}
                />

                <BackupCodeSecurityItem
                    context={data}
                    refresh={refresh}
                />
            </div>
        </Loading>
    );
};

export default SecurityTab;