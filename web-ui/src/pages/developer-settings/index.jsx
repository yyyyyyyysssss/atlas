import { useState } from 'react';
import './index.css';
import { Flex, Tabs, theme } from "antd";
import { CodeXml } from 'lucide-react';
import { Outlet, useNavigate } from 'react-router-dom';

const DeveloperSettings = () => {
    const { token } = theme.useToken();
    const navigate = useNavigate();

    return (
        <Flex
            justify="center"
            style={{ width: '100%', minHeight: '100%' }}
        >
            <div style={{
                width: '100%',
                backgroundColor: token.colorBgContainer,
                borderRadius: token.borderRadiusLG,
                // ======= ✨ 关键修改：将此容器设为横向 Flex 布局 =======
                display: 'flex', 
                flexDirection: 'row',
                alignItems: 'stretch', // 让左右两侧高度拉伸一致
                minHeight: '550px'     // 给一个最小高度，防止内容为空时高度塌陷
            }}>
                <Tabs
                    tabPosition="left"
                    style={{ 
                        height: '100%',
                        // ======= 确保 Tabs 不会收缩 =======
                        flexShrink: 0 
                    }}
                    onChange={(key) => navigate(`/developer/settings/${key}`)}
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
                        // ======= ✨ 关键修改：使右侧区域自动撑满剩余宽度 =======
                        flex: 1,
                        minWidth: 0, // 防止 flex 子项溢出
                        padding: '8px' // 适当给一点内部间距
                    }}
                >
                    <Outlet />
                </div>
            </div>
        </Flex>
    );
};

export default DeveloperSettings;