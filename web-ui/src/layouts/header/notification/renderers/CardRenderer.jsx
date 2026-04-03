import React from 'react';
import { Card, Typography, Tag, Descriptions, Button, Space, Image, Flex, theme } from 'antd';
import { ExternalLink, MousePointer2 } from 'lucide-react';

const { Text, Paragraph } = Typography;

const CardRenderer = ({ content }) => {
    const { token } = theme.useToken();

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
    } = content;

    // 2. 映射 Tag 颜色
    const statusColorMap = {
        success: 'success',
        warning: 'warning',
        error: 'error',
        default: 'default',
        processing: 'processing'
    };

    return (
        <Card
            size="small"
            style={{
                marginTop: 12,
                borderRadius: token.borderRadiusLG,
                border: `1px solid ${token.colorBorderSecondary}`,
                overflow: 'hidden'
            }}
            // 如果有整体链接，点击卡片跳转
            onClick={() => link && window.open(link, '_blank')}
            title={subTitle &&
                <Text
                    strong
                >
                    {subTitle}
                </Text>
            }
            extra={tagText && (
                <Tag color={statusColorMap[tagType] || 'default'} style={{ marginRight: 0 }}>
                    {tagText}
                </Tag>
            )}
        >
            <Flex vertical gap={12}>
                {/* 图片区 */}
                {imageUrl && (
                    <Image
                        src={imageUrl}
                        alt="card-image"
                        style={{ borderRadius: token.borderRadius, width: '100%', maxHeight: 180, objectFit: 'cover' }}
                        preview={false}
                    />
                )}

                {/* 正文区 */}
                {bodyContent && (
                    <Paragraph ellipsis={{ rows: 3 }} style={{ marginBottom: 0,  }}>
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
                                <Text style={{color : field.highlight ? token.colorError : 'inherit'}} strong={field.highlight}>
                                    {field.value}
                                </Text>
                            ),
                        }))}
                    />
                )}

                {/* 按钮动作区 */}
                {actions.length > 0 && (
                    <Flex justify="end" style={{ borderTop: `1px solid ${token.colorSplit}`, paddingTop: 12 }}>
                        <Space size="small">
                            {actions.map((btn, index) => (
                                <Button
                                    key={index}
                                    size="small"
                                    type={btn.type === 'primary' ? 'primary' : 'default'}
                                    danger={btn.type === 'danger'}
                                    icon={btn.actionType === 'URL' ? <ExternalLink size={12} /> : <MousePointer2 size={12} />}
                                    onClick={(e) => {
                                        e.stopPropagation(); // 阻止触发卡片的整体跳转
                                        if (btn.actionType === 'URL') {
                                            window.open(btn.url, '_blank');
                                        } else {
                                            console.log(`执行 ${btn.actionType} 请求: ${btn.url}`);
                                            // 这里可以接入你的 axios/fetch 请求逻辑
                                        }
                                    }}
                                >
                                    {btn.label}
                                </Button>
                            ))}
                        </Space>
                    </Flex>
                )}
            </Flex>
        </Card>
    );
};

export default CardRenderer;