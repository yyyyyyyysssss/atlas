import { useState } from 'react';
import './index.css';
import { Flex, Tabs, theme } from "antd";
import { CodeXml, FileKey, KeyRound } from 'lucide-react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useDomain } from '../../../../router/DomainProvider';
import useFullParams from '../../../../hooks/useFullParams';


const ProjectApplicationClient = () => {

    const { applicationId } = useFullParams()

    const { token } = theme.useToken()

    const navigate = useNavigate()

    const location = useLocation()

    const { domainId } = useDomain()

    const activeKey = location.pathname
        .split('/client/')[1]
        ?.split('/')[0] || 'oauth2'

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
                    activeKey={activeKey}
                    tabPosition="left"
                    style={{
                        height: '100%',
                        flexShrink: 0
                    }}
                    onChange={(key) => navigate(`/project/${domainId}/application/${applicationId}/client/${key}`)}
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
                                    <KeyRound size={16} />
                                    <span style={{ fontSize: 15 }}>OAuth2 客户端</span>
                                </Flex>
                            ),
                        },
                        {
                            key: 'saml2',
                            label: (
                                <Flex align="center" gap={12} style={{ padding: '4px 8px' }}>
                                    <FileKey size={16} />
                                    <span style={{ fontSize: 15 }}>Saml2 客户端</span>
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

export default ProjectApplicationClient