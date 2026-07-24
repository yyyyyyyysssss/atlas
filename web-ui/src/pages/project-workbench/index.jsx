import React, { useState } from 'react';
import { Card, Row, Col, Button, Flex, Typography, Modal, Form, Input, theme, Space, Layout } from 'antd';
import { Plus, Folder, Zap, Octagon } from 'lucide-react';
import './index.css';
import HeaderCard from '../workbench/HeaderCard';
import { useAuth } from '../../router/AuthProvider';
import { useNavigate } from 'react-router-dom';
import ProjectShortcutCard from './components/ProjectShortcutCard';
import UserActivityCard from './components/UserActivityCard';
import IntegrationGuideCard from './components/IntegrationGuideCard';

const { Text } = Typography;

const { Content } = Layout;

const ProjectWorkbench = () => {

    const { token } = theme.useToken()


    return (
        <Content style={{ padding: token.paddingLG }}>
            <Flex vertical gap={token.marginLG}>
                <HeaderCard />
                <Row gutter={[token.marginLG, token.marginLG]}>
                    <Col xs={24} lg={16} xl={17}>
                        <Flex vertical gap={token.marginLG}>
                            <ProjectShortcutCard />
                            <IntegrationGuideCard />
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

export default ProjectWorkbench;