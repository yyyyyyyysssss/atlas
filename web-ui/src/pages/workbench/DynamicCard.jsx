import React, { useEffect, useState } from 'react';
import { Card, Flex, Typography, Button, theme, Modal, Tag, Divider, Space, Checkbox, Table, Image, Badge, Drawer, List, Calendar } from 'antd';
import { ArrowRight, History } from 'lucide-react';
import { useRequest } from 'ahooks';
import { fetchAnnouncementUserList, getAnnouncementLatest } from '../../services/NotificationService';
import { AnnouncementType } from '../../enums/notification';
import { useSseEvent } from '../../hooks/useSseEvent';
import AnnouncementDetailModal from '../notification-center/announcement/components/AnnouncementDetailModal';

const { Text} = Typography;


const DynamicCard = () => {
  const { token } = theme.useToken()

  const [drawerOpen, setDrawerOpen] = useState(false)

  const [selectedId, setSelectedId] = useState(null)

  const [latestAnnouncement, setLatestAnnouncement] = useState({})

  const { loading: latestLoading, run: fetchLatest } = useRequest(
    getAnnouncementLatest,
    {
      manual: true,
      onSuccess: (data) => {
        // 初始请求成功后，同步到状态
        if (data) {
          setLatestAnnouncement(data)
        }
      }
    }
  )

  // 获取分页历史列表
  const { data: historyData, loading: historyLoading, run: fetchHistory, mutate: setHistoryData } = useRequest(
    (params) => fetchAnnouncementUserList(params || { pageNum: 1, pageSize: 20 }),
    { manual: true }
  )

  useEffect(() => {
    fetchLatest()
  }, [])

  useSseEvent('announcement_event', (data) => {
    const payload = JSON.parse(data)
    setLatestAnnouncement(payload.body)
  })

  const dynamicCardStyle = {
    background: `linear-gradient(135deg, ${token.colorPrimary}12 0%, ${token.colorBgContainer} 100%)`,
    border: `1px solid ${token.colorPrimary}18`,
    borderRadius: token.borderRadiusLG,
    boxShadow: 'none',
  };

  const handleViewDetails = async (item) => {
    setSelectedId(item.id)
    // 如果点击的是“最新卡片”，更新最新卡片的状态
    if (latestAnnouncement?.id === item.id) {
      setLatestAnnouncement(prev => ({ ...prev, isRead: true }))
    }
    // 如果历史列表已加载，同步更新历史列表中的那一项
    if (historyData?.list) {
      const newList = historyData.list.map(ann =>
        ann.id === item.id ? { ...ann, isRead: true } : ann
      );
      setHistoryData({ ...historyData, list: newList });
    }
  }

  const handleOpenHistory = () => {
    setDrawerOpen(true);
    fetchHistory()
  }

  return (
    <>
      <Card
        title={
          <Flex justify="space-between" align="center" style={{ width: '100%' }}>
            <Space size={4}>
              <Text strong style={{ color: token.colorPrimary }}>系统动态</Text>
            </Space>
            <Button
              type="text"
              size="small"
              icon={<Text type="secondary" style={{ fontSize: 12 }}><History size={14} /></Text>}
              onClick={handleOpenHistory}
            >
              <Text type="secondary" style={{ fontSize: 12 }}>查看历史</Text>
            </Button>
          </Flex>
        }
        variant="borderless"
        loading={latestLoading}
        style={dynamicCardStyle}
      >
        <AnnouncementCard
          item={latestAnnouncement}
          isLatest={true}
          onClick={handleViewDetails}
        />
      </Card>
      <AnnouncementDetailModal id={selectedId} onCancel={() => setSelectedId(null)}/>
      <Drawer
        title="系统动态历史"
        placement="right"
        width={400}
        onClose={() => setDrawerOpen(false)}
        open={drawerOpen}
        loading={historyLoading}
        styles={{ body: { background: token.colorFillAlter, padding: '12px 16px' } }}
      >
        <List
          itemLayout="vertical"
          dataSource={historyData?.list || []}
          renderItem={(item) => (
            <List.Item
              className="atlas-float-trigger"
              onClick={() => handleViewDetails(item)}
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
              <AnnouncementCard
                item={item}
              />
            </List.Item>
          )}
        />
      </Drawer>
    </>
  );
};


const AnnouncementCard = ({ item, isLatest = false, onClick }) => {

  const { token } = theme.useToken()

  const typeConfig = AnnouncementType[item.type] || { label: item.type, color: 'default' }

  return (
    <Flex vertical gap="middle">
      <Flex vertical gap={4}>
        <Flex justify="space-between" align="center">
          <Badge
            dot={item?.isRead === false} // 注意后端返回的是 0(未读) 或 1(已读)
            offset={[5, 0]}          // 微调位置：[向右偏移, 向下偏移]
            status="processing"
            color={token.colorPrimary}
          >
            <Text strong>{item?.title || '暂无动态'}</Text>
          </Badge>
          <Tag color={typeConfig.color} variant="flat" style={{ marginRight: 0 }}>{typeConfig.label}</Tag>
        </Flex>
        <Text type="secondary" style={{ fontSize: token.fontSizeSM, lineHeight: 1.6 }}>
          {item.description}
        </Text>
      </Flex>
      {isLatest ? (
        <Button
          type="primary"
          block
          variant="filled"
          icon={<ArrowRight size={14} />}
          onClick={() => onClick(item)}
        >
          查看详情
        </Button>
      ) : (
        <Flex justify="space-between" align="center" style={{ marginTop: 4 }}>
          <Text type="secondary" style={{ fontSize: 12, opacity: 0.6 }}>
            {item?.publishTime?.split(' ')[0]}
          </Text>
          {item?.version && (
            <Text type="secondary" style={{ fontSize: 11 }}>{item.version}</Text>
          )}
        </Flex>
      )}
    </Flex>
  )
}

export default DynamicCard;