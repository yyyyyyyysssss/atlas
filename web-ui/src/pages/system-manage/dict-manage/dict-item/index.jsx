import { useEffect, useState } from 'react'
import useFullParams from '../../../../hooks/useFullParams'
import './index.css'
import { Button, Flex, Form, Input, InputNumber, Modal, Popconfirm, Radio, Select, Space, Switch, Table, Typography } from 'antd'
import { useTranslation } from 'react-i18next';
import HasPermission from '../../../../components/HasPermission';
import SmartTable from '../../../../components/smart-table';
import { batchDictItemChildren, createDictItem, deleteDictItemById, fetchDictItemChildren, fetchDictItemDetails, fetchDictItemList, updateDictItem, updateDictItemEnabled } from '../../../../services/SystemService';
import { useRequest } from 'ahooks';
import Loading from '../../../../components/loading';
import ActionDropdown from '../../../../components/ActionDropdown';
import { getMessageApi } from '../../../../utils/MessageUtil';
import Highlight from '../../../../components/Highlight';
import { OperationMode } from '../../../../enums/common';
import SmartUpload from '../../../../components/smart-upload';


const DictItem = () => {

    const { t } = useTranslation()

    const { dictId } = useFullParams()

    const [searchForm] = Form.useForm()

    const initQueryParam = {
        pageNum: 1,
        pageSize: 10,
        enabled: null,
        keyword: null,
        dictId: dictId
    }

    const [queryParam, setQueryParam] = useState(initQueryParam)

    const [editForm] = Form.useForm()


    const [dictItemOperation, setDictItemOperation] = useState({
        open: false,
        title: null,
        operationType: null,
    })

    const [expandedRowData, setExpandedRowData] = useState({})

    const [expandedRowKeys, setExpandedRowKeys] = useState([])

    const [dictItemEnabledLoadingMap, setDictItemEnabledLoadingMap] = useState({})

    const { runAsync: getDictItemDataAsync, loading: getDictItemDataLoading } = useRequest(fetchDictItemList, {
        manual: true
    })

    const { runAsync: createDictItemAsync, loading: createDictItemLoading } = useRequest(createDictItem, {
        manual: true
    })

    const { runAsync: updateDictItemAsync, loading: updateDictItemLoading } = useRequest(updateDictItem, {
        manual: true
    })

    const { runAsync: updateDictItemEnabledAsync, loading: updateDictItemEnabledLoading } = useRequest(updateDictItemEnabled, {
        manual: true
    })

    const { runAsync: getDictItemChildrenAsync, loading: getDictItemChildrenLoading } = useRequest(fetchDictItemChildren, {
        manual: true
    })

    const { runAsync: batchDictItemChildrenAsync, loading: batchDictItemChildrenLoading } = useRequest(batchDictItemChildren, {
        manual: true
    })

    const { runAsync: deleteDictItemByIdAsync, loading: deleteDictItemByIdLoading } = useRequest(deleteDictItemById, {
        manual: true
    })

    const { runAsync: getDictItemDetailsAsync, loading: getDictItemDetailsLoading } = useRequest(fetchDictItemDetails, {
        manual: true
    })


    const getData = async (queryParam) => {
        return await getDictItemDataAsync(queryParam)
    }

    const handleAddDictItem = () => {
        setDictItemOperation({
            open: true,
            title: '新增字典项',
            operationType: OperationMode.ADD,
        })
        const dictDetail = { dictId: dictId }
        editForm.setFieldsValue(dictDetail)
    }

    const handleEditDictItem = async (dictItemId) => {
        setDictItemOperation({
            open: true,
            title: '修改字典项',
            operationType: OperationMode.EDIT,
        })
        const dictDetail = await getDictItemDetailsAsync(dictItemId)
        editForm.setFieldsValue(dictDetail)
    }

    const handleAddDictItemChildren = (record) => {
        setDictItemOperation({
            open: true,
            title: `新增 [${record.label}] 子项`,
            operationType: OperationMode.ADD,
        })
        const dictDetail = { dictId: record.dictId, parentId: record.id }
        editForm.setFieldsValue(dictDetail)
    }

    const handleSaveDictItem = async () => {
        const editFormData = await editForm.validateFields()
        if (dictItemOperation.operationType === OperationMode.ADD) {
            await createDictItemAsync(editFormData)
        } else {
            await updateDictItemAsync(editFormData)
        }
        getMessageApi().success(t('操作成功'))
        handleCloseModal()
        handleRefresh(false, expandedRowKeys)
    }

    const handleUpdateEnabled = async (id, parentId, checked) => {
        setDictItemEnabledLoadingMap(prev => ({ ...prev, [id]: true }))
        try {
            await updateDictItemEnabledAsync(id, checked)
            if (checked) {
                getMessageApi().success(t('启用成功'))
            } else {
                getMessageApi().success(t('停用成功'))
            }
        } finally {
            setDictItemEnabledLoadingMap(prev => ({ ...prev, [id]: false }))
            handleRefresh(false, expandedRowKeys)
        }

    }

    const handleDeleteDictItem = async (dictItemId) => {
        await deleteDictItemByIdAsync(dictItemId)
        getMessageApi().success('删除成功')
        handleRefresh(false, expandedRowKeys)
    }

    const handleCloseModal = () => {
        setDictItemOperation({
            open: false,
            title: null,
            operationType: null,
        })
    }

    const handleRefresh = async (resetExpanded = true, expandedRowKeys = []) => {
        const newQueryParam = { ...queryParam }
        setQueryParam(newQueryParam)
        if (resetExpanded) {
            setExpandedRowKeys([]) // 仅在需要重置时清空
            setExpandedRowData({}) // 仅在需要重置时清空
        } else if (expandedRowKeys.length > 0) {
            const dictItemList = await batchDictItemChildrenAsync(expandedRowKeys)
            const childrenMap = dictItemList.reduce((map, item) => {
                map[item.id] = item.children || []
                return map
            }, {})
            setExpandedRowData((prev) => ({
                ...prev,
                ...childrenMap, // 合并新的子项数据
            }))
        }

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
        setExpandedRowKeys([])
        setExpandedRowData({})
    }

    const columns = [
        {
            key: 'id',
            dataIndex: 'id',
            hidden: true
        },
        {
            key: 'parentId',
            dataIndex: 'parentId',
            hidden: true
        },
        {
            key: 'label',
            title: '字典项名称',
            dataIndex: 'label',
            align: 'center',
            fixed: 'left',
            visible: true
        },
        {
            key: 'value',
            title: '字典项值',
            dataIndex: 'value',
            align: 'center',
            visible: true
        },
        {
            key: 'alias',
            title: '别名',
            dataIndex: 'alias',
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
                const { id, parentId, enabled } = record
                const handleChange = (checked) => {
                    handleUpdateEnabled(id, parentId, checked)
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
                                    loading={!!dictItemEnabledLoadingMap[id]}
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
                                loading={!!dictItemEnabledLoadingMap[id]}
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
                            <Space>
                                <Typography.Link style={{ whiteSpace: 'nowrap' }} onClick={() => handleEditDictItem(record.id)}>
                                    {t('编辑')}
                                </Typography.Link>
                                <Typography.Link style={{ whiteSpace: 'nowrap' }} onClick={() => handleAddDictItemChildren(record)}>
                                    {t('新增子项')}
                                </Typography.Link>
                            </Space>
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
                                                    是否删除 <Highlight>{record.label}</Highlight> 字典项？删除后将无法恢复！
                                                </>
                                            ),
                                            onOk: async () => {
                                                await handleDeleteDictItem(record.id)
                                            },
                                            confirmLoading: deleteDictItemByIdLoading,
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

    const handleExpand = async (expanded, record) => {
        if (expanded && !expandedRowData[record.id]) {
            getDictItemChildren(record.id)
        }
        if (expanded) {
            // 如果展开，加入该行的 id 到 expandedRowKeys 中
            setExpandedRowKeys((prev) => [...prev, record.id])
        } else {
            // 如果收起，移除该行的 id
            setExpandedRowKeys((prev) => prev.filter((key) => key !== record.id))
        }
    }

    const getDictItemChildren = async (dictItemId) => {
        const subData = await getDictItemChildrenAsync(dictItemId)
        setExpandedRowData((prev) => ({
            ...prev,
            [dictItemId]: subData, // 将数据存入状态中
        }))
    }

    const expandedRowRender = (record) => {
        const subData = expandedRowData[record.id] // 获取当前行的子数据
        if (!subData) {
            return <Flex justify='center' align='center' style={{ width: '100%' }}><Loading /></Flex>
        }
        if (subData.length === 0) {
            return null
        }
        const dynamicColumns = [
            {
                title: '字典项名称',
                dataIndex: 'label',
                key: 'label',
                align: 'center',
                fixed: 'left',
                filters: [
                    ...new Set(subData.map(item => item.label))  // 动态提取唯一值作为筛选项
                ].map(value => ({
                    text: value,
                    value: value,
                })),
                filterSearch: true,
                onFilter: (value, record) => record.label.includes(value),
            },
            ...columns.filter(col => col.dataIndex !== 'label')
        ]
        return (
            <Table
                columns={dynamicColumns}
                dataSource={expandedRowData[record.id]}
                pagination={true}
                rowKey="id"
                expandable={{
                    expandedRowRender,
                    onExpand: handleExpand,
                    expandedRowKeys: expandedRowKeys
                }}
            />
        )
    }


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
                    <Button type="primary" onClick={handleSearch} loading={getDictItemDataLoading}>{t('查询')}</Button>
                    <Button onClick={handleReset} loading={getDictItemDataLoading}>{t('重置')}</Button>
                </Space>
            </Flex>
            <SmartTable
                className='w-full'
                columns={columns}
                headerExtra={(
                    <Space>
                        <HasPermission hasPermissions='system:dict:write'>
                            <Button type="primary" onClick={handleAddDictItem}>{t('新增')}</Button>
                        </HasPermission>
                    </Space>
                )}
                fetchData={getData}
                loading={getDictItemDataLoading}
                queryParam={queryParam}
                setQueryParam={setQueryParam}
                expandable={{
                    expandedRowRender,
                    onExpand: handleExpand,
                    expandedRowKeys: expandedRowKeys
                }}
            />
            <Modal
                title={t(dictItemOperation.title)}
                width={400}
                centered
                confirmLoading={createDictItemLoading || updateDictItemLoading}
                open={dictItemOperation.open}
                onOk={handleSaveDictItem}
                onCancel={handleCloseModal}
                onClose={handleCloseModal}
                maskClosable={false}
                keyboard={false}
                okText={t('保存')}
                cancelText={t('取消')}
                okButtonProps={{
                    disabled: getDictItemDetailsLoading
                }}
                afterClose={() => editForm.resetFields()}
            >
                <Loading spinning={getDictItemDetailsLoading}>
                    <Form
                        form={editForm}
                        labelCol={{ span: 6 }}
                        wrapperCol={{ span: 18 }}
                        layout="horizontal"
                    >
                        <Form.Item name="id" hidden>
                            <Input />
                        </Form.Item>
                        <Form.Item name="dictId" hidden>
                            <Input />
                        </Form.Item>
                        <Form.Item name="parentId" hidden>
                            <Input />
                        </Form.Item>
                        <Form.Item
                            label="名称"
                            name="label"
                            rules={[
                                {
                                    required: true,
                                    message: `字典项名称不能为空`,
                                },
                            ]}
                        >
                            <Input placeholder="请输入字典项名称" />
                        </Form.Item>
                        <Form.Item
                            label="值"
                            name="value"
                            rules={[
                                {
                                    required: true,
                                    message: `字典项值不能为空`,
                                },
                            ]}
                        >
                            <Input placeholder="请输入字典项值" disabled={dictItemOperation.operationType == OperationMode.EDIT} />
                        </Form.Item>
                        <Form.Item
                            label="别名"
                            name="alias"
                        >
                            <Input placeholder="请输入字典项别名" />
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
                            name="imgUrl"
                            label="图片"
                        >
                            <SmartUpload
                                accept=".svg,.png,.jpg,.jpeg"
                                maxCount={1}
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

export default DictItem