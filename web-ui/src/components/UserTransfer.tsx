import React, { useEffect, useMemo, useState } from 'react'
import { Flex, Spin, Tooltip, Transfer, TransferProps, Typography } from 'antd'
import { useRequest } from 'ahooks'
import { fetchUserOptions } from '../services/SystemService'
import Loading from './loading'
import { BrushCleaning, RotateCw } from 'lucide-react'

type CustomRenderResult = React.ReactNode | { label: React.ReactNode; value: string };

interface UserTransferProps extends TransferProps {
    value: string[]
    onChange: (targetKeys: Array<any>) => void
    disabledKeys?: string[]
    loading: boolean
    customRender?: (item: any) => CustomRenderResult
}


const UserTransfer: React.FC<UserTransferProps> = ({
    value,
    onChange,
    showSearch = true,
    disabledKeys = [],
    loading = false,
    customRender,
    ...transferProps
}) => {

    const [userList, setUserList] = useState<Array<any>>([])

    const [targetKeys, setTargetKeys] = useState<Array<string>>([])

    const [initLoading, setInitLoading] = useState<boolean>(false)

    const { runAsync: getUserOptionsAsync, loading: getUserOptionsLoading } = useRequest(fetchUserOptions, {
        manual: true
    })


    useEffect(() => {
        setInitLoading(true)
        getUserOptionsAsync()
            .then((data) => {
                setUserList(data)
                setInitLoading(false)
            })
            .catch(() => {
                setInitLoading(false)
            })
    }, [])

    useEffect(() => {
        setTargetKeys(value)
    }, [value])

    const processedList = useMemo(() => {
        return userList.map(item => ({
            ...item,
            disabled: disabledKeys?.includes(item.value) // 这里的 value 对应你的 rowKey
        }));
    }, [userList, disabledKeys])

    const renderItem = (item: any) => {
        if(customRender){
            return customRender(item) ?? item.label
        }
        return {
            key: item.value,
            label: item.label,
            value: item.value,
            disabled: true
        }
    }

    const handleChange = (newTargetKeys: Array<any>) => {
        onChange?.(newTargetKeys)
    }

    const handleSearch = (dir: any, value: any) => {

    }

    const handleClear = () => {
        onChange?.([])
    }

    return (
        <Loading spinning={initLoading || loading}>
            <Transfer
                style={{
                    width: '100%',
                    display: 'flex',
                    justifyContent: 'center',
                    alignItems: 'center'
                }}
                listStyle={{
                    width: '100%',
                    minHeight: '300px',
                }}
                titles={[
                    <Typography.Text>可选用户</Typography.Text>,
                    <Flex gap={10} justify='space-between' align='center'>
                        <Flex gap={6} style={{ paddingLeft: '5px' }}>
                            <Tooltip title='清空'>
                                <Typography.Text onClick={handleClear} style={{ cursor: 'pointer' }}>
                                    <BrushCleaning size={16} />
                                </Typography.Text>
                            </Tooltip>
                        </Flex>
                        <Typography.Text>已选用户</Typography.Text>
                    </Flex>

                ]}
                dataSource={processedList}
                targetKeys={targetKeys}
                showSearch={showSearch}
                rowKey={(record) => record.value}
                render={renderItem}
                onChange={handleChange}
                onSearch={handleSearch}
                filterOption={(inputValue, option) => {
                    return option.label.includes(inputValue)
                }}
                {...transferProps}
            />
        </Loading>
    )
}

export default UserTransfer