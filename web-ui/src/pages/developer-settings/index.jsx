import { useState } from 'react';
import './index.css'
import { Flex, Tabs, theme, Typography } from "antd"
import { Blocks, CodeXml, Link } from 'lucide-react';
import OAuth2ApplicationTab from './components/OAuth2ApplicationTab';


const DeveloperSettings = () => {

    const { token } = theme.useToken();

    const [activeTab, setActiveTab] = useState('oauth2')

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
                            key: 'oauth2',
                            label: (
                                <Flex align="center" gap={12} style={{ padding: '4px 8px' }}>
                                    <CodeXml size={16} />
                                    <span style={{ fontSize: 15 }}>OAuth2 应用</span>
                                </Flex>
                            ),
                            children: <div style={{ padding: '32px 48px' }}>
                                <OAuth2ApplicationTab />
                            </div>,
                        }
                    ]}
                />
            </div>
        </Flex>
    )
}

export default DeveloperSettings