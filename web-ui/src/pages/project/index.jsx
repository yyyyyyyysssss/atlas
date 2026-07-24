import React, { useRef, useState } from "react";
import "./index.css";
import { List, theme, App, Card, Avatar, Typography, Tooltip, Flex, Button, Tag, Badge } from "antd";
import { useNavigate } from "react-router-dom";
import { useInfiniteScroll, useRequest } from "ahooks";
import { FolderOutlined, DeleteOutlined, EditOutlined, FolderOpenOutlined, PlusOutlined, UndoOutlined, RightOutlined } from "@ant-design/icons";
import { deleteProject, getProjectPage, restoreProject } from "../../services/ProjectService";
import Loading from "../../components/loading";
import { Box } from "lucide-react";

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

    const { runAsync: restoreProjectAsync, loading: restoreProjectLoading } = useRequest(restoreProject, {
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

    const projectList = data?.list || []

    const hancleCreate = () => {
        navigate('create', { replace: true })
    }

    const updateLocalItemStatus = (id, newStatus) => {
        if (!data) return;
        mutate({
            ...data,
            list: data.list.map((item) => {
                if (item.id === id) {
                    return { ...item, status: newStatus };
                }
                return item;
            })
        });
    };

    // 处理删除项目
    const handleDeleteProject = (id) => {
        modal.confirm({
            title: "确定要归档该项目吗？",
            content: "归档后该项目及其关联应用将暂停服务，您可以随时在列表中重新启用。",
            okText: "确认归档",
            okType: "danger",
            loading: deleteProjectLoading,
            cancelText: "取消",
            onOk: async () => {
                await deleteProjectAsync(id);
                message.success("项目已归档");
                updateLocalItemStatus(id, "archived");
            }
        });
    };

    const handleRestoreProject = async (id) => {
        await restoreProjectAsync(id)
        message.success("项目已重新启用");
        updateLocalItemStatus(id, "active");
    }

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
                    onClick={hancleCreate}
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
                            split={false}
                            loading={deleteProjectLoading || restoreProjectLoading}
                            renderItem={(project) => {
                                const isActive = project.status === "active"
                                return (
                                    <List.Item
                                        key={project.id}
                                        onClick={() => {
                                            if (isActive) {
                                                navigate(`${project.id}`, { replace: true })
                                            }
                                        }}
                                        style={{
                                            opacity: isActive ? 1 : 0.6,
                                            cursor: isActive ? "pointer" : "not-allowed",
                                            filter: isActive ? "none" : "grayscale(60%)",
                                            transition: "all 0.2s ease",
                                            padding: "4px 0",
                                            marginBottom: 8
                                        }}
                                    >
                                        <Flex align="center" justify="space-between" style={{ width: "100%" }} gap={16}>
                                            <div style={{ width: "100%" }}>
                                                <ProjectItem
                                                    project={project}
                                                    onDelete={(id) => handleDeleteProject(id)}
                                                    onEdit={(id) => navigate(`${id}`, { replace: true })}
                                                    onRestore={(id) => handleRestoreProject(id)}
                                                />
                                            </div>
                                            <Tooltip title={isActive ? "进入项目空间" : "项目未启用，请先启用或恢复"} placement="top">
                                                <Button
                                                    type="text"
                                                    disabled={project.status !== 'active'}
                                                    icon={<RightOutlined style={{ fontSize: 20 }} />}
                                                    onClick={(e) => {
                                                        e.stopPropagation();
                                                        navigate(`/project/${project.projectCode}`, { replace: true });
                                                    }}
                                                    style={{
                                                        flexShrink: 0,
                                                        color: token.colorTextTertiary,
                                                        borderRadius: "50%", // 圆形按钮，看起来更精致
                                                        width: 36,
                                                        height: 36,
                                                        display: "flex",
                                                        alignItems: "center",
                                                        justifyContent: "center"
                                                    }}
                                                />
                                            </Tooltip>
                                        </Flex>

                                    </List.Item>
                                )
                            }}
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


const STATUS_MAP = {
    active: { text: "启用", color: "processing" },
    suspended: { text: "暂停", color: "warning" },
    archived: { text: "归档", color: "error" }
};

// 单个项目卡片组件
const ProjectItem = ({ project, onDelete, onEdit, onRestore }) => {
    const { token } = useToken();
    const { message } = App.useApp();
    const [hovered, setHovered] = useState(false);

    const handleCopy = (text, e) => {
        e?.stopPropagation();
        navigator.clipboard.writeText(text);
        message.success("项目标识已复制");
    };

    const currentStatus = STATUS_MAP[project.status] || { text: project.status, color: "default" };

    return (
        <Card
            size="small"
            variant="borderless"
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            styles={{
                body: { padding: 14 }
            }}
            style={{
                borderRadius: token.borderRadiusLG,
                background: hovered ? token.colorBgTextHover : token.colorBgContainer,
                transition: "all 0.2s ease",
            }}
        >
            <Flex justify="space-between" align="center">
                {/* LEFT */}
                <Flex gap={12} align="center" style={{ flex: 1 }}>
                    <Avatar
                        size={44}
                        shape="square"
                        src={project.iconUrl}
                        icon={<Box />}
                        style={{
                            borderRadius: 10,
                            backgroundColor: project.iconUrl ? "transparent" : token.colorPrimaryBg,
                            color: token.colorPrimary
                        }}
                    />

                    <Flex vertical style={{ flex: 1 }} gap={4}>
                        {/* title row */}
                        <Flex align="center" gap={8}>
                            <Text
                                strong
                                style={{
                                    fontSize: token.fontSizeLG,
                                    color: token.colorText
                                }}
                            >
                                {project.projectName}
                            </Text>

                            {/* 状态 Tag */}
                            <Tag color={currentStatus.color} bordered={false} style={{ margin: 0, fontSize: 12 }}>
                                {currentStatus.text}
                            </Tag>

                            {project.builtin && (
                                <Tooltip title="系统内置项目，禁止删除或修改启用状态">
                                    <Tag color="gold" bordered={false} style={{ margin: 0, fontSize: 12 }}>
                                        内置
                                    </Tag>
                                </Tooltip>
                            )}
                        </Flex>

                        {/* meta row */}
                        <Flex gap={10} align="center" wrap="wrap">
                            <Tooltip title={project.description || "暂无项目描述"}>
                                <Text
                                    type="secondary"
                                    style={{
                                        fontSize: token.fontSizeSM,
                                        maxWidth: '50%',
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
                        {project.status === "active" && (
                            <>
                                <Tooltip title="编辑项目">
                                    <Button
                                        type="text"
                                        icon={<EditOutlined />}
                                        onClick={(e) => {
                                            e.stopPropagation()
                                            onEdit?.(project.id)
                                        }}
                                        style={{ opacity: hovered ? 1 : 0.5 }}
                                    />
                                </Tooltip>
                                {project.builtin === false && (
                                    <Tooltip title="删除项目">
                                        <Button
                                            type="text"
                                            icon={<DeleteOutlined />}
                                            onClick={(e) => {
                                                e.stopPropagation()
                                                onDelete?.(project.id)
                                            }}
                                            style={{ opacity: hovered ? 1 : 0.5 }}
                                        />
                                    </Tooltip>
                                )}

                            </>
                        )}

                        {project.status === "archived" && (
                            <Tooltip title="重新启用">
                                <Button
                                    type="text"
                                    icon={<UndoOutlined />}
                                    onClick={(e) => {
                                        e.stopPropagation()
                                        onRestore?.(project.id)
                                    }}
                                    style={{ opacity: hovered ? 1 : 0.5 }}
                                />
                            </Tooltip>
                        )}

                        {project.status === "suspended" && null}
                    </div>
                </div>
            </Flex>
        </Card>
    );
};