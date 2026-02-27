import { Space, Flex, Form, Input, Button, Popconfirm, Row, Col, theme, Radio } from 'antd'
import { OperationMode } from '../../../../enums/common';
import { useEffect, useState } from 'react';
import { createCompany, fetchCompanyDetails, fetchDictByCode, updateCompany } from '../../../../services/SystemService';
import { getMessageApi } from '../../../../utils/MessageUtil';
import { useRequest } from 'ahooks';
import Loading from '../../../../components/loading';
import { useTranslation } from 'react-i18next';
import OptionSelect from '../../../../components/OptionSelect';
import EditableTable from '../../../../components/smart-table/EditableTable';
import HasPermission from '../../../../components/HasPermission';


const CompanyDetails = ({ companyId, parentId, parentName, operationMode, changeOperationMode, onSuccess }) => {

    const { t } = useTranslation()

    const [form] = Form.useForm()

    const { token } = theme.useToken()

    const { runAsync: createCompanyAsync, loading: createCompanyLoading } = useRequest(createCompany, {
        manual: true
    })

    const { runAsync: updateCompanyAsync, loading: updateCompanyLoading } = useRequest(updateCompany, {
        manual: true
    })

    const { runAsync: getCompanyDetailAsync, loading: getCompanyDetailLoading } = useRequest(fetchCompanyDetails, {
        manual: true
    })

    useEffect(() => {
        const fetchData = async (companyId) => {
            const companyData = await getCompanyDetailAsync(companyId)
            form.setFieldsValue({ ...companyData })
        }

        switch (operationMode) {
            case OperationMode.VIEW.value:
                fetchData(companyId)
                break
            case OperationMode.ADD.value:
                form.resetFields()
                form.setFieldsValue({
                    parentId: parentId,
                    parentName: parentName
                })
                break
            case OperationMode.EDIT.value:
                break
        }

    }, [operationMode, companyId, form, parentName])

    if (!operationMode) {
        return <></>
    }

    const saveCompany = async () => {
        const companyInfo = await form.validateFields()
        let companyId
        if (operationMode === OperationMode.ADD.value) {
            companyId = await createCompanyAsync(companyInfo)
        } else {
            await updateCompanyAsync(companyInfo)
            companyId = companyInfo.id
        }
        onSuccess(companyId)
        getMessageApi().success('保存成功')
    }

    const resetForm = () => {
        form.resetFields()
    }

    const bankInfoColumns = [
        {
            key: 'id',
            dataIndex: 'id',
            hidden: true
        },
        {
            key: 'bankBranchName',
            title: '开户行',
            dataIndex: 'bankBranchName',
            align: 'center',
            editable: true,
            required: true,
        },
        {
            key: 'bankAccountName',
            title: '开户名称',
            dataIndex: 'bankAccountName',
            align: 'center',
            editable: true,
            required: true,
        },
        {
            key: 'bankAccountNo',
            title: '开户账号',
            dataIndex: 'bankAccountNo',
            align: 'center',
            editable: true,
            required: true,
        }
    ]


    return (
        <Loading spinning={getCompanyDetailLoading || createCompanyLoading || updateCompanyLoading}>
            <Flex
                className='w-full'
                gap={10}
                vertical
            >
                <Flex
                    style={{ marginLeft: '20px' }}
                    hidden={!(operationMode === OperationMode.VIEW.value)}
                >
                    <HasPermission hasPermissions='system:company:write'>
                        <Button type="primary" onClick={() => changeOperationMode(OperationMode.EDIT.value)}>{t('编辑')}</Button>
                    </HasPermission>
                </Flex>
                <Form
                    form={form}
                    style={{ width: '100%' }}
                    labelCol={{ span: 8 }}
                    wrapperCol={{ span: 16 }}
                    layout="horizontal"
                    disabled={operationMode === OperationMode.VIEW.value}
                >
                    <Form.Item name="id" noStyle />
                    <Form.Item name="parentId" noStyle />
                    <Row gutter={16}>
                        <Col span={8}>
                            <Form.Item
                                label="上级公司"
                                name="parentName"
                            >
                                <Input disabled />
                            </Form.Item>
                        </Col>
                        <Col span={8}>
                            <Form.Item
                                label="公司名称"
                                name="companyName"
                                rules={[
                                    {
                                        required: true,
                                        message: `公司名称不能为空`,
                                    },
                                ]}
                            >
                                <Input />
                            </Form.Item>
                        </Col>
                        <Col span={8}>
                            <Form.Item
                                label="公司简称"
                                name="companyShortName"
                                rules={[
                                    {
                                        required: true,
                                        message: `公司简称不能为空`,
                                    },
                                ]}
                            >
                                <Input />
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={8}>
                            <Form.Item
                                name="socialCreditCode"
                                label="统一社会信用代码"
                                labelCol={{ span: 10 }}
                                wrapperCol={{ span: 14 }}
                                required
                            >
                                <Input placeholder="请输入统一社会信用代码" />
                            </Form.Item>
                        </Col>
                        <Col span={8}>
                            <Form.Item name="registerCityName" noStyle />
                            <Form.Item
                                name="registerCityCode"
                                label="注册城市"
                                required
                            >
                                <OptionSelect
                                    loadData={() => fetchDictByCode("REGION", "CITY")}
                                    placeholder="请选择注册城市"
                                    onChange={(value, options) => {
                                        form.setFieldsValue({ registerCityCode: value, registerCityName: options.label })
                                    }}
                                />
                            </Form.Item>

                        </Col>
                        <Col span={8}>
                            <Form.Item name="enabled" label="启用状态" required>
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
                        <Col span={8}>
                            <Form.Item name="officePhone" label="办公电话">
                                <Input placeholder="请输入办公电话" />
                            </Form.Item>
                        </Col>
                        <Col span={16}>
                            <Form.Item
                                name="officeAddress"
                                label="办公地址"
                                labelCol={{ span: 4 }}
                                wrapperCol={{ span: 20 }}
                            >
                                <Input placeholder="请输入办公地址" />
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={8}>
                            <Form.Item name="officeEmail" label="办公邮箱">
                                <Input placeholder="请输入办公邮箱" />
                            </Form.Item>
                        </Col>
                    </Row>
                    <Row gutter={16}>
                        <Col span={24}>
                            <Form.List
                                name="companyBankList"
                                noStyle
                            >
                                {(fields, { add, remove }, { errors }) => (
                                    <EditableTable
                                        columns={bankInfoColumns}
                                        name='companyBankList'
                                        mode='multi-add'
                                        fields={fields}
                                        errors={errors}
                                        add={add}
                                        remove={remove}
                                        operationMode={operationMode}
                                        scroll={{
                                            y: 200
                                        }}
                                    />
                                )}
                            </Form.List>
                        </Col>
                    </Row>
                </Form>
                <Flex
                    justify='flex-end'
                    align='center'
                    style={{
                        position: 'sticky',
                        backgroundColor: token.colorBgElevated,
                        bottom: 0,
                        left: 0,
                        width: '100%',
                        padding: '10px'
                    }}
                >
                    <Space>
                        {operationMode !== OperationMode.VIEW.value && (
                            <Button loading={createCompanyLoading || updateCompanyLoading} onClick={saveCompany} type="primary">{t('提交')}</Button>
                        )}
                        {operationMode === OperationMode.EDIT.value && (
                            <Button onClick={() => changeOperationMode(OperationMode.VIEW.value)}>{t('取消')}</Button>
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

export default CompanyDetails