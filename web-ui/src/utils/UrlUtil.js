

export const urlParamParse = (urlParams) => {
    var regex = /(?:[?&]|^)([^=#?]+)=([^&#]*)/g,
        p = {},
        match;
    while ((match = regex.exec(decodeURIComponent(urlParams)))) {
        p[match[1]] = match[2];
    }
    return p;
}