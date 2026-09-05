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
    emitAuthUnload() {
        this.dispatchEvent(new CustomEvent('authUnload'))
    }
    onAuthUnload(callback) {
        this.addEventListener('authUnload', callback);
        return () => this.removeEventListener('authUnload', callback)
    }
}

export const authEventBus = new AuthEventBus()


export function useAuthEvent({ onSignout, onAuthUnload }) {
    useEffect(() => {
        let unsubscribeSignout
        let unsubscribeAuthUnload

        if (onSignout) {
            unsubscribeSignout = authEventBus.onSignout(onSignout)
        }
        if (onAuthUnload) {
            unsubscribeAuthUnload = authEventBus.onAuthUnload(onAuthUnload)
        }

        // 组件卸载时统一取消订阅
        return () => {
            if (unsubscribeSignout) unsubscribeSignout()
            if (unsubscribeAuthUnload) unsubscribeAuthUnload()
        };
    }, [onSignout, onAuthUnload])
}