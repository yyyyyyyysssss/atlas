let globalSignout = () => {}

export const setGlobalSignout = (signoutFn) => {
    globalSignout = signoutFn
}

export const useGlobalSignout = () => globalSignout

