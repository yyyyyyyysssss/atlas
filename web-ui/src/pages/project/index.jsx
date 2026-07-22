import React, { useRef, useState } from "react";
import "./index.css";
import { List, theme, App, Card, Avatar, Typography, Tooltip, Flex, Button } from "antd";
import { useNavigate } from "react-router-dom";
import { useInfiniteScroll, useRequest } from "ahooks";
import { FolderOutlined, DeleteOutlined, EditOutlined, FolderOpenOutlined, PlusOutlined } from "@ant-design/icons";
import { deleteProject, getProjectPage } from "../../services/ProjectService";
import Loading from "../../components/loading";

const { Text, Title, Paragraph } = Typography;
const { useToken } = theme;
const PAGE_SIZE = 10;

const Project = () => {
    const { token } = useToken();
    const navigate = useNavigate();
    const scrollRef = useRef(null);
    const { message, modal } = App.useApp();

    const { runAsync: deleteProjectAsync, loading: deleteProjectLoading } = useRequest(deleteProject, {
        manual: true
    });

    // 无限滚动加载
    const {
        data,
        loading,
        loadingMore,
        noMore,
        mutate
    } = useInfiniteScroll(
        async (d) => {
            const pageNum = d ? d.nextPage : 1;
            const res = await getProjectPage(pageNum, PAGE_SIZE);
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

    const projectList = data?.list || [];

    // 处理删除项目
    const handleDeleteProject = (id, e) => {
        e?.stopPropagation(); // 阻止冒泡，避免触发跳转

        modal.confirm({
            title: "确定要删除该项目吗？",
            content: "项目删除后，关联的所有数据配置及应用权限都将被清除，此操作不可逆！",
            okText: "确定删除",
            okType: "danger",
            loading: deleteProjectLoading,
            cancelText: "取消",
            onOk: async () => {
                await deleteProjectAsync(id);
                message.success("项目删除成功");
                if (data) {
                    mutate({
                        ...data,
                        total: data.total - 1,
                        list: data.list.filter((item) => item.id !== id)
                    });
                }
            }
        });
    };

    return (
        <div style={{ padding: "16px 24px" }}>
            <Flex justify="space-between" align="flex-start" style={{ marginBottom: 20 }}>
                <div>
                    <Title level={3} style={{ margin: 0 }}>
                        项目管理
                    </Title>
                    <Paragraph type="secondary" style={{ margin: "4px 0 0 0", fontSize: 13 }}>
                        统一管理您的身份应用与 OAuth2 / OIDC 客户端，配置鉴权策略与 SSO 单点登录，构建安全隔离的认证与授权体系。
                    </Paragraph>
                </div>
                <Button
                    type="primary"
                    icon={<PlusOutlined />}
                    size="middle"
                    style={{ borderRadius: token.borderRadius }}
                >
                    创建项目
                </Button>
            </Flex>


            <div
                ref={scrollRef}
                style={{
                    height: 550,
                    overflow: "auto",
                    paddingRight: 8
                }}
            >
                {loading && projectList.length === 0 ? (
                    <div style={{ padding: 20 }}>
                        <Loading spinning />
                    </div>
                ) : (
                    <>
                        <List
                            dataSource={projectList}
                            renderItem={(project) => (
                                <List.Item
                                    key={project.id}
                                    onClick={() => navigate(`/project/${project.id}`)}
                                    style={{ cursor: "pointer", padding: "12px 0" }}
                                >
                                    <div style={{ width: "100%" }}>
                                        <ProjectItem
                                            project={project}
                                            onDelete={(id, e) => handleDeleteProject(id, e)}
                                            onEdit={(id, e) => {
                                                e?.stopPropagation();
                                                navigate(`/project/${id}/edit`);
                                            }}
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

                        {noMore && projectList.length > PAGE_SIZE && (
                            <div style={{ textAlign: "center", padding: 12, color: token.colorTextTertiary }}>
                                没有更多数据了
                            </div>
                        )}
                    </>
                )}
            </div>
        </div>
    );
};

export default Project;

// 单个项目卡片组件
const ProjectItem = ({ project, onDelete, onEdit }) => {
    const { token } = useToken();
    const { message } = App.useApp();
    const [hovered, setHovered] = useState(false);

    const handleCopy = (text, e) => {
        e?.stopPropagation();
        navigator.clipboard.writeText(text);
        message.success("项目标识已复制");
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
                background: hovered ? token.colorBgTextHover : token.colorBgContainer,
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
                        src={project.iconUrl}
                        icon={hovered ? <FolderOpenOutlined /> : <FolderOutlined />}
                        style={{
                            borderRadius: 10,
                            backgroundColor: project.iconUrl ? "transparent" : token.colorPrimaryBg,
                            color: token.colorPrimary
                        }}
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
                                {project.projectName}
                            </Text>
                        </Flex>

                        {/* meta row */}
                        <Flex gap={10} align="center" wrap="wrap">
                            <Tooltip title={project.description || "暂无项目描述"}>
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
                                    {project.description || "暂无项目描述"}
                                </Text>
                            </Tooltip>

                            <Text type="secondary" style={{ fontSize: token.fontSizeSM }}>
                                |
                            </Text>

                            <Text type="secondary" style={{ fontSize: token.fontSizeSM }}>
                                项目标识:
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
                                onClick={(e) => handleCopy(project.projectCode, e)}
                            >
                                {project.projectCode}
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
                        创建于：{project.createTime}
                    </Text>

                    <div style={{ display: "flex", justifyContent: "flex-end", alignItems: "center", height: 44 }}>
                        <Tooltip title="编辑项目">
                            <Button
                                type="text"
                                icon={<EditOutlined />}
                                onClick={(e) => onEdit?.(project.id, e)}
                                style={{
                                    opacity: hovered ? 1 : 0.5
                                }}
                            />
                        </Tooltip>
                        <Tooltip title="删除项目">
                            <Button
                                type="text"
                                icon={<DeleteOutlined />}
                                onClick={(e) => onDelete?.(project.id, e)}
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