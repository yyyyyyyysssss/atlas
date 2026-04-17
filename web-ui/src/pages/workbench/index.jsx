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
import WeeklyScheduleCard from './WeeklyScheduleCard';

const { Content } = Layout;

const Workbench = () => {

  const { token } = theme.useToken()

  return (
    <Content style={{ padding: token.paddingLG }}>
      <Flex vertical gap={token.marginLG}>
        <HeaderCard />

        <Row gutter={[token.marginLG, token.marginLG]}>
          <Col xs={24} lg={16}>
            <Flex vertical gap={token.marginLG}>
              {/* 快捷开始 */}
              <ShortcutCard />
              {/* 工作日程 */}
              <WeeklyScheduleCard/>
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