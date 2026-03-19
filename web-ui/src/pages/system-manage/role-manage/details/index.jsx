import React, { useEffect } from 'react';
import './index.css'
import useFullParams from '../../../../hooks/useFullParams';
import { Button, Col, Flex, Form, Input, Radio, Row, Space } from 'antd';
import { useRequest } from 'ahooks';
import { createRole, fetchAuthorityTree, fetchOrgOptions, fetchRoleDetails, fetchSearchUser, updateRole } from '../../../../services/SystemService';
import Loading from '../../../../components/loading';
import { OperationMode } from '../../../../enums/common';
import RemoteSearchSelect from '../../../../components/RemoteSearchSelect';
import OptionTreeSelect from '../../../../components/OptionTreeSelect';
import { getMessageApi } from '../../../../utils/MessageUtil';
import useBack from '../../../../hooks/useBack';
import { useTranslation } from 'react-i18next'
import OptionSelect from '../../../../components/OptionSelect';
import { RoleDataScope } from '../../../../enums/system';




const RoleDetails = () => {

    const { t } = useTranslation()

    const { roleId, operationMode } = useFullParams()

    const [form] = Form.useForm()

    const { goBack } = useBack()

    const { runAsync: getRoleDetailsAsync, loading: getRoleDetailsLoading } = useRequest(fetchRoleDetails, {
        manual: true
    })

    const { runAsync: createRoleAsync, loading: createRoleLoading } = useRequest(createRole, {
        manual: true
    })

    const { runAsync: updateRoleAsync, loading: updateRoleLoading } = useRequest(updateRole, {
        manual: true
    })

    const fetchData = async (roleId) => {
        if (!roleId) {
            return
        }
        form.resetFields()
        const roleData = await getRoleDetailsAsync(roleId)
        form.setFieldsValue({ ...roleData })
    }

    useEffect(() => {
        switch (operationMode) {
            case OperationMode.VIEW.value:
            case OperationMode.EDIT.value:
                fetchData(roleId)
                break
            case OperationMode.ADD.value:
                form.resetFields()
                break
        }

    }, [operationMode, roleId])

    const saveRole = async () => {
        const values = await form.validateFields()
        if (operationMode === OperationMode.ADD.value) {
            await createRoleAsync(values)
        } else if (operationMode === OperationMode.EDIT.value) {
            await updateRoleAsync(values)
        }
        getMessageApi().success(t('操作成功'))
        goBack()
    }

    return (
        <Loading spinning={getRoleDetailsLoading || createRoleLoading || updateRoleLoading}>
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
                                label="角色名称"
                                name="name"
                                rules={[
                                    {
                                        required: true,
                                        message: `角色名称不能为空`,
                                    },
                                ]}
                            >
                                <Input />
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={24}>
                            <Form.Item
                                label="角色编码"
                                name="code"
                                rules={[
                                    {
                                        required: true,
                                        message: `角色编码不能为空`,
                                    },
                                ]}
                            >
                                <Input disabled={operationMode == 'EDIT'} />
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
                                label="分配权限"
                                name="authorityIds"
                            >
                                <OptionTreeSelect
                                    fetchData={fetchAuthorityTree}
                                    multiple={true}
                                    fieldNames={{
                                        title: 'label',
                                        value: 'value',
                                        children: 'children'
                                    }}
                                />
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={24}>
                            <Form.Item
                                label="分配用户"
                                name="userIds"
                            >
                                <RemoteSearchSelect
                                    mode='multiple'
                                    fetchData={fetchSearchUser}
                                    labelField='fullName'
                                    valueField='id'
                                    placeholder='请输入用户名称'
                                    allowClear
                                />
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={24}>
                            <Form.Item
                                label="数据权限"
                                name="dataScope"
                            >
                                <OptionSelect
                                    loadData={Object.values(RoleDataScope)}
                                />
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={24}>
                            <Form.Item
                                noStyle
                                dependencies={['dataScope']}
                            >
                                {({ getFieldValue }) => {
                                    const isCustom = getFieldValue('dataScope') === RoleDataScope.CUSTOM.value
                                    return isCustom ? (
                                        <Form.Item
                                            label="数据范围"
                                            name="customDataScope"
                                        >
                                            <OptionTreeSelect
                                                fetchData={fetchOrgOptions}
                                                multiple={true}
                                                includeParents={false}
                                            />
                                        </Form.Item>
                                    ) : null
                                }}
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
                            <Button loading={createRoleLoading || updateRoleLoading} onClick={saveRole} type="primary">{t('提交')}</Button>
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

export default RoleDetails;