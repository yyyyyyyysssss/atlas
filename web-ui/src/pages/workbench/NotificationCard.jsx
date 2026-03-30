import React from 'react';
import { Card, List, Avatar, Button, Typography, Badge, Space, theme } from 'antd';
import { Bell, Mail, Info, AlertCircle } from 'lucide-react';

const { Text } = Typography;


const notifications = [
  { id: 1, title: '来自 张三 的站内信：请查收权限附件', time: '09:15', type: 'message', isRead: false },
  { id: 2, title: '安全告警：账号在异地尝试登录', time: '昨天', type: 'alert', isRead: true },
];

const NotificationCard = ({ onReadAll }) => {
  const { token } = theme.useToken();

  return (
    <Card
      title={<Space><Bell size={18} /> 消息通知</Space>}
      extra={<Button type="link" onClick={onReadAll}>全部标记已读</Button>}
      variant="borderless"
    >
      <List
        itemLayout="horizontal"
        dataSource={notifications}
        renderItem={(item) => (
          <List.Item
            className="atlas-float-trigger"
            actions={[<Button type="link">查看</Button>]}
          >
            <List.Item.Meta
              avatar={
                <Avatar
                  style={{
                    backgroundColor: item.type === 'alert' ? token.colorErrorBg : token.colorFillAlter,
                    color: item.type === 'alert' ? token.colorError : token.colorPrimary
                  }}
                  icon={item.type === 'message' ? <Mail size={14} /> : (item.type === 'alert' ? <AlertCircle size={14} /> : <Info size={14} />)}
                />
              }
              title={
                <Badge
                  dot={item?.isRead === false}
                  offset={[3, 0]}
                  status="processing"
                  color={token.colorPrimary}
                >
                  <Text strong>{item.title}</Text>
                </Badge>
              }
              description={
                <Text type="secondary" style={{ fontSize: '12px' }}>
                  {item.time} · {item.type === 'system' ? '系统公告' : '站内信'}
                </Text>
              }
            />
          </List.Item>
        )}
      />
    </Card>
  );
};

export default NotificationCard;