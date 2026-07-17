import React, { useRef, useState } from "react";
import './index.css';
import { List, theme, App, Card, Avatar, Typography, Tooltip, Flex, Button } from "antd";
import { useNavigate } from "react-router-dom";
import { useInfiniteScroll, useRequest } from "ahooks";
import { deleteApplication, getApplicationPage } from "../../../../../services/DeveloperSettingsService";
import Loading from "../../../../../components/loading";
import { AppstoreOutlined, DeleteOutlined, EditOutlined } from "@ant-design/icons";
import { useDomain } from "../../../../../router/DomainProvider";

const { Text } = Typography;
const { useToken } = theme;
const PAGE_SIZE = 10;

const OAuth2ClientApplicationList = () => {
    const { token } = useToken();
    const navigate = useNavigate();
    const scrollRef = useRef(null);
    const { message, modal } = App.useApp();

    const { runAsync: deleteApplicationAsync, loading: deleteApplicationLoading } = useRequest(deleteApplication, {
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
            const res = await getApplicationPage(pageNum, PAGE_SIZE);
            const hasMore = pageNum * PAGE_SIZE < res.total;

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
    );

    const appList = data?.list || [];

    const handleDeleteApplication = (id, e) => {
        // 阻止点击删除按钮时触发 List.Item 的 onClick 路由跳转
        e?.stopPropagation(); 
        
        modal.confirm({
            title: '确定要删除该 OAuth2 应用吗？',
            content: '删除后，该应用分配的 Client Secret 将全部失效，已颁发的 Token 也会被强制注销，此操作不可逆！',
            okText: '确定删除',
            okType: 'danger',
            loading: deleteApplicationLoading,
            cancelText: '取消',
            onOk: async () => {
                await deleteApplicationAsync(id);
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
                                onClick={() => navigate(`/project/${domainId}/application/oauth2/${app.id}`)}
                                style={{ cursor: "pointer", padding: "12px 0" }}
                            >
                                <div style={{ width: "100%" }}>
                                    <OAuth2ClientApplicationItem
                                        oauth2App={app}
                                        onDelete={(id, e) => handleDeleteApplication(id, e)}
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
    );
};

export default OAuth2ClientApplicationList;



const OAuth2ClientApplicationItem = ({ oauth2App, onDelete }) => {
    const { token } = useToken();
    const { message } = App.useApp();
    const [hovered, setHovered] = useState(false);

    const handleCopy = (text) => {
        navigator.clipboard.writeText(text);
        message.success("Client ID 已复制");
    };

    return (
        <Card
            size="small"
            variant="borderless"
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            styles={{
                body: {
                    padding: 14
                }
            }}
            style={{
                borderRadius: token.borderRadiusLG,
                background: hovered
                    ? token.colorBgTextHover
                    : token.colorBgContainer,

                transition: "all 0.2s ease",
                cursor: "pointer"
            }}
        >
            <Flex justify="space-between" align="center">

                {/* LEFT */}
                <Flex gap={12} align="center" style={{ flex: 1 }}>

                    <Avatar
                        size={44}
                        shape="square"
                        src={oauth2App.logoUrl}
                        icon={<AppstoreOutlined />}
                        style={{ borderRadius: 10 }}
                    />

                    <Flex vertical style={{ flex: 1 }} gap={4}>

                        {/* title row */}
                        <Flex justify="space-between" align="center">
                            <Text
                                strong
                                style={{
                                    fontSize: token.fontSizeLG,
                                    color: token.colorText
                                }}
                            >
                                {oauth2App.applicationName}
                            </Text>
                        </Flex>

                        {/* meta row */}
                        <Flex gap={10} align="center" wrap="wrap">
                            <Tooltip title={oauth2App.description || "暂无描述"}>
                                <Text
                                    type="secondary"
                                    style={{
                                        fontSize: token.fontSizeSM,
                                        maxWidth: 200,
                                        overflow: "hidden",
                                        textOverflow: "ellipsis",
                                        whiteSpace: "nowrap",
                                        display: "inline-block"
                                    }}
                                >
                                    {oauth2App.description || "暂无描述"}
                                </Text>
                            </Tooltip>

                            <Text
                                type="secondary"
                                style={{
                                    fontSize: token.fontSizeSM,
                                }}
                            >
                                |
                            </Text>

                            <Text
                                type="secondary"
                                style={{
                                    fontSize: token.fontSizeSM,
                                }}
                            >
                                客户端 ID:
                            </Text>

                            <Text
                                style={{
                                    fontSize: token.fontSizeSM,
                                    fontFamily: "monospace",
                                    cursor: "pointer",
                                    color: token.colorText,
                                    background: token.colorFillQuaternary,
                                    padding: "0 6px",
                                    borderRadius: 4
                                }}
                                onClick={(e) => {
                                    e.stopPropagation();
                                    handleCopy(oauth2App.clientId);
                                }}
                            >
                                {oauth2App.clientId}
                            </Text>
                        </Flex>

                    </Flex>
                </Flex>

                {/* RIGHT ACTION */}
                <div style={{ position: "relative", minWidth: 60 }}>
                    <Text
                        type="secondary"
                        style={{
                            fontSize: 11,
                            position: "absolute",
                            top: -6,
                            right: 70,
                            whiteSpace: "nowrap",
                            opacity: 0.75
                        }}
                    >
                        创建于：{oauth2App.createTime}
                    </Text>

                    <div style={{ display: "flex", justifyContent: "flex-end", alignItems: "center", height: 44 }}>
                        <Tooltip title='编辑 OAuth2 客户端'>
                            <Button
                                type="text"
                                icon={<EditOutlined />}
                                style={{
                                    opacity: hovered ? 1 : 0.5
                                }}
                            />
                        </Tooltip>
                        <Tooltip title='删除 OAuth2 客户端'>
                            <Button
                                type="text"
                                icon={<DeleteOutlined />}
                                onClick={(e) => {
                                    e.stopPropagation()
                                    onDelete?.(oauth2App.id)
                                }}
                                style={{
                                    opacity: hovered ? 1 : 0.5
                                }}
                            />
                        </Tooltip>
                    </div>
                </div>
            </Flex>
        </Card>
    );
};