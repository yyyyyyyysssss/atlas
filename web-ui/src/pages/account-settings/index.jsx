import React, { useState } from 'react';
import { Tabs, Flex, theme } from 'antd';
import { UserOutlined, SafetyCertificateOutlined, SettingOutlined } from '@ant-design/icons';
import ProfileTab from './components/ProfileTab';
import SecurityTab from './components/SecurityTab';
import PreferencesTab from './components/PreferencesTab';

const AccountSettings = () => {
    const { token } = theme.useToken();
    const [activeTab, setActiveTab] = useState('profile')
    return (
        <Flex justify="center" style={{ width: '100%', minHeight: '100%' }}>
            <div style={{
                width: '100%',
                backgroundColor: token.colorBgContainer,
                borderRadius: token.borderRadiusLG,
            }}>
                <Tabs
                    tabPosition="left"
                    activeKey={activeTab}
                    onChange={setActiveTab}
                    style={{ height: '100%' }}
                    tabBarStyle={{
                        width: 240,
                        padding: '24px 0',
                        borderRight: `1px solid ${token.colorBorderSecondary}`
                    }}
                    items={[
                        {
                            key: 'profile',
                            label: (
                                <Flex align="center" gap={12} style={{ padding: '4px 8px' }}>
                                    <UserOutlined style={{ fontSize: 16 }} />
                                    <span style={{ fontSize: 15 }}>个人资料</span>
                                </Flex>
                            ),
                            children: <div style={{ padding: '32px 48px' }}><ProfileTab onNavigateToSecurity={() => setActiveTab('security')} /></div>,
                        },
                        {
                            key: 'security',
                            label: (
                                <Flex align="center" gap={12} style={{ padding: '4px 8px' }}>
                                    <SafetyCertificateOutlined style={{ fontSize: 16 }} />
                                    <span style={{ fontSize: 15 }}>安全设置</span>
                                </Flex>
                            ),
                            children: <div style={{ padding: '32px 48px' }}><SecurityTab /></div>,
                        },
                        {
                            key: 'preferences',
                            label: (
                                <Flex align="center" gap={12} style={{ padding: '4px 8px' }}>
                                    <SettingOutlined style={{ fontSize: 16 }} />
                                    <span style={{ fontSize: 15 }}>偏好设置</span>
                                </Flex>
                            ),
                            children: <div style={{ padding: '32px 48px' }}><PreferencesTab /></div>,
                        }
                    ]}
                />
            </div>
        </Flex>
    );
};

export default AccountSettings;