import { Typography, Button, theme } from "antd";
import './index.css'
import { Outlet, useLocation, useNavigate, useOutletContext } from 'react-router-dom'
import { PlusOutlined } from "@ant-design/icons";
import { useDomain } from '../../../../../router/DomainProvider'
import useFullParams from "../../../../../hooks/useFullParams";

const { Title, Text } = Typography

const { useToken } = theme;

const Saml2ClientApplication = () => {

    const { token } = useToken()

    const navigate = useNavigate()

    const { domainId } = useDomain()

    const location = useLocation();

    const { applicationId } = useFullParams()

    const outletContext = useOutletContext()

    const isCreatePage = location.pathname.endsWith("/create");

    const createNewApplication = () => {
        navigate(`/project/${domainId}/application/${applicationId}/client/saml2/create`)
    };

    return (
        <div style={{ padding: `${token.paddingLG}px ${token.paddingXL}px` }}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-end", marginBottom: token.marginXL }}>
                <div>
                    <Title level={4} style={{ margin: 0, fontWeight: 600 }}>Saml2 客户端</Title>
                    <Text type="secondary" style={{ fontSize: 13 }}>管理和配置您的 Saml2 客户端凭证</Text>
                </div>
                {!isCreatePage && (
                    <Button type="primary" icon={<PlusOutlined />} onClick={createNewApplication}>
                        创建客户端
                    </Button>
                )}
            </div>
            <Outlet context={outletContext} />
        </div>
    )
}

export default Saml2ClientApplication