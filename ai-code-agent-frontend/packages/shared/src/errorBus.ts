export type ApiErrorHandler = (message: string) => void

const handlers = new Set<ApiErrorHandler>()

/** 订阅全局 API 错误，返回取消订阅函数。由 App 层注册为 UI 提示（如 antd message）。 */
export function onApiError(handler: ApiErrorHandler): () => void {
    handlers.add(handler)
    return () => {
        handlers.delete(handler)
    }
}

/** 上报一个 API 错误，触发所有订阅者 */
export function reportApiError(message: string): void {
    handlers.forEach((handler) => handler(message))
}

export type SessionExpiredHandler = () => void

const sessionExpiredHandlers = new Set<SessionExpiredHandler>()

/**
 * 订阅会话过期（HTTP 401）事件，返回取消订阅函数。
 * 由 App 层注册为「清理凭证 + 跳转登录 + 提示一次」，具体去重逻辑由订阅方实现。
 */
export function onSessionExpired(handler: SessionExpiredHandler): () => void {
    sessionExpiredHandlers.add(handler)
    return () => {
        sessionExpiredHandlers.delete(handler)
    }
}

/** 上报一次会话过期（401），触发所有订阅者 */
export function reportSessionExpired(): void {
    sessionExpiredHandlers.forEach((handler) => handler())
}
