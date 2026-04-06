
import React, { useEffect, useState } from 'react';
import { Flex, Typography, theme, Tag, Divider, Space, Checkbox, Table, Image, Modal } from 'antd';
import { useRequest } from 'ahooks';
import { getAnnouncementUserDetails } from '../../../../services/NotificationService';
import { AnnouncementType } from '../../../../enums/notification';
import MarkdownView from '../../../../components/MarkdownView';

const { Text } = Typography;

const AnnouncementDetailModal = ({ id, onCancel, width = 700 }) => {

    const { token } = theme.useToken()

    const { data: data = {}, runAsync: detailAsync, loading: detailLoading } = useRequest(getAnnouncementUserDetails, { manual: true })

    useEffect(() => {
        if (id) {
            detailAsync(id)
        }
    }, [id])

    const typeConfig = AnnouncementType[data.type] || { label: data.type, color: 'default' }

    return (
        <Modal
            title={data?.title}
            open={!!id}
            onCancel={onCancel}
            onClose={onCancel}
            footer={null}
            loading={detailLoading}
            width={width}
            destroyOnHidden
        >
            <Flex vertical gap={12}>
                {/* 元信息 */}
                <Flex justify="space-between" align="center" style={{ fontSize: token.fontSizeSM }}>
                    <Flex gap={16} wrap="wrap">
                        {data.type === AnnouncementType.RELEASE.value && data.version && (
                            <Space size={4}>
                                <Text type='secondary'>版本: {data.version}</Text>
                            </Space>
                        )}
                        <Space size={4}>
                            <Text type='secondary'>{data.publishTime}</Text>
                        </Space>
                        <Space size={4}>
                            <Text type='secondary'>发布人: {data.creatorName}</Text>
                        </Space>
                    </Flex>
                    <Tag bordered={false} color={typeConfig.color}>
                        {typeConfig.label}
                    </Tag>
                </Flex>

                <Divider style={{ margin: '8px 0' }} />

                {/* 内容 */}
                <Flex
                    vertical
                    style={{
                        lineHeight: 1.8,
                        maxHeight: '60vh',
                        overflowY: 'auto',
                        // 解决方案：
                        width: '100%',
                        position: 'relative', // 帮助内部绝对定位元素（如语言标签）定位
                        paddingBottom: 24,    // 给底部留出缓冲空间，防止最后一个组件贴底
                        paddingRight: 6
                    }}
                >
                    <div style={{ width: '100%', flex: '1 0 auto' }}>
                        <MarkdownView content={data.content} />
                    </div>
                </Flex>
            </Flex>
        </Modal>
    )
}

export default AnnouncementDetailModal