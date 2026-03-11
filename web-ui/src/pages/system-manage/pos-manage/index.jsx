import { useEffect, useState } from 'react'
import './index.css'
import { bindPositionUser, createPosition, deletePositionById, fetchPositionDetails, fetchPositionList, fetchSearchUser, fetchUserIdByPositionId, updatePosition } from '../../../services/SystemService'
import { Button, Drawer, Flex, Form, Input, Modal, Popconfirm, Radio, Select, Skeleton, Space, Splitter, Switch, Table, Tree, Typography } from 'antd'
import Highlight from '../../../components/Highlight'
import HasPermission from '../../../components/HasPermission'
import { getMessageApi } from '../../../utils/MessageUtil'
import { useRequest } from 'ahooks'
import SmartTable from '../../../components/smart-table'
import RemoteSearchSelect from '../../../components/RemoteSearchSelect'
import Loading from '../../../components/loading'
import { useTranslation } from 'react-i18next'
import UserTransfer from '../../../components/UserTransfer'
import OptionSelect from '../../../components/OptionSelect'
import { PositionStatus } from '../../../enums/system'
import OrgTree from '../../../components/OrgTree'
import PositionDetails from './details'



const PositionManage = () => {

    const { t } = useTranslation()

    const [selectOrg, setSelectOrg] = useState({
        orgId: null,
        orgType: null,
    })

    const handleSelect = (orgId, record) => {
        setSelectOrg({
            orgId: orgId,
            orgType: record.orgType,
            orgPath: record.orgPath
        })
    }

    return (
        <Flex flex={1} gap={10} className='h-full'>
            <Splitter>
                <Splitter.Panel style={{ padding: '10px' }} defaultSize="25%" min="20%" max="50%">
                    <Flex
                        vertical
                    >
                        <OrgTree
                            showAll
                            selectFirst
                            onSelect={handleSelect}
                        />
                    </Flex>
                </Splitter.Panel>
                <Splitter.Panel style={{ padding: '20px' }}>
                    <PositionDetails
                        orgId={selectOrg.orgId}
                        orgType={selectOrg.orgType}
                        orgPath={selectOrg.orgPath}
                    />
                </Splitter.Panel>
            </Splitter>
        </Flex>
    )
}

export default PositionManage