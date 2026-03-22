import { createSlice } from '@reduxjs/toolkit'


const initialState = {
    authInfo: {
        
    }
}

export const authSlice = createSlice({
    name: 'auth',
    initialState: initialState,
    reducers: {
        reset: () => initialState,
        setAuthInfo: (state, action) => {
            const { payload } = action
            const { authInfo } = payload
            state.authInfo = authInfo
        }
    }
})

export const { reset, setAuthInfo } = authSlice.actions

export default authSlice.reducer