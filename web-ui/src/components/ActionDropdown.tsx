import React, { HtmlHTMLAttributes } from 'react'
import { DownOutlined } from '@ant-design/icons';
import { Dropdown, DropdownProps, MenuProps, Modal, ModalFuncProps, theme, Typography } from 'antd'
import { useTranslation } from 'react-i18next';


interface ActionDropdownItem {
    key: string,
    label?: React.ReactNode
    danger?: boolean
    confirm?: {
        title?: string
        content?: React.ReactNode
        okText?: string
        cancelText?: string
        onOk: () => Promise<any> | void
        confirmLoading?: boolean
    }
    onClick?: () => void
}

interface ActionDropdownProps extends DropdownProps{
    items: ActionDropdownItem[]
    triggerText?: string
    disabled?: boolean
}

const ActionDropdown: React.FC<ActionDropdownProps> = ({
    items,
    triggerText = '更多',
    disabled,
}) => {

    const { t } = useTranslation()

    const { token } = theme.useToken()

    const [modal, contextHolder] = Modal.useModal()

    const menuItems: MenuProps['items'] = items.map(item => ({
        key: item.key,
        label: (
            <Typography.Link
                type={item.danger ? 'danger' : undefined}
                style={{ display: 'flex', justifyContent: 'center', width: '100%', color: token.colorLink}}
                onClick={e => {
                    e.stopPropagation()
                    if (item.confirm) {
                        modal.confirm({
                            title: item.confirm.title ?? t('确定操作'),
                            content: item.confirm.content,
                            okText: item.confirm.okText ?? t('确定'),
                            cancelText: item.confirm.cancelText ?? t('取消'),
                            maskClosable: false,
                            confirmLoading: item.confirm.confirmLoading,
                            onOk: item.confirm.onOk,
                        } as ModalFuncProps & { confirmLoading?: boolean })
                    } else if (item.onClick) {
                        item.onClick()
                    }
                }}
            >
                {item.label}
            </Typography.Link>
        ),
    }))
    return (
        <>
            <Dropdown menu={{ items: menuItems }} trigger={['click']} disabled={disabled}>
                <Typography.Link style={{ whiteSpace: 'nowrap' }} onClick={e => e.stopPropagation()}>{t(triggerText)}<DownOutlined /></Typography.Link>
            </Dropdown>
            {contextHolder}
        </>
    )
}

export default ActionDropdown