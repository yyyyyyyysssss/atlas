import React, { useEffect, useState } from 'react';
import { Card, Flex, Typography, Button, theme, Modal, Tag, Divider, Space, Checkbox, Table, Image, Badge, Drawer, List, Calendar } from 'antd';
import { ArrowRight, History } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { useRequest } from 'ahooks';
import { fetchAnnouncementUserList, getAnnouncementLatest, getAnnouncementUserDetails } from '../../services/NotificationService';
import { AnnouncementType } from '../../enums/notification';
import { useSseEvent } from '../../hooks/useSseEvent';

const { Title, Text, Paragraph, Link } = Typography;


const DynamicCard = () => {
  const { token } = theme.useToken()

  const [modalOpen, setModalOpen] = useState(false)

  const [drawerOpen, setDrawerOpen] = useState(false)

  const [selectedItem, setSelectedItem] = useState(null)

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

  const { runAsync: detailAsync, loading: detailLoading } = useRequest(getAnnouncementUserDetails, { manual: true })

  useEffect(() => {
    fetchLatest()
  }, [])

  useSseEvent('announcement_event', (data) => {
    setLatestAnnouncement(data)
  })

  const dynamicCardStyle = {
    background: `linear-gradient(135deg, ${token.colorPrimary}12 0%, ${token.colorBgContainer} 100%)`,
    border: `1px solid ${token.colorPrimary}18`,
    borderRadius: token.borderRadiusLG,
    boxShadow: 'none',
  };

  const handleViewDetails = async (item) => {
    setModalOpen(true)
    const details = await detailAsync(item.id)
    setSelectedItem(details)
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

      {/* 详情模态框 */}
      <Modal
        title={selectedItem?.title}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        footer={null}
        loading={detailLoading}
        width={700}
        zIndex={1010}
      >
        {selectedItem && <AnnouncementDetailView data={selectedItem} />}
      </Modal>

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


export const AnnouncementDetailView = ({ data }) => {

  const { token } = theme.useToken()

  return (
    <Flex vertical gap={12}>
      {/* 元信息 */}
      <Flex gap={16} wrap="wrap" style={{ fontSize: token.fontSizeSM, color: token.colorTextSecondary }}>
        <Space size={4}>
          <Text type='secondary'>版本: {data.version}</Text>
        </Space>
        <Space size={4}>
          <Text type='secondary'>{data.publishTime}</Text>
        </Space>
        <Space size={4}>
          <Text type='secondary'>发布人: {data.creatorName}</Text>
        </Space>
      </Flex>

      <Divider style={{ margin: '8px 0' }} />

      {/* 内容 */}
      <Flex
        vertical
        style={{
          lineHeight: 1.8,
          maxHeight: '60vh',
          overflowY: 'auto',
          // 解决方案：
          width: '100%',
          position: 'relative', // 帮助内部绝对定位元素（如语言标签）定位
          paddingBottom: 24,    // 给底部留出缓冲空间，防止最后一个组件贴底
          paddingRight: 6
        }}
      >
        <div style={{ width: '100%', flex: '1 0 auto' }}>
          <AnnouncementMarkdownView content={data.content} />
        </div>
      </Flex>
    </Flex>
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

export const AnnouncementMarkdownView = ({ content }) => {

  const { token } = theme.useToken()

  const markdownComponents = {
    // 1. 标题映射 (H1 - H4)
    h1: ({ children }) => <Title level={2} style={{ marginTop: 24, marginBottom: 16 }}>{children}</Title>,
    h2: ({ children }) => <Title level={3} style={{ marginTop: 20, marginBottom: 14 }}>{children}</Title>,
    h3: ({ children }) => <Title level={4} style={{ marginTop: 16, marginBottom: 12 }}>{children}</Title>,
    h4: ({ children }) => <Title level={5} style={{ marginTop: 12, marginBottom: 10 }}>{children}</Title>,

    // 2. 核心：解决缩进与标号缺失问题
    ul: ({ children }) => (
      <ul style={{
        paddingLeft: 28,
        marginBottom: 16,
        listStyleType: 'disc', // 强制开启圆点
        color: token.colorText
      }}>
        {children}
      </ul>
    ),
    ol: ({ children }) => (
      <ol style={{
        paddingLeft: 28,
        marginBottom: 16,
        listStyleType: 'decimal', // 强制开启数字 1. 2. 3.
        color: token.colorText
      }}>
        {children}
      </ol>
    ),
    li: ({ children }) => (
      <li style={{ marginBottom: 8, lineHeight: 1.8 }}>
        {children}
      </li>
    ),

    // 3. 段落与强调
    p: ({ children }) => <Paragraph style={{ marginBottom: '0', lineHeight: 1.8 }}>{children}</Paragraph>,
    strong: ({ children }) => <Text strong>{children}</Text>,
    em: ({ children }) => <Text italic>{children}</Text>,

    // 4. 链接与分割线
    a: ({ href, children }) => <Link href={href} target="_blank">{children}</Link>,
    hr: () => <Divider style={{ opacity: 0.6 }} />,

    // 引用块 (Blockquote)
    blockquote: ({ children }) => {

      return (
        <Flex
          align="center"
          style={{
            margin: '16px 0',
            padding: `${token.paddingXS}px ${token.paddingLG}px`,
            background: token.colorFillAlter, // 自动适配暗色模式
            borderLeft: `4px solid ${token.colorPrimaryBorder}`,
            borderRadius: `0 ${token.borderRadiusSM}px ${token.borderRadiusSM}px 0`,
          }}
        >
          {/* 直接渲染 children，但通过 CSS 消除内部 p 标签的 margin */}
          <div
            style={{
              color: token.colorTextSecondary,
              fontSize: token.fontSize,
              fontStyle: 'italic', // 增加一点设计感，符合“摘要”的语义
            }}
          >
            {children}
          </div>
        </Flex>
      )
    },
    table: MarkdownTable,
    // 任务列表适配
    input: ({ type, checked }) => {
      if (type === 'checkbox') {
        return <Checkbox checked={checked} style={{ marginRight: 8, verticalAlign: 'middle' }} readOnly />;
      }
      return null;
    },
    code: ({ className, children, ...props }) => {
      // 判定逻辑：
      // 1. 如果有 language- 前缀的 className，通常是代码块 (如 ```java)
      // 2. 如果 children 是多行文本，通常是代码块
      const isBlock = /language-(\w+)/.test(className || '') || String(children).includes('\n');

      if (!isBlock) {
        // --- 行内代码渲染 ---
        return (
          <code
            style={{
              display: 'inline',
              padding: '2px 4px',
              margin: '0 4px',
              borderRadius: token.borderRadiusSM,
              backgroundColor: token.colorFillTertiary,
              fontFamily: 'monospace',
              fontSize: '0.9em',
              border: `1px solid ${token.colorBorderSecondary}`,
            }}
            {...props}
          >
            {children}
          </code>
        );
      }

      // --- 块级代码渲染 ---
      return (
        <pre
          style={{
            background: token.colorFillSecondary,
            padding: 12,
            borderRadius: token.borderRadius,
            overflowX: 'auto',
            marginBottom: 16,
          }}
        >
          <code className={className} {...props}>
            {children}
          </code>
        </pre>
      );
    },
    img: ({ src, alt }) => (
      <div style={{ textAlign: 'center', margin: '24px 0' }}>
        <Image
          src={src}
          alt={alt}
          // 样式控制
          style={{
            maxWidth: '100%',
            borderRadius: token.borderRadiusLG,
            border: `1px solid ${token.colorBorderSecondary}`,
            boxShadow: token.boxShadowTertiary,
          }}
        />
        {/* 图片下方的描述文字 */}
        {alt && (
          <div style={{ marginTop: 8 }}>
            <Text type="secondary" style={{ fontSize: 12 }}>
              {alt}
            </Text>
          </div>
        )}
      </div>
    ),
  }

  return (
    <Flex vertical style={{ lineHeight: 1.8, overflowY: 'auto' }}>
      <ReactMarkdown remarkPlugins={[remarkGfm]} components={markdownComponents}>
        {content}
      </ReactMarkdown>
    </Flex>
  )
}


const MarkdownTable = ({ children }) => {
  // 1. 获取表头数据
  // children 通常包含 thead 和 tbody
  const thead = children.find(child => child.type === 'thead');
  const tbody = children.find(child => child.type === 'tbody');

  // 2. 解析列定义 (Columns)
  // 找到 thead -> tr -> th 列表
  const headerRow = thead?.props?.children;
  const headerCells = Array.isArray(headerRow) ? headerRow[0]?.props?.children : headerRow?.props?.children;

  const columns = React.Children.map(headerCells, (cell, index) => {
    return {
      title: cell.props.children,
      dataIndex: `col${index}`,
      key: `col${index}`,
      align: 'center',
      // 这里的 padding 和对齐可以根据你的设计感进行微调
      onHeaderCell: () => ({
        style: { fontWeight: 600 }
      })
    };
  });

  // 3. 解析行数据 (DataSource)
  // 找到 tbody -> tr 列表
  const bodyRows = tbody?.props?.children || [];

  const dataSource = React.Children.map(bodyRows, (row, rowIndex) => {
    const cells = row?.props?.children || [];
    const rowData = { key: rowIndex };

    React.Children.forEach(cells, (cell, cellIndex) => {
      rowData[`col${cellIndex}`] = cell.props.children;
    });

    return rowData;
  });

  return (
    <Table
      columns={columns}
      dataSource={dataSource}
      pagination={false}
      bordered
      size="middle"
      className="my-4 shadow-sm"
    />
  );
}

export default DynamicCard;