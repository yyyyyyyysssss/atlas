import React, { useMemo, useState } from 'react';
import {
  Card, Col, Row, List, Avatar, Button, Statistic, Tag,
  Flex, Typography, Layout, Space, theme, Badge, Tooltip, Modal, Checkbox, message
} from 'antd';
import './index.css'
import {
  AppWindow, MessageSquare, Zap, ArrowRight, Trophy,
  Users, ShieldCheck, Bell, Mail, Info, AlertCircle,
  ChevronDown, ChevronUp, Plus, LayoutGrid, Settings2
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { findRouteByPath } from '../../router/router';

const { Title, Text } = Typography;
const { Content } = Layout;

const Workbench = () => {

  const navigate = useNavigate()

  const { token } = theme.useToken();
  const [expanded, setExpanded] = useState(false);

  // 初始显示的快捷功能
  const [shortCuts, setShortCuts] = useState([
    { label: '组织架构', path: '/system/org' },
    { label: '总览', path: '/dashboard/overview' },
    { label: '用户管理', path: '/system/user' },
    { label: '菜单管理', path: '/system/menu' }
  ]);

  const shortCutList = useMemo(() => {
    return shortCuts.map(item => {
      const route = findRouteByPath(item.path);
      return {
        ...item,
        icon: route?.defaultIcon || <Zap size={16} />, // 从路由查找 icon
      };
    });
  }, [shortCuts]);

  // 模拟团队数据
  const allMembers = Array.from({ length: 10 }).map((_, i) => ({
    id: i,
    name: `成员 ${i + 1}`,
    role: i === 0 ? '负责人' : '开发',
    status: i % 3 === 0 ? 'online' : 'offline',
    avatar: `https://api.dicebear.com/7.x/avataaars/svg?seed=${i + 10}`
  }));

  const visibleMembers = expanded ? allMembers : allMembers.slice(0, 6);

  const dynamicCardStyle = {
    background: `linear-gradient(135deg, ${token.colorPrimary}12 0%, ${token.colorBgContainer} 100%)`,
    border: `1px solid ${token.colorPrimary}18`,
    borderRadius: token.borderRadiusLG,
    boxShadow: 'none',
  };

  const notifications = [
    { id: 1, title: '系统升级：Atlas v2.6 核心模块已部署', time: '10:30', type: 'system', read: false },
    { id: 2, title: '来自 张三 的站内信：请查收权限附件', time: '09:15', type: 'message', read: false },
    { id: 3, title: '安全告警：账号在异地尝试登录', time: '昨天', type: 'alert', read: true },
  ];

  const jumpTo = (path) => {
    navigate(path)
  }

  return (
    <Content style={{ padding: token.paddingLG }}>
      <Flex vertical gap={token.marginLG}>

        {/* 1. 顶部身份卡片 */}
        <Card bordered={false}>
          <Flex align="center" justify="space-between" wrap="wrap" gap="middle">
            <Flex align="center" gap="large">
              <Badge dot color={token.colorSuccess}>
                <Avatar size={72} src="https://api.dicebear.com/7.x/avataaars/svg?seed=AtlasDev" style={{ border: `2px solid ${token.colorPrimaryBg}` }} />
              </Badge>
              <Flex vertical>
                <Title level={3} style={{ margin: 0 }}>早安，YS。</Title>
                <Text type="secondary">
                  <Space split={<Text type="separator">|</Text>}>
                    <span>核心架构师</span>
                    <Space size="small"> 数字化运营中心</Space>
                  </Space>
                </Text>
              </Flex>
            </Flex>
            <Flex gap={48}>
              <Statistic title="待处理" value={3} suffix="项" />
              <Statistic title="未读消息" value={12} valueStyle={{ color: token.colorPrimary }} />
              <Statistic title="贡献度" value={95} prefix={<Trophy size={16} />} />
            </Flex>
          </Flex>
        </Card>

        <Row gutter={[token.marginLG, token.marginLG]}>
          <Col xs={24} lg={16}>
            <Flex vertical gap={token.marginLG}>

              {/* 2. 快捷开始 - 增加了添加功能 */}
              <Card
                title={<Space><Zap size={18} fill={token.colorWarning} stroke={token.colorWarning} /> 快捷开始</Space>}
                bordered={false}
              >
                <Row gutter={[16, 16]}>
                  {shortCutList.map((item) => (
                    <Col span={6} key={item.label}>
                      <Button
                        block
                        size="large"
                        onClick={() => jumpTo(item.path)}
                        type="text"
                        className="atlas-float-trigger"
                        style={{
                          background: token.colorFillAlter,
                          height: '60px',
                          borderRadius: token.borderRadiusLG
                        }}
                      >
                        <Flex vertical align="center" justify="center" gap={4}>
                          {item?.icon}
                          <Text size="small" strong>{item.label}</Text>
                        </Flex>
                      </Button>
                    </Col>
                  ))}
                  {/* 添加按钮占位符 */}
                  {shortCuts.length < 8 && (
                    <Col span={6}>
                      <Button
                        block
                        size="large"
                        type="dashed"
                        style={{ height: '60px', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: '4px', borderRadius: token.borderRadiusLG }}
                      >
                        <Plus size={16} />
                        <Text type="secondary" style={{ fontSize: '12px' }}>添加</Text>
                      </Button>
                    </Col>
                  )}
                </Row>
              </Card>

              {/* 3. 消息通知 */}
              <Card title={<Space><Bell size={18} /> 消息通知</Space>} extra={<Button type="link">全部标记已读</Button>} bordered={false}>
                <List
                  itemLayout="horizontal"
                  dataSource={notifications}
                  renderItem={(item) => (
                    <List.Item
                      className="atlas-float-trigger"
                      actions={[<Button type="link">查看</Button>]}
                    >
                      <List.Item.Meta
                        avatar={<Avatar style={{ backgroundColor: item.type === 'alert' ? token.colorErrorBg : token.colorFillAlter, color: item.type === 'alert' ? token.colorError : token.colorPrimary }} icon={item.type === 'message' ? <Mail size={14} /> : (item.type === 'alert' ? <AlertCircle size={14} /> : <Info size={14} />)} />}
                        title={<Space><Text strong={!item.read}>{item.title}</Text>{!item.read && <Badge status="processing" />}</Space>}
                        description={<Text type="secondary" style={{ fontSize: '12px' }}>{item.time} · {item.type === 'system' ? '系统公告' : '站内信'}</Text>}
                      />
                    </List.Item>
                  )}
                />
              </Card>
            </Flex>
          </Col>

          <Col xs={24} lg={8}>
            <Flex vertical gap={token.marginLG}>
              {/* 4. 系统动态 */}
              <Card title={<Text strong style={{ color: token.colorPrimary }}>系统动态</Text>} bordered={false} style={dynamicCardStyle}>
                <Flex vertical gap="middle">
                  <Flex vertical gap={4}>
                    <Text strong>Atlas v2.6.0 发布</Text>
                    <Text type="secondary" style={{ fontSize: token.fontSizeSM, lineHeight: 1.6 }}>优化了 Zookeeper 监听机制，显著降低了多租户场景下的连接开销。</Text>
                  </Flex>
                  <Button type="primary" block variant="filled" icon={<ArrowRight size={14} />}>查看详情</Button>
                </Flex>
              </Card>

              {/* 5. 我的团队 */}
              <Card title={<Space><Users size={16} /> 我的团队</Space>} bordered={false}>
                <Flex vertical gap="large">
                  <Row gutter={[16, 24]}>
                    {visibleMembers.map((m) => (
                      <Col span={8} key={m.id}>
                        <Flex className="atlas-float-trigger" vertical align="center" gap={8}>
                          <Badge dot status={m.status === 'online' ? 'success' : 'default'} offset={[-4, 32]}>
                            <Avatar src={m.avatar} size="large" />
                          </Badge>
                          <Flex vertical align="center" style={{ width: '100%', overflow: 'hidden' }}>
                            <Text strong size="small" ellipsis style={{ maxWidth: '100%' }}>{m.name}</Text>
                            <Text type="secondary" style={{ fontSize: '10px' }}>{m.role}</Text>
                          </Flex>
                        </Flex>
                      </Col>
                    ))}
                  </Row>

                  {allMembers.length > 6 && (
                    <Button
                      type="text"
                      block
                      onClick={() => setExpanded(!expanded)}
                      style={{ color: token.colorTextSecondary, fontSize: '12px' }}
                    >
                      <Space size={4} align="center">
                        <Text type="secondary" style={{ fontSize: '12px', display: 'flex', alignItems: 'center', gap: '4px' }}>
                          {expanded ? (
                            <ChevronUp size={14} style={{ marginTop: '2px' }} />
                          ) : (
                            <ChevronDown size={14} style={{ marginTop: '2px' }} />
                          )}
                          {expanded ? '收起成员' : `展开更多 (${allMembers.length - 6}+)`}
                        </Text>
                      </Space>
                    </Button>
                  )}
                </Flex>
              </Card>
            </Flex>
          </Col>
        </Row>
      </Flex>
    </Content>
  );
};

export default Workbench;