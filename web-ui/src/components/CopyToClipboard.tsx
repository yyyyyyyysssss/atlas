import { useTranslation } from "react-i18next";
import { getMessageApi } from "../utils/MessageUtil";
import { Button, Flex, theme, Tooltip, Typography } from "antd";
import { CopyOutlined } from '@ant-design/icons';

interface CopyToClipboardProps {
    text: string;  // 要复制的文本
    children: React.ReactNode;
}

const CopyToClipboard: React.FC<CopyToClipboardProps> = ({ text, children }) => {
    const { t } = useTranslation()

    const { token } = theme.useToken()

    const handleCopy = () => {
        navigator.clipboard.writeText(text)
        getMessageApi().success(t('已复制到剪贴板'))
    }

    return (
        <Flex justify='space-between' align='center' style={{ backgroundColor: token.controlItemBgHover, paddingLeft: '10px' }} className='py-2 px-3 rounded-md'>
            <Typography.Text>{children || text}</Typography.Text>
            <Button
                type="text"
                icon={<Tooltip title={t('复制')}><CopyOutlined /></Tooltip>}
                onClick={handleCopy}
            />
        </Flex>
    )
}

export default CopyToClipboard