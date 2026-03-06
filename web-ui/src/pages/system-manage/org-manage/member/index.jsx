import { useRequest } from "ahooks"
import { Badge, Button, Checkbox, Flex, Form, Input, Modal, Select, Space, Switch, Table, Tag, Typography } from "antd"
import { useEffect, useRef, useState } from "react"
import { addOrgMembers, fetchOrgMembers, orgMemberMainCheck, removeOrgMembers } from "../../../../services/SystemService"
import HasPermission from "../../../../components/HasPermission"
import { useTranslation } from 'react-i18next';
import UserTransfer from "../../../../components/UserTransfer"
import { getMessageApi } from "../../../../utils/MessageUtil"
import { OrganizationType } from "../../../../enums/system"
import EditableTable from "../../../../components/smart-table/EditableTable"
import {
    ArrowDownOutlined
} from '@ant-design/icons';

const OrgMember = ({ orgId, parentOrgName, orgType }) => {

    const { t } = useTranslation()


    const [modal, contextHolder] = Modal.useModal()

    const [addOrgMemberForm] = Form.useForm()

    const [orgMemberForm] = Form.useForm()

    const [orgMember, setOrgMember] = useState([])

    const orgMemberIdsRef = useRef([])

    const [orgMemberModal, setOrgMemberModal] = useState({
        open: false,
        title: null,
        orgId: null
    })

    const [selectedRowKeys, setSelectedRowKeys] = useState([])

    const [includeChild, setIncludeChild] = useState(false)

    const [availableUsers, setAvailableUsers] = useState(null)

    const { runAsync: fetchOrgMemberAsync, loading: fetchOrgMemberLoading } = useRequest(fetchOrgMembers, {
        manual: true
    })

    const { runAsync: addOrgMemberAsync, loading: addOrgMemberLoading } = useRequest(addOrgMembers, {
        manual: true
    })

    const { runAsync: removeOrgMemberAsync, loading: removeOrgMemberLoading } = useRequest(removeOrgMembers, {
        manual: true
    })

    const { runAsync: orgMemberMainCheckAsync, loading: orgMemberMainCheckLoading } = useRequest(orgMemberMainCheck, {
        manual: true
    })

    const fetchOrgMemberData = async (orgId, includeChild = false) => {
        if (!orgId) {
            return
        }
        let mode
        if (includeChild) {
            mode = 'CHILDREN'
        } else {
            mode = 'CURRENT'
        }
        const data = await fetchOrgMemberAsync(orgId, mode)
        orgMemberForm.setFieldsValue({
            orgMembers: data
        })
        setOrgMember(data)
    }

    useEffect(() => {
        fetchOrgMemberData(orgId)
    }, [orgId])

    useEffect(() => {
        if (orgMember) {
            orgMemberIdsRef.current = orgMember.map(m => m.userId)
        }
    }, [orgMember])

    const refreshOrgMember = () => {
        fetchOrgMemberData(orgId, includeChild)
        setSelectedRowKeys([])
    }

    const handleAddMember = async () => {
        setOrgMemberModal({
            open: true,
            title: parentOrgName,
            orgId: orgId,
        })
        // 获取整个部门的
        if (orgType === OrganizationType.TEAM.value || orgType === OrganizationType.DEPT.value) {
            const allDeptMembers = await fetchOrgMemberAsync(orgId, 'PARENT')
            const allUserIds = allDeptMembers.map(m => m.userId)
            const diffUserIds = allUserIds.filter(id => !orgMemberIdsRef.current.includes(id))
            setAvailableUsers(diffUserIds)
        } else {
            setAvailableUsers(null)
        }

        addOrgMemberForm.setFieldValue('userIds', orgMemberIdsRef.current)
    }

    const handleRemoveMember = async () => {
        if (selectedRowKeys && selectedRowKeys.length > 0) {
            const selectedUserNames = orgMember
                .filter(item => selectedRowKeys.includes(item.id))
                .map(item => item.userFullName);
            modal.confirm({
                title: t('确定移除'),
                okText: t('确定'),
                cancelText: t('取消'),
                okButtonProps: { danger: true },
                maskClosable: false,
                confirmLoading: removeOrgMemberLoading,
                content: (
                    <Flex
                        gap={10}
                        style={{
                            maxHeight: '200px',
                            overflowY: 'auto',
                            padding: '8px',
                        }}
                        vertical
                    >
                        <Typography.Text>{t('您确定要将以下成员从该部门移除吗？')}</Typography.Text>
                        <Flex
                            wrap="wrap"
                        >
                            {selectedUserNames.map(name => (
                                <Tag color="error" key={name} style={{ marginBottom: 4 }}>
                                    {name}
                                </Tag>
                            ))}
                        </Flex>
                        {selectedUserNames.length > 5 && (
                            <Flex justify="flex-end">
                                <Space size={4}>
                                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                                        {t('共计')}
                                    </Typography.Text>
                                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                                        {selectedUserNames.length}
                                    </Typography.Text>
                                    <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                                        {t('人')}
                                    </Typography.Text>
                                </Space>
                            </Flex>
                        )}
                    </Flex>
                ),
                onOk: async () => {
                    await removeOrgMemberAsync(orgId, selectedRowKeys)
                    getMessageApi().success(t('操作成功'))
                    refreshOrgMember()
                },
            })

        }
    }


    const saveOrgMember = async () => {
        const { userIds } = await addOrgMemberForm.validateFields()
        const req = userIds.map(m => ({
            userId: m,
            orgId: orgId
        }))
        await addOrgMemberAsync(orgId, req)
        getMessageApi().success(t('操作成功'))
        closeOrgMember()
        refreshOrgMember()
    }

    const closeOrgMember = () => {
        setOrgMemberModal({
            open: false,
            title: null,
            orgId: null,
        })
    }

    const handleSetMain = async (record, checked) => {
        const org = await orgMemberMainCheckAsync(orgId, record.userId)
        if ((org === null || org === undefined) && checked === false) {
            getMessageApi().error('该用户必须保留至少一个主部门归属，无法取消。')
            return
        } else {
            modal.confirm({
                title: t('确定设置主部门归属'),
                okText: t('确定'),
                cancelText: t('取消'),
                okButtonProps: { danger: true },
                maskClosable: false,
                confirmLoading: addOrgMemberLoading,
                content: (
                    <Flex
                        gap={12}
                        style={{
                            maxHeight: '250px',
                            overflowY: 'auto',
                            padding: '8px 4px',
                        }}
                        vertical
                    >
                        <Typography.Text>
                            {t('系统检测到该用户已有关联的主部门，执行此操作将变更其行政归属：')}
                        </Typography.Text>

                        <Flex align="center" justify="space-between" vertical>

                            <Typography.Text delete type="secondary">{org.orgPathName}</Typography.Text>

                            {/* 箭头指示 */}
                            <ArrowDownOutlined style={{ color: '#ff4d4f', fontSize: '18px' }} />

                            <Typography.Text strong>{record.orgPathName}</Typography.Text>
                        </Flex>

                        <Typography.Text type="secondary" style={{ fontSize: '12px' }}>
                            {t('提示：变更主部门可能会影响该用户的权限计算及汇报关系。')}
                        </Typography.Text>
                    </Flex>
                ),
                onOk: async () => {
                    const req = [{
                        userId: record.userId,
                        orgId: orgId,
                        isMain: checked
                    }]
                    await addOrgMemberAsync(orgId, req)
                    getMessageApi().success(t('操作成功'))
                    refreshOrgMember()
                },
            })
        }

    }

    const handleIncludeChildChange = (e) => {
        const checked = e.target.checked
        setIncludeChild(checked)
        fetchOrgMemberData(orgId, checked)
    }

    const columns = [
        {
            key: 'id',
            dataIndex: 'id',
            hidden: true
        },
        {
            key: 'userId',
            dataIndex: 'userId',
            hidden: true
        },
        {
            key: 'orgId',
            dataIndex: 'orgId',
            hidden: true
        },
        {
            key: 'posId',
            dataIndex: 'posId',
            hidden: true
        },
        {
            key: 'userFullName',
            title: '用户名称',
            dataIndex: 'userFullName',
            align: 'center',
            editable: false,
        },
        {
            key: 'orgName',
            title: '所属组织',
            dataIndex: 'orgName',
            align: 'center',
            editable: false,
            render: (text, record) => {
                const isDept = record.orgType === OrganizationType.DEPT.value
                return (
                    <Flex vertical align="center" gap={4}>
                        <Typography.Text style={{ fontSize: '14px' }}>
                            {text}
                        </Typography.Text>
                        {/* 使用 Tag 区分层级类型 */}
                        <Tag color={isDept ? 'blue' : 'cyan'} style={{ fontSize: '11px', lineHeight: '16px', margin: 0 }}>
                            {isDept ? '部门' : '团队'}
                        </Tag>
                    </Flex>
                )
            }
        },
        {
            key: 'posName',
            title: '岗位',
            dataIndex: 'posName',
            align: 'center',
            editable: true,
            required: true,
            editRender: ({ value, onChange }) => {
                return <Input value={value} onChange={onChange} />
            },
        },
        {
            key: 'isMain',
            title: '主部门',
            dataIndex: 'isMain',
            align: 'center',
            editable: false,
            render: (_, record) => {
                const { isMain } = record
                return (
                    <Switch
                        size="small"
                        checked={isMain}
                        onChange={(checked) => handleSetMain(record, checked)}
                    />
                )
            }
        },
    ]

    const updateOrgMember = async (_, rowIndex) => {
        const formValues = await orgMemberForm.validateFields()
        const om = formValues.orgMembers[rowIndex]

    }

    const deleteOrgMember = async (_, rowIndex) => {
        const formValues = await orgMemberForm.validateFields()
        const om = formValues.orgMembers[rowIndex]
        await removeOrgMemberAsync(orgId, [om.id])
        getMessageApi().success(t('操作成功'))
        refreshOrgMember()
    }

    return (
        <Flex
            gap={16}
            vertical
        >
            <Flex
                gap={20}
                align="center"
            >
                <HasPermission hasPermissions='system:org:write'>
                    <Button type="primary" onClick={() => handleAddMember()} className='w-20'>{t('添加成员')}</Button>
                </HasPermission>
                <HasPermission hasPermissions='system:org:write'>
                    <Button
                        danger
                        onClick={() => handleRemoveMember()}
                        className='w-20'
                        disabled={selectedRowKeys.length === 0}
                    >
                        {t('移除成员')}
                    </Button>
                </HasPermission>
                <Checkbox
                    checked={includeChild}
                    onChange={handleIncludeChildChange}
                >
                    {t('包含下级成员')}
                </Checkbox>
            </Flex>
            <Form form={orgMemberForm} component={false}>
                <Form.List
                    name="orgMembers"
                    noStyle
                >
                    {(fields, { add, remove }) => (
                        <EditableTable
                            columns={columns}
                            name='orgMembers'
                            mode='single-edit'
                            loading={fetchOrgMemberLoading || addOrgMemberLoading || removeOrgMemberLoading || orgMemberMainCheckLoading}
                            fields={fields}
                            editPermission={'system:org:write'}
                            addPermission={'no-show'}
                            deletePermission={'system:org:write'}
                            add={add}
                            remove={remove}
                            onSave={updateOrgMember}
                            onDelete={deleteOrgMember}
                            rowSelection={{
                                selectedRowKeys,
                                onChange: (keys) => setSelectedRowKeys(keys)
                            }}
                        />
                    )}
                </Form.List>
            </Form>
            {/* <Table
                style={{ width: '100%' }}
                columns={columns}
                loading={fetchOrgMemberLoading || addOrgMemberLoading || removeOrgMemberLoading}
                dataSource={orgMember}
                rowKey={(record) => record.id}
                rowSelection={{
                    selectedRowKeys,
                    onChange: (keys) => setSelectedRowKeys(keys)
                }}
                pagination={false}
            /> */}
            <Modal
                title={`添加成员[${orgMemberModal.title}]`}
                width={600}
                centered
                open={orgMemberModal.open}
                onOk={saveOrgMember}
                onCancel={closeOrgMember}
                onClose={closeOrgMember}
                confirmLoading={addOrgMemberLoading}
                maskClosable={false}
                destroyOnHidden
                okText={t('保存')}
                cancelText={t('取消')}
                afterClose={() => addOrgMemberForm.resetFields()}
            >
                <Form
                    form={addOrgMemberForm}
                >
                    <Form.Item name="userIds">
                        <UserTransfer
                            loading={fetchOrgMemberLoading}
                            disabledKeys={availableUsers}
                            customRender={(item) => {

                                return {
                                    key: item.value,
                                    label: (
                                        <Flex justify="space-between" style={{ width: '100%' }}>
                                            <Typography.Text>{item.label}</Typography.Text>
                                            {item.disabled && (
                                                <Typography.Text type="secondary" style={{ fontSize: 12 }}>
                                                    (已在部门)
                                                </Typography.Text>
                                            )}
                                        </Flex>
                                    ),
                                    value: item.value,
                                    disabled: item.disabled
                                }
                            }}
                        />
                    </Form.Item>
                </Form>
            </Modal>
            {contextHolder}
        </Flex>
    )
}

export default OrgMember