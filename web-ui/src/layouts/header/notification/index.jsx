import React, { useEffect, useRef, useState } from 'react';
import { App, Badge, Button, Drawer, Flex, theme } from 'antd';
import { Bell } from 'lucide-react';
import IconBox from '../../../components/icon-box';
import NotificationList from './NotificationList';
import { useSseEvent } from '../../../hooks/useSseEvent';
import MessageRenderer from './MessageRenderer';
import { useRequest } from 'ahooks';
import { fetchUserNotificationUnreadCount, markAllAsRead } from '../../../services/NotificationService';
import Loading from '../../../components/loading';
import { useDispatch } from 'react-redux';
import { setNotificationUnreadCount } from '../../../redux/slices/userSlice';


const Notification = () => {

  const { token } = theme.useToken()

  const dispatch = useDispatch()

  const [drawerOpen, setDrawerOpen] = useState(false)

  const { notification } = App.useApp()

  const [unreadCount, setUnreadCount] = useState(0)

  const listRef = useRef(null)

  const { runAsync: markAllAsReadAsync, loading: markAllAsReadLoading } = useRequest(markAllAsRead, {
    manual: true
  })

  const { runAsync: unreadCountAsync, loading: unreadCountLoading } = useRequest(fetchUserNotificationUnreadCount, {
    manual: true
  })

  const refreshUnreadCount = async () => {
    const result = await unreadCountAsync()
    if (typeof result === 'number') {
      setUnreadCount(result);
    }
  }

  useEffect(() => {
    refreshUnreadCount()
  }, [])

  useEffect(() => {
    dispatch(setNotificationUnreadCount({ notificationUnreadCount: unreadCount }))
  }, [unreadCount, dispatch])

  useSseEvent('notification_event', (data) => {
    let { notificationId, contentType, content } = data
    if (contentType === 'JSON') {
      content = JSON.parse(content)
    }
    notification.open({
      message: data.title,
      description: <MessageRenderer notificationId={notificationId} content={content} />,
      duration: 0,
    })
    refreshUnreadCount()
  })

  const handleClose = () => {
    setDrawerOpen(false)
  }

  const markRead = () => {
    setUnreadCount(prev => Math.max(0, prev - 1))
  }

  const markAllRead = () => {
    setUnreadCount(0)
    listRef.current?.markAllRead()
    // 异步调用接口
    markAllAsReadAsync()
      .catch(() => {
        refreshUnreadCount()
      })
  }

  return (
    <Flex>
      <IconBox onClick={() => setDrawerOpen(true)}>
        <Badge
          count={unreadCount}
          overflowCount={99}
          size="small"
        >
          <Bell size={20} />
        </Badge>
      </IconBox>
      <Drawer
        title="消息通知"
        placement="right"
        width={600}
        onClose={handleClose}
        open={drawerOpen}
        destroyOnHidden
        styles={{ body: { background: token.colorFillAlter, padding: '12px 16px' } }}
        extra={
          <Loading spinning={markAllAsReadLoading}>
            <Button
              type="link"
              size="small"
              onClick={markAllRead}
            >
              全部已读
            </Button>
          </Loading>
        }
      >
        <NotificationList ref={listRef} onMarkRead={markRead} onClose={handleClose} />
      </Drawer>
    </Flex>
  )
}


export default Notification