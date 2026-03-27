import { useEffect, useRef, useState, useCallback } from 'react';

/**
 * 纯原生重连 SSE Hooks
 * @param {string} url - 订阅地址
 * @param {object} options - 回调配置
 */
export const useSse = (url, { onMessage, onConnected } = {}) => {
  const [status, setStatus] = useState('closed'); // connecting, opened, closed
  const sseRef = useRef(null);

  // 使用 Ref 确保回调逻辑永远是最新的，且不会触发 connect 重新定义
  const onMessageRef = useRef(onMessage);
  const onConnectedRef = useRef(onConnected);

  useEffect(() => {
    onMessageRef.current = onMessage;
    onConnectedRef.current = onConnected;
  }, [onMessage, onConnected]);

  const connect = useCallback(() => {
    // 幂等处理：如果已有实例，先关闭
    if (sseRef.current) {
      sseRef.current.close();
    }

    console.log('SSE: [Atlas] 开启原生连接监听:', url);
    setStatus('connecting');

    const sse = new EventSource(url);
    sseRef.current = sse;

    // 1. 连接建立成功
    sse.onopen = () => {
      console.log('SSE: [Atlas] 连接已就绪');
      setStatus('opened');
      onConnectedRef.current?.();
    };

    // 2. 监听业务自定义事件 (对应后端的 .name("message_event"))
    sse.addEventListener('message_event', (e) => {
      try {
        const payload = JSON.parse(e.data);
        onMessageRef.current?.(payload);
      } catch (err) {
        console.error('SSE: 业务消息解析失败', err);
      }
    });

    // 3. 错误处理 (包括后端 30s 超时主动断开)
    sse.onerror = () => {
      // 注意：这里不要 close()，也不要手动重连
      // 浏览器会自动根据后端下发的 retry 时间进行重连
      setStatus('closed');
      console.warn('SSE: 连接暂时断开，浏览器正在尝试自动重连...');
    };
  }, [url]);

  useEffect(() => {
    if (!url) return;

    connect();

    // 组件卸载时，必须手动关闭，否则原生重连会在后台永久运行
    return () => {
      if (sseRef.current) {
        console.log('SSE: [Atlas] 组件卸载，关闭连接');
        sseRef.current.close();
        sseRef.current = null;
      }
    };
  }, [url, connect]);

  return { status };
};