import React from "react";
import { Typography, Button, theme } from "antd";
import { PlusOutlined } from "@ant-design/icons";
import { Outlet, useNavigate } from "react-router-dom";
import { useDomain } from "../../../../router/DomainProvider";

const { Title, Text } = Typography;
const { useToken } = theme;

const OAuth2ClientApplication = () => {
    const { token } = useToken()

    const navigate = useNavigate()

    const { domainId } = useDomain()

    const createNewApplication = () => {
        navigate(`/project/${domainId}/application/oauth2/create`)
    };

    return (
        <div style={{ padding: `${token.paddingLG}px ${token.paddingXL}px` }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-end", marginBottom: token.marginXL }}>
                <div>
                    <Title level={4} style={{ margin: 0, fontWeight: 600 }}>OAuth2 应用</Title>
                    <Text type="secondary" style={{ fontSize: 13 }}>管理和配置您的 OAuth2 客户端凭证及访问权限</Text>
                </div>
                <Button type="primary" icon={<PlusOutlined />} onClick={createNewApplication}>
                    创建应用
                </Button>
            </div>
            {/* 子路由（列表页、创建页、编辑页）将在这里渲染 */}
            <Outlet />
        </div>
    );
};

export default OAuth2ClientApplication;