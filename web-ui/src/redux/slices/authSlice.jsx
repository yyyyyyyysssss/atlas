import { createSlice } from '@reduxjs/toolkit'


const initialState = {
    userInfo: {
        
    }
}

export const authSlice = createSlice({
    name: 'auth',
    initialState: initialState,
    reducers: {
        reset: () => initialState,
        setUserInfo: (state, action) => {
            const { payload } = action
            const { userInfo } = payload
            state.userInfo = userInfo
        },
        updateUserAvatar: (state, action) => {
            const { payload } = action
            const { newAvatar } = payload
            state.userInfo.avatar = newAvatar
        }
    }
})

export const { reset, setUserInfo, updateUserAvatar } = authSlice.actions

export default authSlice.reducer