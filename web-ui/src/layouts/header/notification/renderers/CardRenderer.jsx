import React, { useState } from 'react';
import { Card, Typography, Tag, Descriptions, Button, Space, Image, Flex, theme, Drawer, Modal, App } from 'antd';
import { ArrowRight, ExternalLink, Maximize2, MousePointer2, Send } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { findRouteByPath } from '../../../../router/router';
import Loading from '../../../../components/loading';
import httpWrapper from '../../../../services/AxiosWrapper';

const { Text, Paragraph } = Typography

const themeMap = {
    'primary': 'primary',
    'dashed': 'dashed',
    'link': 'link',
    'text': 'text',
}

const CardRenderer = React.memo(({ content, onClose, onAction }) => {

    const { token } = theme.useToken()

    const navigate = useNavigate()

    const { modal, message } = App.useApp()

    const [routeDrawer, setRouteDrawer] = useState({
        visible: false,
        component: null,
        params: {},
        title: ''
    })

    const [actionLoading, setActionLoading] = useState({})

    // 1. 解构后端传来的 CardBody 字段
    const {
        subTitle,
        content: bodyContent,
        imageUrl,
        fields = [],
        tagText,
        tagType = 'processing',
        actions = [],
        link
    } = content

    // 2. 映射 Tag 颜色
    const statusColorMap = {
        success: 'success',
        warning: 'warning',
        error: 'error',
        default: 'default',
        processing: 'processing'
    }

    const resolveAtlasLink = (linkStr) => {
        // 解析自定义协议链接
        if (!linkStr || !linkStr.startsWith('atlas://')) return null
        const url = new URL(linkStr.replace('atlas://', 'http://dummy/'))
        const path = url.pathname
        const params = Object.fromEntries(url.searchParams.entries())
        return { path, params };
    }

    const handleLinkClick = (link) => {
        if (!link) {
            return
        }
        const resolvedLink = resolveAtlasLink(link)
        if (resolvedLink) {
            const route = findRouteByPath(resolvedLink.path)
            if (route && route.element) {
                setRouteDrawer({
                    visible: true,
                    component: route.element,
                    params: resolvedLink.params,
                    title: route.breadcrumbName
                })
            } else {
                closeDrawer?.()
                navigate(resolvedLink.path, { state: resolvedLink.params })
            }
        }
    }

    const getActionIcon = (actionType) => {
        switch (actionType) {
            case 'URL':
                return <ExternalLink size={12} />
            case 'ROUTE':
                return <ArrowRight size={12} />
            case 'DRAWER':
                return <Maximize2 size={12} />
            case 'API':
                return <Send size={12} />
            default:
                return <MousePointer2 size={12} />
        }
    }

    const handleActionExecute = async (action, index) => {
        const { label, path, actionType, confirmText, target, extra } = action
        // 处理二次确认
        if (confirmText) {
            const confirmed = await new Promise((resolve) => {
                modal.confirm({
                    title: '请确认',
                    content: confirmText,
                    onOk: () => resolve(true),
                    onCancel: () => resolve(false)
                })
            })
            if (!confirmed) {
                return
            }
        }
        switch (actionType) {
            case 'DRAWER':
                handleLinkClick(path)
                break
            case 'API':
                try {
                    setActionLoading(prev => ({ ...prev, [index]: true }))
                    const method = (extra?.method || 'POST').toUpperCase()
                    const params = extra?.params || {}
                    const data = extra?.data || extra || {}
                    await httpWrapper({
                        url: path,
                        method: method,
                        params: method === 'get' ? data : params,
                        data: method !== 'get' ? data : undefined,
                    })
                    message.success(`${label || '操作'}成功`)
                    if (extra?.closeDrawer) {
                        setRouteDrawer(prev => ({ ...prev, visible: false }));
                    }
                } finally {
                    setActionLoading(prev => ({ ...prev, [index]: false }))
                }

                break
            case 'URL':
                window.open(path, target?.toLowerCase() || '_blank')
                break
            case 'ROUTE':
            default:
                navigate(path, { state: extra })
                onClose?.()
                break
        }
    }

    return (
        <>
            <Card
                size="small"
                style={{
                    marginTop: 12,
                    borderRadius: token.borderRadiusLG,
                    border: `1px solid ${token.colorBorderSecondary}`,
                    overflow: 'hidden',
                    cursor: link ? 'pointer' : 'default',
                }}
                // 如果有整体链接，点击卡片跳转
                onClick={(e) => {
                    e.stopPropagation()
                    handleLinkClick(link)
                }}
                title={subTitle &&
                    <Text
                        strong
                    >
                        {subTitle}
                    </Text>
                }
                extra={tagText && (
                    <Tag color={statusColorMap[tagType?.toLowerCase()] || 'default'} style={{ marginRight: 0 }}>
                        {tagText}
                    </Tag>
                )}
            >
                <Flex vertical gap={12}>
                    {/* 图片区 */}
                    {imageUrl && (
                        <Flex
                            justify="center"
                            align="center"
                            style={{
                                width: '100%',
                                backgroundColor: token.colorFillQuaternary, // 加一个浅色背景，增强质感
                                borderRadius: token.borderRadius,
                                overflow: 'hidden',
                                lineHeight: 0, // 消除图片底部空隙
                                cursor: 'default'
                            }}
                            onClick={(e) => e.stopPropagation()}
                        >
                            <Image
                                src={imageUrl}
                                alt="card-image"
                                style={{
                                    maxWidth: '100%',
                                    maxHeight: 220, // 稍微增加上限
                                    objectFit: 'scale-down', // 比 contain 更智能：图小则居中，图大则缩小
                                }}
                                preview={true}
                            />
                        </Flex>
                    )}

                    {/* 正文区 */}
                    {bodyContent && (
                        <Paragraph ellipsis={{ rows: 3 }} style={{ marginBottom: 0, }}>
                            {bodyContent}
                        </Paragraph>
                    )}

                    {/* KV 字段区：核心业务数据 */}
                    {fields.length > 0 && (
                        <Descriptions
                            column={1}
                            size="small"
                            colon={false}
                            styles={{
                                label: { color: token.colorTextDescription, width: '80px' },
                                content: { color: token.colorText, fontWeight: 500 }
                            }}
                            items={fields.map((field, index) => ({
                                key: index,
                                label: field.label,
                                children: (
                                    <Text style={{ color: field.highlight ? token.colorError : 'inherit' }} strong={field.highlight}>
                                        {field.value}
                                    </Text>
                                ),
                            }))}
                        />
                    )}

                    {/* 按钮动作区 */}
                    {actions && actions.length > 0 && (
                        <Flex
                            justify="end"
                            style={{
                                borderTop: `1px solid ${token.colorSplit}`,
                                paddingTop: 12,
                                marginTop: 8
                            }}
                        >
                            <Space size="small">
                                {actions.map((action, index) => (
                                    <Button
                                        key={index}
                                        loading={actionLoading[index]}
                                        size="small"
                                        type={themeMap[action.theme?.toLowerCase()] || 'default'}
                                        danger={action.theme?.toLowerCase() === 'danger'}
                                        icon={getActionIcon(action.actionType)}
                                        onClick={(e) => {
                                            e.stopPropagation()
                                            handleActionExecute(action, index)
                                        }}
                                    >
                                        {action.label}
                                    </Button>
                                ))}
                            </Space>
                        </Flex>
                    )}
                </Flex>
            </Card>
            <Drawer
                title={routeDrawer.title}
                width="80%"
                open={routeDrawer.visible}
                onClose={() => setRouteDrawer(prev => ({ ...prev, visible: false }))}
                destroyOnHidden
                styles={{ body: { padding: 24 } }}
            >
                <React.Suspense fallback={<Loading full />}>
                    {routeDrawer.component && React.cloneElement(routeDrawer.component, {
                        // 1. 透传解析出来的 URL 参数（如 id, type 等）
                        ...routeDrawer.params,
                    })}
                </React.Suspense>
            </Drawer>
        </>
    );
}, (prev, next) => {
    return prev.content === next.content && prev.onActionClick === next.onActionClick;
})

export default CardRenderer;