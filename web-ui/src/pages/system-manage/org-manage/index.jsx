import './index.css'
import { useRef, useState } from 'react'
import { Dropdown, Flex, Splitter, Typography } from 'antd'
import { Building2, Building, Component, Users, Plus } from 'lucide-react';
import { OperationMode } from '../../../enums/common';
import HasPermission from '../../../components/HasPermission';
import { useTranslation } from 'react-i18next';
import OrgDetails from './details';
import { OrganizationType } from '../../../enums/system';
import OrgTree from '../../../components/OrgTree';

// 根据当前组织类型获取下一级组织类型
const getNextOrgType = (currentType) => {
    const typeOrder = [OrganizationType.GROUP.value, OrganizationType.COMPANY.value, OrganizationType.DEPT.value, OrganizationType.TEAM.value]
    const currentIndex = typeOrder.indexOf(currentType)
    if (currentIndex === -1 || currentIndex === typeOrder.length - 1) {
        return null // 未知类型或已是最低层级，返回 null 表示不允许新增
    }
    return typeOrder[currentIndex + 1]
}

const OrgTypeIcon = {
    [OrganizationType.GROUP.value]: <Building2 size={16} />,
    [OrganizationType.COMPANY.value]: <Building size={16} />,
    [OrganizationType.DEPT.value]: <Component size={16} />,
    [OrganizationType.TEAM.value]: <Users size={16} />,
}

const OrgManage = () => {

    const { t } = useTranslation()

    const [selectedOrg, setSelectedOrg] = useState(null)

    const orgTreeRef = useRef()

    const selectedOrgTreeNode = (key) => {
        handleSelectOrg(key)
    }

    const handleAddOrg = (type, orgItem) => {
        if (type === 'child') {
            const nextOrgType = getNextOrgType(orgItem.orgType)
            if (!nextOrgType) {
                // 如果已是最低层级，不允许新增子级
                return
            }
            setSelectedOrg({
                id: null,
                parentId: orgItem.id,
                parentCode: orgItem.code,
                orgType: nextOrgType,
                operationMode: OperationMode.ADD.value
            })
        } else {
            const parentOrgItem = flattenTree.flattenList.find(f => f.id === orgItem.parentId)
            setSelectedOrg({
                id: null,
                parentId: parentOrgItem.id,
                parentCode: parentOrgItem.code,
                orgType: orgItem.orgType,
                operationMode: OperationMode.ADD.value
            })
        }
        // 将选中的取消
        setSelectedKeys([])
    }

    // 选中组织
    const handleSelectOrg = async (orgId, orgType = null) => {
        const selectedOrg = flattenTree.flattenList.find(f => f.id == orgId)
        // 不取消选中
        setSelectedKeys([orgId])
        setSelectedOrg({
            id: orgId,
            parentId: null,
            parentCode: null,
            orgType: orgType || selectedOrg.orgType,
            operationMode: OperationMode.VIEW.value
        })
    }

    const selectOrg = (orgId, orgRecord) => {
        setSelectedOrg({
            id: orgId,
            parentId: null,
            parentCode: null,
            orgType: orgRecord.orgType,
            operationMode: OperationMode.VIEW.value
        })
    }

    const changeOperationMode = (operationMode) => {
        setSelectedOrg({
            ...selectedOrg,
            operationMode: operationMode
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
                            ref={orgTreeRef}
                            onSelect={selectOrg}
                            itemRender={(item) => {
                                return (
                                    <OrgItem
                                        item={item}
                                        onAddOrg={handleAddOrg}
                                    />
                                )
                            }}
                        />
                    </Flex>
                </Splitter.Panel>
                <Splitter.Panel style={{ padding: '20px' }}>
                    <OrgDetails
                        orgId={selectedOrg?.id}
                        parentId={selectedOrg?.parentId}
                        parentCode={selectedOrg?.parentCode}
                        orgType={selectedOrg?.orgType}
                        operationMode={selectedOrg?.operationMode}
                        changeOperationMode={changeOperationMode}
                        selectedOrgTreeNode={selectedOrgTreeNode}
                        onSuccess={() => {
                            orgTreeRef.current.refresh()
                        }}
                    />
                </Splitter.Panel>
            </Splitter>
        </Flex>
    )
}

const OrgItem = ({ item, selected = item.selected, onAddOrg }) => {

    const { t } = useTranslation()

    const canAddChild = getNextOrgType(item.orgType) !== null

    return (
        <Flex
            justify='space-between'
            align='center'
            style={{ height: '38px' }}
        >
            <Flex align='center' gap={8}>
                {OrgTypeIcon[item.orgType]}
                <Typography.Text>
                    {item.title}
                </Typography.Text>
            </Flex>

            <HasPermission requireAll={true} hasPermissions={['system:org:write', 'system:org:delete']}>
                <div className={`flex items-center transition-opacity ${selected ? 'opacity-100' : 'opacity-0'}`}>
                    <Dropdown
                        menu={{
                            items: [
                                ...(canAddChild ? [{
                                    key: 'child',
                                    label: t('新增子级')
                                }] : []),
                                {
                                    key: 'brother',
                                    label: t('新增同级')
                                }
                            ],
                            onClick: (info) => {
                                const event = info.domEvent
                                const key = info.key
                                event.stopPropagation()
                                onAddOrg(key, item)
                            }
                        }}
                    >
                        <div
                            className='org-ops-btn'
                            onClick={e => {
                                e.stopPropagation()
                            }}
                        >
                            <Plus size={18} />
                        </div>
                    </Dropdown>
                </div>
            </HasPermission>
        </Flex>
    )
}

export default OrgManage