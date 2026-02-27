

export const apiRequestWrapper = async (requestFn, { raw } = {}) => {
    try {
        const res = await requestFn()
        return raw ? res : res?.data
    } catch (err) {
        handleError(err)
        throw err
    }
}

const handleError = (error) => {
    // 这里可以加入更多的错误处理逻辑，比如发送错误日志、显示错误信息等
}