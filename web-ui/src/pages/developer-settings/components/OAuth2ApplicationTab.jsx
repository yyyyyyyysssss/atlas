import React, { useState } from "react";
import { Typography, List, Button, Space, theme, Card, Tooltip } from "antd";
import { PlusOutlined, ArrowLeftOutlined, EditOutlined, DeleteOutlined } from "@ant-design/icons";

const { Title, Paragraph, Text } = Typography;
const { useToken } = theme;

// 1. 模拟数据（加入系统的 logo 地址）
const mockApps = [
    {
        id: "1",
        name: "Atlas",
        description: "",
        logo: "/logo128.png" // 替换为你的真实 Logo URL
    },
];

const OAuth2ApplicationTab = () => {
    const { token } = useToken();

    const [view, setView] = useState("list");
    const [selectedApp, setSelectedApp] = useState(null);

    const handleViewDetail = (app) => {
        setSelectedApp(app);
        setView("detail");
    };

    const handleBackToList = () => {
        setSelectedApp(null);
        setView("list");
    };

    // --- 视图：精细化应用列表 ---
    const renderListView = () => (
        <div>
            {/* 头部布局 */}
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "flex-end", marginBottom: token.marginXL }}>
                <div>
                    <Title level={4} style={{ margin: 0, fontWeight: 600 }}>OAuth2 应用</Title>
                    <Text type="secondary" style={{ fontSize: 13 }}>管理和配置您的 OAuth2 客户端凭证及访问权限</Text>
                </div>
                <Button type="primary" icon={<PlusOutlined />} onClick={() => setView("create")}>
                    新建应用
                </Button>
            </div>

            {/* 纯净行列表 */}
            <List
                itemLayout="horizontal"
                dataSource={mockApps}
                renderItem={(app) => {
                    return (
                        <List.Item
                            className="atlas-float-trigger"
                            style={{
                                padding: '16px',
                                marginBottom: 12, // 利用外边距制造自然分界
                                background: token.colorBgContainer, // 纯白卡片
                                borderRadius: token.borderRadiusLG,
                                border: `1px solid ${token.colorBorderSecondary}`,
                                transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                            }}
                        >
                            <div style={{ display: 'flex', alignItems: 'center', flex: 1, marginRight: 24 }}>
                                {/* Logo 区 */}
                                <div style={{ flexShrink: 0, marginRight: 16 }}>
                                    {app.logo ? (
                                        <img
                                            src={app.logo}
                                            alt={app.name}
                                            style={{
                                                width: 40, // 略微放大到 40px，卡片式更显大气
                                                height: 40,
                                                borderRadius: token.borderRadius,
                                                objectFit: "contain",
                                                display: 'block'
                                            }}
                                        />
                                    ) : (
                                        <div style={{
                                            width: 40,
                                            height: 40,
                                            borderRadius: token.borderRadius,
                                            background: token.colorTextQuaternary,
                                            display: "flex",
                                            alignItems: "center",
                                            justifyContent: "center"
                                        }}>
                                            <Text type="tertiary" style={{ fontSize: 11, fontWeight: 600 }}>APP</Text>
                                        </div>
                                    )}
                                </div>

                                {/* 文本区：名字与描述 */}
                                <div style={{ display: 'flex', flexDirection: 'column', minWidth: 0 }}>
                                    <Text strong style={{ fontSize: 15, lineHeight: "22px", color: token.colorText }}>
                                        {app.name}
                                    </Text>
                                    {app.description ? (
                                        <Text
                                            type="secondary"
                                            ellipsis // 超过长度自动省略号，防止撑破卡片
                                            style={{ fontSize: 13, marginTop: 4, lineHeight: "18px" }}
                                        >
                                            {app.description}
                                        </Text>
                                    ) : (
                                        // 优雅降级：如果没有描述，给一个精致的灰字占位，或者直接不渲染
                                        <Text type="secondary" style={{ fontSize: 13, marginTop: 4, lineHeight: "18px" }}>
                                            暂无应用描述
                                        </Text>
                                    )}
                                </div>
                            </div>

                            {/* 右侧操作区：紧凑的图标组 */}
                            <div style={{ flexShrink: 0 }}>
                                <Space size={4}> {/* 4px - 8px 的紧凑间距，图标放在一起更精致 */}
                                    <Tooltip title="编辑应用">
                                        <Button
                                            type="text"
                                            shape="circle"
                                            size="large" // 大号文本按钮拥有更大的气泡点击区域，体验更好
                                            icon={<EditOutlined style={{ fontSize: 16, color: token.colorTextDescription }} />}
                                            onClick={(e) => {
                                                e.stopPropagation(); // 防止冒泡触发卡片点击
                                                handleEdit(app);
                                            }}
                                        />
                                    </Tooltip>
                                    <Tooltip title="删除应用">
                                        <Button
                                            type="text"
                                            shape="circle"
                                            size="large"
                                            danger
                                            icon={<DeleteOutlined style={{ fontSize: 16 }} />}
                                            onClick={(e) => {
                                                e.stopPropagation();
                                                handleDelete(app);
                                            }}
                                        />
                                    </Tooltip>
                                </Space>
                            </div>
                        </List.Item>
                    );
                }}
            />
        </div>
    );

    // --- 视图：新建应用 ---
    const renderCreateView = () => (
        <div>
            <Button type="link" icon={<ArrowLeftOutlined />} onClick={handleBackToList} style={{ paddingLeft: 0, marginBottom: token.marginLG }}>
                返回应用列表
            </Button>
            <Title level={4}>创建新 OAuth2 应用</Title>
            <div style={{ marginTop: token.marginLG }}>
                {/* 在此插入 Form */}
                <Text type="secondary">[ Form 表单区域 ]</Text>
            </div>
        </div>
    );

    // --- 视图：配置详情 ---
    const renderDetailView = () => (
        <div>
            <Button type="link" icon={<ArrowLeftOutlined />} onClick={handleBackToList} style={{ paddingLeft: 0, marginBottom: token.marginLG }}>
                返回应用列表
            </Button>
            <div style={{ display: "flex", alignItems: "center", gap: 12, marginBottom: token.marginXL }}>
                {selectedApp?.logo && (
                    <img src={selectedApp.logo} alt="" style={{ width: 40, height: 40, borderRadius: token.borderRadius, objectFit: "contain" }} />
                )}
                <Title level={4} style={{ margin: 0 }}>{selectedApp?.name}</Title>
            </div>

            <Space direction="vertical" size="large" style={{ width: "100%" }}>
                <div>
                    <Title level={5} style={{ marginBottom: token.marginSM }}>凭证安全</Title>
                    <Paragraph type="secondary">
                        Client ID: <Text code>{selectedApp?.clientId}</Text>
                    </Paragraph>
                    <Paragraph type="secondary">
                        Client Secret: <Text type="danger">•••••••••••••••••••••••••</Text>
                    </Paragraph>
                </div>
            </Space>
        </div>
    );

    return (
        <div style={{ padding: `${token.paddingLG}px ${token.paddingXL}px` }}>
            {view === "list" && renderListView()}
            {view === "create" && renderCreateView()}
            {view === "detail" && renderDetailView()}
        </div>
    );
};

export default OAuth2ApplicationTab;