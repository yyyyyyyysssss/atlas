import { useEffect, useMemo, useRef, useState } from "react";
import { Select, SelectProps } from "antd";
import { useRequest } from 'ahooks';
import Loading from "./loading";
import NoDataEmpty from "./NoDataEmpty";
import { fetchDictByCode } from "../services/SystemService";




interface OptionSelectProps extends SelectProps<any> {
    value?: any; // 选中的值
    mode: any,
    labelFieldName: string
    valueFieldName: string
    onChange?: (value: any,selectedOption: any) => void; // 值变化时的回调
    loadData: (() => Promise<any[]> | string | any[]) // 获取选项数据的接口
}

const OptionSelect: React.FC<OptionSelectProps> = ({
    loadData,
    value,
    labelFieldName = 'label',
    valueFieldName = 'value',
    mode = null,
    onChange,
    ...props
}) => {

    const [optionsData, setOptionsData] = useState<any[]>([])

    const [loaded, setLoaded] = useState(false)

    const isFetching = useRef(false)

    useEffect(() => {
        if (value) {
            fetchData()
        }
    }, [value])

    const { runAsync: fetchOptionsAsync, loading: fetchOptionsLoading } = useRequest(async () => {
        if (typeof loadData === 'function') {
            return await loadData() // 从接口获取数据
        } else if (typeof loadData === 'string') {
            return await fetchDictByCode(loadData) // 从字典接口获取数据
        } else if (Array.isArray(loadData)) {
            return loadData
        }
        return []
    }, { manual: true })

    const fetchData = async () => {
        if (loaded || isFetching.current) return
        isFetching.current = true
        const optionList = await fetchOptionsAsync()
        setLoaded(true)
        setOptionsData(optionList)
    }

    // 当下拉框打开时请求数据
    const handleDropdownVisibleChange = (open: boolean) => {
        if (open && !loaded) {
            fetchData()
        }
    }

    const availableKeys = useMemo(() => {
        const keys = new Set<string>()
        optionsData.forEach((node) => {
            keys.add(node[valueFieldName])
        })
        return keys
    }, [optionsData])

    const safeValue = useMemo(() => {
        if (mode === 'multiple') {
            return value?.filter((id: any) => availableKeys.has(id))
        }
        return value
    }, [value, availableKeys])

    const handleChange = (value: any) => {
        if (onChange) {
            const selectedOption = optionsData.find((option) => option[valueFieldName] === value)
            onChange?.(value, selectedOption)
        }
    }

    return (
        <Loading spinning={fetchOptionsLoading}>
            <Select
                mode={mode}
                style={{ width: '100%' }}
                placeholder="请选择"
                value={safeValue ? String(value) : undefined}
                onChange={handleChange}
                notFoundContent={fetchOptionsLoading ? <Loading size="small" style={{ display: 'block', margin: '0 auto' }} /> : <NoDataEmpty />}
                options={optionsData}
                fieldNames={{
                    label: labelFieldName,
                    value: valueFieldName,
                }}
                allowClear
                showSearch
                optionFilterProp={labelFieldName}
                onOpenChange={handleDropdownVisibleChange}
                {...props}
            />
        </Loading>
    )

}

export default OptionSelect;