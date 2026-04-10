import { Flex, Spin } from 'antd';
import './index.css'
import React from 'react';


interface LoadingProps extends React.HTMLAttributes<HTMLDivElement> {
    full?: boolean,
    spinning?: boolean,
    children?: React.ReactNode
    size?: any
    resetProps?: any,
}

const Loading: React.FC<LoadingProps> = ({
    full = false,
    spinning = true,
    children,
    size,
    ...resetProps
}) => {

    // 全屏模式
    if (full) {
        return (
            <Flex
                style={{
                    flex: 1,
                    height: '100%',
                    width: '100%',
                }}
                justify="center"
                align="center"
            >
                <Spin spinning={spinning} size={size} {...resetProps}>
                    {children}
                </Spin>
            </Flex>
        )
    }

    return (
        <Spin
            spinning={spinning}
            size={size}
            {...resetProps}
        >
            {children}
        </Spin>
    )
}

export default Loading