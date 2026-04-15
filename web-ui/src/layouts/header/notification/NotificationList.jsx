import React, { useCallback, useEffect, useState } from 'react';
import { Card, List, Avatar, App, Typography, Badge, Space, theme, Flex } from 'antd';
import { Mail, Info, ShieldAlert } from 'lucide-react';
import NoDataEmpty from '../../../components/NoDataEmpty';
import { formatRelativeTime } from '../../../utils/format';
import MessageRenderer from './MessageRenderer';
import { useRequest } from 'ahooks';
import { fetchUserNotificationList } from '../../../services/NotificationService';


const { Text } = Typography;

const NotificationList = ({ limit = 10, onClose }) => {

    const { token } = theme.useToken()

    const { runAsync: getUserNotificationAsync, loading: getUserNotificationLoading } = useRequest(fetchUserNotificationList, {
        manual: true
    })

    const [data, setData] = useState([])

    const [total, setTotal] = useState(0)

    const fetchData = async (pageNum, pageSize) => {
        const result = await getUserNotificationAsync(pageNum, pageSize)
        setData(prevData => [...prevData, ...result.list])
        setTotal(result.total)
    }

    useEffect(() => {
        fetchData(1, limit)
    }, [limit])

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

    const bodyRender = useCallback((item) => {
        let { notificationId, contentType, content } = item
        if (contentType === 'JSON') {
            content = JSON.parse(content)
        }
        return (
            <MessageRenderer
                notificationId={notificationId}
                content={content}
                onClose={onClose}
            />
        )
    }, [onClose])

    return (
        <>
            <List
                itemLayout="horizontal"
                dataSource={data}
                loading={getUserNotificationLoading}
                locale={{ emptyText: <NoDataEmpty /> }}
                renderItem={(item) => {
                    const { notificationId, title, category, receiveTime, isRead } = item

                    const categoryConfig = getCategoryIcon(category);

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
                                        <Text type="secondary" style={{ fontSize: 11, fontWeight: 'normal' }}>{formatRelativeTime(receiveTime)}</Text>
                                    </Flex>
                                }
                                description={bodyRender(item)}
                            />
                        </List.Item>
                    );
                }}
            />
        </>
    )
}

export default NotificationList