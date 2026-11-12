import './index.css'
import { Affix, Alert, App, Badge, Button, Card, Checkbox, Descriptions, Divider, Flex, Form, Input, List, Modal, Popconfirm, Space, Switch, Tag, theme, Tooltip, Typography } from 'antd'
import { ArrowLeftOutlined, PlusOutlined, DeleteOutlined, QuestionCircleOutlined, SafetyCertificateOutlined, DownloadOutlined, CheckCircleOutlined, CopyOutlined, ClockCircleOutlined, KeyOutlined } from '@ant-design/icons';
import { useNavigate, useOutletContext } from 'react-router-dom';
import { useRequest } from 'ahooks';
import { useEffect, useState } from 'react';
import useFullParams from '../../../../../../hooks/useFullParams';
import Loading from '../../../../../../components/loading';
import ApplicationCard from '../../../components/ApplicationCard';
import { getCasClientDetail, saveCasClient } from '../../../../../../services/ApplicationService';
import AttributeMappingForm from '../../../components/AttributeMappingForm';


const MAX_CALLBACK_URLS = 10

const { Title, Paragraph, Text } = Typography;

const { TextArea } = Input;

const CasClientApplicationEdit = () => {

    const { domainId: projectCode, id, applicationId } = useFullParams()

    const { token } = theme.useToken()

    const navigate = useNavigate();

    const [form] = Form.useForm()

    const { message } = App.useApp()

    const { application, loading: applicationLoading } = useOutletContext() || {}

    const { runAsync: saveCasClientAsync, loading: saveCasClientLoading } = useRequest(saveCasClient, {
        manual: true
    })

    const { data: casClientDetail, loading: casClientLoading, refresh: refreshCasClientDetail } = useRequest(
        () => getCasClientDetail(projectCode, applicationId, id),
        {
            ready: !!id,
            refreshDeps: [id],
            onSuccess: (data) => {
                if (data) {
                    form.setFieldsValue({
                        ...data,
                        attributeMappings: Object.entries(data.attributeMappings || {}).map(
                            ([casAttribute, userAttribute]) => ({
                                casAttribute,
                                userAttribute,
                            })
                        )
                    });
                }
            }
        }
    )

    const mode = id ? 'edit' : 'create';

    useEffect(() => {
        if (mode === 'create') {
            form.resetFields()
        }
    }, [mode, form])

    const onFinish = async (values) => {
        const { attributeMappings = [] } = values
        const mappings = attributeMappings.reduce((map, item) => {
            map[item.casAttribute] = item.userAttribute;
            return map
        }, {})
        const req = {
            ...values,
            attributeMappings: mappings
        }
        await saveCasClientAsync(projectCode, applicationId, req)
        message.success('保存成功');
    }

    const handleCancel = () => {
        // 返回上一页
        navigate(-1)
    }

    return (
        <Loading spinning={casClientLoading || saveCasClientLoading}>
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
                        initialValues={{
                            proxyEnabled: false,
                            attributeMappings: [
                                { casAttribute: 'sub', userAttribute: 'sub' },
                                { casAttribute: 'preferred_username', userAttribute: 'preferredUsername' },
                                { casAttribute: 'name', userAttribute: 'fullName' },
                                { casAttribute: 'picture', userAttribute: 'avatar' },
                                { casAttribute: 'email', userAttribute: 'email' },
                                { casAttribute: 'email_verified', userAttribute: 'emailVerified' }
                            ],
                        }}
                        style={{
                            width: '100%'
                        }}
                        onFinish={onFinish}
                    >
                        <Form.Item name="id" noStyle />
                        <Form.Item
                            label="客户端名称："
                            name="clientName"
                            rules={[{ required: true, message: '请输入客户端名称' }, { max: 40, message: '客户端名称最多 40 个字符' }]}
                        >
                            <Input placeholder="请输入客户端名称" maxLength={40} showCount />
                        </Form.Item>
                        <Form.Item
                            label="服务标识："
                            name="service"
                            rules={[
                                { required: true, message: '请输入服务标识' },
                                { type: 'url', message: '请输入合法的 URL 地址' },
                                { max: 1000, message: '服务标识回调地址最多 1000 个字符' },
                            ]}
                            extra="CAS 认证成功后，Atlas 将携带 Service Ticket（ST）重定向至此地址。"
                        >
                            <Input
                                placeholder="https://sp.example.com/cas/callback"
                                maxLength={1000}
                            />
                        </Form.Item>

                        <Form.Item
                            name="proxyEnabled"
                            valuePropName="checked"
                            label={
                                <Flex align="center" gap={4}>
                                    <Typography.Text>
                                        启用代理认证：
                                    </Typography.Text>
                                    <Tooltip title="开启后，允许该客户端通过 CAS Proxy 机制获取代理票据（PT），并代表用户访问其他受 CAS 保护的服务。">
                                        <QuestionCircleOutlined
                                            style={{
                                                color: token.colorTextSecondary,
                                                cursor: 'pointer',
                                            }}
                                        />
                                    </Tooltip>
                                </Flex>
                            }
                        >
                            <Switch
                                checkedChildren="开启"
                                unCheckedChildren="关闭"
                            />
                        </Form.Item>

                        <Form.Item
                            noStyle
                            shouldUpdate={(prev, current) =>
                                prev.proxyEnabled !== current.proxyEnabled
                            }
                        >
                            {({ getFieldValue }) =>
                                getFieldValue('proxyEnabled') ? (
                                    <Form.Item
                                        label="代理票据回调地址："
                                        required
                                        extra="用于接收 CAS 服务端发送的PGT，支持配置多个受信任的回调地址。"
                                    >
                                        <Form.List
                                            name="proxyCallbackUrls"
                                            initialValue={['']}
                                            rules={[
                                                {
                                                    validator: async (_, urls) => {
                                                        if (!urls?.length) {
                                                            throw new Error('请至少添加一个代理票据回调地址');
                                                        }

                                                        if (urls.length > MAX_CALLBACK_URLS) {
                                                            throw new Error(
                                                                `代理票据回调地址不能超过 ${MAX_CALLBACK_URLS} 个`
                                                            );
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
                                                                key={key}
                                                                required={false}
                                                                style={{ marginBottom: 0 }}
                                                            >
                                                                <Flex gap={8} align="center">
                                                                    <Form.Item
                                                                        {...fieldProps}
                                                                        validateTrigger={['onChange', 'onBlur']}
                                                                        rules={[
                                                                            {
                                                                                required: true,
                                                                                whitespace: true,
                                                                                message: '请输入代理票据回调地址或删除此空行',
                                                                            },
                                                                            {
                                                                                type: 'url',
                                                                                message: '请输入合法的 URL 地址（需包含 http:// 或 https://）',
                                                                            },
                                                                        ]}
                                                                        noStyle
                                                                    >
                                                                        <Input
                                                                            placeholder="https://example.com/cas/proxy/callback"
                                                                            style={{ flex: 1 }}
                                                                        />
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

                                                    <Form.ErrorList errors={errors} />
                                                </Flex>
                                            )}
                                        </Form.List>
                                    </Form.Item>
                                ) : null
                            }
                        </Form.Item>

                        <Divider
                            style={{
                                fontSize: 14,
                                color: token.colorTextSecondary,
                            }}
                        >
                            CAS 属性映射
                        </Divider>

                        <AttributeMappingForm
                            sourceField="casAttribute"
                            sourceLabel="CAS 属性"
                            sourcePlaceholder="例如 email"
                            sourceRequiredMessage="请输入 CAS 属性名称"
                            extra="将 Atlas 用户属性映射为 SP 接收的 CAS Attribute 名称。"
                        />

                        <Divider style={{ fontSize: 14, color: token.colorTextSecondary }}>其他配置</Divider>

                        <Form.Item
                            label="客户端描述："
                            name="description"
                            rules={[
                                { max: 500, message: '客户端描述最多 500 个字符' },
                            ]}
                        >
                            <TextArea
                                rows={3}
                                placeholder="请输入应用描述（选填）"
                                maxLength={500}
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
                            <Form.Item style={{ marginBottom: 0 }}>
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
                        </div>

                    </Form>
                </Flex>
                <Flex
                    gap={30}
                    flex={1}
                    vertical
                >
                    <ApplicationCard application={application} />
                    {mode === 'edit' && (
                        <Card
                            title={
                                <Space>
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
                                <Descriptions.Item label="服务标识">
                                    <Text>
                                        {casClientDetail?.service || '-'}
                                    </Text>
                                </Descriptions.Item>

                                <Descriptions.Item label="创建日期">
                                    <Space>
                                        <Text>
                                            {casClientDetail?.createTime || '-'}
                                        </Text>
                                    </Space>
                                </Descriptions.Item>

                                <Descriptions.Item label="最后使用日期">
                                    <Space>
                                        <Text>
                                            {casClientDetail?.lastUsedTime || '-'}
                                        </Text>
                                    </Space>
                                </Descriptions.Item>
                            </Descriptions>
                        </Card>
                    )}
                </Flex>
            </Flex>
        </Loading>
    )
}

export default CasClientApplicationEdit