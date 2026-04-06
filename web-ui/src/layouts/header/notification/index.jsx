import React, { useState } from 'react';
import { Badge, Drawer, Flex, theme } from 'antd';
import { Bell } from 'lucide-react';
import IconBox from '../../../components/icon-box';
import NotificationList from './NotificationList';


const Notification = () => {

  const { token } = theme.useToken()

  const [drawerOpen, setDrawerOpen] = useState(false)

  const closeDrawer = () => {
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
        onClose={closeDrawer}
        open={drawerOpen}
        // loading={historyLoading}
        styles={{ body: { background: token.colorFillAlter, padding: '12px 16px' } }}
      >
        <NotificationList closeDrawer={closeDrawer} />
      </Drawer>
    </Flex>
  )
}


export default Notification