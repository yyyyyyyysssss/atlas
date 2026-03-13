import { useEffect, useState } from 'react'
import './index.css'
import { Button, Checkbox, Drawer, Flex, Form, Input, Modal, Popconfirm, Radio, Select, Skeleton, Space, Splitter, Switch, Table, Tag, Tree, Typography } from 'antd'
import { useRequest } from 'ahooks'
import { createPosition, deletePositionById, fetchPositionList, fetchSearchUser, updatePosition } from '../../../../services/SystemService'
import { OperationMode } from '../../../../enums/common'
import OptionSelect from '../../../../components/OptionSelect'
import { OrganizationType, PositionStatus, PositionType } from '../../../../enums/system'
import { useTranslation } from 'react-i18next'
import SmartTable from '../../../../components/smart-table'
import HasPermission from '../../../../components/HasPermission'
import RemoteSearchSelect from '../../../../components/RemoteSearchSelect'
import { getMessageApi } from '../../../../utils/MessageUtil'

const PositionDetails = ({ orgId, orgType, orgPath }) => {

    const { t } = useTranslation()

    const initQueryParam = {
        pageNum: 1,
        pageSize: 10,
        keyword: null,
        status: null,
        orgPath: orgPath,
        includeChildren: true
    }

    const [modal, contextHolder] = Modal.useModal()

    const [searchForm] = Form.useForm()

    const [editForm] = Form.useForm()

    const [queryParam, setQueryParam] = useState(initQueryParam)

    const [positionOperation, setPositionOperation] = useState({
        open: false,
        title: null,
        operationType: null,
    })

    const { runAsync: getPositionDataAsync, loading: getPositionDataLoading } = useRequest(fetchPositionList, {
        manual: true
    })

    const { runAsync: createPositionAsync, loading: createPositionLoading } = useRequest(createPosition, {
        manual: true
    })

    const { runAsync: updatePositionAsync, loading: updatePositionLoading } = useRequest(updatePosition, {
        manual: true
    })

    useEffect(() => {
        if (orgId && orgPath) {
            const newQueryParam = { ...queryParam, orgPath: orgPath }
            setQueryParam(newQueryParam)
        }
    }, [orgId, orgPath])


    const getData = async (queryParam) => {
        return await getPositionDataAsync(queryParam)
    }

    const handleSearch = () => {
        searchForm.validateFields()
            .then(values => {
                const newQueryParam = { ...queryParam, ...values, pageNum: 1 }
                setQueryParam(newQueryParam)
            })
    }

    const handleReset = () => {
        searchForm.resetFields()
        setQueryParam({ ...initQueryParam })
    }

    const handleRefresh = () => {
        const newQueryParam = { ...queryParam }
        setQueryParam(newQueryParam)
    }

    const handleincludeChildren = (checked) => {
        const newQueryParam = { ...queryParam, includeChildren: checked }
        setQueryParam(newQueryParam)
    }

    const handleAddPosition = () => {
        setPositionOperation({
            open: true,
            title: '新增岗位',
            operationType: OperationMode.ADD.value,
        })
        const positionType = (orgType === OrganizationType.GROUP.value || orgType === OrganizationType.COMPANY.value) ? PositionType.BASE.value : PositionType.ORG.value
        editForm.setFieldsValue({
            orgId: orgId,
            status: PositionStatus.ACTIVE.value,
            type: positionType
        })
    }

    const handleEditPosition = (record) => {
        setPositionOperation({
            open: true,
            title: '编辑岗位',
            operationType: OperationMode.EDIT.value
        })
        editForm.setFieldsValue(record)
    }

    const handleClose = () => {
        setPositionOperation({
            open: false,
            title: null,
            operationType: null,
        })
        editForm.resetFields()
    }

    const handleSavePosition = async () => {
        const values = await editForm.validateFields()
        const positionData = {
            ...values
        }
        if (positionOperation.operationType === OperationMode.ADD.value) {
            await createPositionAsync(positionData)
        } else {
            await updatePositionAsync(positionData)
        }
        getMessageApi().success(t('新增成功'))
        handleClose()
        handleRefresh()
    }

    const columns = [
        {
            key: 'id',
            dataIndex: 'id',
            hidden: true,
        },
        {
            key: 'orgId',
            dataIndex: 'orgId',
            hidden: true,
        },
        {
            key: 'posName',
            title: '岗位名称',
            dataIndex: 'posName',
            align: 'center',
            fixed: 'left',
            width: '140px',
            showSorterTooltip: { target: 'full-header' },
            sorter: (a, b) => a.posName.localeCompare(b.posName),
        },
        {
            key: 'posCode',
            title: '岗位编码',
            dataIndex: 'posCode',
            align: 'center',
        },
        {
            key: 'type',
            title: '岗位类型',
            dataIndex: 'type',
            align: 'center',
            render: (_, { type }) => {
                const config = PositionType[type]
                return (
                    <Tag color={config?.color || 'default'}>
                        {config?.label || type}
                    </Tag>
                )
            }
        },
        {
            key: 'status',
            title: '状态',
            dataIndex: 'status',
            width: '100px',
            align: 'center',
            render: (_, { status }) => {
                const config = PositionStatus[status]
                return (
                    <Tag color={config?.color || 'default'}>
                        {config?.label || status}
                    </Tag>
                )
            }
        },
        {
            key: 'level',
            title: '职级',
            dataIndex: 'level',
            align: 'center',
        },
        {
            key: 'createTime',
            title: '创建时间',
            dataIndex: 'createTime',
            align: 'center',
        },
        {
            key: 'creatorName',
            title: '创建人',
            dataIndex: 'creatorName',
            align: 'center',
        },
        {
            key: 'updateTime',
            title: '修改时间',
            dataIndex: 'updateTime',
            align: 'center',
        },
        {
            key: 'updaterName',
            title: '修改人',
            dataIndex: 'updaterName',
            align: 'center',
        },
        {
            key: 'operation',
            title: '操作',
            dataIndex: 'operation',
            align: 'center',
            fixed: 'right',
            render: (_, record) => {
                return (
                    <Space size="middle">
                        <HasPermission hasPermissions='system:position:write'>
                            <Typography.Link onClick={() => handleEditPosition(record)} style={{ marginInlineEnd: 8 }}>
                                {t('编辑')}
                            </Typography.Link>
                        </HasPermission>
                    </Space>
                )
            }
        }
    ]

    return (
        <Flex
            gap={16}
            vertical
        >
            <Flex
                justify='center'
            >
                <Form
                    form={searchForm}
                    layout='inline'
                    onFinish={handleSearch}
                >
                    <Form.Item name="keyword" label="岗位信息" style={{ width: 350 }}>
                        <Input placeholder="请输入岗位名称或编码" allowClear />
                    </Form.Item>
                    <Form.Item name="status" label="状态">
                        <OptionSelect
                            loadData={Object.values(PositionStatus)}
                            placeholder="请选择状态"
                        />
                    </Form.Item>
                    <Form.Item style={{ display: 'none' }}>
                        <Button htmlType="submit" />
                    </Form.Item>
                </Form>
                <Space>
                    <Button style={{ width: '80px' }} type="primary" onClick={handleSearch} loading={getPositionDataLoading}>{t('查询')}</Button>
                    <Button style={{ width: '80px' }} onClick={handleReset} loading={getPositionDataLoading}>{t('重置')}</Button>
                </Space>
            </Flex>
            <SmartTable
                className='w-full'
                columns={columns}
                headerExtra={
                    <Space>
                        <HasPermission hasPermissions='system:position:write'>
                            <Button type="primary" onClick={handleAddPosition}>{t('新增')}</Button>
                        </HasPermission>
                        <Flex gap={8} justify='end'>
                            <Typography.Text type="secondary" style={{ fontSize: '12px', cursor: 'pointer' }}>
                                {t('包含下级组织岗位')}
                            </Typography.Text>
                            <Checkbox
                                checked={queryParam.includeChildren}
                                onChange={(e) => handleincludeChildren(e.target.checked)}
                            />
                        </Flex>
                    </Space>
                }
                autoFetch={false}
                fetchData={getData}
                loading={getPositionDataLoading}
                queryParam={queryParam}
                setQueryParam={setQueryParam}
            />
            <Modal
                title={t(positionOperation.title)}
                width={400}
                centered
                open={positionOperation.open}
                confirmLoading={createPositionLoading || updatePositionLoading}
                onOk={handleSavePosition}
                onCancel={handleClose}
                onClose={handleClose}
                maskClosable={false}
                keyboard={false}
                okText={t('保存')}
                cancelText={t('取消')}
            >
                <Form
                    form={editForm}
                    labelCol={{ span: 6 }}
                    wrapperCol={{ span: 18 }}
                    layout="horizontal"
                >
                    <div
                        className='w-full mt-5'
                    >
                        <Form.Item name="id" hidden>
                            <Input />
                        </Form.Item>
                        <Form.Item name="orgId" hidden>
                            <Input />
                        </Form.Item>
                        <Form.Item
                            label="岗位名称"
                            name="posName"
                            rules={[
                                {
                                    required: true,
                                    message: `岗位名称不能为空`,
                                },
                            ]}
                        >
                            <Input />
                        </Form.Item>
                        <Form.Item
                            label="岗位编码"
                            name="posCode"
                        >
                            <Input disabled />
                        </Form.Item>
                        <Form.Item
                            label="状态"
                            name="status"
                            rules={[
                                {
                                    required: true,
                                    message: `状态不能为空`,
                                },
                            ]}
                        >
                            <OptionSelect
                                loadData={Object.values(PositionStatus)}
                                placeholder="请选择状态"
                            />
                        </Form.Item>
                        <Form.Item
                            label="职级"
                            name="level"
                            rules={[
                                {
                                    required: true,
                                    message: `职级不能为空`,
                                },
                            ]}
                        >
                            <OptionSelect
                                loadData='POSITION_LEVEL'
                                placeholder="请选择职级"
                            />
                        </Form.Item>
                        <Form.Item
                            label="岗位类型"
                            name="type"
                        >
                            <OptionSelect
                                loadData={Object.values(PositionType)}
                                disabled
                                placeholder="请选择岗位类型"
                            />
                        </Form.Item>
                        <Form.Item
                            label="备注"
                            name="remark"
                        >
                            <Input.TextArea placeholder="请输入备注" />
                        </Form.Item>
                    </div>
                </Form>

            </Modal>
            {/* <Modal
                title={bindUser.title}
                width={600}
                centered
                open={bindUser.open}
                onOk={handleBindUserSave}
                onCancel={handleBindUserClose}
                onClose={handleBindUserClose}
                confirmLoading={bindPositionUserLoading}
                maskClosable={false}
                destroyOnHidden
                okText={t('保存')}
                cancelText={t('取消')}
                afterClose={() => bindUserForm.resetFields()}
                okButtonProps={{
                    disabled: getUserIdByPositionIdLoading
                }}
            >

                <Form
                    form={bindUserForm}
                >
                    <Loading spinning={getUserIdByPositionIdLoading}>
                        <Form.Item name="userIds">
                            <UserTransfer />
                        </Form.Item>
                    </Loading>
                </Form>
            </Modal> */}
            {contextHolder}
        </Flex>
    )
}

export default PositionDetails