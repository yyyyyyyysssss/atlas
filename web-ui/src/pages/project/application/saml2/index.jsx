import { Typography, Button, theme } from "antd";
import './index.css'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { PlusOutlined } from "@ant-design/icons";
import { useDomain } from '../../../../router/DomainProvider'

const { Title, Text } = Typography

const { useToken } = theme;

const Saml2ClientApplication = () => {

    const { token } = useToken()

    const navigate = useNavigate()

    const { domainId } = useDomain()

    const location = useLocation();

    const isCreatePage = location.pathname.endsWith("/create");

    const createNewApplication = () => {
        navigate(`/project/${domainId}/application/saml2/create`)
    };

    return (
        <div style={{ padding: `${token.paddingLG}px ${token.paddingXL}px` }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-end", marginBottom: token.marginXL }}>
                <div>
                    <Title level={4} style={{ margin: 0, fontWeight: 600 }}>Saml2 应用</Title>
                    <Text type="secondary" style={{ fontSize: 13 }}>管理和配置您的 Saml2 客户端凭证</Text>
                </div>
                {!isCreatePage && (
                    <Button type="primary" icon={<PlusOutlined />} onClick={createNewApplication}>
                        创建应用
                    </Button>
                )}
            </div>
            <Outlet />
        </div>
    )
}

export default Saml2ClientApplication