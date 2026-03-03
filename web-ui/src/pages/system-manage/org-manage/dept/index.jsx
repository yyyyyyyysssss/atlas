
import { Space, Flex, Form, Input, Button, Row, Col, InputNumber, Table, Modal, Tag, Typography, Drawer } from 'antd'
import { OperationMode } from '../../../../enums/common';
import { useEffect, useState } from 'react';
import { createOrg, fetchOrgSubUnits, updateOrg } from '../../../../services/SystemService';
import HasPermission from '../../../../components/HasPermission';
import { getMessageApi } from '../../../../utils/MessageUtil';
import { useRequest } from 'ahooks';
import { useTranslation } from 'react-i18next';
import OptionSelect from '../../../../components/OptionSelect';
import { OrganizationStatus, OrganizationType } from '../../../../enums/system';
import OrgDeptTeam from '../team';
import OrgMember from '../member';

const OrgDept = ({ orgId }) => {

    const { t } = useTranslation()

    const [deptForm] = Form.useForm()

    const [orgDept, setOrgDept] = useState([])

    const [deptModal, setDeptModal] = useState({
        open: false,
        operationMode: null,
        title: ''
    })

    const [deptTeamDrawer, setDeptTeamDrawer] = useState({
        open: false,
        deptId: null,
        title: ''
    })

    const [deptUserDrawer, setDeptUserDrawer] = useState({
        open: false,
        deptId: null,
        title: ''
    })

    const { runAsync: fetchOrgDeptAsync, loading: fetchOrgDeptLoading } = useRequest(fetchOrgSubUnits, {
        manual: true
    })

    const { runAsync: createOrgDeptAsync, loading: createOrgDeptLoading } = useRequest(createOrg, {
        manual: true
    })

    const { runAsync: updateOrgDeptAsync, loading: updateOrgDeptLoading } = useRequest(updateOrg, {
        manual: true
    })

    const fetchDept = async (orgId) => {
        if (!orgId) {
            return
        }
        const deptList = await fetchOrgDeptAsync(orgId, OrganizationType.DEPT.value)
        setOrgDept(deptList)
    }

    useEffect(() => {
        fetchDept(orgId)
    }, [orgId])

    const handleAddDept = () => {
        deptForm.resetFields()
        deptForm.setFieldValue('parentId', orgId)
        setDeptModal({
            open: true,
            operationMode: OperationMode.ADD.value,
            title: '新增部门'
        })
    }

    const handleEditDept = (dept) => {
        deptForm.resetFields()
        deptForm.setFieldsValue({ ...dept })
        setDeptModal({
            open: true,
            operationMode: OperationMode.EDIT.value,
            title: '修改部门'
        })
    }

    const saveDept = async () => {
        const formValues = await deptForm.validateFields()
        const req = {
            ...formValues,
            orgType: OrganizationType.DEPT.value
        }
        if (deptModal.operationMode === OperationMode.ADD.value) {
            await createOrgDeptAsync(req)
            getMessageApi().success(t('新增成功'))
        } else {
            await updateOrgDeptAsync(req)
            getMessageApi().success(t('修改成功'))
        }
        closeDeptModal()
        fetchDept(orgId)
    }

    const closeDeptModal = () => {
        setDeptModal({
            open: false,
            operationMode: null,
            title: ''
        })
    }

    const deptColumns = [
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
            key: 'orgName',
            title: '部门名称',
            dataIndex: 'orgName',
            align: 'center'
        },
        {
            key: 'orgCode',
            title: '部门编码',
            dataIndex: 'orgCode',
            align: 'center'
        },
        {
            key: 'status',
            title: '状态',
            dataIndex: 'status',
            align: 'center',
            render: (_, { status }) => {
                const config = OrganizationStatus[status]
                return (
                    <Tag color={config?.color || 'default'}>
                        {config?.label || status}
                    </Tag>
                )
            }
        },
        {
            key: 'sort',
            title: '排序',
            dataIndex: 'sort',
            align: 'center'
        },
        {
            key: 'action',
            title: '操作',
            dataIndex: 'action',
            align: 'center',
            render: (_, record) => {
                return (
                    <Space size='middle'>
                        <HasPermission hasPermissions='system:org:write'>
                            <Typography.Link onClick={() => handleEditDept(record)}>{t('编辑')}</Typography.Link>
                        </HasPermission>
                        <Typography.Link style={{ whiteSpace: 'nowrap' }} onClick={() => openDeptTeamDrawer(record)}>
                            {t('团队列表')}
                        </Typography.Link>
                        <Typography.Link style={{ whiteSpace: 'nowrap' }} onClick={() => openDeptUserDrawer(record)}>
                            {t('成员列表')}
                        </Typography.Link>
                    </Space>
                )
            }
        }
    ]

    const openDeptTeamDrawer = (dept) => {
        setDeptTeamDrawer({
            open: true,
            deptId: dept.id,
            title: dept.orgName
        })
    }

    const closeDeptTeamDrawer = () => {
        setDeptTeamDrawer({
            open: false,
            deptId: null,
            title: null
        })
    }

    const openDeptUserDrawer = (dept) => {
        setDeptUserDrawer({
            open: true,
            deptId: dept.id,
            title: dept.orgName
        })
    }

    const closeDeptUserDrawer = () => {
        setDeptUserDrawer({
            open: false,
            deptId: null,
            title: null
        })
    }

    return (
        <Flex
            gap={16}
            vertical
        >
            <HasPermission hasPermissions='system:org:write'>
                <Button type="primary" onClick={() => handleAddDept()} className='w-20'>{t('新增部门')}</Button>
            </HasPermission>
            <Table
                columns={deptColumns}
                loading={fetchOrgDeptLoading}
                dataSource={orgDept}
                rowKey={(record) => record.id}
                pagination={false}
            />
            <Modal
                title={deptModal.title}
                width={400}
                centered
                open={deptModal.open}
                confirmLoading={createOrgDeptLoading || updateOrgDeptLoading}
                onOk={saveDept}
                onCancel={closeDeptModal}
                onClose={closeDeptModal}
                okText={t('保存')}
                cancelText={t('取消')}
                afterClose={() => deptForm.resetFields()}
            >
                <Form
                    form={deptForm}
                    labelCol={{ span: 6 }}
                    wrapperCol={{ span: 18 }}
                    layout="horizontal"
                >
                    <Form.Item name="id" noStyle />
                    <Form.Item name="parentId" noStyle />
                    <Row gutter={16}>
                        <Col span={24}>
                            <Form.Item
                                label="部门名称"
                                name="orgName"
                                rules={[
                                    {
                                        required: true,
                                        message: `部门名称不能为空`,
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
                                label="部门编码"
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
                                rules={[
                                    {
                                        required: true,
                                        message: `请选择状态`,
                                    },
                                ]}
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
                                label="排序"
                                name="sort"
                            >
                                <InputNumber precision={0} style={{ width: '100%' }} />
                            </Form.Item>
                        </Col>
                    </Row>
                </Form>
            </Modal>
            <Drawer
                title={t('团队列表') + `[${deptTeamDrawer.title}]`}
                closable={{ 'aria-label': 'Close Button' }}
                onClose={closeDeptTeamDrawer}
                open={deptTeamDrawer.open}
                width={700}
                destroyOnHidden
            >
                <OrgDeptTeam
                    deptId={deptTeamDrawer.deptId}
                />
            </Drawer>
            <Drawer
                title={t('成员列表') + `[${deptUserDrawer.title}]`}
                closable={{ 'aria-label': 'Close Button' }}
                onClose={closeDeptUserDrawer}
                open={deptUserDrawer.open}
                width={700}
            >
                <OrgMember
                    orgId={deptUserDrawer.deptId}
                />
            </Drawer>
        </Flex>
    )
}

export default OrgDept