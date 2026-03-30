import { useEffect, useRef, useState, useCallback } from 'react';

/**
 * 
 * @param {string} url - 订阅地址
 * @param {object} options - 回调配置
 */
export const useSse = (url, { onMessage, onConnected } = {}) => {

  const [status, setStatus] = useState('closed'); // connecting, opened, closed
  const sseRef = useRef(null);
  const reconnectTimerRef = useRef(null); // 用于手动重连计时
  const reconnectCountRef = useRef(0);    // 记录连续失败次数，用于退避算法

  // 使用 Ref 确保回调逻辑永远是最新的，且不会触发 connect 重新定义
  const onMessageRef = useRef(onMessage);
  const onConnectedRef = useRef(onConnected);

  useEffect(() => {
    onMessageRef.current = onMessage;
    onConnectedRef.current = onConnected;
  }, [onMessage, onConnected]);

  const connect = useCallback(() => {
    // 清除任何现有的重连定时器
    if (reconnectTimerRef.current) clearTimeout(reconnectTimerRef.current);
    // 幂等处理：如果已有实例，先关闭
    if (sseRef.current) {
      sseRef.current.close();
    }

    console.log('SSE Connecting');
    setStatus('connecting');

    const sse = new EventSource(url);
    sseRef.current = sse;

    // 1. 连接建立成功
    sse.onopen = () => {
      console.log('SSE Connected');
      setStatus('opened');
      reconnectCountRef.current = 0; // 连接成功，重置计数器
      onConnectedRef.current?.();
    };

    // 2. 监听业务自定义事件 (对应后端的 .name("message_event"))
    sse.addEventListener('announcement_event', (e) => {
      try {
        const payload = JSON.parse(e.data);
        onMessageRef.current?.(payload);
      } catch (err) {
        console.error('SSE: Business message parsing failed', err);
      }
    });

    // 3. 错误处理 (包括后端 30s 超时主动断开)
    sse.onerror = () => {
      // 注意：这里不要 close()，也不要手动重连
      // 浏览器会自动根据后端下发的 retry 时间进行重连
      setStatus('closed');
      sse.close(); // 关键：主动关闭旧实例，准备手动开启新实例
      // 指数退避算法：防止后端挂掉时，前端疯狂请求导致浏览器卡顿
      // 延迟时间 = min(1000 * 2^n, 30秒)
      const delay = Math.min(1000 * Math.pow(2, reconnectCountRef.current), 30000);
      console.warn(
        `SSE: Connection lost. Reconnecting (attempt ${reconnectCountRef.current + 1}) in ${delay / 1000}s...`
      );
      reconnectTimerRef.current = setTimeout(() => {
        reconnectCountRef.current += 1;
        connect();
      }, delay);
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