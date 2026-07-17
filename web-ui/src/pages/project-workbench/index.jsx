import React, { useState } from 'react';
import { Card, Row, Col, Button, Flex, Typography, Modal, Form, Input, theme, Space, Layout } from 'antd';
import { Plus, Folder, Zap, Octagon } from 'lucide-react';
import './index.css';
import HeaderCard from '../workbench/HeaderCard';
import { useAuth } from '../../router/AuthProvider';
import { useNavigate } from 'react-router-dom';

const { Text } = Typography;

const { Content } = Layout;

const ProjectWorkbench = () => {
    const { token } = theme.useToken()

    const [isModalOpen, setIsModalOpen] = useState(false)

    const [form] = Form.useForm()

    const { loadDomain } = useAuth()

    const navigate = useNavigate()

    // 模拟初始项目数据
    const [projects, setProjects] = useState([
        { id: '1', name: 'atlas', code: 'atlas-ee82f65a' },
    ]);

    // 跳转到项目专属页面
    const handleJumpToProject = async (project) => {
        const { code } = project
        navigate(`/project/${code}`)
    };

    // 打开新增弹窗
    const showModal = () => {
        setIsModalOpen(true);
    };

    // 保存新项目
    const handleSave = () => {
        form.validateFields().then((values) => {
            const newProject = {
                id: Date.now().toString(), // 简单生成个唯一ID
                name: values.projectName,
                code: values.projectCode,
            };
            setProjects([...projects, newProject]);
            setIsModalOpen(false);
            form.resetFields();
        }).catch((info) => {
            console.log('校验失败:', info);
        });
    };

    // 取消新增
    const handleCancel = () => {
        setIsModalOpen(false);
        form.resetFields();
    };

    return (
        <Content style={{ padding: token.paddingLG }}>
            <Flex vertical gap={token.marginLG}>
                <HeaderCard />
                <Flex vertical gap={token.marginLG}>
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
                                            <Octagon size={30} color={token.colorPrimary} />
                                            <Flex vertical align="start" gap={2}>
                                                <Text style={{ fontSize: token.fontSizeXL }} strong>{item.name}</Text>
                                                <Text type="secondary" style={{ fontSize: token.fontSizeSM }}>项目ID：{item.code}</Text>
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
                                    onClick={showModal}
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
                </Flex>
            </Flex>
            <Modal
                title="新增项目"
                open={isModalOpen}
                onOk={handleSave}
                onCancel={handleCancel}
                okText="保存"
                cancelText="取消"
                destroyOnHidden
            >
                <Form
                    form={form}
                    layout="vertical"
                    style={{ marginTop: '20px' }}
                >
                    <Form.Item
                        name="projectName"
                        label="项目名称"
                        rules={[{ required: true, message: '请输入项目名称' }]}
                    >
                        <Input placeholder="请输入项目名称" />
                    </Form.Item>
                    <Form.Item
                        name="description"
                        label="项目描述"
                    >
                        <Input.TextArea placeholder="为项目添加说明" />
                    </Form.Item>
                </Form>
            </Modal>
        </Content>
    );
};

export default ProjectWorkbench;