import React, { createContext, useEffect, useMemo, useState } from 'react';
import { sseManager } from './SseManager';
import { useAuth } from '../router/AuthProvider';
import { useAuthEvent } from '../events/AuthEventBus';

export const SseContext = createContext({ status: 'closed' });


const getClientId = () => {
    let clientId = sessionStorage.getItem('sse_client_id');
    if (!clientId) {
        clientId = 'client_' + Math.random().toString(36).substring(2, 9) + Date.now().toString(36)
        sessionStorage.setItem('sse_client_id', clientId)
    }
    return clientId
}

export const SseProvider = ({ url, children }) => {

    const [status, setStatus] = useState('closed')

    const { accessToken } = useAuth()

    useAuthEvent({
        onSignout: () => sseManager.destroy(),
        onUnload: () => sseManager.destroy(),
    })

    const sseUrl = useMemo(() => {
        if (!accessToken) {
            return null
        }
        const baseUrl = '/api/notification/v1/notification/sse/subscribe'
        const terminal = `web_${getClientId()}`
        return `${baseUrl}?terminal=${terminal}&access_token=${accessToken}`;
    }, [accessToken])

    useEffect(() => {
        if (!sseUrl) {
            console.log('SSE: No URL provided, ensuring connection is closed.');
            sseManager.destroy()
            return
        }
        // 启动连接
        sseManager.connect(sseUrl)

        // 监听状态变化同步到 React
        const unbind = sseManager.onStatusChange((s) => setStatus(s))

        return () => {
            unbind();
            sseManager.destroy();
        };
    }, [sseUrl]);

    return (
        <SseContext.Provider value={{ status }}>
            {children}
        </SseContext.Provider>
    );
};