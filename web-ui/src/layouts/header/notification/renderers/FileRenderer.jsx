import React, { useMemo, useState } from 'react';
import { Flex, Typography, Button, theme, Tooltip } from 'antd';
import { FileImage, FileText, Download, FileArchive, FileCode } from 'lucide-react';
import { formatFileSize } from '../../../../utils/format';
import { downloadFile } from '../../../../utils/Download';

const { Text } = Typography;


const FileRenderer = ({ content, mode = 'list' }) => {

    const { token } = theme.useToken()

    const [downloading, setDownloading] = useState(false)

    const handleDownload = async (e, url, name) => {
        e.stopPropagation()
        if (downloading) {
            return
        }
        setDownloading(true)
        downloadFile({ url: url, filename: name })
        // 人为延迟，让用户看到 Loading 动画
        await new Promise(resolve => setTimeout(resolve, 500))
        setDownloading(false)
    }

    const fileMeta = useMemo(() => {
        const { fileType, fileUrl, fileName } = content

        // 1. 优先判断是否为图片
        // 逻辑：MIME类型包含image 或者 后缀名匹配
        const isImage = fileType?.startsWith('image/') ||
            fileUrl?.match(/\.(jpg|jpeg|png|gif|webp|svg)$/i) ||
            fileName?.match(/\.(jpg|jpeg|png|gif|webp|svg)$/i)

        // 2. 扩展判断：是否为压缩包
        const isArchive = fileType?.includes('zip') || fileType?.includes('rar') ||
            fileName?.match(/\.(zip|rar|7z|tar|gz)$/i)

        // 3. 扩展判断：是否为文档/代码
        const isPdf = fileType?.includes('pdf') || fileName?.endsWith('.pdf')

        return { isImage, isArchive, isPdf }
    }, [content])

    // 根据推断结果选择图标
    const getFileIcon = () => {
        if (fileMeta.isImage) return <FileImage size={18} color={token.colorPrimary} />
        if (fileMeta.isArchive) return <FileArchive size={18} color={token.colorWarning} />
        return <FileText size={18} color={token.colorInfo} />
    }


    return (
        <Flex vertical gap={8} style={{ marginTop: 8 }}>
            <Flex
                justify='space-between'
                align='center'
                style={{
                    padding: '8px 12px',
                    background: token.colorFillAlter,
                    borderRadius: token.borderRadius,
                    border: `1px solid ${token.colorBorderSecondary}`,
                    cursor: 'pointer',
                    transition: 'all 0.2s',
                }}
                onClick={(e) => {
                    handleDownload(e, content.fileUrl, content.fileName)
                }}
            >
                <Flex
                    gap={8}
                >
                    {getFileIcon()}
                    <Flex gap={10} align='center'>
                        <Text ellipsis={{ tooltip: content.fileName }} style={{ fontSize: 12, flexShrink: 1 }}>
                            {content.fileName}
                        </Text>
                        <Text type="secondary" style={{ fontSize: 12, flexShrink: 0 }}>
                            [{formatFileSize(content.fileSize)}]
                        </Text>
                    </Flex>
                </Flex>
                <Tooltip title="下载附件">
                    <Button
                        type="text"
                        size="small"
                        icon={<Download size={14} />}
                        loading={downloading}
                        onClick={(e) => {
                            handleDownload(e, content.fileUrl, content.fileName)
                        }}
                    />
                </Tooltip>
            </Flex>
        </Flex>
    )
}

export default FileRenderer