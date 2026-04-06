import React from 'react';
import { Flex, Typography, Button, theme, Tooltip } from 'antd';
import { FileImage, FileText, Download, FileArchive, FileCode } from 'lucide-react';

const { Paragraph } = Typography;


const TextRenderer = React.memo(({ content, closeDrawer, onActionClick }) => {

    return (
        <Paragraph
            type="secondary"
            ellipsis={{ rows: 2 }}
            style={{
                fontSize: 13,
                marginTop: 4,
                marginBottom: 0,
                whiteSpace: 'pre-wrap'
            }}
        >
            {content}
        </Paragraph>
    )
}, (prev, next) => {
    return prev.content === next.content && prev.onActionClick === next.onActionClick
})

export default TextRenderer