import React, { useMemo } from 'react';
import { Card, Flex, Badge, Avatar, Typography, Statistic, Space, theme, Tag } from 'antd';
import { Trophy } from 'lucide-react';
import { useSelector } from 'react-redux';
import { useIsSuperAdmin } from '../../components/HasPermission';

const { Title, Text } = Typography;

const getGreeting = () => {
  const hour = new Date().getHours();
  if (hour < 6) return '凌晨好'
  if (hour < 9) return '早安'
  if (hour < 12) return '上午好'
  if (hour < 14) return '午安'
  if (hour < 17) return '下午好'
  if (hour < 19) return '傍晚好'
  if (hour < 22) return '晚安'
  return '夜深了'
}

const HeaderCard = () => {

  const { token } = theme.useToken()

  const { fullName, avatar, orgName, posName, todayTaskCount, notificationUnreadCount } = useSelector(state => state.user.userInfo)

  const isSuperAdmin = useIsSuperAdmin()

  // 获取动态问候语
  const greeting = useMemo(() => getGreeting(), [])

  const renderSubTitle = () => {
    // 有职位或有组织，正常显示
    if (posName || orgName) {
      return (
        <Text type="secondary">
          <Space split={posName && orgName ? <Text type="secondary">|</Text> : null}>
            {posName && <Text type="secondary">{posName}</Text>}
            {orgName && <Text type="secondary">{orgName}</Text>}
          </Space>
        </Text>
      );
    }

    // 两者都为空，判断是否为超管
    if (isSuperAdmin) {
      return (
        <Tag bordered={false} color="orange" style={{ margin: 0 }}>
          系统首席架构师
        </Tag>
      )
    }

    // 普通用户且无组织职位（兜底）
    return <Text type="secondary">暂无归属信息</Text>;
  }

  return (
    <Card variant="borderless">
      <Flex align="center" justify="space-between" wrap="wrap" gap="middle">
        <Flex align="center" gap="large">
          <Badge dot status="processing" color={token.colorPrimary}>
            <Avatar
              size={72}
              src={avatar}
              style={{ border: `2px solid ${token.colorPrimaryBg}` }}
            />
          </Badge>
          <Flex gap={10} vertical>
            <Title level={3} style={{ margin: 0 }}>
              {greeting}，{fullName}。
            </Title>
            <Text type="secondary">
              {renderSubTitle()}
            </Text>
          </Flex>
        </Flex>
        <Flex gap={48}>
          <Statistic title="待处理" value={todayTaskCount} suffix="项" />
          <Statistic title="未读消息" value={notificationUnreadCount} valueStyle={{ color: token.colorPrimary }} />
          <Statistic title="贡献度" value={95} prefix={<Trophy size={16} />} />
        </Flex>
      </Flex>
    </Card>
  );
};

export default HeaderCard;