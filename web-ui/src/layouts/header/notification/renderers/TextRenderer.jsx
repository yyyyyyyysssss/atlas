import React from 'react';
import { Flex, Typography, Button, theme, Tooltip } from 'antd';
import { FileImage, FileText, Download, FileArchive, FileCode } from 'lucide-react';

const { Paragraph } = Typography;


const TextRenderer = ({ content, mode = 'list' }) => {

    return (
        <Paragraph
            type="secondary"
            ellipsis={mode === 'list' ? { rows: 2 } : false}
            style={{
                fontSize: 13,
                marginTop: 4,
                marginBottom: 0,
                whiteSpace: mode === 'detail' ? 'pre-wrap' : 'normal'
            }}
        >
            {content}
        </Paragraph>
    )
}

export default TextRenderer