import React, { useEffect, useState } from 'react';
import { Card, Flex, Typography, Button, theme, Modal, Tag, Divider, Space, Checkbox } from 'antd';
import { ArrowRight } from 'lucide-react';
import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import { useRequest } from 'ahooks';
import { getAnnouncementLatest } from '../../services/NotificationService';
import { AnnouncementType } from '../../enums/notification';

const { Title, Text, Paragraph, Link } = Typography;


const DynamicCard = () => {
  const { token } = theme.useToken()

  const [modalOpen, setModalOpen] = useState(false)

  const { data: latestData = {}, loading: latestLoading, run: fetchLatest } = useRequest(getAnnouncementLatest, { manual: true })

  useEffect(() => {
    fetchLatest()
  }, [])

  const dynamicCardStyle = {
    background: `linear-gradient(135deg, ${token.colorPrimary}12 0%, ${token.colorBgContainer} 100%)`,
    border: `1px solid ${token.colorPrimary}18`,
    borderRadius: token.borderRadiusLG,
    boxShadow: 'none',
  };

  const handleViewDetails = () => {
    setModalOpen(true);
  };

  const typeConfig = AnnouncementType[latestData?.type] || { label: latestData?.type, color: 'default' };

  return (
    <>
      <Card
        title={
          <Flex justify="space-between" align="center" style={{ width: '100%' }}>
            <Text strong style={{ color: token.colorPrimary }}>系统动态</Text>
            <Tag color={typeConfig.color} variant="flat">{typeConfig.label}</Tag>
          </Flex>
        }
        bordered={false}
        loading={latestLoading}
        style={dynamicCardStyle}
      >
        <Flex vertical gap="middle">
          <Flex vertical gap={4}>
            <Text strong>{latestData.title}</Text>
            <Text type="secondary" style={{ fontSize: token.fontSizeSM, lineHeight: 1.6 }}>
              {latestData.description}
            </Text>
          </Flex>
          <Button
            type="primary"
            block
            variant="filled"
            icon={<ArrowRight size={14} />}
            onClick={handleViewDetails}
          >
            查看详情
          </Button>
        </Flex>
      </Card>

      {/* 详情模态框 */}
      <Modal
        title={latestData.title}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        footer={null}
        width={700}
      >
        <AnnouncementDetailView data={latestData} />
      </Modal>
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
      <Flex vertical style={{ lineHeight: 1.8, maxHeight: '60vh', overflowY: 'auto' }}>
        <AnnouncementMarkdownView
          content={data.content}
        />
      </Flex>
    </Flex>
  );
};


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
    p: ({ children }) => <Paragraph style={{ marginBottom: 16, lineHeight: 1.8 }}>{children}</Paragraph>,
    strong: ({ children }) => <Text strong>{children}</Text>,
    em: ({ children }) => <Text italic>{children}</Text>,

    // 4. 链接与分割线
    a: ({ href, children }) => <Link href={href} target="_blank">{children}</Link>,
    hr: () => <Divider style={{ opacity: 0.6 }} />,

    // 5. 引用块 (Blockquote)
    blockquote: ({ children }) => (
      <div style={{
        padding: '8px 20px',
        margin: '16px 0',
        borderLeft: `4px solid ${token.colorPrimary}`,
        background: token.colorFillAlter,
        borderRadius: `0 ${token.borderRadiusSM}px ${token.borderRadiusSM}px 0`
      }}>
        {children}
      </div>
    ),

    // 8. 任务列表适配
    input: ({ type, checked }) => {
      if (type === 'checkbox') {
        return <Checkbox checked={checked} style={{ marginRight: 8, verticalAlign: 'middle' }} readOnly />;
      }
      return null;
    }
  }

  return (
    <Flex vertical style={{ lineHeight: 1.8, maxHeight: '60vh', overflowY: 'auto' }}>
      <ReactMarkdown remarkPlugins={[remarkGfm]} components={markdownComponents}>
        {content}
      </ReactMarkdown>
    </Flex>
  )
}

export default DynamicCard;