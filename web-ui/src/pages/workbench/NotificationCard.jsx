import React from 'react';
import { Card, Space } from 'antd';
import { Bell } from 'lucide-react';
import NotificationList from '../../layouts/header/notification/NotificationList';


const NotificationCard = () => {

  return (
    <Card
      title={<Space><Bell size={18} /> 消息通知</Space>}
      variant="borderless"
    >
      <NotificationList
        limit={3}
      />
    </Card>
  )
}

export default NotificationCard