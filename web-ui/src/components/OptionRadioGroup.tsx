import { useRequest } from "ahooks";
import { Radio, RadioGroupProps } from "antd";
import { useEffect, useMemo, useRef, useState } from "react";
import { fetchDictByCode } from "../services/SystemService";
import Loading from "./loading";



interface OptionOptionRadioGroupProps{
    value?: any; // 选中的值
    labelFieldName: string
    valueFieldName: string
    gap: number
    onChange?: (value: any, selectedOption: any) => void; // 值变化时的回调
    loadData: (() => Promise<any[]> | string | any[]) // 获取选项数据的接口
}

const OptionRadioGroup: React.FC<OptionOptionRadioGroupProps> = ({
    loadData,
    value,
    labelFieldName = "label",
    valueFieldName = "value",
    gap = 8,
    onChange,
    ...props
}) => {

    const [optionsData, setOptionsData] = useState<any[]>([])

    const [loaded, setLoaded] = useState(false)

    const isFetching = useRef(false)

    useEffect(() => {
        fetchData()
    }, [])

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
        if (loaded || isFetching.current) return
        isFetching.current = true
        const optionList = await fetchOptionsAsync()
        setLoaded(true)
        setOptionsData(optionList)
    }

    const options = useMemo(() => {
        return optionsData.map((item) => ({
            label: item[labelFieldName],
            value: item[valueFieldName],
            style: { padding: `${gap}px` }
        }))
    }, [optionsData, labelFieldName, valueFieldName, gap])


    const handleChange = (e: any) => {
        if(onChange){
            const selectedValue = e.target.value
            const selectedOption = optionsData.find((item) => item[valueFieldName] === selectedValue)
            onChange(selectedValue, selectedOption)
        }
    }

    return (
        <Loading spinning={fetchOptionsLoading}>
            <Radio.Group
                value={value ? String(value) : undefined}
                onChange={handleChange}
                options={options}
                {...props}
            />
        </Loading>

    )
}

export default OptionRadioGroup