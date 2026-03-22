import React from 'react';
import { Card, Flex, Typography, Button, theme } from 'antd';
import { ArrowRight } from 'lucide-react';

const { Text } = Typography;

const DynamicCard = () => {
  const { token } = theme.useToken();

  const dynamicCardStyle = {
    background: `linear-gradient(135deg, ${token.colorPrimary}12 0%, ${token.colorBgContainer} 100%)`,
    border: `1px solid ${token.colorPrimary}18`,
    borderRadius: token.borderRadiusLG,
    boxShadow: 'none',
  };

  return (
    <Card 
      title={<Text strong style={{ color: token.colorPrimary }}>系统动态</Text>} 
      bordered={false} 
      style={dynamicCardStyle}
    >
      <Flex vertical gap="middle">
        <Flex vertical gap={4}>
          <Text strong>Atlas v2.6.0 发布</Text>
          <Text type="secondary" style={{ fontSize: token.fontSizeSM, lineHeight: 1.6 }}>
            优化了 Zookeeper 监听机制，显著降低了多租户场景下的连接开销。
          </Text>
        </Flex>
        <Button 
          type="primary" 
          block 
          variant="filled" 
          icon={<ArrowRight size={14} />}
        >
          查看详情
        </Button>
      </Flex>
    </Card>
  );
};

export default DynamicCard;