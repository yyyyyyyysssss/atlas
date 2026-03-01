import './index.css'
import { Space, Flex, Form, Input, Button, Popconfirm, Row, Col, InputNumber } from 'antd'
import { UploadOutlined } from '@ant-design/icons';
import { OperationMode } from '../../../../enums/common';
import { useEffect, useState } from 'react';
import { createOrg, fetchOrgDetails, fetchUserOptions, updateOrg } from '../../../../services/SystemService';
import HasPermission from '../../../../components/HasPermission';
import { getMessageApi } from '../../../../utils/MessageUtil';
import { useRequest } from 'ahooks';
import Loading from '../../../../components/loading';
import { useTranslation } from 'react-i18next';
import OptionSelect from '../../../../components/OptionSelect';
import { OrganizationStatus } from '../../../../enums/system';

const OrgDetails = ({ orgId, parentId, parentCode, orgType, operationMode, changeOperationMode, onSuccess }) => {

    const { t } = useTranslation()

    const [form] = Form.useForm()

    const [orgData, setOrgData] = useState()

    const { runAsync: fetchOrgDetailsAsync, loading: fetchOrgDetailsLoading } = useRequest(fetchOrgDetails, {
        manual: true
    })

    const { runAsync: createOrgAsync, loading: createOrgLoading } = useRequest(createOrg, {
        manual: true
    })

    const { runAsync: updateOrgAsync, loading: updateOrgLoading } = useRequest(updateOrg, {
        manual: true
    })

    useEffect(() => {

        const fetchData = async (orgId) => {
            if (!orgId) {
                return
            }
            const orgData = await fetchOrgDetailsAsync(orgId)
            form.setFieldsValue({ ...orgData, parentCode: parentCode })
            setOrgData(orgData)
        }
        switch (operationMode) {
            case OperationMode.VIEW.value:
                fetchData(orgId)
                break
            case OperationMode.ADD.value:
                form.resetFields()
                form.setFieldsValue({
                    parentCode: parentCode,
                    parentId: parentId,
                    orgType: orgType
                })
                break
            case OperationMode.EDIT.value:
            case OperationMode.CANCEL.value:
                break
        }
    }, [operationMode, orgId, orgType, form, parentId, parentCode])

    if (!operationMode || (operationMode === OperationMode.CANCEL.value && !orgId)) {
        return null
    }

    const resetForm = () => {
        form.resetFields()
    }

    const saveOrg = async () => {
        const formValues = await form.validateFields()
        const orgInfo = {
            ...formValues,
        }
        let orgId
        if (operationMode === OperationMode.ADD.value) {
            orgId = await createOrgAsync(orgInfo)
        } else {
            await updateOrgAsync(orgInfo)
            orgId = orgInfo.id
        }
        getMessageApi().success('保存成功')
        onSuccess(orgId)
    }



    return (
        <Loading spinning={fetchOrgDetailsLoading || createOrgLoading || updateOrgLoading}>
            <Flex
                className='w-full'
                gap={20}
                vertical
            >
                <Flex
                    hidden={!(operationMode === OperationMode.VIEW.value || operationMode === OperationMode.CANCEL.value)}
                >
                    <HasPermission hasPermissions='system:org:write'>
                        <Button type="primary" onClick={() => changeOperationMode(OperationMode.EDIT.value)}>{t('编辑')}</Button>
                    </HasPermission>
                </Flex>
                <Form
                    form={form}
                    style={{ width: '100%' }}
                    labelCol={{ span: 2 }}
                    wrapperCol={{ span: 20 }}
                    layout="horizontal"
                    disabled={operationMode === OperationMode.VIEW.value || operationMode === OperationMode.CANCEL.value}
                >
                    <Form.Item name="id" noStyle />
                    <Form.Item name="parentId" noStyle />
                    <Form.Item name="orgType" noStyle />
                    <Row gutter={16}>
                        <Col span={24}>
                            <Form.Item
                                label="组织名称"
                                name="orgName"
                                rules={[
                                    {
                                        required: true,
                                        message: `组织名称不能为空`,
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
                                label="组织编码"
                                name="orgCode"
                            >
                                <Input disabled />
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={24}>
                            <Form.Item
                                label="状态"
                                name="status"
                            >
                                <OptionSelect
                                    loadData={Object.values(OrganizationStatus)}
                                    placeholder="请选择状态"
                                />
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={24}>
                            <Form.Item
                                label="负责人"
                                name="leaderId"
                            >
                                <OptionSelect
                                    loadData={() => fetchUserOptions()}
                                    placeholder="请选择负责人"
                                />
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={24}>
                            <Form.Item
                                label="排序"
                                name="sort"
                            >
                                <InputNumber precision={0} style={{ width: '100%' }} />
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={24}>
                            <Form.Item
                                label="备注"
                                name="remark"
                            >
                                <Input.TextArea placeholder="请输入备注" />
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
                        {operationMode !== OperationMode.VIEW.value && operationMode !== OperationMode.CANCEL.value && (
                            <Button loading={createOrgLoading || updateOrgLoading} onClick={saveOrg} type="primary">{t('提交')}</Button>
                        )}
                        {operationMode !== OperationMode.VIEW.value && operationMode !== OperationMode.CANCEL.value && (
                            <Button onClick={() => changeOperationMode(OperationMode.CANCEL.value)}>{t('取消')}</Button>
                        )}
                        {operationMode === OperationMode.ADD.value && (
                            <Popconfirm
                                okText={t('确定')}
                                cancelText={t('取消')}
                                title={t('确定重置')}
                                onConfirm={resetForm}
                                style={{ marginInlineEnd: 8 }}
                            >
                                <Button>{t('重置')}</Button>
                            </Popconfirm>
                        )}
                    </Space>
                </Flex>
            </Flex>
        </Loading>
    )
}

export default OrgDetails