import { useEffect, useMemo, useRef, useState } from "react";
import { fetchRoleOptions } from "../services/SystemService";
import { Checkbox, Divider, Flex, Select } from "antd";
import { useTranslation } from 'react-i18next';


interface Role {
    value: string;
    label: string;
}

interface RoleSelectProps {
    value?: string[];
    onChange?: (value: string[]) => void;
    type?: string;
}

const RoleSelect: React.FC<RoleSelectProps> = ({ value, onChange, type = 'select' }) => {

    const { t } = useTranslation()

    const [roleData, setRoleData] = useState<Role[]>([])

    const [loading, setLoading] = useState(false)

    const [loaded, setLoaded] = useState(false)

    const isFetching = useRef(false) 

    const fetchData = async () => {
        if (loaded || isFetching.current) return
        isFetching.current = true
        setLoading(true)
        try {
            const roleOptions = await fetchRoleOptions()
            setRoleData(roleOptions)
            setLoaded(true)
        } finally {
            setLoading(false)
        }
    }

    useEffect(() => {
        if (type === 'checkbox') {
            fetchData()
        }
    }, [type])

    useEffect(() => {
        if (value && value.length > 0) {
            fetchData()
        }
    }, [value])

    const handleDropdownVisibleChange = (open: boolean) => {
        if (open) {
            fetchData()
        }
    }

    const allIds = useMemo(() => roleData.map((r) => r.value), [roleData])

    const checkAll = allIds.length > 0 && (value || []).length === allIds.length
    const indeterminate = !checkAll && (value || []).length > 0

    const handleCheckAllChange = (e: any) => {
        if (e.target.checked) {
            onChange?.(allIds)
        } else {
            onChange?.([])
        }
    }

    const availableKeys = useMemo(() => {
        const keys = new Set<string>()
        roleData.forEach((node) => {
            keys.add(node.value)
        })
        return keys
    }, [roleData])

    const safeValue = useMemo(() => {
        return (value || []).filter((id) => availableKeys.has(id))
    }, [value, availableKeys])

    return (
        <>
            {type === 'select' ?
                (
                    <Select
                        mode="multiple"
                        style={{ width: '100%' }}
                        placeholder="请选择角色"
                        value={safeValue}
                        onChange={onChange}
                        loading={loading}
                        options={roleData}
                        allowClear
                        showSearch
                        optionFilterProp="label"
                        onOpenChange={handleDropdownVisibleChange}
                    />
                )
                :
                (
                    <Flex
                        vertical
                    >
                        <Checkbox indeterminate={indeterminate} onChange={handleCheckAllChange} checked={checkAll}>
                            {t('选择全部')}
                        </Checkbox>
                        <Divider />
                        <Checkbox.Group
                            style={{ display: 'flex', flexWrap: 'wrap', gap: '10px 16px' }}
                            options={roleData.map((item) => ({
                                label: item.label,
                                value: item.value
                            }))}
                            value={safeValue}
                            onChange={(checkedValues) => onChange?.(checkedValues as string[])}
                        />
                    </Flex>
                )
            }
        </>
    )
}

export default RoleSelect