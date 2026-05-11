import { createMD5 } from 'hash-wasm';


export const calculateMD5AsFile = (file, chunkSize = 1024 * 1024 * 5) => {

    return new Promise(async (resolve, reject) => {
        const chunks = Math.ceil(file.size / chunkSize)
        let currentChunk = 0
        const fileReader = new FileReader()
        const md5Hash = await createMD5()
        function loadNext() {
            const start = currentChunk * chunkSize
            const end = Math.min(start + chunkSize, file.size)
            fileReader.readAsArrayBuffer(file.slice(start, end))
        }

        fileReader.onload = (e) => {
            // 追加分块数据
            const view = new Uint8Array(e.target.result)
            md5Hash.update(view)
            currentChunk++
            if (currentChunk < chunks) {
                loadNext()
            } else {
                resolve(md5Hash.digest())
            }
        }
        fileReader.onerror = (error) => {
            console.warn('文件计算md5错误: ', error)
            reject(error)
        }
        loadNext()
    })
}