import { App, Button, Drawer, Flex, Form, Input, Modal, Popconfirm, Radio, Select, Skeleton, Space, Switch, theme, Tooltip, Typography } from 'antd'
import './index.css'
import { useNavigate } from 'react-router-dom';
import React, { useEffect, useState } from 'react';
import { bindRoleByUserId, createUser, deleteUserById, fetchOrgOptions, fetchUserDetails, fetchUserList, resetPassword, updateUser, updateUserEnabled } from '../../../services/SystemService';
import { CopyOutlined } from '@ant-design/icons';
import Highlight from '../../../components/Highlight';
import RoleSelect from '../../../components/RoleSelect';
import HasPermission from '../../../components/HasPermission';
import { useRequest } from 'ahooks';
import SmartTable from '../../../components/smart-table';
import ActionDropdown from '../../../components/ActionDropdown';
import { useTranslation } from 'react-i18next';
import { OperationMode } from '../../../enums/common';


const initQueryParam = {
    pageNum: 1,
    pageSize: 10,
    enabled: null,
    keyword: null
}

const UserManage = () => {

    const { t } = useTranslation()

    const { token } = theme.useToken()

    const { modal, message } = App.useApp()

    const navigate = useNavigate()

    const [editForm] = Form.useForm()

    const [bindRoleForm] = Form.useForm()

    const [searchForm] = Form.useForm()

    const [queryParam, setQueryParam] = useState(initQueryParam)

    const { runAsync: getUserDataAsync, loading: getUserDataLoading } = useRequest(fetchUserList, {
        manual: true
    })

    const { runAsync: createUserAsync, loading: createUserLoading } = useRequest(createUser, {
        manual: true
    })

    const { runAsync: updateUserAsync, loading: updateUserLoading } = useRequest(updateUser, {
        manual: true
    })

    const { runAsync: resetPasswordAsync, loading: resetPasswordLoading } = useRequest(resetPassword, {
        manual: true
    })

    const { runAsync: deleteUserByIdAsync, loading: deleteUserByIdLoading } = useRequest(deleteUserById, {
        manual: true
    })

    const { runAsync: bindRoleByUserIdAsync, loading: bindRoleByUserIdLoading } = useRequest(bindRoleByUserId, {
        manual: true
    })

    const { runAsync: getUserDetailsAsync, loading: getUserDetailsLoading } = useRequest(fetchUserDetails, {
        manual: true
    })

    const [userEnabledLoadingMap, setUserEnabledLoadingMap] = useState({})

    const [bindRole, setBindRole] = useState({
        open: false,
        title: null,
        userItem: null,
    })

    useEffect(() => {
        if (bindRole && bindRole.open === true) {
            const roleIds = bindRole?.userItem?.roleIds ?? []
            bindRoleForm.setFieldsValue({ ...bindRole.userItem, roleIds: roleIds })
        }
    }, [bindRole])

    const getData = async (queryParam) => {
        return await getUserDataAsync(queryParam)
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

    const handleAddUser = () => {
        navigate('/system/user/details', {
            state: {
                operationMode: OperationMode.ADD.value
            }
        })
    }

    const handleEditUser = (userId) => {
        navigate('/system/user/details', {
            state: {
                userId: userId,
                operationMode: OperationMode.EDIT.value
            }
        })
    }

    const handleSaveUser = () => {
        editForm.validateFields()
            .then(
                (values) => {
                    if (userOperation.operationType === 'ADD') {
                        createUserAsync(values)
                            .then(
                                (data) => {
                                    handleClose()
                                    handleRefresh()
                                    modal.success({
                                        title: t('创建成功'),
                                        okText: t('知道了'),
                                        content: (
                                            <div style={{ userSelect: 'text' }}>
                                                <p>
                                                    用户 <strong>{values.fullName}</strong> 的初始密码为：
                                                </p>
                                                <Flex justify='space-between' align='center' style={{ backgroundColor: token.controlItemBgHover, paddingLeft: '10px' }} className='py-2 px-3 rounded-md'>
                                                    <Typography.Text>{data.initialPassword}</Typography.Text>
                                                    <Button
                                                        type="text"
                                                        icon={<Tooltip title={t('复制')}><CopyOutlined /></Tooltip>}
                                                        onClick={() => {
                                                            navigator.clipboard.writeText(newPassword);
                                                            message.success(t('已复制到剪贴板'));
                                                        }}
                                                    />
                                                </Flex>
                                            </div>
                                        ),
                                    })
                                }
                            )
                    } else {
                        updateUserAsync(values)
                            .then(
                                () => {
                                    message.success(t('修改成功'))
                                    handleClose()
                                    handleRefresh()
                                }
                            )
                    }

                }
            )
    }

    const handleUpdateEnabled = async (id, enabled) => {
        setUserEnabledLoadingMap(prev => ({ ...prev, [id]: true }))
        try {
            await updateUserEnabled(id, enabled)
            if (enabled) {
                message.success(t('启用成功'))
            } else {
                message.success(t('停用成功'))
            }
            handleRefresh()
        } finally {
            setUserEnabledLoadingMap(prev => ({ ...prev, [id]: false }))
        }

    }

    const handleResetPassword = async (userItem) => {
        const newPassword = await resetPasswordAsync(userItem.id)
        modal.success({
            title: t('新密码已生成'),
            okText: t('知道了'),
            content: (
                <div style={{ userSelect: 'text' }}>
                    <p>
                        用户 <strong>{userItem.fullName}</strong> 的新密码为：
                    </p>
                    <Flex justify='space-between' align='center' style={{ backgroundColor: token.controlItemBgHover, paddingLeft: '10px' }} className='py-2 px-3 rounded-md'>
                        <Typography.Text>{newPassword}</Typography.Text>
                        <Button
                            type="text"
                            icon={<Tooltip title={t('复制')}><CopyOutlined /></Tooltip>}
                            onClick={() => {
                                navigator.clipboard.writeText(newPassword);
                                message.success(t('已复制到剪贴板'));
                            }}
                        />
                    </Flex>
                </div>
            ),
        })
    }

    const handleDelete = async (id) => {
        await deleteUserByIdAsync(id)
        message.success('删除成功')
        handleRefresh()

    }

    const handleBindRole = (userId) => {
        setBindRole({
            open: true,
            title: t(`分配角色`),
            userItem: null,
        })
        getUserDetailsAsync(userId)
            .then(
                (userData) => {
                    setBindRole(prev => {
                        if (prev.open) {
                            return {
                                ...prev,
                                title: t(`分配角色`) + `[${userData.fullName}]`,
                                userItem: userData
                            }
                        }
                        return prev
                    })
                }
            )
    }

    const handleBindRoleSave = () => {
        bindRoleForm.validateFields()
            .then(values => {
                bindRoleByUserIdAsync(values.id, values.roleIds)
                    .then(
                        () => {
                            message.success(t('操作成功'))
                            handleBindRoleClose()
                            handleRefresh()
                        }
                    )
            })
    }

    const handleBindRoleClose = () => {
        setBindRole({
            open: false,
            title: null,
            userItem: null,
        })
    }

    const columns = [
        {
            key: 'fullName',
            title: '用户名称',
            dataIndex: 'fullName',
            align: 'center',
            fixed: 'left',
            width: '140px',
            showSorterTooltip: { target: 'full-header' },
            visible: true,
            sorter: (a, b) => a.fullName.localeCompare(b.fullName),
        },
        {
            key: 'username',
            title: '用户账号',
            dataIndex: 'username',
            align: 'center',
            visible: true,
        },
        {
            key: 'phone',
            title: '用户手机号',
            dataIndex: 'phone',
            align: 'center',
            visible: true,
        },
        {
            key: 'email',
            title: '用户邮箱',
            dataIndex: 'email',
            align: 'center',
            visible: true,
        },
        {
            key: 'enabled',
            title: '状态',
            dataIndex: 'enabled',
            width: '100px',
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
                            hasPermissions='system:user:write'
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
                                    loading={!!userEnabledLoadingMap[id]}
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
                            hasPermissions='system:user:write'
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
                                loading={!!userEnabledLoadingMap[id]}
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
            key: 'operation',
            title: '操作',
            dataIndex: 'operation',
            align: 'center',
            fixed: 'right',
            visible: true,
            render: (_, record) => {
                return (
                    <span>
                        <HasPermission
                            hasPermissions='system:user:write'
                        >
                            <Typography.Link onClick={() => handleBindRole(record.id)} style={{ marginInlineEnd: 8 }}>
                                {t('分配角色')}
                            </Typography.Link>
                            <Typography.Link onClick={() => handleEditUser(record.id)} style={{ marginInlineEnd: 8 }}>
                                {t('编辑')}
                            </Typography.Link>
                            <Typography.Link
                                style={{ marginInlineEnd: 8 }}
                                onClick={() => {
                                    modal.confirm({
                                        title: t('确定重置'),
                                        okText: t('确定'),
                                        cancelText: t('取消'),
                                        maskClosable: false,
                                        confirmLoading: resetPasswordLoading,
                                        content: (
                                            <>
                                                是否重置 <Highlight>{record.fullName}</Highlight> 的密码？
                                            </>
                                        ),
                                        onOk: async () => {
                                            await handleResetPassword(record)
                                        },
                                    })
                                }}
                            >
                                {t('重置密码')}
                            </Typography.Link>
                        </HasPermission>
                        <HasPermission
                            hasPermissions='system:user:delete'
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
                                                    是否删除 <Highlight>{record.fullName}</Highlight> 的账号？删除后将无法恢复！
                                                </>
                                            ),
                                            onOk: async () => {
                                                await handleDelete(record.id)
                                            },
                                            confirmLoading: deleteUserByIdLoading,
                                        }
                                    }
                                ]}
                            />
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
                    <Form.Item name="keyword" label="用户信息" style={{ width: 365 }}>
                        <Input placeholder="请输入用户名称、账号、手机号、邮箱" allowClear />
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
                    <Button className="min-w-[88px]" type="primary" onClick={handleSearch} loading={getUserDataLoading}>{t('查询')}</Button>
                    <Button className="min-w-[88px]" onClick={handleReset} loading={getUserDataLoading}>{t('重置')}</Button>
                </Space>
            </Flex>
            <SmartTable
                className='w-full'
                columns={columns}
                headerExtra={(
                    <Space>
                        <HasPermission hasPermissions='system:user:write'>
                            <Button type="primary" onClick={handleAddUser}>{t('新增')}</Button>
                        </HasPermission>
                    </Space>
                )}
                fetchData={getData}
                loading={getUserDataLoading}
                queryParam={queryParam}
                setQueryParam={setQueryParam}
            />
            <Drawer
                title={bindRole.title}
                closable={{ 'aria-label': 'Close Button' }}
                onClose={handleBindRoleClose}
                open={bindRole.open}
                width={400}
                footer={
                    <Space>
                        <Button loading={bindRoleByUserIdLoading} type="primary" onClick={handleBindRoleSave}>{t('保存')}</Button>
                        <Button onClick={handleBindRoleClose}>{t('取消')}</Button>
                    </Space>
                }
                afterClose={() => bindRoleForm.resetFields()}
                destroyOnHidden
            >
                <Form
                    form={bindRoleForm}
                >
                    <Skeleton loading={getUserDetailsLoading} active>
                        <Form.Item name="id" hidden>
                            <Input />
                        </Form.Item>
                        <Form.Item
                            name="roleIds"
                        >
                            <RoleSelect type='checkbox' />
                        </Form.Item>
                    </Skeleton>
                </Form>
            </Drawer>
        </Flex>
    )
}

export default UserManage