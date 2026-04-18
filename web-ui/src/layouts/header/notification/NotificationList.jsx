import React, { useCallback, useEffect, useImperativeHandle, useState } from 'react';
import { Card, List, Avatar, App, Typography, Badge, Space, theme, Flex } from 'antd';
import { Mail, Info, ShieldAlert } from 'lucide-react';
import NoDataEmpty from '../../../components/NoDataEmpty';
import { formatRelativeTime } from '../../../utils/format';
import MessageRenderer from './MessageRenderer';
import { useRequest } from 'ahooks';
import { fetchUserNotificationList } from '../../../services/NotificationService';


const { Text } = Typography;

const NotificationList = ({ limit = 10, onMarkRead, onClose, ref }) => {

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

    const handleMarkAllAsRead = async () => {
        setData(prev => prev.map(item => ({ ...item, isRead: true })))
    }

    useImperativeHandle(ref, () => ({
        markAllRead: handleMarkAllAsRead,
        // 你还可以暴露其他方法，比如手动刷新
        refresh: () => fetchData(1, limit)
    }))

    const markRead = useCallback((notificationId) => {
        setData((prevData) =>
            prevData.map((item) =>
                item.notificationId === notificationId ? { ...item, isRead: true } : item
            )
        )
        onMarkRead?.()
    }, [])

    return (
        <>
            <List
                itemLayout="horizontal"
                dataSource={data}
                loading={getUserNotificationLoading}
                locale={{ emptyText: <NoDataEmpty /> }}
                renderItem={(item) => {
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
                            <MessageRenderer
                                message={item}
                                onMarkRead={markRead}
                                onClose={onClose}
                            />
                        </List.Item>
                    );
                }}
            />
        </>
    )
}

export default NotificationList