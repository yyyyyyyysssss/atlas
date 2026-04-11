import './index.css'
import { Segmented } from 'antd';
import {
    MoonOutlined,
    SunOutlined,
} from '@ant-design/icons';
import { useDispatch, useSelector } from 'react-redux';
import { useRequest } from 'ahooks';
import { changeAppearance } from '../../../services/UserProfileService';
import { switchTheme } from '../../../redux/slices/userSlice';
import { motion, AnimatePresence } from 'framer-motion';
import { useState } from 'react';

const ThemeSwitch = () => {

    const themeValue = useSelector(state => state.user.userInfo?.settings?.appearance?.theme || 'dark')

    const dispatch = useDispatch()

    const { runAsync: changeAppearanceAsync, loading: changeAppearanceLoading } = useRequest(changeAppearance, {
        manual: true
    })

    const handleSwitchTheme = (newTheme) => {
        if (newTheme === themeValue) {
            return
        }
        dispatch(switchTheme({ theme: newTheme }))
        changeAppearanceAsync({
            theme: newTheme
        })
    }


    return (
        <>
            <Segmented
                size='middle'
                shape="round"
                value={themeValue}
                onChange={handleSwitchTheme}
                options={[
                    { value: 'light', icon: <SunOutlined /> },
                    { value: 'dark', icon: <MoonOutlined /> },
                ]}
            />
        </>

    )
}

export default ThemeSwitch

