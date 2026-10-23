import React from "react";
import { Typography, Button, theme } from "antd";
import { PlusOutlined } from "@ant-design/icons";
import { Outlet, useLocation, useNavigate } from "react-router-dom";
import { useDomain } from "../../../../../router/DomainProvider";
import useFullParams from "../../../../../hooks/useFullParams";

const { Title, Text } = Typography;
const { useToken } = theme;

const OAuth2ClientApplication = () => {
    const { token } = useToken()

    const navigate = useNavigate()

    const { domainId } = useDomain()

    const { applicationId } = useFullParams()

    const location = useLocation();

    const isCreatePage = location.pathname.endsWith("/create");

    const createNewApplication = () => {
        navigate(`/project/${domainId}/application/${applicationId}/client/oauth2/create`)
    };

    return (
        <div style={{ padding: `${token.paddingLG}px ${token.paddingXL}px` }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-end", marginBottom: token.marginXL }}>
                <div>
                    <Title level={4} style={{ margin: 0, fontWeight: 600 }}>OAuth2 客户端</Title>
                    <Text type="secondary" style={{ fontSize: 13 }}>管理和配置您的 OAuth2 客户端凭证及访问权限</Text>
                </div>
                {!isCreatePage && (
                    <Button type="primary" icon={<PlusOutlined />} onClick={createNewApplication}>
                        创建客户端
                    </Button>
                )}
            </div>
            <Outlet />
        </div>
    );
};

export default OAuth2ClientApplication;