
import './index.css'
import { Affix, Alert, App, Badge, Button, Card, Checkbox, Descriptions, Divider, Flex, Form, Input, List, Modal, Popconfirm, Space, Switch, Tag, theme, Tooltip, Typography } from 'antd'
import { ArrowLeftOutlined, PlusOutlined, DeleteOutlined, QuestionCircleOutlined, SafetyCertificateOutlined, DownloadOutlined, CheckCircleOutlined, CopyOutlined, ClockCircleOutlined, KeyOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { useRequest } from 'ahooks';
import { useState } from 'react';
import useFullParams from '../../../../../hooks/useFullParams';
import SmartUpload from '../../../../../components/smart-upload';
import { addClientSecret, deleteClientSecret, getApplicationDetail, saveApplication } from '../../../../../services/ApplicationService';
import Loading from '../../../../../components/loading';

const MAX_CALLBACK_URLS = 5

const { Title, Paragraph, Text } = Typography;

const OAuth2ClientApplicationEdit = () => {

    const { domainId: projectCode, id } = useFullParams()

    const { token } = theme.useToken()

    const navigate = useNavigate();

    const [form] = Form.useForm()

    const { message, modal } = App.useApp()

    const { data: applicationDetail, loading: detailApplicationLoading, refresh: refreshApplicationDetail } = useRequest(
        () => getApplicationDetail(projectCode, id),
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

    const { runAsync: saveApplicationAsync, loading: saveApplicationLoading } = useRequest(saveApplication, {
        manual: true
    })

    const { runAsync: addClientSecretAsync, loading: addClientSecretLoading } = useRequest(addClientSecret, {
        manual: true
    })

    const { runAsync: deleteClientSecretAsync, loading: deleteClientSecretLoading } = useRequest(deleteClientSecret, {
        manual: true
    })

    const mode = id ? 'edit' : 'create';

    const [modalVisible, setModalVisible] = useState(false);

    const [credentialData, setCredentialData] = useState({
        id: '',
        clientId: '',
        clientSecret: '',
        applicationName: ''
    });

    const handleCancel = () => {
        // 返回上一页
        navigate(-1)
    }

    const handleAddSecret = async (applicationDetail) => {
        const res = await addClientSecretAsync(projectCode, applicationDetail.id)
        const { id, clientId, clientSecret } = res
        setCredentialData({
            id: id,
            clientId: clientId,
            clientSecret: clientSecret,
            applicationName: applicationDetail.applicationName
        });
        setModalVisible(true)
    }

    const handleDeleteSecret = async (clientSecretId) => {
        modal.confirm({
            title: '确定要删除该密钥吗？',
            content: '删除后，系统将不再接受使用此密钥发起的新认证请求。已签发的 Token 在其有效期内仍可正常使用。',
            okText: '确定删除',
            okType: 'danger', // 红色高亮警告按钮
            loading: deleteClientSecretLoading,
            cancelText: '取消',
            onOk: async () => {
                await deleteClientSecretAsync(projectCode, applicationDetail.id, clientSecretId)
                message.success('密钥删除成功')
                refreshApplicationDetail()
            }
        });
    }

    const onFinish = async (values) => {
        const res = await saveApplicationAsync(projectCode, values)
        message.success('保存成功');
        if (mode === 'create') {
            const { id, clientId, clientSecret } = res
            setCredentialData({
                id: id,
                clientId: clientId,
                clientSecret: clientSecret,
                applicationName: values.applicationName
            });
            setModalVisible(true)
        }
    }

    const handleModalClose = () => {
        setModalVisible(false);
        if (mode === 'create') {
            if (credentialData.id) {
                navigate(`/project/${projectCode}/application/oauth2/${credentialData.id}`, { replace: true });
            }
        } else {
            refreshApplicationDetail()
        }
    }

    return (
        <Loading spinning={detailApplicationLoading || saveApplicationLoading} full>
            <Flex
                gap={50}
                style={{
                    padding: `${token.paddingLG}px ${token.paddingXL}px`,
                    margin: '0',
                    width: '100%'
                }}
            >
                <Flex flex={1}>
                    <Form
                        form={form}
                        layout="vertical"
                        autoComplete="off"
                        initialValues={{ scopes: ['openid', 'profile'] }}
                        onFinish={onFinish}
                    >
                        <Form.Item name="id" noStyle />
                        <Form.Item
                            label="应用名称："
                            name="applicationName"
                            rules={[{ required: true, message: '请输入应用名称' }, { max: 40, message: '应用名称最多 40 个字符' }]}
                        >
                            <Input placeholder="请输入应用名称" maxLength={40} showCount />
                        </Form.Item>
                        <Form.Item
                            label="应用图标："
                            name="logoUrl"
                            rules={[{ required: true, message: '请上传应用 Logo' }]}
                            extra="支持 JPG、PNG 格式，建议尺寸为 128x128 像素，大小不超过 2MB"
                        >
                            <SmartUpload
                                accept="image/jpeg,image/png"
                                maxSize={2 * 1024 * 1024}
                                maxCount={1}
                            />
                        </Form.Item>
                        <Form.Item
                            label="应用主页："
                            name="homePageUrl"
                            rules={[
                                { required: true, message: '请输入应用主页 URL' },
                                { type: 'url', message: '请输入合法的 URL 地址 (需包含 http:// 或 https://)' }
                            ]}
                            extra="用户在授权页点击应用名称时跳转的公开主页"
                        >
                            <Input placeholder="https://example.com" />
                        </Form.Item>
                        <Form.Item
                            label="回调 URL："
                            required
                            extra="授权成功后重定向的受信任回调地址，支持配置多条路径"
                        >
                            <Form.List
                                name="redirectUri"
                                initialValue={['']}
                                rules={[
                                    {
                                        validator: async (_, names) => {
                                            if (!names || names.length === 0) {
                                                return Promise.reject(new Error('请至少添加一个回调 URL'));
                                            }
                                            if (names.length > MAX_CALLBACK_URLS) {
                                                return Promise.reject(new Error(`回调 URL 数量不能超过 ${MAX_CALLBACK_URLS} 个`));
                                            }
                                        },
                                    },
                                ]}
                            >
                                {(fields, { add, remove }, { errors }) => (
                                    <Flex vertical gap={12}>
                                        {fields.map((field, index) => {
                                            const { key, ...fieldProps } = field;

                                            return (
                                                <Form.Item
                                                    required={false}
                                                    key={key}
                                                    style={{ marginBottom: 0 }}
                                                >
                                                    <Flex gap={8} align="center">
                                                        {/* 输入框包裹项 */}
                                                        <Form.Item
                                                            key={key}
                                                            {...fieldProps}
                                                            validateTrigger={['onChange', 'onBlur']}
                                                            rules={[
                                                                { required: true, whitespace: true, message: '请输入回调 URL 或删除此空行' },
                                                                { type: 'url', message: '请输入合法的 URL 地址 (需包含 http:// 或 https://)' }
                                                            ]}
                                                            noStyle
                                                        >
                                                            <Input placeholder="https://example.com/oauth2/callback" style={{ flex: 1 }} />
                                                        </Form.Item>

                                                        {index === 0 ? (
                                                            fields.length < MAX_CALLBACK_URLS ? (
                                                                <Button
                                                                    type="text"
                                                                    icon={<PlusOutlined />}
                                                                    onClick={() => add()}
                                                                    style={{ color: token.colorPrimary }}
                                                                />
                                                            ) : (
                                                                <Button
                                                                    type="text"
                                                                    disabled
                                                                    icon={<PlusOutlined />}
                                                                />
                                                            )
                                                        ) : (
                                                            <Button
                                                                type="text"
                                                                danger
                                                                icon={<DeleteOutlined />}
                                                                onClick={() => remove(field.name)}
                                                            />
                                                        )}
                                                    </Flex>
                                                </Form.Item>
                                            );
                                        })}
                                        <Form.ErrorList errors={errors} style={{ margin: 0 }} />
                                    </Flex>
                                )}
                            </Form.List>
                        </Form.Item>

                        <Form.Item
                            label="允许申请的权限范围："
                            name="scopes"
                            rules={[{ required: true, message: '请至少选择一个权限范围' }]}
                            extra="勾选该应用可以向用户申请获取的权限。未勾选的权限在授权时将被拒绝。"
                        >
                            <Checkbox.Group>
                                <Flex gap={16} wrap>
                                    <Checkbox value="openid" disabled>openid (用户标识)</Checkbox>
                                    <Checkbox value="profile">profile (用户资料)</Checkbox>
                                    <Checkbox value="email">email (电子邮箱)</Checkbox>
                                    <Checkbox value="phone">phone (手机号码)</Checkbox>
                                </Flex>
                            </Checkbox.Group>
                        </Form.Item>

                        <Form.Item
                            name="allowDeviceFlow"
                            valuePropName="checked"
                            label={
                                <Flex gap={4} align="center">
                                    <Typography.Text>支持设备码授权：</Typography.Text>
                                    <Tooltip title="允许无浏览器或输入受限的设备（如智能电视、CLI 命令行工具、IoT设备）通过设备验证码进行授权登录。">
                                        <QuestionCircleOutlined style={{ color: token.colorTextSecondary, cursor: 'pointer' }} />
                                    </Tooltip>
                                </Flex>
                            }
                        >
                            <Switch checkedChildren="开启" unCheckedChildren="关闭" />
                        </Form.Item>

                        <Divider style={{ fontSize: 14, color: token.colorTextSecondary }}>法务与合规 (选填)</Divider>
                        <Form.Item
                            label="隐私政策 URL："
                            name="privacyPolicyUrl"
                            rules={[{ type: 'url', message: '请输入合法的 URL 地址' }]}
                            extra="用户登录授权页时展示的隐私权政策链接"
                        >
                            <Input placeholder="https://example.com/privacy" />
                        </Form.Item>

                        <Form.Item
                            label="服务条款 URL："
                            name="termsServiceUrl"
                            rules={[{ type: 'url', message: '请输入合法的 URL 地址' }]}
                            extra="用户登录授权页时展示的服务条款链接"
                        >
                            <Input placeholder="https://example.com/terms" />
                        </Form.Item>

                        <Divider style={{ fontSize: 14, color: token.colorTextSecondary }}>开发者信息 (选填)</Divider>

                        <Form.Item
                            label="开发者名称："
                            name="developerName"
                            rules={[{ max: 64, message: '开发者名称最多 64 个字符' }]}
                            extra="展示在用户登录授权页，告知用户该应用由谁提供服务"
                        >
                            <Input placeholder="请输入个人开发者姓名或公司全称" maxLength={64} />
                        </Form.Item>

                        <Form.Item
                            label="开发者联系邮箱："
                            name="developerEmail"
                            rules={[
                                { type: 'email', message: '请输入合法的邮箱地址' },
                                { max: 128, message: '邮箱长度不能超过 128 个字符' }
                            ]}
                            extra="用于合规审计及用户隐私问题申诉联系"
                        >
                            <Input placeholder="developer@example.com" maxLength={128} />
                        </Form.Item>

                        <Form.Item
                            label="应用描述："
                            name="description"
                            rules={[{ max: 200, message: '应用描述最多 200 个字符' }]}
                        >
                            <Input.TextArea
                                rows={4}
                                placeholder="简要描述该应用的功能和用途..."
                                maxLength={200}
                                showCount
                            />
                        </Form.Item>
                        <div
                            style={{
                                position: 'sticky',
                                bottom: 0,
                                background: token.colorBgContainer,
                                padding: '16px 0',
                                zIndex: 10,
                            }}
                        >
                            <Form.Item style={{  marginBottom: 0 }}>
                                <Flex gap={12}>
                                    <Button
                                        type="primary"
                                        htmlType="submit"
                                        loading={saveApplicationLoading}
                                    >
                                        保存
                                    </Button>
                                    <Button
                                        type="default"
                                        onClick={handleCancel}
                                        disabled={false}
                                    >
                                        取消
                                    </Button>
                                </Flex>
                            </Form.Item>
                        </div>
                    </Form> 
                </Flex>

                <Flex
                    gap={30}
                    flex={1}
                    vertical
                >
                    {mode === 'edit' && (
                        <>
                            <Card
                                title={
                                    <Space>
                                        <SafetyCertificateOutlined />
                                        客户端信息
                                    </Space>
                                }
                                variant='borderless'
                            >
                                <Descriptions
                                    column={1}
                                    size="small"
                                    styles={{
                                        label: {
                                            width: 120
                                        }
                                    }}
                                >
                                    <Descriptions.Item label="客户端 ID">
                                        <Text>
                                            {applicationDetail?.clientId || '-'}
                                        </Text>
                                    </Descriptions.Item>

                                    <Descriptions.Item label="创建日期">
                                        <Space>
                                            <Text>
                                                {applicationDetail?.createTime || '-'}
                                            </Text>
                                        </Space>
                                    </Descriptions.Item>

                                    <Descriptions.Item label="最后使用日期">
                                        <Space>
                                            <Text>
                                                {applicationDetail?.lastUsedTime || '-'}
                                            </Text>
                                        </Space>
                                    </Descriptions.Item>
                                </Descriptions>
                            </Card>

                            <Card
                                title="客户端密钥"
                                variant='borderless'
                                extra={
                                    <Tooltip
                                        title={applicationDetail?.clientSecrets?.length >= 2 ? "每个应用最多只能同时存在 2 个有效密钥" : ""}
                                    >
                                        <Button
                                            type="primary"
                                            icon={<PlusOutlined />}
                                            loading={addClientSecretLoading}
                                            disabled={applicationDetail?.clientSecrets?.length >= 2}
                                            onClick={() => handleAddSecret(applicationDetail)}
                                        >
                                            生成新密钥
                                        </Button>
                                    </Tooltip>
                                }
                            >
                                <Space direction="vertical" size={12}>
                                    <Alert
                                        type="warning"
                                        showIcon
                                        message="无法再查看和下载客户端密钥。如果您丢失了下方的密钥，请添加新密钥。请务必安全地存储客户端密钥，并妥善保管。切勿将密钥提交到代码库。"
                                    />

                                    <List
                                        itemLayout="horizontal"
                                        dataSource={applicationDetail?.clientSecrets || []}
                                        renderItem={(secret) => {
                                            const isExpired = !!secret.clientSecretExpiresAt;
                                            const isOnlyOne = applicationDetail?.clientSecrets?.length <= 1;

                                            return (
                                                <List.Item
                                                    style={{
                                                        padding: '14px 12px',
                                                        borderRadius: token.borderRadiusLG,
                                                        marginBottom: 8,
                                                        background: token.colorFillQuaternary,
                                                        border: `1px solid ${token.colorBorderSecondary}`,
                                                    }}
                                                    actions={[
                                                        <Tooltip title={isOnlyOne ? '至少需要保留一个密钥，无法删除' : ''}>
                                                            <Button
                                                                type="text"
                                                                danger
                                                                onClick={() => handleDeleteSecret(secret.id)}
                                                                icon={<DeleteOutlined />}
                                                                disabled={isOnlyOne}
                                                            />
                                                        </Tooltip>
                                                    ]}
                                                >

                                                    <Descriptions
                                                        column={1}
                                                        size="small"
                                                        styles={{
                                                            label: {
                                                                width: 120
                                                            }
                                                        }}
                                                    >
                                                        <Descriptions.Item label="客户端密钥">
                                                            <Text>
                                                                ****{secret.clientSecretHint}
                                                            </Text>
                                                        </Descriptions.Item>

                                                        <Descriptions.Item label="创建日期">
                                                            <Space>
                                                                <Text>
                                                                    {secret?.createTime}
                                                                </Text>
                                                            </Space>
                                                        </Descriptions.Item>

                                                        <Descriptions.Item label="过期时间">
                                                            <Space>
                                                                <Text>
                                                                    {secret?.clientSecretExpiresAt || '-'}
                                                                </Text>
                                                            </Space>
                                                        </Descriptions.Item>


                                                    </Descriptions>

                                                </List.Item>
                                            );
                                        }}
                                    />
                                </Space>
                            </Card>
                        </>
                    )}


                </Flex>
            </Flex>
            <OAuth2CredentialDisplayModal
                open={modalVisible}
                clientId={credentialData.clientId}
                clientSecret={credentialData.clientSecret}
                applicationName={credentialData.applicationName}
                onClose={handleModalClose}
            />
        </Loading>
    )
}

export default OAuth2ClientApplicationEdit

export const OAuth2CredentialDisplayModal = ({
    open = false,
    clientId = '',
    clientSecret = '',
    applicationName = '',
    onClose,
}) => {

    const { token } = theme.useToken();
    const { message, modal } = App.useApp();

    const [hasCopiedSecret, setHasCopiedSecret] = useState(false);
    const [hasDownloaded, setHasDownloaded] = useState(false);

    // 一键复制指定文本
    const handleCopy = async (text, label, setCopyState) => {
        if (!text) return;
        try {
            await navigator.clipboard.writeText(text);
            message.success(`${label}已成功复制到剪贴板！`);
            if (setCopyState) setCopyState(true);
        } catch (err) {
            message.error('复制失败，请尝试手动选中文本复制。');
        }
    };

    // 一键下载文本凭证
    const handleDownload = () => {
        if (!clientId || !clientSecret) return;
        try {
            const fileContent = `=== OAuth2 应用凭证 (Credentials) ===\n应用名称: ${applicationName}\n生成时间: ${new Date().toLocaleString()}\n\nClient ID:     ${clientId}\nClient Secret: ${clientSecret}\n\n* 安全提示：请妥善保管您的凭证。Client Secret 仅在此处展示一次，切勿上传至公开代码仓库（如 GitHub）。`;
            const blob = new Blob([fileContent], { type: 'text/plain;charset=utf-8' });
            const url = URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = `credentials-${clientId.slice(0, 8)}.txt`;
            link.click();
            URL.revokeObjectURL(url);

            message.success('凭证文件下载成功！');
            setHasDownloaded(true);
        } catch (err) {
            message.error('文件下载失败，请尝试手动复制。');
        }
    };

    // 通关安全确认
    const handleDoneClick = () => {
        // 安全拦截：如果用户极其粗心，连最核心的 Secret 都没复制或下载，直接弹窗警告
        if (!hasCopiedSecret && !hasDownloaded) {
            modal.confirm({
                title: '凭证安全防丢确认',
                icon: <SafetyCertificateOutlined style={{ color: token.colorError }} />,
                content: '系统检测到您尚未复制或下载【Client Secret (密钥)】。关闭此窗口后，该密钥将永久加密，【永远无法再次查看】！确定已经安全备份了吗？',
                okText: '确定关闭',
                cancelText: '去备份',
                okButtonProps: { danger: true },
                onOk: () => {
                    executeClose();
                }
            });
        } else {
            executeClose();
        }
    };

    const executeClose = () => {
        setHasCopiedSecret(false);
        setHasDownloaded(false);
        if (onClose) onClose();
    };

    const credentialBoxStyle = {
        padding: '20px',
        background: token.colorFillAlter,
        borderRadius: token.borderRadiusLG,
        border: `1px solid ${token.colorBorderSecondary}`,
    };

    return (
        <Modal
            title={
                <Space>
                    <SafetyCertificateOutlined style={{ color: token.colorSuccess }} />
                    <span>应用创建成功 - 请保存您的凭证</span>
                </Space>
            }
            open={open}
            closable={false}      // 🔒 禁右上角 X
            maskClosable={false}  // 🔒 禁阴影点击
            keyboard={false}      // 🔒 禁 Esc 键
            width={520}
            destroyOnHidden       // 💡 关闭时彻底销毁 dom，防止密钥在内存中长期驻留
            footer={[
                <Button key="download" icon={<DownloadOutlined />} onClick={handleDownload}>
                    下载文本凭证
                </Button>,
                <Button key="done" type="primary" icon={<CheckCircleOutlined />} onClick={handleDoneClick}>
                    我已经安全保存
                </Button>
            ]}
        >
            <Flex vertical gap={16} style={{ marginTop: 16 }}>
                <Alert
                    message={
                        <span>
                            应用 <strong>{applicationName}</strong> 已成功注册。
                            <span style={{ color: token.colorError, fontWeight: 'bold' }}>
                                请注意：Client Secret 仅在此展示一次。
                            </span>
                            为了您的系统安全，请妥善保存。
                        </span>
                    }
                    type="warning"
                    showIcon
                />

                <div style={credentialBoxStyle}>
                    <Flex vertical gap={16}>
                        {/* Client ID 条目 */}
                        <div>
                            <Text type="secondary" block style={{ marginBottom: 6, fontSize: 13, userSelect: 'none' }}>
                                Client ID (客户端标识)
                            </Text>
                            <Space.Compact style={{ width: '100%' }}>
                                <Input
                                    value={clientId}
                                    readOnly
                                    style={{ fontFamily: 'monospace' }}
                                />
                                <Button
                                    icon={<CopyOutlined />}
                                    onClick={() => handleCopy(clientId, 'Client ID')}
                                    title="复制 Client ID"
                                />
                            </Space.Compact>
                        </div>

                        {/* Client Secret 条目 */}
                        <div>
                            <Text type="secondary" style={{ marginBottom: 6, fontSize: 13, userSelect: 'none' }}>
                                Client Secret (客户端密钥)
                            </Text>
                            <Space.Compact style={{ width: '100%' }}>
                                <Input.Password
                                    value={clientSecret}
                                    readOnly
                                    style={{ fontFamily: 'monospace' }}
                                />
                                <Button
                                    icon={<CopyOutlined />}
                                    onClick={() => handleCopy(clientSecret, '客户端密钥', setHasCopiedSecret)}
                                    title="复制 Client Secret"
                                />
                            </Space.Compact>
                        </div>
                    </Flex>
                </div>
            </Flex>
        </Modal>
    )
}