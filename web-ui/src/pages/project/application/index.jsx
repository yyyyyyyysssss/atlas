import { useState } from 'react';
import './index.css';
import { Flex, Tabs, theme } from "antd";
import { CodeXml } from 'lucide-react';
import { Outlet, useNavigate } from 'react-router-dom';
import { useDomain } from '../../../router/DomainProvider';



const ProjectApplication = () => {

    const { token } = theme.useToken()

    const navigate = useNavigate()

    const { domainId } = useDomain()

    return (
        <Flex
            justify="center"
            style={{ width: '100%', minHeight: '100%' }}
        >
            <div style={{
                width: '100%',
                backgroundColor: token.colorBgContainer,
                borderRadius: token.borderRadiusLG,
                display: 'flex',
                flexDirection: 'row',
                alignItems: 'stretch',
                minHeight: '550px'
            }}>
                <Tabs
                    tabPosition="left"
                    style={{
                        height: '100%',
                        flexShrink: 0
                    }}
                    onChange={(key) => navigate(`/project/${domainId}/application/${key}`)}
                    tabBarStyle={{
                        width: 240,
                        padding: '24px 0',
                        borderRight: `1px solid ${token.colorBorderSecondary}`,
                        height: '100%'
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
                        }
                    ]}
                />

                {/* 右侧内容区域 */}
                <div
                    style={{
                        flex: 1,
                        minWidth: 0,
                        padding: '8px'
                    }}
                >
                    <Outlet />
                </div>
            </div>
        </Flex>
    );
}

export default ProjectApplication