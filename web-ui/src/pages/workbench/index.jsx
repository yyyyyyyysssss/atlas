import React, { useMemo, useState } from 'react';
import {
  Col, Row,
  Flex, Layout, theme
} from 'antd';
import './index.css'
import HeaderCard from './components/HeaderCard';
import ProjectShortcutCard from './components/ProjectShortcutCard';
import AccountSecurityCard from './components/AccountSecurityCard';
import UserActivityCard from './components/UserActivityCard';
import { useSelector } from 'react-redux';
import IntegrationGuideCard from './components/IntegrationGuideCard';
import MyApplicationsCard from './components/MyApplicationsCard';

const { Content } = Layout;

const Workbench = () => {

  const { token } = theme.useToken()

  const roles = useSelector((state) => state.auth.authInfo?.roles || [])

  const isDeveloper = useMemo(() => roles.includes('developer'), [roles])

  return (
    <Content style={{ padding: token.paddingLG }}>
      <Flex vertical gap={token.marginLG}>
        <HeaderCard />

        <Row gutter={[token.marginLG, token.marginLG]}>
          <Col xs={24} lg={16}>
            <Flex vertical gap={token.marginLG}>
              {isDeveloper === true ? <ProjectShortcutCard /> : <MyApplicationsCard />}
              {isDeveloper === true ? <IntegrationGuideCard /> : <AccountSecurityCard />}
            </Flex>
          </Col>

          <Col xs={24} lg={8} xl={7}>
            <UserActivityCard />
          </Col>
        </Row>
      </Flex>
    </Content>
  );
};

export default Workbench;