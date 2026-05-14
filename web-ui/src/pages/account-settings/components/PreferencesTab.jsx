import React, { useEffect, useState } from 'react';
import { Form, Typography, theme, Flex, Divider, Select, Segmented, Switch, Space, Tooltip, Skeleton } from 'antd';
import { MonitorOutlined, CheckOutlined } from '@ant-design/icons';
import { useDispatch, useSelector } from 'react-redux';
import { COLOR_PRIMARY_OPTIONS, DEFAULT_PRIMARY_COLOR } from '../../../layouts/header/theme-color';
import { changeAppearance, changeNotificationSetting } from '../../../services/UserProfileService';
import { useRequest } from 'ahooks';
import { switchLanguage, switchTheme, switchColorPrimary, setNotificationSetting } from '../../../redux/slices/userSlice';
import { useTranslation } from 'react-i18next';
import { fetchDictByCode } from '../../../services/SystemService';

const { Title, Text, Paragraph } = Typography;

const PreferencesTab = () => {
    const { token } = theme.useToken();

    const language = useSelector(state => state.user.userInfo?.settings?.appearance?.language || 'zh')
    const themeValue = useSelector(state => state.user.userInfo?.settings?.appearance?.theme || 'light')
    const colorPrimary = useSelector(state => state.user.userInfo?.settings?.appearance?.colorPrimary || DEFAULT_PRIMARY_COLOR)
    const notificationSetting = useSelector(state => state.user.userInfo?.settings?.notification)

    const dispatch = useDispatch()

    const { i18n } = useTranslation()

    const { runAsync: changeAppearanceAsync, loading: changeAppearanceLoading } = useRequest(changeAppearance, {
        manual: true
    })

    const { runAsync: changeNotificationSettingAsync, loading: changeNotificationSettingLoading } = useRequest(changeNotificationSetting, {
        manual: true
    })

    const [notificationTypes, setNotificationTypes] = useState([])

    const { runAsync: notificationDictAsync, loading: notificationDictLoading } = useRequest(() => fetchDictByCode('NOTIFICATION_TYPE'), {
        manual: true,
        onSuccess: (res) => {
            if (res && res.length > 0) {
                setNotificationTypes(res)
            }
        }
    })

    useEffect(() => {
        notificationDictAsync()
    }, [])


    const handleUpdate = (key, value) => {
        if (key === 'theme') {
            dispatch(switchTheme({ theme: value }))
            changeAppearanceAsync({
                theme: value
            })
            return
        }

        if (key === 'language') {
            dispatch(switchLanguage({ language: value }))
            i18n.changeLanguage(value)
            changeAppearanceAsync({
                language: value
            })
            return
        }

        if (key === 'colorPrimary') {
            dispatch(switchColorPrimary({ colorPrimary: value }))
            changeAppearanceAsync({
                colorPrimary: value
            })
            return
        }

        if (key = 'notification') {
            const { type, checked } = value
            const updatedSettings = {
                ...notificationSetting,
                [type]: checked
            }
            dispatch(setNotificationSetting({notificationSetting: updatedSettings}))
            changeNotificationSettingAsync(updatedSettings)
        }
    }

    return (
        <div style={{ width: '100%' }}>
            <Title level={4} style={{ marginBottom: 8, color: token.colorTextHeading }}>系统偏好</Title>
            <Paragraph type="secondary" style={{ marginBottom: 24 }}>
                自定义 Atlas 的外观、语言及通知接收方式。
            </Paragraph>
            <Divider style={{ borderColor: token.colorBorderSecondary }} />

            <Form layout="vertical">
                <Form.Item label="语言区域 (Language)">
                    <Select
                        size="large"
                        defaultValue="zh"
                        value={language}
                        onChange={(val) => handleUpdate('language', val)}
                        options={[
                            { value: 'zh', label: '简体中文' },
                            { value: 'en', label: 'English (US)' },
                        ]}
                    />
                </Form.Item>
                <Form.Item label="主题外观 (Theme)">
                    <Segmented
                        size="large"
                        value={themeValue}
                        onChange={(val) => handleUpdate('theme', val)}
                        options={[
                            { label: '浅色模式', value: 'light' },
                            { label: '深色模式', value: 'dark' },
                        ]}
                    />
                </Form.Item>
                <Form.Item label="主题色 (Primary Color)">
                    <Space size="middle" wrap>
                        {COLOR_PRIMARY_OPTIONS.map(item => (
                            <Tooltip key={item.color} title={item.label}>
                                <div
                                    onClick={() => handleUpdate('colorPrimary', item.color)}
                                    style={{
                                        width: 32,
                                        height: 32,
                                        borderRadius: '50%',
                                        backgroundColor: item.color,
                                        cursor: 'pointer',
                                        display: 'flex',
                                        justifyContent: 'center',
                                        alignItems: 'center',
                                        transition: 'transform 0.2s',
                                        boxShadow: colorPrimary === item.color ? `0 0 0 2px ${token.colorBgContainer}, 0 0 0 4px ${item.color}` : 'none',
                                    }}
                                    onMouseEnter={(e) => {
                                        if (colorPrimary !== item.color) e.currentTarget.style.transform = 'scale(1.1)'
                                    }}
                                    onMouseLeave={(e) => {
                                        if (colorPrimary !== item.color) e.currentTarget.style.transform = 'scale(1)'
                                    }}
                                >
                                    {colorPrimary === item.color && <CheckOutlined style={{ color: '#fff', fontSize: 16 }} />}
                                </div>
                            </Tooltip>
                        ))}
                    </Space>
                </Form.Item>
                <Divider style={{ borderColor: token.colorBorderSecondary }} />

                <Title level={4} style={{ marginBottom: 16 }}>通知接收设置</Title>

                {notificationDictLoading ? (
                    <Skeleton active paragraph={{ rows: 2 }} />
                ) : (
                    notificationTypes.map((type, index) => (
                        <Flex
                            key={type.value}
                            justify="space-between"
                            align="center"
                            style={{ marginBottom: index === notificationTypes.length - 1 ? 0 : 24 }}
                        >
                            <div>
                                <Text strong style={{ display: 'block', fontSize: 16 }}>{type.label}</Text>
                                <Text type="secondary">{type.description || '暂无描述'}</Text>
                            </div>
                            <Switch
                                value={notificationSetting?.[type.value] ?? true}
                                onChange={(checked) => handleUpdate('notification', { type: type.value, checked: checked })}
                            />
                        </Flex>
                    ))
                )}
            </Form>
        </div>
    );
}

export default PreferencesTab;