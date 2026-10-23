import { useRef, useState } from 'react';
import './index.css';
import { Box, CodeXml, FileKey, KeyRound } from 'lucide-react';
import { Outlet, useLocation, useNavigate } from 'react-router-dom';
import { useDomain } from '../../../router/DomainProvider';
import { List, theme, App, Card, Avatar, Typography, Tooltip, Flex, Button, Tag, Badge, Switch, Popconfirm, Modal, Form, Input, Row, Col } from "antd";
import { useInfiniteScroll, useRequest } from "ahooks";
import { FolderOutlined, DeleteOutlined, EditOutlined, FolderOpenOutlined, PlusOutlined, UndoOutlined, RightOutlined, PoweroffOutlined, PauseCircleOutlined, PlayCircleOutlined } from "@ant-design/icons";
import { getApplicationDetail, getApplicationPage, saveApplication, updateApplicationEnabled } from '../../../services/ApplicationService';
import Loading from '../../../components/loading';
import SmartUpload from '../../../components/smart-upload';

const { Text, Title, Paragraph } = Typography;
const { useToken } = theme;
const PAGE_SIZE = 10;

const ProjectApplication = () => {

    const { token } = useToken();

    const navigate = useNavigate();

    const scrollRef = useRef(null);

    const { message, modal } = App.useApp();

    const { domainId } = useDomain()

    const [form] = Form.useForm()

    const [applicationModal, setApplicationModal] = useState({
        'open': false,
        'isEdit': false
    })

    const { runAsync: updateApplicationEnabledAsync, loading: updateApplicationEnabledLoading } = useRequest(updateApplicationEnabled, {
        manual: true
    });

    const { runAsync: saveApplicationAsync, loading: saveApplicationLoading } = useRequest(saveApplication, {
        manual: true
    });

    const { runAsync: detailAsync, loading: detailLoading } = useRequest(getApplicationDetail, {
        manual: true
    });

    // 无限滚动加载
    const {
        data,
        loading,
        loadingMore,
        noMore,
        reload,
        mutate
    } = useInfiniteScroll(
        async (d) => {
            const pageNum = d ? d.nextPage : 1;
            const res = await getApplicationPage(domainId, pageNum, PAGE_SIZE);
            const hasMore = pageNum * PAGE_SIZE < res.total;

            return {
                list: res.list,
                total: res.total,
                nextPage: hasMore ? pageNum + 1 : pageNum,
                hasMore: hasMore
            };
        },
        {
            target: scrollRef,
            isNoMore: (d) => !d?.hasMore,
        }
    );

    const applicationList = data?.list || []


    const handleManageClient = (application) => {
        navigate(`/project/${domainId}/application/${application.id}/client`);
    }

    const hancleCreate = () => {
        setApplicationModal({
            'open': true,
            'isEdit': false
        })
    }

    const handleEnabled = async (application, enabled) => {
        await updateApplicationEnabledAsync(domainId, application.id, enabled)
        // 刷新列表
        reload()

    }

    const handleEdit = async (application) => {
        setApplicationModal({
            'open': true,
            'isEdit': true
        })
        const data = await detailAsync(domainId, application.id)
        form.setFieldsValue(data)
    }

    const handleSave = async () => {
        const saveData = await form.validateFields()
        await saveApplicationAsync(domainId, saveData)
        message.success('操作成功')
        handleModalClose()
        reload()
    }

    const handleModalClose = () => {
        setApplicationModal({
            'open': false,
            'isEdit': false
        })
    }

    return (
        <div style={{ padding: "16px 24px" }}>
            <Flex justify="space-between" align="flex-start" style={{ marginBottom: 20 }}>
                <div>
                    <Title level={3} style={{ margin: 0 }}>
                        应用管理
                    </Title>
                    <Paragraph type="secondary" style={{ margin: "4px 0 0 0", fontSize: 13 }}>
                        管理应用信息与访问配置，统一维护应用关联的认证客户端及单点登录集成。
                    </Paragraph>
                </div>
                <Button
                    type="primary"
                    icon={<PlusOutlined />}
                    size="middle"
                    onClick={hancleCreate}
                    style={{ borderRadius: token.borderRadius }}
                >
                    创建应用
                </Button>
            </Flex>


            <div
                ref={scrollRef}
                style={{
                    height: 550,
                    overflow: "auto",
                    paddingRight: 8
                }}
            >
                {loading && applicationList.length === 0 ? (
                    <div style={{ padding: 20 }}>
                        <Loading spinning />
                    </div>
                ) : (
                    <>
                        <List
                            dataSource={applicationList}
                            split={false}
                            loading={loading || updateApplicationEnabledLoading}
                            renderItem={(application) => {
                                const isActive = application.enabled
                                return (
                                    <List.Item
                                        key={application.id}
                                        onClick={(e) => {
                                            e.stopPropagation();
                                            handleManageClient(application)
                                        }}
                                        style={{
                                            opacity: isActive ? 1 : 0.6,
                                            filter: isActive ? "none" : "grayscale(60%)",
                                            transition: "all 0.2s ease",
                                            padding: "4px 0",
                                            marginBottom: 8,
                                        }}
                                    >
                                        <Flex align="center" justify="space-between" style={{ width: "100%" }} gap={16}>
                                            <div style={{ width: "100%" }}>
                                                <ApplicationItem
                                                    application={application}
                                                    onEnabled={handleEnabled}
                                                    onEdit={handleEdit}
                                                />
                                            </div>
                                            <Tooltip title='管理客户端' placement="top">
                                                <Button
                                                    type="text"
                                                    icon={<RightOutlined style={{ fontSize: 20 }} />}
                                                    onClick={(e) => {
                                                        e.stopPropagation();
                                                        handleManageClient(application)
                                                    }}
                                                    style={{
                                                        flexShrink: 0,
                                                        color: token.colorTextTertiary,
                                                        borderRadius: "50%", // 圆形按钮，看起来更精致
                                                        width: 36,
                                                        height: 36,
                                                        display: "flex",
                                                        alignItems: "center",
                                                        justifyContent: "center"
                                                    }}
                                                />
                                            </Tooltip>
                                        </Flex>

                                    </List.Item>
                                )
                            }}
                        />

                        {loadingMore && (
                            <div style={{ textAlign: "center", padding: 12 }}>
                                <Loading spinning size="small" />
                            </div>
                        )}

                        {noMore && applicationList.length > PAGE_SIZE && (
                            <div style={{ textAlign: "center", padding: 12, color: token.colorTextTertiary }}>
                                没有更多数据了
                            </div>
                        )}
                    </>
                )}
            </div>

            <Modal
                title={applicationModal.isEdit ? '编辑应用' : '新增应用'}
                open={applicationModal.open}
                onCancel={handleModalClose}
                onOk={handleSave}
                confirmLoading={detailLoading || saveApplicationLoading}
                okText="保存"
                cancelText="取消"
                destroyOnHidden
                maskClosable={false}
                afterClose={() => form.resetFields()}
                width={760}
            >
                <Loading spinning={detailLoading || saveApplicationLoading}>
                    <Form
                        form={form}
                        layout="vertical"
                        requiredMark="optional"
                        initialValues={{
                            enabled: true,
                        }}
                    >
                        <Form.Item name="id" noStyle />

                        <Row gutter={16}>
                            <Col span={12}>
                                <Form.Item
                                    label="应用名称"
                                    name="applicationName"
                                    rules={[
                                        { required: true, message: '请输入应用名称' },
                                        { max: 40, message: '应用名称最多 40 个字符' },
                                    ]}
                                    extra="展示在应用列表及用户登录、授权页面中"
                                >
                                    <Input
                                        placeholder="请输入应用名称"
                                        maxLength={40}
                                        showCount
                                    />
                                </Form.Item>
                            </Col>

                            <Col span={12}>
                                <Form.Item
                                    label="应用主页"
                                    name="homePageUrl"
                                    rules={[
                                        { required: true, message: '请输入应用主页 URL' },
                                        {
                                            type: 'url',
                                            message: '请输入合法的 URL 地址（需包含 http:// 或 https://）',
                                        },
                                    ]}
                                    extra="用户在授权页点击应用名称时跳转的公开主页"
                                >
                                    <Input placeholder="https://example.com" />
                                </Form.Item>
                            </Col>

                            <Col span={12}>
                                <Form.Item
                                    label="隐私政策 URL"
                                    name="privacyPolicyUrl"
                                    rules={[
                                        { type: 'url', message: '请输入合法的 URL 地址' },
                                    ]}
                                    extra="用户登录授权页展示的隐私政策链接"
                                >
                                    <Input placeholder="https://example.com/privacy" />
                                </Form.Item>
                            </Col>

                            <Col span={12}>
                                <Form.Item
                                    label="服务条款 URL"
                                    name="termsServiceUrl"
                                    rules={[
                                        { type: 'url', message: '请输入合法的 URL 地址' },
                                    ]}
                                    extra="用户登录授权页展示的服务条款链接"
                                >
                                    <Input placeholder="https://example.com/terms" />
                                </Form.Item>
                            </Col>

                            <Col span={12}>
                                <Form.Item
                                    label="开发者名称"
                                    name="developerName"
                                    rules={[
                                        { max: 64, message: '开发者名称最多 64 个字符' },
                                    ]}
                                    extra="个人开发者姓名或公司全称"
                                >
                                    <Input
                                        placeholder="请输入开发者名称"
                                        maxLength={64}
                                    />
                                </Form.Item>
                            </Col>

                            <Col span={12}>
                                <Form.Item
                                    label="开发者联系邮箱"
                                    name="developerEmail"
                                    rules={[
                                        { type: 'email', message: '请输入合法的邮箱地址' },
                                        { max: 128, message: '邮箱长度不能超过 128 个字符' },
                                    ]}
                                    extra="用于合规审计及隐私问题申诉"
                                >
                                    <Input
                                        placeholder="developer@example.com"
                                        maxLength={128}
                                    />
                                </Form.Item>
                            </Col>

                            <Col span={24}>
                                <Form.Item
                                    label="应用图标"
                                    name="logoUrl"
                                    rules={[
                                        { required: true, message: '请上传应用 Logo' },
                                    ]}
                                    extra="支持 JPG、PNG 格式，建议尺寸为 128×128 像素，大小不超过 2MB"
                                >
                                    <SmartUpload
                                        accept="image/jpeg,image/png"
                                        maxSize={2 * 1024 * 1024}
                                        maxCount={1}
                                    />
                                </Form.Item>
                            </Col>

                            <Col span={24}>
                                <Form.Item
                                    label="应用描述"
                                    name="description"
                                    rules={[
                                        { max: 200, message: '应用描述最多 200 个字符' },
                                    ]}
                                >
                                    <Input.TextArea
                                        rows={3}
                                        placeholder="简要描述该应用的功能和用途..."
                                        maxLength={200}
                                        showCount
                                    />
                                </Form.Item>
                            </Col>
                        </Row>
                    </Form>
                </Loading>
            </Modal>
        </div>
    );
}

export default ProjectApplication


const ApplicationItem = ({ application, onEnabled, onEdit }) => {
    const { token } = useToken();
    const { message } = App.useApp();
    const [hovered, setHovered] = useState(false);

    return (
        <Card
            size="small"
            variant="borderless"
            onMouseEnter={() => setHovered(true)}
            onMouseLeave={() => setHovered(false)}
            styles={{
                body: { padding: 14 }
            }}
            style={{
                borderRadius: token.borderRadiusLG,
                background: hovered ? token.colorBgTextHover : token.colorBgContainer,
                transition: "all 0.2s ease",
            }}
        >
            <Flex justify="space-between" align="center">
                {/* LEFT */}
                <Flex gap={12} align="center" style={{ flex: 1 }}>
                    <Avatar
                        size={44}
                        shape="square"
                        src={application.logoUrl}
                        style={{
                            borderRadius: token.borderRadiusLG,
                            color: token.colorPrimary
                        }}
                    />

                    <Flex vertical style={{ flex: 1 }} gap={4}>
                        {/* title row */}
                        <Flex align="center" gap={8}>
                            <Text
                                strong
                                style={{
                                    fontSize: token.fontSizeLG,
                                    color: token.colorText
                                }}
                            >
                                {application.applicationName}
                            </Text>


                            <Tag color={application.enabled ? 'processing' : 'error'} bordered={false} style={{ margin: 0, fontSize: 12 }}>
                                {application.enabled ? '启用' : '停用'}
                            </Tag>

                        </Flex>

                        {/* meta row */}
                        <Flex gap={10} align="center" wrap="wrap">
                            <Tooltip title={application.description || "暂无应用描述"}>
                                <Text
                                    type="secondary"
                                    style={{
                                        fontSize: token.fontSizeSM,
                                        maxWidth: '50%',
                                        overflow: "hidden",
                                        textOverflow: "ellipsis",
                                        whiteSpace: "nowrap",
                                        display: "inline-block"
                                    }}
                                >
                                    {application.description || "暂无应用描述"}
                                </Text>
                            </Tooltip>
                        </Flex>
                    </Flex>
                </Flex>

                {/* RIGHT ACTION */}
                <div style={{ position: "relative", minWidth: 60 }}>
                    <Text
                        type="secondary"
                        style={{
                            fontSize: 11,
                            position: "absolute",
                            top: -6,
                            right: 70,
                            whiteSpace: "nowrap",
                            opacity: 0.75
                        }}
                    >
                        创建于：{application.createTime}
                    </Text>

                    <div style={{ display: "flex", justifyContent: "flex-end", alignItems: "center", height: 44 }}>
                        <Tooltip title="编辑">
                            <Button
                                type="text"
                                icon={<EditOutlined />}
                                onClick={(e) => {
                                    e.stopPropagation()
                                    onEdit?.(application)
                                }}
                                style={{
                                    opacity: hovered ? 1 : 0.5,
                                    color: token.colorTextSecondary,
                                }}
                            />
                        </Tooltip>
                        <Tooltip title={application.enabled ? "停用应用" : "启用应用"}>
                            <Button
                                type="text"
                                icon={
                                    application.enabled ? (
                                        <PauseCircleOutlined />
                                    ) : (
                                        <PlayCircleOutlined />
                                    )
                                }
                                onClick={(e) => {
                                    e.stopPropagation();
                                    onEnabled?.(application, !application.enabled);
                                }}
                                style={{
                                    opacity: hovered ? 1 : 0.5,
                                    color: token.colorTextSecondary,
                                }}
                            />
                        </Tooltip>
                    </div>
                </div>
            </Flex>
        </Card>
    );
};