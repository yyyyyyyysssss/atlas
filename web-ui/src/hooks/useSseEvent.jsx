import { useEffect, useRef } from 'react';
import { sseManager } from '../sse/SseManager';

/**
 * 订阅特定 SSE 事件
 * @param {string} eventName 事件名称 (对应后端 .name())
 * @param {function} onMessage 回调函数
 */
export const useSseEvent = (eventName, onMessage) => {
  const onMessageRef = useRef(onMessage);

  // 保证回调永远是最新的，避免闭包问题
  useEffect(() => {
    onMessageRef.current = onMessage;
  }, [onMessage]);

  useEffect(() => {
    if (!eventName) return;

    // 订阅管理器的事件
    const unsubscribe = sseManager.subscribe(eventName, (data) => {
      onMessageRef.current?.(data);
    });

    return () => {
      unsubscribe();
    };
  }, [eventName]);
};