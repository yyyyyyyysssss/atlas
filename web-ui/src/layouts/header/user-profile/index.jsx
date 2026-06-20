import { useMemo, useRef, useState } from 'react';
import { useAuth } from '../../../router/AuthProvider';
import { App, Avatar, Button, Divider, Drawer, Dropdown, Flex, Form, Image, Input, Modal, Tooltip, Typography, Upload } from 'antd';
import { Lock, LogOut, Pencil, UserPen } from 'lucide-react';
import { logout } from '../../../services/LoginService';
import { useDispatch, useSelector } from 'react-redux';
import { useRequest } from 'ahooks';
import './index.css'
import { useTranslation } from 'react-i18next'
import { useNavigate } from 'react-router-dom';

const UserProfile = () => {

    const { t } = useTranslation()

    const { signout } = useAuth()

    const navigate = useNavigate()

    const { fullName, avatar } = useSelector(state => state.user.userInfo)

    const language = useSelector(state => state.user.userInfo?.settings?.appearance?.language || 'zh')

    const [profileForm] = Form.useForm()

    const { message } = App.useApp()

    const dispatch = useDispatch()

    const [form] = Form.useForm()




    const handleLogout = async () => {
        await logout()
        await signout()
        navigate('/login')
    }

    return (
        <>
            <Dropdown
                menu={{
                    items: [
                        {
                            key: 'logout',
                            label: (
                                <Typography.Link onClick={handleLogout}>
                                    <Flex
                                        gap={8}
                                        align='center'
                                    >
                                        <LogOut size={16} />
                                        <Typography.Text
                                            className='user-profile-menu-label'
                                        >
                                            {language === 'zh' ? '退出' : t('登出')}
                                        </Typography.Text>
                                    </Flex>
                                </Typography.Link>
                            )
                        }
                    ]
                }}
                trigger={['hover']}
            >
                <Flex
                    gap={5}
                    justify='center'
                    align='center'
                    style={{
                        cursor: 'pointer',
                        padding: '8px',
                        borderRadius: 'var(--ant-border-radius)',
                        userSelect: 'none',
                        maxWidth: '180px'
                    }}
                    onMouseEnter={(e) => e.target.style.backgroundColor = 'var(--ant-control-item-bg-hover)'}
                    onMouseLeave={(e) => e.target.style.backgroundColor = 'transparent'}
                >
                    <Avatar src={avatar} />
                    <Typography.Text type='secondary'>
                        {fullName}
                    </Typography.Text>
                </Flex>
            </Dropdown>
        </>
    )
}

export default UserProfile