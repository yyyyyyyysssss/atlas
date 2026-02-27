import { Button, Flex, Form, Input, InputNumber, Modal, Popconfirm, Radio, Select, Space, Switch, Tooltip, Typography } from 'antd'
import './index.css'
import SmartTable from '../../../components/smart-table'
import HasPermission from '../../../components/HasPermission'
import { createDict, deleteDictById, fetchDictDetails, fetchDictList, updateDict, updateDictEnabled } from '../../../services/SystemService'
import { useState } from 'react'
import { useTranslation } from 'react-i18next';
import { getMessageApi } from '../../../utils/MessageUtil'
import { useRequest } from 'ahooks';
import { OperationMode } from '../../../enums/common'
import Loading from '../../../components/loading'
import ActionDropdown from '../../../components/ActionDropdown'
import Highlight from '../../../components/Highlight'
import EllipsisTooltipText from '../../../components/EllipsisTooltipText'
import { useNavigate } from 'react-router-dom'


const initQueryParam = {
    pageNum: 1,
    pageSize: 10,
    enabled: null,
    keyword: null
}

const DictManage = () => {

    const { t } = useTranslation()

    const navigate = useNavigate()

    const [searchForm] = Form.useForm()

    const [editForm] = Form.useForm()

    const [queryParam, setQueryParam] = useState(initQueryParam)

    const [dictOperation, setDictOperation] = useState({
        open: false,
        title: null,
        operationType: null,
    })

    const [dictEnabledLoadingMap, setDictEnabledLoadingMap] = useState({})

    const { runAsync: getDictDataAsync, loading: getDictDataLoading } = useRequest(fetchDictList, {
        manual: true
    })

    const { runAsync: createDictAsync, loading: createDictLoading } = useRequest(createDict, {
        manual: true
    })

    const { runAsync: updateDictAsync, loading: updateDictLoading } = useRequest(updateDict, {
        manual: true
    })

    const { runAsync: deleteDictByIdAsync, loading: deleteDictByIdLoading } = useRequest(deleteDictById, {
        manual: true
    })

    const { runAsync: getDictDetailsAsync, loading: getDictDetailsLoading } = useRequest(fetchDictDetails, {
        manual: true
    })

    const getData = async (queryParam) => {
        return await getDictDataAsync(queryParam)
    }

    const handleAddDict = () => {
        setDictOperation({
            open: true,
            title: '新增字典',
            operationType: OperationMode.ADD,
        })
    }

    const handleEditDict = async (dictId) => {
        setDictOperation({
            open: true,
            title: '修改字典',
            operationType: OperationMode.EDIT,
        })
        const dictDetail = await getDictDetailsAsync(dictId)
        editForm.setFieldsValue(dictDetail)
    }

    const handleSaveDict = async () => {
        const editFormData = await editForm.validateFields()
        if (dictOperation.operationType === OperationMode.ADD) {
            await createDictAsync(editFormData)
        } else {
            await updateDictAsync(editFormData)
        }
        getMessageApi().success(t('操作成功'))
        handleCloseModal()
        handleRefresh()
    }

    const handleUpdateEnabled = async (id, enabled) => {
        setDictEnabledLoadingMap(prev => ({ ...prev, [id]: true }))
        try {
            await updateDictEnabled(id, enabled)
            if (enabled) {
                getMessageApi().success(t('启用成功'))
            } else {
                getMessageApi().success(t('停用成功'))
            }
            handleRefresh()
        } finally {
            setDictEnabledLoadingMap(prev => ({ ...prev, [id]: false }))
        }

    }

    const handleDelete = async (id) => {
        await deleteDictByIdAsync(id)
        getMessageApi().success('删除成功')
        handleRefresh()

    }

    const handleSearch = () => {
        searchForm.validateFields()
            .then(values => {
                const newQueryParam = { ...queryParam, ...values, pageNum: 1 }
                setQueryParam(newQueryParam)
            })
    }

    const handleCloseModal = () => {
        setDictOperation({
            open: false,
            title: null,
            operationType: null,
            dictDetail: null,
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

    const handleSettingDictItem = (dictId) => {
        navigate(`/system/dict/${dictId}`)
    }

    const columns = [
        {
            key: 'id',
            dataIndex: 'id',
            hidden: true
        },
        {
            key: 'code',
            title: '字典编码',
            dataIndex: 'code',
            align: 'center',
            fixed: 'left',
            visible: true
        },
        {
            key: 'name',
            title: '字典名称',
            dataIndex: 'name',
            align: 'center',
            visible: true
        },
        {
            key: 'enabled',
            title: '状态',
            dataIndex: 'enabled',
            align: 'center',
            visible: true,
            render: (_, record) => {
                const { id, enabled } = record
                const handleChange = (checked) => {
                    handleUpdateEnabled(id, checked)
                }
                return enabled ?
                    (
                        <HasPermission
                            hasPermissions='system:dict:write'
                            fallback={
                                <Switch
                                    disabled
                                    checkedChildren={t('启用')}
                                    unCheckedChildren={t('停用')}
                                    checked={enabled}
                                />
                            }

                        >
                            <Popconfirm
                                okText={t('确定')}
                                cancelText={t('取消')}
                                title={t('确定停用')}
                                onConfirm={() => handleChange(false)}
                                style={{ marginInlineEnd: 8 }}
                            >
                                <Switch
                                    loading={!!dictEnabledLoadingMap[id]}
                                    checkedChildren={t('启用')}
                                    unCheckedChildren={t('停用')}
                                    checked={enabled}
                                />
                            </Popconfirm>
                        </HasPermission>


                    )
                    :
                    (
                        <HasPermission
                            hasPermissions='system:dict:write'
                            fallback={
                                <Switch
                                    disabled
                                    checkedChildren={t('启用')}
                                    unCheckedChildren={t('停用')}
                                    checked={enabled}
                                />
                            }
                        >
                            <Switch
                                loading={!!dictEnabledLoadingMap[id]}
                                checkedChildren={t('启用')}
                                unCheckedChildren={t('停用')}
                                checked={enabled}
                                onChange={handleChange}
                            />
                        </HasPermission>
                    )
            }
        },
        {
            key: 'sort',
            title: '排序',
            dataIndex: 'sort',
            align: 'center',
            visible: true
        },
        {
            key: 'description',
            title: '描述',
            dataIndex: 'description',
            align: 'center',
            width: '140px',
            visible: true,
            render: (_, record) => {
                return <EllipsisTooltipText text={record.description} />
            }
        },
        {
            key: 'createTime',
            title: '创建时间',
            dataIndex: 'createTime',
            align: 'center',
            visible: true,
        },
        {
            key: 'creatorName',
            title: '创建人',
            dataIndex: 'creatorName',
            align: 'center',
            visible: true,
        },
        {
            key: 'updateTime',
            title: '修改时间',
            dataIndex: 'updateTime',
            align: 'center',
            visible: true,
        },
        {
            key: 'updaterName',
            title: '修改人',
            dataIndex: 'updaterName',
            align: 'center',
            visible: true,
        },
        {
            key: 'operation',
            title: '操作',
            dataIndex: 'operation',
            align: 'center',
            fixed: 'right',
            visible: true,
            render: (_, record) => {
                return (
                    <Space>
                        <HasPermission
                            hasPermissions='system:dict:write'
                        >
                            <Typography.Link onClick={() => handleEditDict(record.id)} style={{ marginInlineEnd: 8 }}>
                                {t('编辑')}
                            </Typography.Link>
                            <Typography.Link onClick={() => handleSettingDictItem(record.id)} style={{ marginInlineEnd: 8 }}>
                                {t('设置字典项')}
                            </Typography.Link>
                        </HasPermission>
                        <HasPermission
                            hasPermissions='system:dict:delete'
                        >
                            <ActionDropdown
                                items={[
                                    {
                                        key: 'delete',
                                        label: t('删除'),
                                        danger: true,
                                        confirm: {
                                            title: t('确定删除'),
                                            content: (
                                                <>
                                                    是否删除 <Highlight>{record.name}</Highlight> 字典？删除后将无法恢复！
                                                </>
                                            ),
                                            onOk: async () => {
                                                await handleDelete(record.id)
                                            },
                                            confirmLoading: deleteDictByIdLoading,
                                        }
                                    }
                                ]}
                            />
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
                    <Form.Item name="keyword" label="字典信息" style={{ width: 365 }}>
                        <Input placeholder="请输入字典编码或名称" allowClear />
                    </Form.Item>
                    <Form.Item name="enabled" label="状态">
                        <Select
                            placeholder="请选择状态"
                            style={{ width: 120 }}
                            allowClear
                            options={[
                                {
                                    label: '启用',
                                    value: true
                                },
                                {
                                    label: '停用',
                                    value: false
                                }
                            ]}
                        />
                    </Form.Item>
                    <Form.Item style={{ display: 'none' }}>
                        <Button htmlType="submit" />
                    </Form.Item>
                </Form>
                <Space>
                    <Button type="primary" onClick={handleSearch} loading={getDictDataLoading}>{t('查询')}</Button>
                    <Button onClick={handleReset} loading={getDictDataLoading}>{t('重置')}</Button>
                </Space>
            </Flex>
            <SmartTable
                className='w-full'
                columns={columns}
                headerExtra={(
                    <Space>
                        <HasPermission hasPermissions='system:dict:write'>
                            <Button type="primary" onClick={handleAddDict}>{t('新增')}</Button>
                        </HasPermission>
                    </Space>
                )}
                fetchData={getData}
                loading={getDictDataLoading}
                queryParam={queryParam}
                setQueryParam={setQueryParam}
            />
            <Modal
                title={t(dictOperation.title)}
                width={400}
                centered
                confirmLoading={createDictLoading || updateDictLoading}
                open={dictOperation.open}
                onOk={handleSaveDict}
                onCancel={handleCloseModal}
                onClose={handleCloseModal}
                maskClosable={false}
                keyboard={false}
                okText={t('保存')}
                cancelText={t('取消')}
                okButtonProps={{
                    disabled: getDictDetailsLoading
                }}
                afterClose={() => editForm.resetFields()}
            >
                <Loading spinning={getDictDetailsLoading}>
                    <Form
                        form={editForm}
                        labelCol={{ span: 6 }}
                        wrapperCol={{ span: 18 }}
                        layout="horizontal"
                    >
                        <Form.Item name="id" hidden>
                            <Input />
                        </Form.Item>
                        <Form.Item
                            label="字典名称"
                            name="name"
                            rules={[
                                {
                                    required: true,
                                    message: `字典名称不能为空`,
                                },
                            ]}
                        >
                            <Input placeholder="请输入字典名称" />
                        </Form.Item>
                        <Form.Item
                            label="字典编码"
                            name="code"
                            rules={[
                                {
                                    required: true,
                                    message: `字典编码不能为空`,
                                },
                            ]}
                        >
                            <Input placeholder="请输入字典编码" disabled={dictOperation.operationType == OperationMode.EDIT} />
                        </Form.Item>
                        <Form.Item
                            label="描述"
                            name="description"
                        >
                            <Input.TextArea placeholder="请输入字典描述" />
                        </Form.Item>
                        <Form.Item
                            label="启用状态"
                            name="enabled"
                            initialValue={true}
                        >
                            <Radio.Group
                                options={[
                                    { value: true, label: '启用' },
                                    { value: false, label: '停用' }
                                ]}
                            />
                        </Form.Item>
                        <Form.Item
                            label="排序:"
                            name="sort"
                        >
                            <InputNumber precision={0} style={{ width: '100%' }} />
                        </Form.Item>
                    </Form>
                </Loading>
            </Modal>
        </Flex>
    )
}

export default DictManage