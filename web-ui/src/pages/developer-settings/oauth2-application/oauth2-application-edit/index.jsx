
import './index.css'
import useFullParams from '../../../../hooks/useFullParams'
import { Alert, App, Button, Checkbox, Flex, Form, Input, Modal, Space, Switch, theme, Tooltip, Typography } from 'antd'
import SmartUpload from '../../../../components/smart-upload'
import { ArrowLeftOutlined, PlusOutlined, DeleteOutlined, QuestionCircleOutlined, SafetyCertificateOutlined, DownloadOutlined, CheckCircleOutlined, CopyOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import { saveApplication } from '../../../../services/DeveloperSettingsService';
import { useRequest } from 'ahooks';
import { useState } from 'react';

const MAX_CALLBACK_URLS = 5

const { Title, Paragraph, Text } = Typography;

const OAuth2ApplicationEdit = () => {

    const { id } = useFullParams()

    const { token } = theme.useToken()

    const navigate = useNavigate();

    const [form] = Form.useForm()

    const { message, modal } = App.useApp()

    const { runAsync: saveApplicationAsync, loading: saveApplicationLoading } = useRequest(saveApplication, {
        manual: true
    })

    const mode = id ? 'edit' : 'create';

    const [modalVisible, setModalVisible] = useState(false);

    const [credentialData, setCredentialData] = useState({
        clientId: '',
        clientSecret: '',
        applicationName: ''
    });

    const handleCancel = () => {
        // 返回上一页
        navigate(-1)
    }

    const onFinish = async (values) => {
        console.log('Form values:', values);
        const res = await saveApplicationAsync(values)
        message.success('保存成功');
        if (mode === 'create') {
            const { clientId, clientSecret } = res
            setCredentialData({
                clientId: clientId,
                clientSecret: clientSecret,
                applicationName: values.applicationName
            });
            setModalVisible(true);
        }
    }

    return (
        <Flex
            vertical
            style={{
                padding: `${token.paddingLG}px ${token.paddingXL}px`,
                margin: '0',
                maxWidth: 800,
                width: '100%'
            }}
        >
            <Form
                form={form}
                layout="vertical"
                autoComplete="off"
                initialValues={{ scopes: ['openid', 'profile'] }}
                onFinish={onFinish}
            >
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
                            <Checkbox value="openid" disabled>openid (用户唯一标识)</Checkbox>
                            <Checkbox value="profile">profile (基本用户资料)</Checkbox>
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
                <Form.Item
                    label="应用描述"
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

                <Form.Item style={{ marginTop: 32, marginBottom: 0 }}>
                    <Flex gap={12}>
                        <Button
                            type="primary"
                            htmlType="submit"
                            loading={false}
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
            </Form>
            <OAuth2CredentialDisplayModal
                open={modalVisible}
                clientId={credentialData.clientId}
                clientSecret={credentialData.clientSecret}
                applicationName={credentialData.applicationName}
                onClose={() => {
                    setModalVisible(false)
                }}
            />
        </Flex>
    )
}

export default OAuth2ApplicationEdit

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