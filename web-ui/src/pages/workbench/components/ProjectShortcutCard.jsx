import React, { useState } from 'react';
import { Card, Row, Col, Button, Flex, Typography, Modal, Form, Input, theme, Space, Layout } from 'antd';
import { Plus, Folder, Zap, Octagon, Box } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../../../router/AuthProvider';
import { getProjectPage } from '../../../services/ProjectService';
import { useRequest } from 'ahooks';


const { Text } = Typography;

const ProjectShortcutCard = ({ limit = 3 }) => {

    const { token } = theme.useToken()

    const { loadDomain } = useAuth()

    const navigate = useNavigate()

    const {
        data: projectData,
        loading: getProjectPageLoading,
        runAsync: getProjectPageAsync
    } = useRequest(
        () => getProjectPage(1, limit, 'active'),
    )

    const projects = projectData?.list || []

    // 跳转到项目专属页面
    const handleJumpToProject = async (project) => {
        const { projectCode } = project
        navigate(`/project/${projectCode}`)
    }

    const handleAddProject = () => {
        navigate('/project-workspace/create', { replace: true })
    }

    return (
        <Card
            title={
                <Space>
                    <Zap size={18} fill={token.colorWarning} stroke={token.colorWarning} />
                    <span>快速访问</span>
                </Space>
            }
            variant="borderless"
        >
            <Row gutter={[16, 16]}>
                {/* 项目列表渲染 */}
                {projects.map((item) => (
                    <Col span={6} key={item.id}>
                        <Button
                            block
                            size="large"
                            onClick={() => handleJumpToProject(item)}
                            type="text"
                            className="atlas-float-trigger"
                            style={{
                                background: token.colorFillAlter,
                                height: '80px',
                                borderRadius: token.borderRadiusLG,
                            }}
                        >
                            <Flex align="center" gap={12} style={{ width: '100%', padding: '0 8px' }}>
                                <Box size={30} color={token.colorPrimary} />
                                <Flex vertical align="start" gap={2}>
                                    <Text style={{ fontSize: token.fontSizeLG }} strong>{item.projectName}</Text>
                                    <Text type="secondary" style={{ fontSize: token.fontSizeSM }}>项目标识：{item.projectCode}</Text>
                                </Flex>
                            </Flex>
                        </Button>
                    </Col>
                ))}

                {/* 新增项目虚线按钮 */}
                <Col span={6}>
                    <Button
                        block
                        size="large"
                        type="dashed"
                        onClick={handleAddProject}
                        style={{ height: '80px', borderRadius: token.borderRadiusLG }}
                    >
                        <Flex vertical align="center" justify="center" gap={4}>
                            <Text type="secondary"><Plus size={16} /></Text>
                            <Text type="secondary" style={{ fontSize: token.fontSize }}>新增项目</Text>
                        </Flex>
                    </Button>
                </Col>
            </Row>
        </Card>
    )
}

export default ProjectShortcutCard