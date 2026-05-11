// 生成随机的 code_verifier
export const generateVerifier = (length = 64) => {
    const charset = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~';
    const values = new Uint32Array(length);
    window.crypto.getRandomValues(values);
    return Array.from(values)
        .map((x) => charset[x % charset.length])
        .join('');
};

//  将 ArrayBuffer 转换为 Base64UrlSafe 字符串
const base64UrlEncode = (buffer) => {
    return btoa(String.fromCharCode(...new Uint8Array(buffer)))
        .replace(/\+/g, '-')
        .replace(/\//g, '_')    
        .replace(/=/g, '');
};

//  生成 code_challenge
export const generateChallenge = async (verifier) => {
    const encoder = new TextEncoder();
    const data = encoder.encode(verifier);
    const hash = await window.crypto.subtle.digest('SHA-256', data);
    return base64UrlEncode(hash);
};