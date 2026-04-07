import { useEffect, useState } from 'react'
import './index.css'
import { Flex, Splitter } from 'antd'
import { useTranslation } from 'react-i18next'
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