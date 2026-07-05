import React, { useState } from "react";
import './index.css'
import { Typography, List, Button, Space, theme, Card, Tooltip, Flex, Avatar, Empty } from "antd";
import { PlusOutlined, ArrowLeftOutlined, EditOutlined, DeleteOutlined, AppstoreOutlined } from "@ant-design/icons";
import OAuth2ApplicationItem from "../components/OAuth2ApplicationItem";
import { useNavigate } from "react-router-dom";
import NoDataEmpty from "../../../components/NoDataEmpty";
import { useRequest } from "ahooks";
import { getApplicationPage } from "../../../services/DeveloperSettingsService";

const { Title, Paragraph, Text } = Typography;
const { useToken } = theme;

const OAuth2Application = () => {

    const { token } = useToken();

    const navigate = useNavigate();

    const [pagination, setPagination] = useState({ pageNum: 1, pageSize: 10 });

    const { data: pageResult, loading: pageLoading, run: refreshPage } = useRequest(
        () => getApplicationPage(pagination.pageNum, pagination.pageSize),
        {
            refreshDeps: [pagination],
            manual: false,
        }
    );

    const appList = pageResult?.list || []

    const total = pageResult?.total || 0

    const createNewApplication = () => {
        navigate('/developer/settings/oauth2/application/create')
    }

    return (
        <div style={{ padding: `${token.paddingLG}px ${token.paddingXL}px` }}>
            <div>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-end", marginBottom: token.marginXL }}>
                    <div>
                        <Title level={4} style={{ margin: 0, fontWeight: 600 }}>OAuth2 应用</Title>
                        <Text type="secondary" style={{ fontSize: 13 }}>管理和配置您的 OAuth2 客户端凭证及访问权限</Text>
                    </div>
                    {appList.length > 0 && (
                        <Button type="primary" icon={<PlusOutlined />} onClick={createNewApplication}>
                            创建应用
                        </Button>
                    )}
                </div>
                {appList.length > 0 ? (
                    // 有数据时渲染列表
                    appList.map(app => (
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