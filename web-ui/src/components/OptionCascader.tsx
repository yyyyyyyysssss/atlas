import { useRequest } from "ahooks";
import { Cascader, CascaderProps } from "antd";
import { useEffect, useMemo, useRef, useState } from "react";
import { fetchDictByCode } from "../services/SystemService";
import Loading from "./loading";
import NoDataEmpty from "./NoDataEmpty";



interface OptionCascaderProps extends CascaderProps {
    value?: any; // 选中的值
    labelFieldName: string;
    valueFieldName: string;
    childrenFieldName: string;
    onChange?: (value: any, selectedOption: any) => void; // 值变化时的回调
    loadData: () => Promise<any[]> | string; // 获取选项数据的接口
}


const OptionCascader: React.FC<OptionCascaderProps> = ({
    loadData,
    value,
    labelFieldName = 'label',
    valueFieldName = 'value',
    childrenFieldName = 'children',
    onChange,  // 声明 onChange
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
            return await loadData(); // 从接口获取数据
        } else if (typeof loadData === 'string') {
            return await fetchDictByCode(loadData) // 从字典接口获取数据
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

    const handleOpenChange = (open: boolean) => {
        if (open && !loaded) {
            fetchData()
        }
    }

    const filter = (inputValue: any, path: any) => {

        return path.some((option: { label: string }) => option.label.toLowerCase().includes(inputValue.toLowerCase()))
    }


    return (
        <Cascader
            fieldNames={{ label: labelFieldName, value: valueFieldName, children: childrenFieldName }}
            value={value ? value.map(String) : []}
            options={optionsData}
            onChange={onChange}
            onOpenChange={handleOpenChange}
            showSearch={{ filter }}
            notFoundContent={fetchOptionsLoading ? <Loading size="small" style={{ display: 'block', margin: '0 auto' }} /> : <NoDataEmpty />}
        />
    )
}

export default OptionCascader
