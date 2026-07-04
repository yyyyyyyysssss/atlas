import React, { useState } from "react";
import './index.css'
import { Typography, List, Button, Space, theme, Card, Tooltip, Flex, Avatar, Empty } from "antd";
import { PlusOutlined, ArrowLeftOutlined, EditOutlined, DeleteOutlined, AppstoreOutlined } from "@ant-design/icons";
import OAuth2ApplicationItem from "../components/OAuth2ApplicationItem";
import { useNavigate } from "react-router-dom";
import NoDataEmpty from "../../../components/NoDataEmpty";

const { Title, Paragraph, Text } = Typography;
const { useToken } = theme;

// 1. 模拟数据（加入系统的 logo 地址）
const mockApps = [
    {
        id: "1",
        name: "Atlas",
        clientId: '32b00b1e89af-90d2e0e46d20ebb92f6c',
        description: "使用Atlas的OAuth2客户端凭证进行身份验证和授权",
        logo: "/logo128.png",
        createTime: "2023-08-01 10:00:00",
    },
];

const OAuth2Application = () => {

    const { token } = useToken();

    const navigate = useNavigate();

    return (
        <div style={{ padding: `${token.paddingLG}px ${token.paddingXL}px` }}>
            <div>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-end", marginBottom: token.marginXL }}>
                    <div>
                        <Title level={4} style={{ margin: 0, fontWeight: 600 }}>OAuth2 应用</Title>
                        <Text type="secondary" style={{ fontSize: 13 }}>管理和配置您的 OAuth2 客户端凭证及访问权限</Text>
                    </div>
                    {mockApps.length > 0 && (
                        <Button type="primary" icon={<PlusOutlined />} onClick={() => navigate('/developer/settings/oauth2/application/create')}>
                            创建应用
                        </Button>
                    )}
                </div>
                {mockApps.length > 0 ? (
                    // 有数据时渲染列表
                    mockApps.map(app => (
                        <div key={app.id} onClick={() => navigate(`/developer/settings/oauth2/application/${app.id}`)} style={{ cursor: 'pointer' }}>
                            <OAuth2ApplicationItem oauth2App={app} />
                        </div>
                    ))
                ) : (
                    // 无数据时渲染 Empty 引导页
                    <div style={{ marginTop: 100 }}>
                        <NoDataEmpty
                            description="暂无 OAuth2 应用"
                        >
                            <Button type="primary" onClick={() => navigate('/developer/settings/oauth2/application/create')}>
                                立即创建OAuth2 应用
                            </Button>
                        </NoDataEmpty>
                    </div>
                )}
            </div>
        </div>
    );
};

export default OAuth2Application;