
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
import EditableTable from '../../../../components/smart-table/EditableTable';
import OrgMember from '../member';


const OrgDeptTeam = ({ deptId }) => {

    const { t } = useTranslation()

    const [teamForm] = Form.useForm()

    const [deptTeam, setDeptTeam] = useState([])

    const [teamUserDrawer, setTeamUserDrawer] = useState({
        open: false,
        teamId: null,
        title: ''
    })

    const { runAsync: fetchOrgDeptTeamAsync, loading: fetchOrgDeptTeamLoading } = useRequest(fetchOrgSubUnits, {
        manual: true
    })

    const { runAsync: createOrgDeptTeamAsync, loading: createOrgDeptTeamLoading } = useRequest(createOrg, {
        manual: true
    })

    const { runAsync: updateOrgDeptTeamAsync, loading: updateOrgDeptTeamLoading } = useRequest(updateOrg, {
        manual: true
    })

    const fetchDeptTeam = async (deptId) => {
        if (!deptId) {
            return
        }
        const deptList = await fetchOrgDeptTeamAsync(deptId, OrganizationType.TEAM.value)
        setDeptTeam(deptList)
        teamForm.setFieldsValue({
            teams: deptList
        })
    }

    useEffect(() => {
        fetchDeptTeam(deptId)
    }, [deptId])

    const saveTeam = async (_, rowIndex) => {
        const formValues = await teamForm.validateFields()
        const teams = formValues.teams[rowIndex]
        const req = {
            ...teams,
            parentId: deptId,
            orgType: OrganizationType.TEAM.value
        }
        if (teams.type === 'add') {
            await createOrgDeptTeamAsync(req)
            getMessageApi().success(t('新增成功'))
        } else {
            await updateOrgDeptTeamAsync(req)
            getMessageApi().success(t('修改成功'))
        }
        fetchDeptTeam(deptId)
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
            key: 'orgName',
            title: '团队名称',
            dataIndex: 'orgName',
            align: 'center',
            editable: true,
            required: true,
        },
        {
            key: 'orgCode',
            title: '团队编码',
            dataIndex: 'orgCode',
            align: 'center',
            inputType: 'custom',
            editable: true,
            editRender: ({ value, onChange }) => {
                return <Input value={value} disabled />
            },
        },
        {
            key: 'status',
            title: '状态',
            dataIndex: 'status',
            align: 'center',
            inputType: 'custom',
            editable: true,
            required: true,
            editRender: ({ value, onChange }) => {
                return <OptionSelect
                    loadData={Object.values(OrganizationStatus)}
                    value={value}
                    onChange={onChange}
                    placeholder="请选择状态"
                />
            },
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
            align: 'center',
            inputType: 'custom',
            editable: true,
            editRender: ({ value, onChange }) => {
                return <InputNumber precision={0} style={{ width: '100%' }} value={value} onChange={onChange} />
            },
        },
    ]

    const openTeamUserDrawer = (team) => {
        setTeamUserDrawer({
            open: true,
            teamId: team.id,
            title: team.orgName
        })
    }

    const closeTeamUserDrawer = () => {
        setTeamUserDrawer({
            open: false,
            teamId: null,
            title: null
        })
    }

    return (
        <Form form={teamForm} component={false}>
            <Flex gap={8} vertical>
                <Form.List
                    name="teams"
                    noStyle
                >
                    {(fields, { add, remove }) => (
                        <EditableTable
                            columns={columns}
                            name='teams'
                            mode='single-edit'
                            loading={fetchOrgDeptTeamLoading || createOrgDeptTeamLoading || updateOrgDeptTeamLoading}
                            fields={fields}
                            editPermission={'system:org:write'}
                            deletePermission={'no-show'}
                            add={add}
                            remove={remove}
                            onSave={saveTeam}
                            renderExtraActions={(record, rowIndex, isAnyRowEditing) => {

                                return (
                                    <Typography.Link onClick={() => openTeamUserDrawer(record)} style={{ whiteSpace: 'nowrap' }} disabled={isAnyRowEditing}>
                                        {t('成员列表')}
                                    </Typography.Link>
                                )
                            }}
                        />
                    )}
                </Form.List>
                <Drawer
                    title={t('成员列表') + `[${teamUserDrawer.title}]`}
                    closable={{ 'aria-label': 'Close Button' }}
                    onClose={closeTeamUserDrawer}
                    open={teamUserDrawer.open}
                    width={700}
                >
                    <OrgMember
                        orgId={teamUserDrawer.teamId}
                    />
                </Drawer>
            </Flex>
        </Form>
    )
}

export default OrgDeptTeam