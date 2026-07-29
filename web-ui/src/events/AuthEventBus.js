import { useEffect } from "react";

class AuthEventBus extends EventTarget {

    // 主动登出事件
    emitSignout() {
        this.dispatchEvent(new CustomEvent('signout'))
    }
    onSignout(callback) {
        this.addEventListener('signout', callback)
        // 返回一个取消订阅的函数
        return () => {
            this.removeEventListener('signout', callback)
        }
    }

    // 关闭页签/浏览器事件（不含刷新）
    emitUnload() {
        this.dispatchEvent(new CustomEvent('unload'))
    }
    onUnload(callback) {
        this.addEventListener('unload', callback);
        return () => this.removeEventListener('unload', callback)
    }
}

export const authEventBus = new AuthEventBus()


export function useAuthEvent({ onSignout, onUnload }) {
    useEffect(() => {
        let unsubscribeSignout
        let unsubscribeUnload

        if (onSignout) {
            unsubscribeSignout = authEventBus.onSignout(onSignout)
        }
        if (onUnload) {
            unsubscribeUnload = authEventBus.onUnload(onUnload)
        }

        // 组件卸载时统一取消订阅
        return () => {
            if (unsubscribeSignout) unsubscribeSignout()
            if (unsubscribeUnload) unsubscribeUnload()
        };
    }, [onSignout, onUnload])
}