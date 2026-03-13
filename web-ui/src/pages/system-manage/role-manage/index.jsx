import { useEffect, useState } from 'react'
import './index.css'
import { bindAuthorityByRoleId, bindRoleUser, createRole, deleteRoleById, fetchAuthorityTree, fetchRoleDetails, fetchRoleList, fetchSearchUser, fetchUserIdByRoleId, updateRole, updateRoleEnabled } from '../../../services/SystemService'
import { Button, Drawer, Flex, Form, Input, Modal, Popconfirm, Radio, Select, Skeleton, Space, Switch, Table, Typography } from 'antd'
import AuthorityTreeSelect from '../../../components/AuthorityTreeSelect'
import AuthorityTree from '../../../components/AuthorityTree'
import Highlight from '../../../components/Highlight'
import HasPermission from '../../../components/HasPermission'
import { getMessageApi } from '../../../utils/MessageUtil'
import { useRequest } from 'ahooks'
import SmartTable from '../../../components/smart-table'
import RemoteSearchSelect from '../../../components/RemoteSearchSelect'
import Loading from '../../../components/loading'
import { useTranslation } from 'react-i18next'
import UserTransfer from '../../../components/UserTransfer'
import OptionTreeSelect from '../../../components/OptionTreeSelect'

const initQueryParam = {
    pageNum: 1,
    pageSize: 10,
    keyword: null,
    enabled: null
}

const RoleManage = () => {

    const { t } = useTranslation()

    const [modal, contextHolder] = Modal.useModal()

    const [searchForm] = Form.useForm()

    const [editForm] = Form.useForm()

    const [bindUserForm] = Form.useForm()


    const [bindAuthorityForm] = Form.useForm()

    const [queryParam, setQueryParam] = useState(initQueryParam)

    const { runAsync: getRoleDataAsync, loading: getRoleDataLoading } = useRequest(fetchRoleList, {
        manual: true
    })

    const { runAsync: createRoleAsync, loading: createRoleLoading } = useRequest(createRole, {
        manual: true
    })

    const { runAsync: updateRoleAsync, loading: updateRoleLoading } = useRequest(updateRole, {
        manual: true
    })

    const { runAsync: deleteRoleByIdAsync, loading: deleteRoleByIdLoading } = useRequest(deleteRoleById, {
        manual: true
    })

    const { runAsync: bindAuthorityByRoleIdAsync, loading: bindAuthorityByRoleIdLoading } = useRequest(bindAuthorityByRoleId, {
        manual: true
    })

    const { runAsync: getUserIdByRoleIdAsync, loading: getUserIdByRoleIdLoading } = useRequest(fetchUserIdByRoleId, {
        manual: true
    })

    const { runAsync: bindRoleUserAsync, loading: bindRoleUserLoading } = useRequest(bindRoleUser, {
        manual: true
    })

    const { runAsync: getRoleDetailsAsync, loading: getRoleDetailsLoading } = useRequest(fetchRoleDetails, {
        manual: true
    })

    const [roleEnabledLoadingMap, setRoleEnabledLoadingMap] = useState({})

    const [roleOperation, setRoleOperation] = useState({
        open: false,
        title: null,
        operationType: null,
        roleItem: null,
    })

    const [bindAuthority, setBindAuthority] = useState({
        open: false,
        title: null,
        roleItem: null,
    })

    const [bindUser, setBindUser] = useState({
        open: false,
        title: null,
        roleId: null,
    })

    useEffect(() => {
        if (roleOperation && roleOperation.open === true && roleOperation.operationType === 'EDIT') {
            editForm.setFieldsValue(roleOperation.roleItem)
        }
    }, [roleOperation])

    useEffect(() => {
        if (bindAuthority && bindAuthority.open === true) {
            bindAuthorityForm.setFieldsValue(bindAuthority.roleItem)
        }
    }, [bindAuthority])


    const getData = async (queryParam) => {
        return await getRoleDataAsync(queryParam)
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

    const handleAddRole = () => {
        setRoleOperation({
            open: true,
            title: '新增角色',
            operationType: 'ADD',
            roleItem: null,
        })
    }

    const handleEditRole = (roleId) => {
        setRoleOperation({
            open: true,
            title: '编辑角色',
            operationType: 'EDIT',
            roleItem: null,
        })
        getRoleDetailsAsync(roleId)
            .then(
                (roleData) => {
                    setRoleOperation(prev => {
                        if (prev.open) {
                            return {
                                ...prev,
                                roleItem: roleData
                            }
                        }
                        return prev
                    })
                }
            )

    }

    const handleClose = () => {
        setRoleOperation({
            open: false,
            title: null,
            operationType: null,
            roleItem: null,
        })
        editForm.resetFields()
    }

    const handleSaveRole = () => {
        editForm.validateFields()
            .then(
                (values) => {
                    if (roleOperation.operationType === 'ADD') {
                        createRoleAsync(values)
                            .then(
                                () => {
                                    getMessageApi().success(t('新增成功'))
                                    handleClose()
                                    handleRefresh()
                                }
                            )
                    } else {
                        updateRoleAsync(values)
                            .then(
                                () => {
                                    getMessageApi().success(t('修改成功'))
                                    handleClose()
                                    handleRefresh()
                                }
                            )
                    }

                }
            )
    }

    const handleUpdateEnabled = async (id, enabled) => {
        setRoleEnabledLoadingMap(prev => ({ ...prev, [id]: true }))
        try {
            await updateRoleEnabled(id, enabled)
            if (enabled) {
                getMessageApi().success(t('启用成功'))
            } else {
                getMessageApi().success(t('停用成功'))
            }
            handleRefresh()
        } finally {
            setRoleEnabledLoadingMap(prev => ({ ...prev, [id]: false }))
        }
    }

    const handleDelete = async (id) => {
        deleteRoleByIdAsync(id)
            .then(
                () => {
                    getMessageApi().success(t('删除成功'))
                    handleRefresh()
                }
            )
    }

    const handleBindAuthority = (roleId) => {
        setBindAuthority({
            open: true,
            title: `分配权限`,
            roleItem: null,
        })
        getRoleDetailsAsync(roleId)
            .then(
                (roleData) => {
                    setBindAuthority(prev => {
                        if (prev.open) {
                            return {
                                ...prev,
                                title: `分配权限[${roleData.name}]`,
                                roleItem: roleData,
                            }
                        }
                        return prev
                    })
                }
            )
    }

    const handleBindAuthoritySave = () => {
        bindAuthorityForm.validateFields()
            .then(values => {
                bindAuthorityByRoleIdAsync(values.id, values.authorityIds)
                    .then(
                        () => {
                            getMessageApi().success('分配权限成功')
                            handleBindAuthorityClose()
                            handleRefresh()
                        }
                    )
            })
    }

    const handleBindAuthorityClose = () => {
        setBindAuthority({
            open: false,
            title: null,
            roleItem: null,
        })
    }

    const handleBindUser = async (role) => {
        const roleId = role.id
        const roleName = role.name
        setBindUser({
            open: true,
            title: t('分配用户') + `[${roleName}]`,
            roleId: roleId
        })
        const userIds = await getUserIdByRoleIdAsync(roleId)
        setBindUser(prev => {
            if (prev.open) {
                return {
                    ...prev,
                    userIds: userIds
                }
            }
            return prev
        })
        bindUserForm.setFieldValue('userIds', userIds)
    }

    const handleBindUserSave = async () => {
        const { userIds } = await bindUserForm.validateFields()
        await bindRoleUserAsync(bindUser.roleId, userIds)
        getMessageApi().success(t('操作成功'))
        handleBindUserClose()
    }

    const handleBindUserClose = () => {
        setBindUser({
            open: false,
            title: null,
            roleId: null,
        })
    }

    const columns = [
        {
            key: 'name',
            title: '角色名称',
            dataIndex: 'name',
            align: 'center',
            fixed: 'left',
            width: '140px',
            showSorterTooltip: { target: 'full-header' },
            sorter: (a, b) => a.name.localeCompare(b.name),
        },
        {
            key: 'code',
            title: '角色编码',
            dataIndex: 'code',
            align: 'center',
        },
        {
            key: 'enabled',
            title: '状态',
            dataIndex: 'enabled',
            width: '100px',
            align: 'center',
            render: (_, record) => {
                const { id, enabled } = record
                const handleChange = (checked) => {
                    if (checked) {
                        handleUpdateEnabled(id, true)
                    }
                }
                return enabled ?
                    (
                        <HasPermission
                            hasPermissions='system:role:write'
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
                                onConfirm={() => handleUpdateEnabled(record.id, false)}
                                style={{ marginInlineEnd: 8 }}
                            >
                                <Switch
                                    loading={!!roleEnabledLoadingMap[id]}
                                    checkedChildren={t('启用')}
                                    unCheckedChildren={t('停用')}
                                    checked={enabled}
                                    onChange={handleChange}
                                />
                            </Popconfirm>
                        </HasPermission>
                    )
                    :
                    (
                        <HasPermission
                            hasPermissions='system:role:write'
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
                                loading={!!roleEnabledLoadingMap[id]}
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
                    <span>
                        <HasPermission hasPermissions='system:role:write'>
                            <Typography.Link onClick={() => handleBindUser(record)} style={{ marginInlineEnd: 8 }}>
                                {t('分配用户')}
                            </Typography.Link>
                            <Typography.Link onClick={() => handleBindAuthority(record.id)} style={{ marginInlineEnd: 8 }}>
                                {t('分配权限')}
                            </Typography.Link>
                            <Typography.Link onClick={() => handleEditRole(record.id)} style={{ marginInlineEnd: 8 }}>
                                {t('编辑')}
                            </Typography.Link>
                        </HasPermission>
                        <HasPermission hasPermissions='system:role:delete'>
                            <Typography.Link
                                style={{ marginInlineEnd: 8 }}
                                onClick={() => {
                                    modal.confirm({
                                        title: t('确定删除'),
                                        okText: t('确定'),
                                        cancelText: t('取消'),
                                        maskClosable: false,
                                        confirmLoading: deleteRoleByIdLoading,
                                        content: (
                                            <>
                                                是否删除 <Highlight>{record.name}</Highlight> 角色？删除后将无法恢复！
                                            </>
                                        ),
                                        onOk: async () => {
                                            await handleDelete(record.id)
                                        },
                                    })
                                }}
                            >
                                {t('删除')}
                            </Typography.Link>
                        </HasPermission>
                    </span>
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
                    <Form.Item name="keyword" label="角色信息" style={{ width: 350 }}>
                        <Input placeholder="请输入角色名称或编码" allowClear />
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
                    <Button style={{ width: '80px' }} type="primary" onClick={handleSearch} loading={getRoleDataLoading}>{t('查询')}</Button>
                    <Button style={{ width: '80px' }} onClick={handleReset} loading={getRoleDataLoading}>{t('重置')}</Button>
                </Space>
            </Flex>
            <SmartTable
                className='w-full'
                columns={columns}
                headerExtra={
                    <Space>
                        <HasPermission hasPermissions='system:role:write'>
                            <Button type="primary" onClick={handleAddRole}>{t('新增')}</Button>
                        </HasPermission>
                    </Space>
                }
                fetchData={getData}
                loading={getRoleDataLoading}
                queryParam={queryParam}
                setQueryParam={setQueryParam}
            />
            <Modal
                title={t(roleOperation.title)}
                width={400}
                centered
                open={roleOperation.open}
                confirmLoading={createRoleLoading || updateRoleLoading}
                onOk={handleSaveRole}
                onCancel={handleClose}
                onClose={handleClose}
                maskClosable={false}
                keyboard={false}
                okText={t('保存')}
                okButtonProps={{
                    disabled: getRoleDetailsLoading
                }}
                cancelText={t('取消')}
            >
                <Form
                    form={editForm}
                    labelCol={{ span: 6 }}
                    wrapperCol={{ span: 18 }}
                    layout="horizontal"
                >
                    <Loading spinning={getRoleDetailsLoading}>
                        <div
                            className='w-full mt-5'
                        >
                            <Form.Item name="id" hidden>
                                <Input />
                            </Form.Item>
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
                                <Input disabled={roleOperation.operationType == 'EDIT'} />
                            </Form.Item>
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
                            <Form.Item
                                label="分配权限"
                                name="authorityIds"
                            >
                                {/* <AuthorityTreeSelect /> */}
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
                        </div>
                    </Loading>
                </Form>

            </Modal>
            <Modal
                title={bindUser.title}
                width={600}
                centered
                open={bindUser.open}
                onOk={handleBindUserSave}
                onCancel={handleBindUserClose}
                onClose={handleBindUserClose}
                confirmLoading={bindRoleUserLoading}
                maskClosable={false}
                destroyOnHidden
                okText={t('保存')}
                cancelText={t('取消')}
                afterClose={() => bindUserForm.resetFields()}
                okButtonProps={{
                    disabled: getUserIdByRoleIdLoading
                }}
            >

                <Form
                    form={bindUserForm}
                >
                    <Loading spinning={getUserIdByRoleIdLoading}>
                        <Form.Item name="userIds">
                            <UserTransfer />
                        </Form.Item>
                    </Loading>
                </Form>
            </Modal>
            <Drawer
                title={bindAuthority.title}
                closable={{ 'aria-label': 'Close Button' }}
                onClose={handleBindAuthorityClose}
                open={bindAuthority.open}
                width={400}
                footer={
                    <Space>
                        <Button loading={bindAuthorityByRoleIdLoading} type="primary" onClick={handleBindAuthoritySave}>{t('保存')}</Button>
                        <Button onClick={handleBindAuthorityClose}>{t('取消')}</Button>
                    </Space>
                }
                destroyOnHidden
            >
                <Form
                    form={bindAuthorityForm}
                >
                    <Skeleton loading={getRoleDetailsLoading} active>
                        <Form.Item name="id" hidden>
                            <Input />
                        </Form.Item>
                        <Form.Item
                            name="authorityIds"
                        >
                            <AuthorityTree />
                        </Form.Item>
                    </Skeleton>
                </Form>
            </Drawer>
            {contextHolder}
        </Flex>
    )
}

export default RoleManage