import React from 'react';
import { Card, List, Avatar, Button, Typography, Badge, Space, theme } from 'antd';
import { Bell, Mail, Info, AlertCircle } from 'lucide-react';

const { Text } = Typography;

const NotificationCard = ({ data, onReadAll }) => {
  const { token } = theme.useToken();

  return (
    <Card 
      title={<Space><Bell size={18} /> 消息通知</Space>} 
      extra={<Button type="link" onClick={onReadAll}>全部标记已读</Button>} 
      bordered={false}
    >
      <List
        itemLayout="horizontal"
        dataSource={data}
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
                <Space>
                  <Text strong={!item.read}>{item.title}</Text>
                  {!item.read && <Badge status="processing" />}
                </Space>
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