import React from 'react';
import { Card, List, Avatar, Button, Typography, Badge, Space, theme, Flex } from 'antd';
import { Bell, Mail, Info, AlertCircle, ChevronRight, FileImage, Download } from 'lucide-react';
import NoDataEmpty from '../../components/NoDataEmpty';
import { downloadFile } from '../../utils/Download';
import { formatFileSize } from '../../utils/format';

const { Text, Paragraph } = Typography;


const notifications = [
  {
    id: 1,
    title: '系统通知：请查收权限附件',
    content: {
      fileUrl: 'http://localhost:9090/file/y-chat-bucket/6d64642055a74317a74330935cc26326.png',
      fileName: '权限附件.png',
      fileSize: '1995'
    },
    time: '09:15',
    type: 'message',
    displayType: 'FILE',
    isRead: false,

  },
  {
    id: 2,
    title: '安全告警：账号异地登录',
    content: '您的账号于 2026-03-30 在上海市尝试登录，若非本人操作请及时修改密码。',
    time: '昨天',
    type: 'alert',
    displayType: 'TEXT',
    isRead: true
  },
];

const NotificationCard = ({ onReadAll }) => {

  return (
    <Card
      title={<Space><Bell size={18} /> 消息通知</Space>}
      extra={notifications.some(i => i.isRead === true) && <Button type="link" onClick={onReadAll}>全部标记已读</Button>}
      variant="borderless"
    >
      <NotificationList />
    </Card>
  );
};


export const NotificationList = () => {

  const { token } = theme.useToken()

  // 根据业务类型获取图标配置
  const getTypeConfig = (type) => {
    const configs = {
      message: { icon: <Mail size={14} />, color: token.colorPrimary, bg: token.colorPrimaryBg },
      alert: { icon: <AlertCircle size={14} />, color: token.colorError, bg: token.colorErrorBg },
    };
    return configs[type] || configs.default
  }

  const handleDownload = (e, url, name) => {
    e.stopPropagation()
    downloadFile({ url: url, filename: name })
  }

  const handleClick = (e, item) => {
    switch (item.displayType) {
      case 'FILE':
        handleDownload(e, item.content.fileUrl, item.content.fileName)
        break
      default:
        break
    }
  }

  return (
    <List
      itemLayout="horizontal"
      dataSource={notifications}
      locale={{ emptyText: <NoDataEmpty /> }}
      renderItem={(item) => {
        const config = getTypeConfig(item.type);

        return (
          <List.Item
            className="atlas-float-trigger"
            onClick={(e) => handleClick(e, item)}
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
                <Avatar style={{ backgroundColor: config.bg, color: config.color }} icon={config.icon} />
              }
              title={
                <Flex justify="space-between" align="center">
                  <Badge
                    dot={item?.isRead === false} // 注意后端返回的是 0(未读) 或 1(已读)
                    offset={[5, 0]}          // 微调位置：[向右偏移, 向下偏移]
                    status="processing"
                    color={token.colorPrimary}
                  >
                    <Text strong={item.isRead === true}>{item.title}</Text>
                  </Badge>
                  <Text type="secondary" style={{ fontSize: 11, fontWeight: 'normal' }}>{item.time}</Text>
                </Flex>
              }
              description={<NotificationBodyRenderer item={item} mode="list" />}
            />
          </List.Item>
        );
      }}
    />
  )
}


const NotificationBodyRenderer = ({ item, mode = 'list' }) => {

  const { token } = theme.useToken()

  if (!item) return null

  switch (item.displayType) {
    case 'FILE':
      const isImage = item.content.fileUrl?.match(/\.(jpg|jpeg|png|gif|webp)$/i);
      return (
        <Flex vertical gap={8} style={{ marginTop: 8 }}>
          <Flex
            justify='space-between'
            align='center'
            style={{
              padding: '8px 12px',
              background: token.colorFillAlter,
              borderRadius: token.borderRadius,
              border: `1px solid ${token.colorBorderSecondary}`,
              cursor: 'pointer',
              transition: 'all 0.2s',
            }}
            className="file-card-hover" // 可以在 CSS 中定义悬浮加深背景
          >
            <Flex gap={8}>
              {isImage ? <FileImage size={18} color={token.colorPrimary} /> : <FileText size={18} color={token.colorInfo} />}
              <Flex gap={10} align='center'>
                <Text ellipsis={{ tooltip: item.content.fileName }} style={{ fontSize: 12, flexShrink: 1 }}>
                  {item.content.fileName}
                </Text>
                <Text type="secondary" style={{ fontSize: 12, flexShrink: 0 }}>
                  [{formatFileSize(item.content.fileSize)}]
                </Text>
              </Flex>
            </Flex>
            <Button
              type="text"
              size="small"
              icon={<Download size={14} />}
            />
          </Flex>
        </Flex>
      );

    case 'CARD':
      // 预留给未来复杂的业务卡片
      return (
        <div style={{ marginTop: 8, fontSize: 13, color: token.colorTextSecondary }}>
          {item.content}
        </div>
      );

    default: // TEXT
      return (
        <Paragraph
          type="secondary"
          ellipsis={mode === 'list' ? { rows: 2 } : false}
          style={{
            fontSize: 13,
            marginTop: 4,
            marginBottom: 0,
            whiteSpace: mode === 'detail' ? 'pre-wrap' : 'normal'
          }}
        >
          {item.content}
        </Paragraph>
      );
  }

}

export default NotificationCard;