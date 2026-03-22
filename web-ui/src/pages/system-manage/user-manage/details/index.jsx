import React, { useEffect } from 'react';
import './index.css'
import { Button, Col, Flex, Form, Input, Radio, Row, Space } from 'antd';
import RoleSelect from '../../../../components/RoleSelect';
import OptionTreeSelect from '../../../../components/OptionTreeSelect';
import useFullParams from '../../../../hooks/useFullParams';
import { useTranslation } from 'react-i18next'
import useBack from '../../../../hooks/useBack';
import { useRequest } from 'ahooks';
import { createUser, fetchOrgOptions, fetchPositionByOrgId, fetchUserDetails, updateUser } from '../../../../services/SystemService';
import { OperationMode } from '../../../../enums/common';
import { getMessageApi } from '../../../../utils/MessageUtil';
import Loading from '../../../../components/loading';
import { useNavigate } from 'react-router-dom';
import OptionSelect from '../../../../components/OptionSelect';

const UserDetails = () => {

    const { t } = useTranslation()

    const { userId, operationMode = OperationMode.ADD.value } = useFullParams()

    const [form] = Form.useForm()

    const navigate = useNavigate()

    const { goBack } = useBack()

    const { runAsync: createUserAsync, loading: createUserLoading } = useRequest(createUser, {
        manual: true
    })

    const { runAsync: updateUserAsync, loading: updateUserLoading } = useRequest(updateUser, {
        manual: true
    })

    const { runAsync: getUserDetailsAsync, loading: getUserDetailsLoading } = useRequest(fetchUserDetails, {
        manual: true
    })

    const fetchData = async (userId) => {
        if (!userId) {
            return
        }
        form.resetFields()
        const userData = await getUserDetailsAsync(userId)
        form.setFieldsValue({ ...userData })
    }

    useEffect(() => {
        switch (operationMode) {
            case OperationMode.VIEW.value:
            case OperationMode.EDIT.value:
                fetchData(userId)
                break
            case OperationMode.ADD.value:
                form.resetFields()
                form.setFieldsValue({
                    enabled: true
                })
                break
        }

    }, [operationMode, userId])

    const saveUser = async () => {
        const values = await form.validateFields()
        if (operationMode === OperationMode.ADD.value) {
            const result = await createUserAsync(values)
            navigate('/success', {
                state: {
                    title: '用户创建成功',
                    subTitle: '初始密码为',
                    code: result.initialPassword,
                    listRouterPath: '/system/user',
                    againRouterPath: '/system/user/details'
                }
            })
        } else if (operationMode === OperationMode.EDIT.value) {
            await updateUserAsync(values)
            getMessageApi().success(t('操作成功'))
            goBack()
        }
    }

    return (
        <Loading spinning={getUserDetailsLoading || createUserLoading || updateUserLoading}>
            <Flex
                justify="center"
                align="center"
                style={{ minHeight: '100%', padding: '20px' }}
                vertical
            >
                <Form
                    form={form}
                    style={{ width: '100%' }}
                    labelCol={{ span: 3 }}
                    wrapperCol={{ span: 21 }}
                    layout="horizontal"
                    disabled={operationMode === OperationMode.VIEW.value}
                >
                    <Form.Item name="id" noStyle hidden />
                    <Row gutter={16}>
                        <Col span={24}>
                            <Form.Item
                                label="用户姓名"
                                name="fullName"
                                rules={[
                                    {
                                        required: true,
                                        message: `用户姓名不能为空`,
                                    },
                                ]}
                            >
                                <Input placeholder="请输入用户姓名" />
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={24}>
                            <Form.Item
                                label="用户账号"
                                name="username"
                                rules={[
                                    {
                                        required: true,
                                        message: `用户账号不能为空`,
                                    },
                                ]}
                            >
                                <Input placeholder="请输入用户账号" disabled={operationMode == OperationMode.EDIT.value} />
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={24}>
                            <Form.Item
                                label="用户邮箱"
                                name="email"
                            >
                                <Input placeholder="请输入用户邮箱" />
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={24}>
                            <Form.Item
                                label="用户手机号"
                                name="phone"
                            >
                                <Input placeholder="请输入用户手机号" />
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={24}>
                            <Form.Item
                                label="启用状态"
                                name="enabled"
                                rules={[
                                    {
                                        required: true,
                                        message: `启用状态不能为空`,
                                    },
                                ]}
                            >
                                <Radio.Group
                                    options={[
                                        { value: true, label: '启用' },
                                        { value: false, label: '停用' }
                                    ]}
                                />
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={24}>
                            <Form.Item
                                label="所属组织"
                                name="orgId"
                                rules={[
                                    {
                                        required: true,
                                        message: `所属组织不能为空`,
                                    },
                                ]}
                            >
                                <OptionTreeSelect
                                    fetchData={fetchOrgOptions}
                                    multiple={false}
                                />
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={24}>
                            <Form.Item
                                noStyle
                                dependencies={['orgId']}
                            >
                                {({ getFieldValue }) => {
                                    const orgId = getFieldValue('orgId')
                                    return orgId ? (
                                        <Form.Item
                                            label="岗位"
                                            name="posId"
                                            rules={[
                                                {
                                                    required: true,
                                                    message: `岗位不能为空`,
                                                },
                                            ]}
                                        >
                                            <OptionSelect
                                                loadData={() => fetchPositionByOrgId(orgId)}
                                                fieldNames={{
                                                    value: 'id',
                                                    label: 'posName',
                                                }}
                                                placeholder="请选择岗位"
                                            />
                                        </Form.Item>
                                    ) : null
                                }}
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={24}>
                            <Form.Item
                                label="分配角色"
                                name="roleIds"
                            >
                                <RoleSelect />
                            </Form.Item>
                        </Col>
                    </Row>
                </Form>
                <Flex
                    justify='flex-end'
                    align='center'
                    style={{
                        position: 'sticky',
                        bottom: 0,
                        left: 0,
                        width: '100%',
                        padding: '10px'
                    }}
                >
                    <Space>
                        {operationMode !== OperationMode.VIEW.value && (
                            <Button loading={createUserLoading || updateUserLoading} onClick={saveUser} type="primary">{t('提交')}</Button>
                        )}
                        {operationMode !== OperationMode.VIEW.value && operationMode !== OperationMode.CANCEL.value && (
                            <Button onClick={() => goBack()}>{t('取消')}</Button>
                        )}
                    </Space>
                </Flex>
            </Flex>
        </Loading>

    )
}

export default UserDetails