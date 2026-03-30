import React, { createContext, useEffect, useState } from 'react';
import { sseManager } from './SseManager';

export const SseContext = createContext({ status: 'closed' });

export const SseProvider = ({ url, children }) => {
    const [status, setStatus] = useState('closed');

    useEffect(() => {
        if (!url) {
            console.log('SSE: No URL provided, ensuring connection is closed.');
            sseManager.destroy();
            return;
        }

        // 启动连接
        sseManager.connect(url);

        // 监听状态变化同步到 React
        const unbind = sseManager.onStatusChange((s) => setStatus(s));

        return () => {
            unbind();
            sseManager.destroy();
        };
    }, [url]);

    return (
        <SseContext.Provider value={{ status }}>
            {children}
        </SseContext.Provider>
    );
};