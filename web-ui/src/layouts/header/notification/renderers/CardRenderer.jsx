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

const CardRenderer = React.memo(({ content, onClose }) => {

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
        if (!linkStr) return null
        // 处理 HTTP/HTTPS 链接 (外部链接)
        if (linkStr.startsWith('http://') || linkStr.startsWith('https://')) {
            return {
                type: 'external',
                path: linkStr,
                openMode: '_blank' // 建议新窗口打开
            }
        }
        if (linkStr.startsWith('atlas://')) {
            try {
                const url = new URL(linkStr.replace('atlas://', 'http://dummy/'));
                const params = Object.fromEntries(url.searchParams.entries());
                return {
                    type: 'internal',
                    path: url.pathname,
                    params: params,
                    // 允许通过链接参数动态控制打开方式，默认 drawer
                    openMode: params.openMode || 'drawer'
                };
            } catch (e) {
                console.error("Atlas Link Parse Error:", e);
                return null;
            }
        }
        return {
            type: 'path',
            path: linkStr,
            openMode: 'router'
        }
    }

    const handleLinkClick = (link) => {
        const resolvedLink = resolveAtlasLink(link)
        if (!resolvedLink) {
            return
        }
        const { type, path, params, openMode } = resolvedLink
        switch (type) {
            case 'external':
                window.open(path, openMode)
                break
            case 'internal':
                if (openMode === 'drawer') {
                    const route = findRouteByPath(path)
                    if (route && route.element) {
                        setRouteDrawer({
                            visible: true,
                            component: route.element,
                            params: params,
                            title: route.breadcrumbName
                        })
                        return
                    }
                }
                closeDrawer?.()
                navigate({ pathname: path, search: new URLSearchParams(params).toString() })
                break
            default:
                closeDrawer?.()
                navigate(path)
                break
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
                    // e.stopPropagation()
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
                        // onClick={(e) => e.stopPropagation()}
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
                                            // e.stopPropagation()
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
            <Flex onClick={(e) => e.stopPropagation()}>
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
            </Flex>
        </>
    );
})

export default CardRenderer;