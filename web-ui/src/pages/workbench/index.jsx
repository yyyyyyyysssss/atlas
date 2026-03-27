import React, { useMemo, useState } from 'react';
import {
  Col, Row,
  Flex, Layout, theme
} from 'antd';
import './index.css'
import HeaderCard from './HeaderCard';
import ShortcutCard from './ShortcutCard';
import NotificationCard from './NotificationCard';
import TeamCard from './TeamCard';
import DynamicCard from './DynamicCard';
import Cookies from 'js-cookie'
import { useSse } from '../../hooks/useSse';

const { Content } = Layout;

const Workbench = () => {

  const { token } = theme.useToken()

  const accessToken = Cookies.get("accessToken");

  const sseUrl = `/api/notification/v1/notification/sse/subscribe?terminal=web&access_token=${accessToken}`;

  const { status } = useSse(sseUrl, {
    // 2. 处理业务消息 (对应后端的 message_event)
    onMessage: (data) => {
      console.log('收到 Atlas 实时消息:', data);
    },

    // 3. 连接成功后的逻辑
    onConnected: () => {
      console.log('通知服务已就绪，开始监听...');
    }
  });


  const notifications = [
    { id: 1, title: '系统升级：Atlas v2.6 核心模块已部署', time: '10:30', type: 'system', read: false },
    { id: 2, title: '来自 张三 的站内信：请查收权限附件', time: '09:15', type: 'message', read: false },
    { id: 3, title: '安全告警：账号在异地尝试登录', time: '昨天', type: 'alert', read: true },
  ];

  return (
    <Content style={{ padding: token.paddingLG }}>
      <Flex vertical gap={token.marginLG}>
        <HeaderCard />

        <Row gutter={[token.marginLG, token.marginLG]}>
          <Col xs={24} lg={16}>
            <Flex vertical gap={token.marginLG}>
              {/* 快捷开始 */}
              <ShortcutCard />
              {/* 消息通知 */}
              <NotificationCard
                data={notifications}
              />
            </Flex>
          </Col>

          <Col xs={24} lg={8}>
            <Flex vertical gap={token.marginLG}>
              {/* 系统动态 */}
              <DynamicCard />
              {/* 我的团队 */}
              <TeamCard />
            </Flex>
          </Col>
        </Row>
      </Flex>
    </Content>
  );
};

export default Workbench;