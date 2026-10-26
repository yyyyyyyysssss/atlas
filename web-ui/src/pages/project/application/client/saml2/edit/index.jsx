import './index.css'
import { Affix, Alert, App, Badge, Button, Card, Checkbox, Descriptions, Divider, Flex, Form, Input, List, Modal, Popconfirm, Space, Switch, Tag, theme, Tooltip, Typography } from 'antd'
import { ArrowLeftOutlined, PlusOutlined, DeleteOutlined, QuestionCircleOutlined, SafetyCertificateOutlined, DownloadOutlined, CheckCircleOutlined, CopyOutlined, ClockCircleOutlined, KeyOutlined } from '@ant-design/icons';
import { useNavigate, useOutletContext } from 'react-router-dom';
import { useRequest } from 'ahooks';
import { useEffect, useState } from 'react';
import useFullParams from '../../../../../../hooks/useFullParams';
import Loading from '../../../../../../components/loading';
import SmartUpload from '../../../../../../components/smart-upload';
import ApplicationCard from '../../../components/ApplicationCard';
import { getSaml2ClientDetail, saveSaml2Client } from '../../../../../../services/ApplicationService';

const { Title, Paragraph, Text } = Typography;

const { TextArea } = Input;

const Saml2ClientApplicationEdit = () => {

    const { domainId: projectCode, id, applicationId } = useFullParams()

    const { token } = theme.useToken()

    const navigate = useNavigate();

    const [form] = Form.useForm()

    const { message } = App.useApp()

    const { application, loading: applicationLoading } = useOutletContext() || {}

    const { runAsync: saveSaml2ClientAsync, loading: saveSaml2ClientLoading } = useRequest(saveSaml2Client, {
        manual: true
    })

    const { data: saml2ClientDetail, loading: saml2ClientLoading, refresh: refreshSaml2ClientDetail } = useRequest(
        () => getSaml2ClientDetail(projectCode, applicationId, id),
        {
            ready: !!id,
            refreshDeps: [id],
            onSuccess: (data) => {
                if (data) {
                    form.setFieldsValue({
                        ...data,
                        attributeMappings: Object.entries(data.attributeMappings || {}).map(
                            ([samlAttribute, userAttribute]) => ({
                                samlAttribute,
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
            map[item.samlAttribute] = item.userAttribute;
            return map
        }, {})
        const req = {
            ...values,
            attributeMappings: mappings
        }
        await saveSaml2ClientAsync(projectCode, applicationId, req)
        message.success('保存成功');
    }

    const handleCancel = () => {
        // 返回上一页
        navigate(-1)
    }

    return (
        <Loading spinning={saml2ClientLoading || saveSaml2ClientLoading}>
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
                            requireSignedAuthnRequest: true,
                            signResponse: true,
                            signAssertion: true,
                            enabled: true,
                            attributeMappings: [
                                { samlAttribute: 'sub', userAttribute: 'sub' },
                                { samlAttribute: 'email', userAttribute: 'email' },
                                { samlAttribute: 'emailVerified', userAttribute: 'emailVerified' },
                                { samlAttribute: 'avatar', userAttribute: 'avatar' },
                                { samlAttribute: 'fullName', userAttribute: 'fullName' },
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
                            label={
                                <Flex align="center" gap={4}>
                                    <Typography.Text>SP Entity ID：</Typography.Text>
                                    <Tooltip title="必须与 SP 发起的 AuthnRequest 中的 Issuer 一致。">
                                        <QuestionCircleOutlined
                                            style={{
                                                color: token.colorTextSecondary,
                                                cursor: 'pointer',
                                            }}
                                        />
                                    </Tooltip>
                                </Flex>
                            }
                            name="entityId"
                            rules={[
                                { required: true, message: '请输入 SP Entity ID' },
                                { max: 255, message: 'Entity ID 最多 255 个字符' },
                            ]}
                            extra="用于唯一标识服务提供商（SP）。"
                        >
                            <Input
                                placeholder="urn:atlas:sp"
                                maxLength={255}
                            />
                        </Form.Item>

                        <Form.Item
                            label="ACS 回调地址："
                            name="acsUrl"
                            rules={[
                                { required: true, message: '请输入 ACS 回调地址' },
                                { type: 'url', message: '请输入合法的 URL 地址' },
                                { max: 1000, message: 'ACS 回调地址最多 1000 个字符' },
                            ]}
                            extra="认证成功后，Atlas 将 SAML Response 发送至此地址。"
                        >
                            <Input
                                placeholder="https://sp.example.com/saml/acs"
                                maxLength={1000}
                            />
                        </Form.Item>

                        <Divider style={{ fontSize: 14, color: token.colorTextSecondary }}>请求签名验证</Divider>

                        <Form.Item
                            name="requireSignedAuthnRequest"
                            valuePropName="checked"
                            label={
                                <Flex align="center" gap={4}>
                                    <Typography.Text>
                                        要求 AuthnRequest 必须签名：
                                    </Typography.Text>
                                    <Tooltip title="开启后，服务端必须验证 AuthnRequest 的 XML 数字签名。">
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
                            shouldUpdate={(prevValues, currentValues) =>
                                prevValues.requireSignedAuthnRequest !==
                                currentValues.requireSignedAuthnRequest
                            }
                        >
                            {({ getFieldValue }) => getFieldValue('requireSignedAuthnRequest') && (
                                <>
                                    <Text strong>SP 验证证书</Text>

                                    <Form.Item
                                        name="verificationCertificate"
                                        dependencies={['requireSignedAuthnRequest']}
                                        rules={[
                                            ({ getFieldValue }) => ({
                                                validator: async (_, value) => {
                                                    if (!getFieldValue('requireSignedAuthnRequest')) {
                                                        return;
                                                    }
                                                    if (!value?.trim()) {
                                                        throw new Error('请输入 SP 的签名验证证书');
                                                    }

                                                    if (
                                                        !value.includes('-----BEGIN CERTIFICATE-----') ||
                                                        !value.includes('-----END CERTIFICATE-----')
                                                    ) {
                                                        return Promise.reject(
                                                            new Error('请输入完整的 PEM X.509 证书'),
                                                        );
                                                    }

                                                    return Promise.resolve();
                                                },
                                            }),
                                        ]}
                                        extra="填写 SP 用于签名 AuthnRequest 的 X.509 公钥证书，PEM 格式。"
                                        style={{ marginTop: token.marginSM }}
                                    >
                                        <Input.TextArea
                                            placeholder={
                                                '-----BEGIN CERTIFICATE-----\n请粘贴 SP 的 X.509 证书\n-----END CERTIFICATE-----'
                                            }
                                            autoSize={{ minRows: 5, maxRows: 12 }}
                                        />
                                    </Form.Item>
                                </>
                            )}
                        </Form.Item>


                        <Divider style={{ fontSize: 14, color: token.colorTextSecondary }}>响应签名策略</Divider>

                        <Form.Item
                            name="signResponse"
                            valuePropName="checked"
                            label="签名 SAML Response："
                            extra="对返回给 SP 的 Response 消息进行数字签名。"
                        >
                            <Switch
                                checkedChildren="开启"
                                unCheckedChildren="关闭"
                            />
                        </Form.Item>

                        <Form.Item
                            name="signAssertion"
                            valuePropName="checked"
                            label="签名 SAML Assertion："
                            extra="对包含用户身份和属性信息的 Assertion 进行数字签名。"
                        >
                            <Switch
                                checkedChildren="开启"
                                unCheckedChildren="关闭"
                            />
                        </Form.Item>

                        <Flex vertical gap={12} style={{ marginTop: token.marginSM }}>
                            <Typography.Text strong>
                                <SafetyCertificateOutlined
                                    style={{ color: token.colorPrimary, marginRight: 8 }}
                                />
                                签名策略说明
                            </Typography.Text>

                            <Flex vertical gap={4}>
                                <Typography.Text strong>仅签名 Response</Typography.Text>
                                <Typography.Text type="secondary">
                                    对整个 SAML Response 进行签名，SP 验证响应的来源与完整性。
                                </Typography.Text>
                            </Flex>

                            <Divider style={{ margin: 0 }} />

                            <Flex vertical gap={4}>
                                <Typography.Text strong>仅签名 Assertion</Typography.Text>
                                <Typography.Text type="secondary">
                                    对用户身份及属性声明进行签名，SP 可独立验证 Assertion 的来源与完整性。
                                </Typography.Text>
                            </Flex>

                            <Divider style={{ margin: 0 }} />

                            <Flex vertical gap={4}>
                                <Typography.Text strong>同时签名</Typography.Text>
                                <Typography.Text type="secondary">
                                    先签名 Assertion，再签名包含该 Assertion 的 Response，形成两层独立签名。
                                </Typography.Text>
                            </Flex>
                        </Flex>

                        <Divider
                            style={{
                                fontSize: 14,
                                color: token.colorTextSecondary,
                            }}
                        >
                            SAML 属性映射
                        </Divider>

                        <Form.Item
                            label="属性映射："
                            extra="将 Atlas 用户属性映射为 SP 接收的 SAML Attribute 名称。"
                        >
                            <Form.List
                                name="attributeMappings"
                                rules={[
                                    {
                                        validator: async (_, mappings) => {
                                            if (!mappings || mappings.length === 0) {
                                                return Promise.reject(
                                                    new Error('请至少添加一条属性映射'),
                                                );
                                            }

                                            const names = mappings
                                                .map((item) =>
                                                    item?.samlAttribute?.trim(),
                                                )
                                                .filter(Boolean);

                                            if (
                                                new Set(names).size !== names.length
                                            ) {
                                                return Promise.reject(
                                                    new Error(
                                                        'SAML 属性名称不能重复',
                                                    ),
                                                );
                                            }
                                        },
                                    },
                                ]}
                            >
                                {(fields, { add, remove }, { errors }) => (
                                    <Flex vertical gap={12}>
                                        {fields.map((field) => (
                                            <Flex
                                                key={field.key}
                                                gap={8}
                                                align="start"
                                                className="saml2-client-edit__mapping-row"
                                            >
                                                <Form.Item
                                                    name={[
                                                        field.name,
                                                        'samlAttribute',
                                                    ]}
                                                    rules={[
                                                        {
                                                            required: true,
                                                            whitespace: true,
                                                            message:
                                                                '请输入 SAML 属性名称',
                                                        },
                                                    ]}
                                                    style={{
                                                        flex: 1,
                                                        minWidth: 0,
                                                        marginBottom: 0,
                                                    }}
                                                >
                                                    <Input placeholder="SAML 属性名称，例如 email" />
                                                </Form.Item>

                                                <Form.Item
                                                    name={[
                                                        field.name,
                                                        'userAttribute',
                                                    ]}
                                                    rules={[
                                                        {
                                                            required: true,
                                                            whitespace: true,
                                                            message:
                                                                '请输入 Atlas 用户属性',
                                                        },
                                                    ]}
                                                    style={{
                                                        flex: 1,
                                                        minWidth: 0,
                                                        marginBottom: 0,
                                                    }}
                                                >
                                                    <Input placeholder="Atlas 用户属性，例如 email" />
                                                </Form.Item>

                                                <Button
                                                    type="text"
                                                    danger
                                                    icon={<DeleteOutlined />}
                                                    onClick={() =>
                                                        remove(field.name)
                                                    }
                                                />
                                            </Flex>
                                        ))}

                                        <Form.ErrorList errors={errors} />

                                        <Button
                                            type="dashed"
                                            icon={<PlusOutlined />}
                                            onClick={() =>
                                                add({
                                                    samlAttribute: '',
                                                    userAttribute: '',
                                                })
                                            }
                                            block
                                        >
                                            添加属性映射
                                        </Button>
                                    </Flex>
                                )}
                            </Form.List>
                        </Form.Item>

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
                </Flex>
            </Flex>
        </Loading>
    )
}

export default Saml2ClientApplicationEdit