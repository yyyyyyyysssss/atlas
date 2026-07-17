import { configureStore } from '@reduxjs/toolkit'
import authReducer from './slices/authSlice'
import userReducer, { initialState as initialUserState } from './slices/userSlice'
import layoutReducer, { initialState as initialLayoutState } from './slices/layoutSlice'
import { debounce } from 'lodash'

const loadState = () => {
    try {
        const serializedState = localStorage.getItem('state')
        if (serializedState === null) {
            return null
        }
        const persistedData = JSON.parse(serializedState)

        return persistedData
    } catch (e) {
        return null
    }
}

const saveState = (state) => {
    const serializedState = JSON.stringify({
        layoutState: {
            tabItems: state.layout.tabItems,
        },
        userState: {
            // 只持久化主题相关的设置，不持久化用户信息（如 username, avatar 等），确保换号登录时不串数据
            settings: {
                appearance: {
                    theme: state.user.userInfo?.settings?.appearance?.theme,
                    colorPrimary: state.user.userInfo?.settings?.appearance?.colorPrimary,
                    language: state.user.userInfo?.settings?.appearance?.language,
                }
            }
        }
    })
    localStorage.setItem('state', serializedState)
}

const loadedState = loadState() || {}

const { layoutState = {}, userState = {} } = loadedState

const reduxStore = configureStore({
    preloadedState: {
        layout: {
            ...initialLayoutState,
            ...layoutState
        },
        user: {
            ...initialUserState,
            userInfo: {
                ...initialUserState.userInfo,
                settings: {
                    ...initialUserState.userInfo.settings,
                    appearance: {
                        ...initialUserState.userInfo.settings.appearance,
                        ...(userState.settings?.appearance || {})
                    }
                }
            }
        }
    },
    reducer: {
        user: userReducer,
        auth: authReducer,
        layout: layoutReducer
    },
    middleware: (getDefaultMiddleware) => getDefaultMiddleware({
        serializableCheck: false,
    })
})

const debouncedSave = debounce((state) => {
    saveState(state)
}, 1000)

reduxStore.subscribe(() => {
    debouncedSave(reduxStore.getState())
})

export default reduxStore