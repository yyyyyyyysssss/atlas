import { useRequest } from "ahooks"
import { Button, Flex, Table } from "antd"
import { useState } from "react"
import { fetchOrgMembers } from "../../../../services/SystemService"
import HasPermission from "../../../../components/HasPermission"
import { useTranslation } from 'react-i18next';

const OrgMember = ({ orgId }) => {

    const { t } = useTranslation()

    const [orgMember, setOrgMember] = useState([])

    const { runAsync: fetchOrgMemberAsync, loading: fetchOrgMemberLoading } = useRequest(fetchOrgMembers, {
        manual: true
    })

    const addMember = () => {

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
            align: 'center'
        },
        {
            key: 'posName',
            title: '岗位',
            dataIndex: 'posName',
            align: 'center'
        },
        {
            key: 'isMain',
            title: '主部门',
            dataIndex: 'isMain',
            align: 'center'
        },
    ]

    return (
        <Flex
            gap={16}
            vertical
        >
            <HasPermission hasPermissions='system:org:write'>
                <Button type="primary" onClick={() => addMember()} className='w-20'>{t('添加成员')}</Button>
            </HasPermission>
            <Table
                style={{ width: '100%' }}
                columns={columns}
                loading={fetchOrgMemberLoading}
                dataSource={orgMember}
                rowKey={(record) => record.id}
                pagination={false}
            />
        </Flex>
    )
}

export default OrgMember