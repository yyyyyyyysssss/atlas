import React from 'react';
import { Card, List, Avatar, Button, Typography, Badge, Space, theme, Flex } from 'antd';
import { Mail, Info, ShieldAlert } from 'lucide-react';
import NoDataEmpty from '../../../components/NoDataEmpty';
import { useSseEvent } from '../../../hooks/useSseEvent';
import TextRenderer from './renderers/TextRenderer';
import FileRenderer from './renderers/FileRenderer';
import { formatRelativeTime } from '../../../utils/format';
import CardRenderer from './renderers/CardRenderer';
import { NOTIFICATION_COMPONENTS } from './NotificationRegistry';


const notifications = [
    {
        title: '请查收权限附件',
        category: 'SYSTEM',
        renderType: 'FILE',
        sendTime: '2026-04-03 11:30:27',
        isRead: false,
        body: {
            fileUrl: 'http://localhost:9090/file/y-chat-bucket/6d64642055a74317a74330935cc26326.png',
            fileName: '权限附件.png',
            fileSize: '1995'
        },
    },
    {
        title: '账号异地登录',
        category: 'SECURITY',
        renderType: 'TEXT',
        sendTime: '2026-04-02 14:22:03',
        isRead: true,
        body: '您的账号于 2026-04-02 在阜阳市尝试登录，若非本人操作请及时修改密码。',
    },
    {
        "title": "Atlas 系统版本更新",
        "category": "SYSTEM", // 修改类别
        "sendTime": "2026-04-03 10:00:00",
        "isRead": false,
        "renderType": "CARD",
        "body": {
            "subTitle": "Atlas v2.7.0 正式发布", // 突出版本号
            "content": "本次更新引入了全新的 AI 智能诊断引擎，大幅提升了基础设施的监控精度。同时修复了 12 个已知稳定性问题。",
            "tagText": "新版本", // 标签改为状态
            "tagType": "success",
            "fields": [
                { "label": "版本号", "value": "v2.7.0 (Stable)", "highlight": true },
                { "label": "更新类型", "value": "功能特性 & 安全补丁", "highlight": false },
                { "label": "升级建议", "value": "建议所有用户升级", "highlight": false }
            ],
            "actions": [
                {
                    "label": "查看完整日志",
                    "url": "/release/v2.7.0", // 这里的 URL 对应你设计的 Modal 路由
                    "type": "default",
                    "actionType": "URL"
                },
                {
                    "label": "立即体验",
                    "url": "/dashboard",
                    "type": "primary",
                    "actionType": "URL"
                }
            ],
            "link": "/release/v2.7.0" // 点击卡片直接打开路由 Modal
        }
    }
];


const { Text } = Typography;

const NotificationList = ({ limit = 10 }) => {

    const { token } = theme.useToken()

    useSseEvent('notification_event', (data) => {
        console.log('Received notification event:', data);
    })

    // 根据业务类型获取图标配置
    const getCategoryIcon = (category) => {
        const configs = {
            SYSTEM: { icon: <Mail size={14} />, color: token.colorPrimary, bg: token.colorPrimaryBg, name: '系统通知：' },
            SECURITY: { icon: <ShieldAlert size={14} />, color: token.colorError, bg: token.colorErrorBg, name: '安全告警：' },
            DEFAULT: {
                icon: <Info size={14} />,
                color: token.colorTextSecondary,
                bg: token.colorFillAlter
            }
        };
        return configs[category] || configs.DEFAULT
    }

    const handleItemClick = (e, item) => {
        console.log('Clicked notification:', item);
    }

    const bodyRender = (item) => {
        switch (item.renderType) {
            case 'TEXT':
                return <TextRenderer content={item.body} />
            case 'FILE':
                return <FileRenderer content={item.body} />
            case 'CARD':
                return <CardRenderer content={item.body} />
            default:
                return <Typography.Text type="secondary">未知</Typography.Text>
        }
    }

    return (
        <List
            itemLayout="horizontal"
            dataSource={notifications}
            locale={{ emptyText: <NoDataEmpty /> }}
            renderItem={(item) => {
                const { title, category, sendTime, isRead } = item

                const categoryConfig = getCategoryIcon(category);

                return (
                    <List.Item
                        className="atlas-float-trigger"
                        onClick={(e) => handleItemClick(e, item)}
                        style={{
                            padding: '16px',
                            marginBottom: 12, // 利用外边距制造自然分界
                            background: token.colorBgContainer, // 纯白卡片
                            borderRadius: token.borderRadiusLG,
                            border: `1px solid ${token.colorBorderSecondary}`,
                            cursor: 'pointer',
                            transition: 'all 0.3s cubic-bezier(0.4, 0, 0.2, 1)',
                        }}
                    >
                        <List.Item.Meta
                            avatar={
                                <Avatar style={{ backgroundColor: categoryConfig.bg, color: categoryConfig.color }} icon={categoryConfig.icon} />
                            }
                            title={
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
                            }
                            description={bodyRender(item)}
                        />
                    </List.Item>
                );
            }}
        />
    )
}

export default NotificationList