import React, { useState } from 'react';
import { Card, Flex, Typography, Button, theme, Modal, Tag, Divider, Space } from 'antd';
import { ArrowRight } from 'lucide-react';
import ReactMarkdown from 'react-markdown';

const { Text, Paragraph, Title } = Typography;

// 模拟数据
const DYNAMIC_DATA = {
  id: '1',
  title: 'Atlas v2.6.0 发布',
  description: '新增网关限流、多层缓存及多种登录方式，系统性能和安全性全面升级。',
  type: '发版',
  typeColor: 'blue',
  timestamp: '2024-03-20 14:30',
  author: '系统管理员',
  version: 'v2.6.0',
  content: `
### 主要改进

#### 1. 网关限流优化
- 支持多种限流策略（**令牌桶**、**滑动窗口**）
- 动态限流阈值配置
- 精确到 IP、用户、接口级别的流量控制

#### 2. 缓存机制升级
> **注意**：本次升级涉及 Redis 节点重启，请避开业务高峰期。

- 多层缓存架构（本地缓存 + Redis）
- 智能缓存预热

### 性能提升
- API 平均响应时间降低 **60%**
- 系统吞吐量提升至 **10 万并发/秒**

---

### 升级建议
- 先在测试环境验证各登录方式的兼容性
- 逐步启用网关限流，监控系统稳定性
  `
};

const DynamicCard = () => {
  const { token } = theme.useToken();
  const [modalOpen, setModalOpen] = useState(false);

  const dynamicCardStyle = {
    background: `linear-gradient(135deg, ${token.colorPrimary}12 0%, ${token.colorBgContainer} 100%)`,
    border: `1px solid ${token.colorPrimary}18`,
    borderRadius: token.borderRadiusLG,
    boxShadow: 'none',
  };

  const handleViewDetails = () => {
    setModalOpen(true);
  };


  return (
    <>
      <Card
        title={
          <Flex justify="space-between" align="center" style={{ width: '100%' }}>
            <Text strong style={{ color: token.colorPrimary }}>系统动态</Text>
            <Tag color={DYNAMIC_DATA.typeColor}>{DYNAMIC_DATA.type}</Tag>
          </Flex>
        }
        bordered={false}
        style={dynamicCardStyle}
      >
        <Flex vertical gap="middle">
          <Flex vertical gap={4}>
            <Text strong>{DYNAMIC_DATA.title}</Text>
            <Text type="secondary" style={{ fontSize: token.fontSizeSM, lineHeight: 1.6 }}>
              {DYNAMIC_DATA.description}
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
        title={DYNAMIC_DATA.title}
        open={modalOpen}
        onCancel={() => setModalOpen(false)}
        footer={null}
        width={700}
      >
        <Flex vertical gap={12}>
          {/* 元信息 */}
          <Flex gap={16} wrap="wrap" style={{ fontSize: token.fontSizeSM, color: token.colorTextSecondary }}>
            <Space size={4}>
              <Text type='secondary'>版本: {DYNAMIC_DATA.version}</Text>
            </Space>
            <Space size={4}>
              <Text type='secondary'>{DYNAMIC_DATA.timestamp}</Text>
            </Space>
            <Space size={4}>
              <Text type='secondary'>发布人: {DYNAMIC_DATA.author}</Text>
            </Space>
          </Flex>

          <Divider style={{ margin: '8px 0' }} />

          {/* 内容 */}
          <Flex vertical style={{ lineHeight: 1.8, maxHeight: '60vh', overflowY: 'auto' }}>
            <ReactMarkdown>
              {DYNAMIC_DATA.content}
            </ReactMarkdown>
          </Flex>
        </Flex>
      </Modal>
    </>
  );
};

export default DynamicCard;