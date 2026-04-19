import { Avatar, Badge, Flex, theme, Typography } from "antd";
import CardRenderer from "./renderers/CardRenderer";
import FileRenderer from "./renderers/FileRenderer";
import TextRenderer from "./renderers/TextRenderer";
import React from "react";
import { markAllAsRead, markAsRead } from "../../../services/NotificationService";
import { useRequest } from "ahooks";
import { ClipboardCheck, Info, Mail, ShieldAlert } from "lucide-react";
import { formatRelativeTime } from "../../../utils/format";

const { Text } = Typography

const RENDERER_MAP = {
    TEXT: TextRenderer,
    FILE: FileRenderer,
    CARD: CardRenderer,
};

const MessageRenderer = ({ message, onMarkRead, onClose }) => {

    const { notificationId, title, category, content, sendTime, isRead } = message

    const { renderType, body } = content

    const Renderer = RENDERER_MAP[renderType]

    const { token } = theme.useToken()

    const { runAsync: markAsReadAsync, loading: markAsReadLoading } = useRequest(markAsRead, {
        manual: true
    })

    if (!Renderer) {
        return <Text type="secondary">未知类型</Text>;
    }

    const handleAction = (notificationId) => {
        if (markAsReadLoading) {
            return
        }
        if (isRead && isRead === true) {
            return
        }
        onMarkRead?.(notificationId)
        markAsReadAsync(notificationId)
    }

    const getCategoryIcon = (category) => {
        const configs = {
            SYSTEM: {
                icon: <Mail size={14} />,
                color: token.colorPrimary,
                bg: token.colorPrimaryBg,
                name: '系统通知：'
            },
            SECURITY: {
                icon: <ShieldAlert size={14} />,
                color: token.colorError,
                bg: token.colorErrorBg,
                name: '安全告警：'
            },
            TODO: {
                icon: <ClipboardCheck size={14} />,
                color: token.colorWarning,
                bg: token.colorWarningBg,
                name: '待办任务：'
            },
            DEFAULT: {
                icon: <Info size={14} />,
                color: token.colorTextSecondary,
                bg: token.colorFillAlter
            }
        };
        return configs[category] || configs.DEFAULT
    }

    const categoryConfig = getCategoryIcon(category)

    return (
        <Flex
            gap={12}
            style={{
                width: '100%',
                cursor: 'pointer'
            }}
            onClick={(e) => {
                handleAction(notificationId)
            }}
        >
            {/* 左侧头像区 */}
            <Avatar style={{ backgroundColor: categoryConfig.bg, color: categoryConfig.color }} icon={categoryConfig.icon} />
            {/* 右侧内容区 */}
            <Flex
                vertical
                style={{
                    flex: 1,
                    minWidth: 0
                }}
            >
                <Flex justify="space-between" align="center">
                    <Badge
                        dot={isRead === false}
                        offset={[5, 0]}
                        status="processing"
                        color={token.colorPrimary}
                    >
                        <Flex>
                            <Text
                                strong
                            >
                                {categoryConfig.name}
                            </Text>
                            <Text strong={isRead === true}>
                                {title}
                            </Text>
                        </Flex>
                    </Badge>
                    <Text type="secondary" style={{ fontSize: 11, fontWeight: 'normal' }}>{formatRelativeTime(sendTime)}</Text>
                </Flex>
                <Renderer
                    content={body}
                    onClose={onClose}
                />
            </Flex>

        </Flex>
    )
}

export default React.memo(MessageRenderer)