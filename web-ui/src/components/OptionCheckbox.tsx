import Checkbox, { CheckboxGroupProps } from "antd/es/checkbox";
import { useEffect, useMemo, useRef, useState } from "react";
import { fetchDictByCode } from "../services/SystemService";
import { useRequest } from "ahooks";
import { Divider, Flex } from "antd";
import Loading from "./loading";



interface OptionCheckboxGroupProps extends CheckboxGroupProps {
    value?: any; // 选中的值
    labelFieldName: string;
    valueFieldName: string;
    gap: number
    selectAll: boolean
    onChange?: (value: any) => void; // 值变化时的回调
    loadData: (() => Promise<any[]> | string | any[]); // 获取选项数据的接口
}

const OptionCheckboxGroup: React.FC<OptionCheckboxGroupProps> = ({
    loadData,
    value,
    labelFieldName = "label",
    valueFieldName = "value",
    gap = 8,
    selectAll = true,
    onChange,
    ...props
}) => {

    const [optionsData, setOptionsData] = useState<any[]>([])

    const [loaded, setLoaded] = useState(false)

    const isFetching = useRef(false)

    // 初始化数据
    useEffect(() => {
        fetchData();
    }, [])

    const options = useMemo(() => {
        return optionsData.map((item) => ({
            label: item[labelFieldName],
            value: item[valueFieldName],
            style: { padding: `${gap}px` }
        }))
    }, [optionsData, labelFieldName, valueFieldName])

    const allIds = useMemo(() => options.map((r) => r.value), [options])

    const checkAll = allIds.length > 0 && (value || []).length === allIds.length
    const indeterminate = !checkAll && (value || []).length > 0

    const handleCheckAllChange = (e: any) => {
        if (e.target.checked) {
            onChange?.(allIds)
        } else {
            onChange?.([])
        }
    }

    const { runAsync: fetchOptionsAsync, loading: fetchOptionsLoading } = useRequest(async () => {
        if (typeof loadData === 'function') {
            return await loadData() // 从接口获取数据
        } else if (typeof loadData === 'string') {
            return await fetchDictByCode(loadData) // 从字典接口获取数据
        } else if(Array.isArray(loadData)){
             return loadData
        }
        return []
    }, { manual: true })

    const fetchData = async () => {
        if (loaded || isFetching.current) return;
        isFetching.current = true;
        const optionList = await fetchOptionsAsync();
        setLoaded(true);
        setOptionsData(optionList);
    }

    return (
        <Loading spinning={fetchOptionsLoading}>
            {selectAll === true ? (
                <Flex
                    vertical
                >
                    <Checkbox indeterminate={indeterminate} onChange={handleCheckAllChange} checked={checkAll}>
                        {'选择全部'}
                    </Checkbox>
                    <Divider />
                    <Checkbox.Group
                        style={{ display: 'flex', flexWrap: 'wrap', gap: '10px 16px' }}
                        options={options}
                        value={value ? value.map(String) : []}
                        onChange={(checkedValues) => onChange?.(checkedValues as string[])}
                        {...props}
                    />
                </Flex>
            ) : (
                <Checkbox.Group
                    style={{ display: 'flex', flexWrap: 'wrap', gap: '10px 16px' }}
                    options={options}
                    value={value ? value.map(String) : []}
                    onChange={(checkedValues) => onChange?.(checkedValues as string[])}
                    {...props}
                />
            )}
        </Loading>

    )
}

export default OptionCheckboxGroup