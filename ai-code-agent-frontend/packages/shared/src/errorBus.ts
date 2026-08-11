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
