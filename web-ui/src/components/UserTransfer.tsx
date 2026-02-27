import React, { useEffect, useState } from 'react'
import { Flex, Spin, Tooltip, Transfer, TransferProps, Typography } from 'antd'
import { useRequest } from 'ahooks'
import { fetchUserOptions } from '../services/SystemService'
import Loading from './loading'
import { BrushCleaning, RotateCw } from 'lucide-react'


interface UserTransferProps extends TransferProps {
    value: string[]
    onChange: (targetKeys: Array<any>) => void
}


const UserTransfer: React.FC<UserTransferProps> = ({
    value,
    onChange,
    showSearch = true,
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

    const renderItem = (item: any) => {

        return {
            label: item.label,
            value: item.value
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
        <Loading spinning={initLoading}>
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
                                <Typography.Text onClick={handleClear} style={{cursor: 'pointer'}}>
                                    <BrushCleaning size={16} />
                                </Typography.Text>
                            </Tooltip>
                        </Flex>
                        <Typography.Text>已选用户</Typography.Text>
                    </Flex>

                ]}
                dataSource={userList}
                targetKeys={targetKeys}
                showSearch={showSearch}
                rowKey={(record) => record.id}
                render={renderItem}
                onChange={handleChange}
                onSearch={handleSearch}
                filterOption={(inputValue, option) => {
                    return option.fullName.includes(inputValue)
                }}
                {...transferProps}
            />
        </Loading>
    )
}

export default UserTransfer