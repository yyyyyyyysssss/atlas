

class SseManager {

    constructor() {
        this.sse = null;
        this.listeners = new Map(); // Map<eventName, Set<callback>>
        this.statusListeners = new Set();
        this.status = 'closed';
        this.reconnectCount = 0;
        this.reconnectTimer = null;
        this.url = '';
    }

    // 状态管理
    _updateStatus(newStatus) {
        this.status = newStatus;
        this.statusListeners.forEach(cb => cb(newStatus));
    }

    // 初始化连接
    connect(url) {
        if (this.sse) return;
        this.url = url;
        this._doConnect();
    }

    _doConnect() {
        if (this.reconnectTimer) clearTimeout(this.reconnectTimer);

        console.log('SSE: Initializing connection...');
        this._updateStatus('connecting');

        const sse = new EventSource(this.url);
        this.sse = sse;

        sse.onopen = () => {
            console.log('SSE: Connected');
            this._updateStatus('opened');
            this.reconnectCount = 0;
        };

        sse.onerror = () => {
            this._updateStatus('closed');
            this.sse.close();
            this.sse = null;

            // 你的指数退避算法
            const delay = Math.min(1000 * Math.pow(2, this.reconnectCount), 30000);
            console.warn(`SSE: Lost. Reconnecting in ${delay / 1000}s...`);

            this.reconnectTimer = setTimeout(() => {
                this.reconnectCount += 1;
                this._doConnect();
            }, delay);
        };

        // 关键：代理所有订阅的事件
        this.listeners.forEach((_, eventName) => {
            this._bindEvent(eventName);
        });
    }

    _bindEvent(eventName) {
        if (!this.sse) return;
        // 使用 addEventListener 监听特定 type
        this.sse.addEventListener(eventName, (e) => {
            try {
                const callbacks = this.listeners.get(eventName);
                callbacks?.forEach(cb => cb(e.data));
            } catch (err) {
                console.error(`SSE: Parse error for [${eventName}]`, err);
            }
        });
    }

    // 订阅接口
    subscribe(eventName, callback) {
        if (!this.listeners.has(eventName)) {
            this.listeners.set(eventName, new Set());
            // 如果已经连接，立即为新事件绑定监听
            if (this.sse) this._bindEvent(eventName);
        }
        this.listeners.get(eventName).add(callback);
        
        return () => {
            const callbacks = this.listeners.get(eventName);
            callbacks.delete(callback);
            if (callbacks.size === 0) {
                this.listeners.delete(eventName);
            }
        };
    }

    onStatusChange(callback) {
        this.statusListeners.add(callback);
        return () => this.statusListeners.delete(callback);
    }

    destroy() {
        if (this.reconnectTimer) clearTimeout(this.reconnectTimer);
        if (this.sse) {
            this.sse.close();
            this.sse = null;
        }
        this._updateStatus('closed');
    }

}

// 导出单例
export const sseManager = new SseManager();
