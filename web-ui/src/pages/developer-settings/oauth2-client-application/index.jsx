import React, { useRef, useState } from "react";
import './index.css'
import { Typography, List, Button, Space, theme, Card, Tooltip, Flex, Avatar, Empty, App } from "antd";
import { PlusOutlined, ArrowLeftOutlined, EditOutlined, DeleteOutlined, AppstoreOutlined } from "@ant-design/icons";
import OAuth2ClientApplicationItem from "../components/OAuth2ClientApplicationItem";
import { useNavigate } from "react-router-dom";
import NoDataEmpty from "../../../components/NoDataEmpty";
import { useInfiniteScroll, useRequest } from "ahooks";
import { deleteApplication, getApplicationPage } from "../../../services/DeveloperSettingsService";
import Loading from "../../../components/loading";

const { Title, Paragraph, Text } = Typography;
const { useToken } = theme;

const PAGE_SIZE = 10;

const OAuth2ClientApplication = () => {

    const { token } = useToken();

    const navigate = useNavigate();

    const [pagination, setPagination] = useState({ pageNum: 1, pageSize: 10 });

    const scrollRef = useRef(null);

    const { message, modal } = App.useApp()

    const { runAsync: deleteApplicationAsync, loading: deleteApplicationLoading } = useRequest(deleteApplication, {
        manual: true
    })

    const {
        data,
        loading,
        loadingMore,
        noMore,
        mutate
    } = useInfiniteScroll(
        async (d) => {
            const pageNum = d ? d.nextPage : 1;

            const res = await getApplicationPage(pageNum, PAGE_SIZE);

            const hasMore = pageNum * PAGE_SIZE < res.total

            return {
                list: d?.list ? [...d.list, ...res.list] : res.list,
                total: res.total,
                nextPage: hasMore ? pageNum + 1 : pageNum,
                hasMore: hasMore
            };
        },
        {
            target: scrollRef,
            isNoMore: (d) => !d?.hasMore,
        }
    )

    const appList = data?.list || []

    const createNewApplication = () => {
        navigate('/developer/settings/oauth2/application/create')
    }

    const handleDeleteApplication = (id) => {
        modal.confirm({
            title: '确定要删除该 OAuth2 应用吗？',
            content: '删除后，该应用分配的 Client Secret 将全部失效，已颁发的 Token 也会被强制注销，此操作不可逆！',
            okText: '确定删除',
            okType: 'danger', // 红色高亮警告按钮
            loading: deleteApplicationLoading,
            cancelText: '取消',
            onOk: async () => {
                await deleteApplicationAsync(id)
                message.success('应用删除成功')
                if (data) {
                    mutate({
                        ...data,
                        total: data.total - 1,
                        list: data.list.filter(item => item.id !== id)
                    });
                }
            }
        });
    }

    return (
        <div style={{ padding: `${token.paddingLG}px ${token.paddingXL}px` }}>
            <div>
                <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-end", marginBottom: token.marginXL }}>
                    <div>
                        <Title level={4} style={{ margin: 0, fontWeight: 600 }}>OAuth2 应用</Title>
                        <Text type="secondary" style={{ fontSize: 13 }}>管理和配置您的 OAuth2 客户端凭证及访问权限</Text>
                    </div>
                    <Button type="primary" icon={<PlusOutlined />} onClick={createNewApplication}>
                        创建应用
                    </Button>
                </div>
                <div
                    ref={scrollRef}
                    style={{
                        height: 550,
                        overflow: "auto",
                        paddingRight: 8
                    }}
                >
                    {loading && appList.length === 0 ? (
                        <div style={{ padding: 20 }}>
                            <Loading spinning />
                        </div>
                    ) : (
                        <>
                            <List
                                dataSource={appList}
                                renderItem={(app) => (
                                    <List.Item
                                        key={app.id}
                                        onClick={() =>
                                            navigate(`/developer/settings/oauth2/application/${app.id}`)
                                        }
                                        style={{ cursor: "pointer", padding: "12px 0" }}
                                    >
                                        <div style={{ width: "100%" }}>
                                            <OAuth2ClientApplicationItem
                                                oauth2App={app}
                                                onDelete={handleDeleteApplication}
                                            />
                                        </div>
                                    </List.Item>
                                )}
                            />

                            {loadingMore && (
                                <div style={{ textAlign: "center", padding: 12 }}>
                                    <Loading spinning size="small" />
                                </div>
                            )}

                            {noMore && appList.length > PAGE_SIZE && (
                                <div style={{ textAlign: "center", padding: 12, color: token.colorTextTertiary }}>
                                    没有更多数据了
                                </div>
                            )}
                        </>
                    )}
                </div>
            </div>
        </div>
    );
};

export default OAuth2ClientApplication;