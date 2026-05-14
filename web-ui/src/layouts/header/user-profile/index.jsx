import { useMemo, useRef, useState } from 'react';
import { useAuth } from '../../../router/AuthProvider';
import { App, Avatar, Button, Divider, Drawer, Dropdown, Flex, Form, Image, Input, Modal, Tooltip, Typography, Upload } from 'antd';
import { Lock, LogOut, Pencil, UserPen } from 'lucide-react';
import { logout } from '../../../services/LoginService';
import { useDispatch, useSelector } from 'react-redux';
import { changePassword } from '../../../services/UserProfileService';
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

    const [password, setPassword] = useState('')

    const [changePasswordOpen, setChangePasswordOpen] = useState(false)

    const handleChangePassword = () => {
        form.validateFields()
            .then(
                (values) => {
                    const { originPassword, newPassword, confirmNewPassword } = values
                    if (newPassword !== confirmNewPassword) {
                        message.warning('两次输入密码不一致')
                        return
                    }
                    const req = {
                        originPassword: originPassword,
                        newPassword: newPassword
                    }
                    changePassword(req)
                        .then(
                            () => {
                                message.success('修改成功')
                                handleClose()
                            }
                        )
                }
            )
    }

    const handleClose = () => {
        setChangePasswordOpen(false)
        form.resetFields()
        setPassword('')
    }

    const getPasswordStrength = (password) => {
        let score = 0
        if (password.length >= 8) score++
        if (/[a-z]/.test(password)) score++
        if (/[A-Z]/.test(password)) score++
        if (/\d/.test(password)) score++
        if (/[\W_]/.test(password)) score++

        if (score <= 2) return 'weak'
        if (score === 3 || score === 4) return 'medium'
        return 'strong'
    }

    const strength = useMemo(() => getPasswordStrength(password), [password])

    const strengthColorMap = {
        weak: { color: 'red', label: '弱' },
        medium: { color: 'orange', label: '中' },
        strong: { color: 'green', label: '强' },
    }




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
                            key: 'profile',
                            label: (
                                <Typography.Link onClick={() => navigate('/account/settings')}>
                                    <Flex
                                        gap={8}
                                        align='center'
                                    >
                                        <UserPen size={16} />
                                        <Typography.Text className='user-profile-menu-label'>{t('个人中心')}</Typography.Text>
                                    </Flex>
                                </Typography.Link>

                            )
                        },
                        {
                            key: 'change_password',
                            label: (
                                <Typography.Link onClick={() => setChangePasswordOpen(true)}>
                                    <Flex
                                        gap={8}
                                        align='center'
                                    >
                                        <Lock size={16} />
                                        <Typography.Text
                                            className='user-profile-menu-label'
                                        >
                                            {t('修改密码')}
                                        </Typography.Text>
                                    </Flex>

                                </Typography.Link>
                            )
                        },
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
            <Modal
                title={t('修改密码')}
                width={400}
                open={changePasswordOpen}
                onOk={handleChangePassword}
                onCancel={handleClose}
                onClose={handleClose}
                okText={t('确认修改')}
                destroyOnHidden
            >
                <Form
                    style={{ marginTop: 20 }}
                    form={form}
                    labelCol={{ span: 7 }}
                    wrapperCol={{ span: 17 }}
                    layout="horizontal"
                >
                    <Form.Item
                        label="原密码"
                        name="originPassword"
                        rules={[
                            {
                                required: true,
                                message: `原密码不能为空`,
                            },
                        ]}
                    >
                        <Input.Password placeholder="请输入原密码" />
                    </Form.Item>
                    <Form.Item
                        label="新密码"
                        name="newPassword"
                        validateTrigger='onBlur'
                        rules={[
                            {
                                required: true,
                                message: `新密码不能为空`,
                            },
                            {
                                min: 6,
                                message: '密码长度不能少于6位',
                            }
                        ]}
                    >
                        <Input.Password
                            placeholder="请输入新密码"
                            onChange={(e) => {
                                const value = e.target.value
                                setPassword(value)
                            }}
                        />
                    </Form.Item>
                    {password && (
                        <div style={{ marginLeft: '30%', marginTop: -12, marginBottom: 12 }}>
                            <span style={{ color: strengthColorMap[strength].color, fontWeight: 500 }}>
                                {t('密码强度')}：{t(strengthColorMap[strength].label)}
                            </span>
                        </div>
                    )}
                    <Form.Item
                        label="确认新密码"
                        name="confirmNewPassword"
                        rules={[
                            {
                                required: true,
                                message: `确认新密码不能为空`,
                            },
                        ]}
                    >
                        <Input.Password placeholder="请输入确认新密码" />
                    </Form.Item>
                </Form>
            </Modal>
        </>
    )
}

export default UserProfile