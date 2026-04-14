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
            tabItems: state.layout.tabItems
        }
    })
    localStorage.setItem('state', serializedState)
}

const loadedState = loadState() || {}

const { layoutState = {} } = loadedState

const reduxStore = configureStore({
    preloadedState: {
        layout: {
            ...initialLayoutState,
            ...layoutState
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