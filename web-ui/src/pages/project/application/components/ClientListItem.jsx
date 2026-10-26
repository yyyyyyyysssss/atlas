import { List, theme, App, Card, Avatar, Typography, Tooltip, Flex, Button } from "antd";
import { useState } from "react";
import { AppstoreOutlined, DeleteOutlined, EditOutlined } from "@ant-design/icons";


const { Text } = Typography;
const { useToken } = theme;

const ClientListItem = ({ client, onEdit, onDelete, clientIdLabel = ' 客户端 ID' }) => {


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
                        icon={<AppstoreOutlined />}
                        style={{ borderRadius: token.borderRadius, color: token.colorPrimary }}
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
                                {client.clientName}
                            </Text>
                        </Flex>

                        {/* meta row */}
                        <Flex gap={10} align="center" wrap="wrap">
                            <Tooltip title={client.description || "暂无描述"}>
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
                                    {client.description || "暂无描述"}
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
                                {clientIdLabel} :
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
                                    handleCopy(client.clientId);
                                }}
                            >
                                {client.clientId}
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
                        创建于：{client.createTime}
                    </Text>

                    <div style={{ display: "flex", justifyContent: "flex-end", alignItems: "center", height: 44 }}>
                        <Tooltip title='编辑'>
                            <Button
                                type="text"
                                icon={<EditOutlined />}
                                style={{
                                    opacity: hovered ? 1 : 0.5
                                }}
                                onClick={(e) => {
                                    if (onEdit) {
                                        onEdit(client.id, e)
                                    }

                                }}
                            />
                        </Tooltip>
                        <Tooltip title='删除'>
                            <Button
                                type="text"
                                icon={<DeleteOutlined />}
                                onClick={(e) => {
                                    if (onDelete) {
                                        onDelete(client.id, e)
                                    }

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
}

export default ClientListItem