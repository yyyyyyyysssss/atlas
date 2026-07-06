import { Flex, Spin, Typography, theme } from 'antd';
import './index.css'
import React from 'react';
import { motion, AnimatePresence } from 'framer-motion';

interface LoadingProps extends React.HTMLAttributes<HTMLDivElement> {
    fullscreen?: boolean,  // 对应之前引用的 fullscreen
    full?: boolean,        // 兼容原有的 full
    spinning?: boolean,
    children?: React.ReactNode,
    size?: 'small' | 'default' | 'large',
    tip?: React.ReactNode, // 接收 tip 文本
    [key: string]: any,
}

const Loading: React.FC<LoadingProps> = ({
    fullscreen = false,
    full = false,
    spinning = true,
    children,
    size = 'default',
    tip,
    ...restProps
}) => {

    // 获取 Ant Design 的主题 Token
    const { token } = theme.useToken();

    // 判断是否为全屏模式（兼容旧的 full 和新的 fullscreen）
    const isFull = fullscreen || full;

    // 自定义具有设计感的 Indicator，使用内联样式注入主题变量
    const customIndicator = (
        <div className="atlas-loading-indicator">
            <svg viewBox="0 0 50 50" className="atlas-spinner-svg">
                <circle
                    cx="25"
                    cy="25"
                    r="20"
                    fill="none"
                    strokeWidth="5"
                    style={{ stroke: token.colorFillSecondary }}
                />
                <circle
                    className="atlas-spinner-path"
                    cx="25"
                    cy="25"
                    r="20"
                    fill="none"
                    strokeWidth="5"
                    style={{ stroke: token.colorPrimary }}
                />
            </svg>
        </div>
    );

    // 如果只是作为局部包裹组件 (有 children)，默认使用 Ant Design 原生的 Spin 体验最好
    if (children && !isFull) {
        return (
            <Spin
                spinning={spinning}
                size={size}
                tip={tip}
                indicator={customIndicator}
                {...restProps}
            >
                {children}
            </Spin>
        )
    }

    // 全屏或者独立占位模式 (不包裹子元素)
    return (
        <>
            {children}
            <AnimatePresence>
                {spinning && (
                    <motion.div
                        initial={{ opacity: 0 }}
                        animate={{ opacity: 1 }}
                        exit={{ opacity: 0 }}
                        transition={{ duration: 0.3 }}
                        className={isFull ? 'atlas-loading-fullscreen' : 'atlas-loading-wrapper'}
                    >
                        {/* 直接使用 Flex 横向布局，脱离 Spin 容器的限制，防止文字被挤压成竖列 */}
                        <Flex align="center" gap={12} className="atlas-loading-content">
                            {customIndicator}
                            {tip && (
                                <Typography.Text
                                    className="atlas-loading-tip"
                                    style={{ color: token.colorTextSecondary }}
                                >
                                    {tip}
                                </Typography.Text>
                            )}
                        </Flex>
                    </motion.div>
                )}
            </AnimatePresence>
        </>
    )
}

export default Loading;