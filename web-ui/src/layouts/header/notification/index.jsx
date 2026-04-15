import React, { useState } from 'react';
import { App, Badge, Drawer, Flex, theme } from 'antd';
import { Bell } from 'lucide-react';
import IconBox from '../../../components/icon-box';
import NotificationList from './NotificationList';
import { useSseEvent } from '../../../hooks/useSseEvent';
import MessageRenderer from './MessageRenderer';


const Notification = () => {

  const { token } = theme.useToken()

  const [drawerOpen, setDrawerOpen] = useState(false)

  const { notification } = App.useApp()

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
  })

  const handleClose = () => {
    setDrawerOpen(false)
  }

  return (
    <Flex>
      <IconBox onClick={() => setDrawerOpen(true)}>
        <Badge
          count={23}
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
      >
        <NotificationList onClose={handleClose} />
      </Drawer>
    </Flex>
  )
}


export default Notification