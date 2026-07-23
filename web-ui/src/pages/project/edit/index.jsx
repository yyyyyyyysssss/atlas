import { useRequest } from 'ahooks'
import useFullParams from '../../../hooks/useFullParams'
import './index.css'
import { getProjectDetail, saveProject } from '../../../services/ProjectService'
import { App, Button, Col, Flex, Form, Input, Row, theme } from 'antd'
import { useNavigate } from 'react-router-dom'
import Loading from '../../../components/loading'

const ProjectEdit = () => {

    const { id } = useFullParams()

    const [form] = Form.useForm()

    const navigate = useNavigate()

    const { token } = theme.useToken()

    const { message } = App.useApp()

    const { runAsync: saveProjectAsync, loading: saveProjectLoading } = useRequest(saveProject, {
        manual: true
    })

    const { data: projectData, loading: detailLoading, refresh: refreshDetail } = useRequest(
        () => getProjectDetail(id),
        {
            ready: !!id,
            refreshDeps: [id],
            onSuccess: (data) => {
                if (data) {
                    form.setFieldsValue({
                        ...data,
                    });
                }
            }
        }
    )

    const mode = id ? 'edit' : 'create';

    const onFinish = async (values) => {
        const res = await saveProjectAsync(values)
        message.success('保存成功');
        if (mode === 'create') {
            const { id } = res
            navigate(`/project-workspace/${id}`, { replace: true });
        }
    }

    const handleCancel = () => {
        navigate('/project-workspace')
    }

    return (
        <Loading spinning={detailLoading || saveProjectLoading} full>
            <Flex
                gap={50}
                style={{
                    padding: `${token.paddingLG}px ${token.paddingXL}px`,
                    margin: '0',
                    width: '100%',
                }}
            >
                <Form
                    form={form}
                    style={{ width: '100%' }}
                    labelCol={{ span: 3 }}
                    wrapperCol={{ span: 21 }}
                    layout="horizontal"
                    autoComplete="off"
                    onFinish={onFinish}
                >
                    <Form.Item name="id" noStyle />
                    <Row gutter={16}>
                        <Col span={24}>
                            <Form.Item
                                label="项目名称："
                                name="projectName"
                                rules={[{ required: true, message: '请输入项目名称' }, { max: 40, message: '项目名称最多 40 个字符' }]}
                            >
                                <Input placeholder="请输入项目名称" maxLength={40} showCount />
                            </Form.Item>
                        </Col>
                    </Row>

                    {mode === 'edit' && (
                        <Row gutter={16}>
                            <Col span={24}>
                                <Form.Item
                                    label="项目编码："
                                    name="projectCode"
                                >
                                    <Input disabled />
                                </Form.Item>
                            </Col>
                        </Row>
                    )}
                    <Row gutter={16}>
                        <Col span={24}>
                            <Form.Item
                                label="描述："
                                name="description"
                                rules={[{ max: 200, message: '描述最多 200 个字符' }]}
                            >
                                <Input.TextArea
                                    rows={4}
                                    placeholder="简要描述该项目的功能和用途..."
                                    maxLength={200}
                                    showCount
                                />
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={24}>
                            <Flex gap={12} justify="end">
                                <Button
                                    type="primary"
                                    htmlType="submit"
                                    loading={saveProjectLoading}
                                >
                                    保存
                                </Button>
                                <Button
                                    type="default"
                                    onClick={handleCancel}
                                >
                                    取消
                                </Button>
                            </Flex>
                        </Col>
                    </Row>

                </Form>
            </Flex>
        </Loading>
    )
}

export default ProjectEdit