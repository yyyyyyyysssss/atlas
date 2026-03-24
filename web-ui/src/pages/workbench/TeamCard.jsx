import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Flex, Badge, Avatar, Typography, Button, Space, theme } from 'antd';
import { Users, ChevronDown, ChevronUp } from 'lucide-react';
import { useRequest } from 'ahooks';
import { fetchUserTeamMember } from '../../services/UserProfileService';

const { Text } = Typography;

const TeamCard = () => {
  const { token } = theme.useToken()

  const [expanded, setExpanded] = useState(false)

  const [memberData, setMemberData] = useState([])

  const { runAsync: getUserTeamMemberAsync, loading: getUserTeamMemberLoading } = useRequest(fetchUserTeamMember, {
    manual: true
  })

  const fetchData = async () => {
    const result = await getUserTeamMemberAsync()
    setMemberData(result)
  }

  useEffect(() => {
    fetchData()
  }, [])

  const visibleMembers = expanded ? memberData : memberData.slice(0, 6);

  return (
    <Card title={<Space><Users size={16} /> 我的团队</Space>} variant="borderless">
      <Flex vertical gap="large">
        <Row gutter={[16, 24]}>
          {visibleMembers.map((m) => (
            <Col span={8} key={m.id}>
              <Flex className="atlas-float-trigger" vertical align="center" gap={8}>
                <Badge dot status={m.status === 'online' ? 'success' : 'default'} offset={[-4, 32]}>
                  <Avatar src={m.avatar} size="large" />
                </Badge>
                <Flex vertical align="center" style={{ width: '100%', overflow: 'hidden' }}>
                  <Text strong size="small" ellipsis style={{ maxWidth: '100%' }}>{m.userFullName}</Text>
                  <Text type="secondary" style={{ fontSize: '10px' }}>{m.role}</Text>
                </Flex>
              </Flex>
            </Col>
          ))}
        </Row>
        {memberData.length > 6 && (
          <Button
            type="text"
            block
            onClick={() => setExpanded(!expanded)}
            style={{ color: token.colorTextSecondary, fontSize: '12px' }}
          >
            <Space size={4}>
              <Typography.Text type='secondary' style={{ fontSize: 12 }}>
                {expanded ? <ChevronUp size={14} /> : <ChevronDown size={14} />}
              </Typography.Text>
              <Typography.Text type='secondary' style={{ fontSize: 12 }}>
                {expanded ? '收起成员' : `展开更多 (${memberData.length - 6}+)`}
              </Typography.Text>
            </Space>
          </Button>
        )}
      </Flex>
    </Card>
  );
};

export default TeamCard;