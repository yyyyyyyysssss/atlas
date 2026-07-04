import { App, Avatar, Button, Flex, theme, Typography } from "antd";
import { PlusOutlined, ArrowLeftOutlined, EditOutlined, DeleteOutlined, AppstoreOutlined, CopyOutlined, CheckOutlined } from "@ant-design/icons";
import { useState } from "react";
import { Tag } from "lucide-react";
import { t } from "i18next";

const { Title, Paragraph, Text } = Typography;
const { useToken } = theme;

const OAuth2ApplicationItem = ({ oauth2App, onEdit }) => {

    const { token } = useToken();

    const { message } = App.useApp();

    const [isHovered, setIsHovered] = useState(false);


    const handleCopy = (text) => {
        navigator.clipboard.writeText(text);
        message.success('Client ID 已复制');
    };

    const maskId = (id) => {
        if (!id || id.length <= 8) return id;
        return `${id.slice(0, 4)}****${id.slice(-4)}`;
    };

    return (
        <Flex
            justify="space-between"
            align="center"
            style={{
                padding: '16px', // 稍微加点内边距更好看
                borderBottom: `1px solid ${token.colorBorderSecondary}`,
                backgroundColor: isHovered ? token.colorFillAlter : 'transparent', // 3. 动态切换背景色
                transition: 'background-color 0.3s ease', // 4. 添加过渡动画
                cursor: 'pointer',
                borderRadius: token.borderRadiusLG // 可选：让悬浮块更圆润
            }}
            onMouseEnter={() => setIsHovered(true)}
            onMouseLeave={() => setIsHovered(false)}
        >
            <Flex gap={16} align="flex-start" style={{ flex: 1 }}>
                <Avatar
                    size={52}
                    shape="square"
                    src={oauth2App.logo}
                    icon={<AppstoreOutlined />}
                    style={{
                        borderRadius: token.borderRadiusSM,
                    }}
                />
                <Flex style={{ width: '100%' }} vertical gap={4}>
                    <Flex justify="space-between" align="center">
                        <Text strong style={{ fontSize: '18px' }}>
                            {oauth2App.name}
                        </Text>
                        <Text type="secondary" style={{ fontSize: 12, marginRight: 20 }}>
                            Client ID:{' '}
                            <Text
                                type="secondary"
                                style={{
                                    fontFamily: 'monospace',
                                    cursor: 'pointer',
                                    padding: '2px 4px',
                                    borderRadius: token.borderRadiusSM,
                                    transition: 'background 0.2s'
                                }}
                                // 点击即复制
                                onClick={(e) => {
                                    e.stopPropagation(); // 阻止触发外层列表项的跳转
                                    handleCopy(oauth2App.clientId);
                                }}
                                // 增加一点交互反馈，让点击感更好
                                onMouseEnter={(e) => e.target.style.backgroundColor = token.colorFillSecondary}
                                onMouseLeave={(e) => e.target.style.backgroundColor = 'transparent'}
                            >
                                {maskId(oauth2App.clientId)}
                            </Text>
                        </Text>
                    </Flex>
                    <Flex justify="space-between" align="center">
                        <Text type="secondary" style={{ fontSize: '12px' }}>
                            {oauth2App.description || '暂无描述'}
                        </Text>
                    </Flex>
                </Flex>
            </Flex>
            <Button
                type="text"
                shape="circle"
                icon={<EditOutlined style={{ fontSize: '18px' }} />}
            />
        </Flex>
    )
}

export default OAuth2ApplicationItem