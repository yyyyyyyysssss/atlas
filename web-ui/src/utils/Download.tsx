import { downloadByUrl } from "../services/FileService"


export interface DownloadOptions {
    url: string
    filename?: string
    headers?: Record<string, string>
    onProgress?: (percent: number) => void
}


export const downloadFile = async ({
    url,
    filename
}: DownloadOptions): Promise<void> => {
    const downloadUrl = url.includes('?') ? `${url}&type=d` : `${url}?type=d`
    triggerDownload(downloadUrl, filename || extractFileName(url))
}

export const downloadFileUsingXHR = async ({
    url,
    filename,
    onProgress,
}: DownloadOptions): Promise<void> => {
    const response = await downloadByUrl(url, onProgress)
    if (response.status !== 200) {
        throw new Error(`下载失败: ${response.statusText}`)
    }
    if (!filename) {
        filename = extractFileName(url)
    }
    const contentType = response.headers['content-type']
    const fileExtension = getFileExtensionFromContentType(contentType)
    if (!filename.includes('.')) {
        filename = `${filename}.${fileExtension}`;
    }
    const blob = await response.data
    const objectUrl = URL.createObjectURL(blob)
    triggerDownload(objectUrl, filename)
    URL.revokeObjectURL(objectUrl)
}

/**
 * 通用的 Blob 下载方法
 * @param blob 文件的 Blob 对象
 * @param filename 下载的文件名（带后缀）
 */
export const downloadFileByBlob = (blob: Blob, filename: string): void => {
    // 1. 创建 Object URL
    const objectUrl = URL.createObjectURL(blob);
    
    // 2. 调用你现有的触发器
    triggerDownload(objectUrl, filename);
}

const triggerDownload = (url: string, filename: string) => {
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    a.style.display = 'none'
    document.body.appendChild(a)
    a.click()
    document.body.removeChild(a)
}

const extractFileName = (url: string): string => {
    const fileName = url?.split('/').pop()?.split('?')[0]
    if (fileName) {
        return fileName
    } else {
        // 使用当前时间拼接文件名
        const currentTime = new Date().toISOString()
        return `file-${currentTime}`
    }
}

const getFileExtensionFromContentType = (contentType: string): string => {
    const extensionMap: { [key: string]: string } = {
        'image/png': 'png',
        'image/jpeg': 'jpg',
        'image/jpg': 'jpg',
        'image/gif': 'gif',
        'application/pdf': 'pdf',
        'application/msword': 'doc',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document': 'docx',
        'application/vnd.ms-excel': 'xls',
        'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': 'xlsx',
        'application/zip': 'zip',
        'application/octet-stream': 'bin',
        'text/plain': 'txt',
        'video/mp4': 'mp4',
        'audio/x-hx-aac-adts': 'aac'
    };

    return extensionMap[contentType] || 'bin'; // 默认 'bin' 扩展名
};