import React, { useRef, useState } from "react";
import './index.css';
import { List, theme, App, Card, Avatar, Typography, Tooltip, Flex, Button } from "antd";
import { useNavigate } from "react-router-dom";
import { useInfiniteScroll, useRequest } from "ahooks";
import { deleteOAuth2Client, getOAuth2ClientPage } from "../../../../../../services/ApplicationService";
import Loading from "../../../../../../components/loading";
import { AppstoreOutlined, DeleteOutlined, EditOutlined } from "@ant-design/icons";
import { useDomain } from "../../../../../../router/DomainProvider";
import useFullParams from "../../../../../../hooks/useFullParams";
import ClientListItem from "../../../components/ClientListItem";

const { Text } = Typography;
const { useToken } = theme;
const PAGE_SIZE = 10;

const OAuth2ClientApplicationList = () => {
    const { token } = useToken();
    const navigate = useNavigate();
    const scrollRef = useRef(null);
    const { message, modal } = App.useApp();

    const { applicationId } = useFullParams()

    const { runAsync: deleteOAuth2ClientAsync, loading: deleteOAuth2ClientLoading } = useRequest(deleteOAuth2Client, {
        manual: true
    });

    const { domainId } = useDomain()

    const {
        data,
        loading,
        loadingMore,
        noMore,
        mutate
    } = useInfiniteScroll(
        async (d) => {
            const pageNum = d ? d.nextPage : 1;
            const res = await getOAuth2ClientPage(domainId, applicationId, pageNum, PAGE_SIZE);
            const hasMore = pageNum * PAGE_SIZE < res.total;

            return {
                list: res.list,
                total: res.total,
                nextPage: hasMore ? pageNum + 1 : pageNum,
                hasMore: hasMore
            };
        },
        {
            target: scrollRef,
            isNoMore: (d) => !d?.hasMore,
        }
    );

    const clentList = data?.list || [];

    const handleDeleteOAuth2Client = (id, e) => {
        // 阻止点击删除按钮时触发 List.Item 的 onClick 路由跳转
        e?.stopPropagation();

        modal.confirm({
            title: '确定要删除该 OAuth2 应用吗？',
            content: '删除后，该应用分配的 Client Secret 将全部失效，已颁发的 Token 也会被强制注销，此操作不可逆！',
            okText: '确定删除',
            okType: 'danger',
            loading: deleteOAuth2ClientLoading,
            cancelText: '取消',
            onOk: async () => {
                await deleteOAuth2ClientAsync(domainId, applicationId, id);
                message.success('应用删除成功');
                if (data) {
                    mutate({
                        ...data,
                        total: data.total - 1,
                        list: data.list.filter(item => item.id !== id)
                    });
                }
            }
        });
    };

    return (
        <div
            ref={scrollRef}
            style={{
                height: 550,
                overflow: "auto",
                paddingRight: 8
            }}
        >
            {loading && clentList.length === 0 ? (
                <div style={{ padding: 20 }}>
                    <Loading spinning />
                </div>
            ) : (
                <>
                    <List
                        dataSource={clentList}
                        renderItem={(client) => (
                            <List.Item
                                key={client.id}
                                onClick={() => navigate(`/project/${domainId}/application/${applicationId}/client/oauth2/${client.id}`)}
                                style={{ cursor: "pointer", padding: "12px 0" }}
                            >
                                <div style={{ width: "100%" }}>
                                    <ClientListItem
                                        client={client}
                                        onDelete={(id, e) => handleDeleteOAuth2Client(id, e)}
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

                    {noMore && clentList.length > PAGE_SIZE && (
                        <div style={{ textAlign: "center", padding: 12, color: token.colorTextTertiary }}>
                            没有更多数据了
                        </div>
                    )}
                </>
            )}
        </div>
    );
};

export default OAuth2ClientApplicationList;