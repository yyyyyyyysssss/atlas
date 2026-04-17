import { createSlice } from '@reduxjs/toolkit'
import { DEFAULT_PRIMARY_COLOR } from '../../layouts/header/theme-color'


export const initialState = {
    userInfo: {
        settings: {
            workbench: {
                shortcuts: []
            },
            appearance: {
                theme: 'dark',
                colorPrimary: DEFAULT_PRIMARY_COLOR,
                language: 'zh'
            }
        },
        todayTaskCount: 0,
        notificationUnreadCount: 0
    }
}


export const userSlice = createSlice({
    name: 'user',
    initialState: initialState,
    reducers: {
        reset: () => initialState,
        setUserInfo: (state, action) => {
            const { payload } = action
            const { userInfo } = payload
            state.userInfo = userInfo
        },
        updateShortcuts: (state, action) => {
            const shortcuts = action.payload
            if (!state.userInfo.settings) {
                state.userInfo.settings = {}
            }
            if (!state.userInfo.settings.workbench) {
                state.userInfo.settings.workbench = {}
            }
            state.userInfo.settings.workbench.shortcuts = shortcuts
        },
        switchTheme: (state, action) => {
            const { payload } = action
            const { theme } = payload
            state.userInfo.settings.appearance.theme = theme
        },
        switchColorPrimary: (state, action) => {
            const { payload } = action
            const { colorPrimary } = payload
            state.userInfo.settings.appearance.colorPrimary = colorPrimary
        },
        switchLanguage: (state, action) => {
            const { payload } = action
            const { language } = payload
            state.userInfo.settings.appearance.language = language
        },
        setTodayTaskCount: (state, action) => {
            const { payload } = action
            const { todayTaskCount } = payload
            state.userInfo.todayTaskCount = todayTaskCount
        },
        setNotificationUnreadCount: (state, action) => {
            const { payload } = action
            const { notificationUnreadCount } = payload
            state.userInfo.notificationUnreadCount = notificationUnreadCount
        },
    }
})

export const { reset, setUserInfo, updateShortcuts, switchTheme, switchColorPrimary, switchLanguage, setTodayTaskCount, setNotificationUnreadCount } = userSlice.actions

export default userSlice.reducer