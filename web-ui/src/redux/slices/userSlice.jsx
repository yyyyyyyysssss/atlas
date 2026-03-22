import { createSlice } from '@reduxjs/toolkit'


const initialState = {
    userInfo: {
        settings: {
            workbench: {
                shortcuts: []
            }
        }
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
        }
    }
})

export const { reset, setUserInfo, updateShortcuts } = userSlice.actions

export default userSlice.reducer