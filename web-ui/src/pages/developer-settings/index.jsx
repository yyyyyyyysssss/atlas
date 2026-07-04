import { useState } from 'react';
import './index.css'
import { Flex, Tabs, theme, Typography } from "antd"
import { Blocks, CodeXml, Link } from 'lucide-react';
import { Outlet, useNavigate } from 'react-router-dom';


const DeveloperSettings = () => {

    const { token } = theme.useToken();

    const navigate = useNavigate();

    const [activeTab, setActiveTab] = useState('oauth2')

    return (
        <Flex
            justify="center"
            style={{ width: '100%', minHeight: '100%' }}
        >
            <div style={{
                width: '100%',
                backgroundColor: token.colorBgContainer,
                borderRadius: token.borderRadiusLG,
            }}>
                <Tabs
                    tabPosition="left"
                    style={{ height: '100%' }}
                    onChange={(key) => navigate(`/developer/settings/${key}`)}
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
                            children: <Outlet />,
                        }
                    ]}
                />
            </div>
        </Flex>
    )
}

export default DeveloperSettings