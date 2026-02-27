import { Tooltip, Typography } from "antd";

interface EllipsisTooltipTextProps {
    text: string        // 文本内容
    
}

const EllipsisTooltipText: React.FC<EllipsisTooltipTextProps> = ({ text }) => {
    return (
        <Tooltip title={text}>
            <Typography.Text style={{ display: 'grid', placeItems: 'stretch' }}>
                <Typography.Text style={{ whiteSpace: 'nowrap', textOverflow: 'ellipsis', overflow: 'hidden' }}>
                    {text}
                </Typography.Text>
            </Typography.Text>
        </Tooltip>
    )
}

export default EllipsisTooltipText
