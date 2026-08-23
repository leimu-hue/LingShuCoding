import { useEffect } from 'react'
import { App as AntdApp, ConfigProvider, Spin } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import { RouterProvider } from 'react-router-dom'
import { onApiError, onSessionExpired } from '@ai-code-agent/shared'
import { router } from './router'
import { useAuthStore } from './store/authStore'

/** 订阅全局 API 错误并转为 antd 提示（必须是 <AntdApp> 的子组件才能使用 useApp） */
function ErrorBridge() {
    const { message } = AntdApp.useApp()
    useEffect(() => onApiError((msg) => message.error(msg)), [message])
    return null
}

/**
 * 订阅会话过期（401）事件：清理凭证并提示一次，随后由路由守卫 RequireAuth 跳转登录。
 * 通过「仅当仍持有 token 时才处理」实现去重——并发请求同时 401 时只有第一个触发清理，
 * 其余请求看到 token 已为空则跳过，从而避免「多次未授权报错 + 延迟跳转」。
 */
function SessionExpiredBridge() {
    const { message } = AntdApp.useApp()
    const clearSession = useAuthStore((s) => s.clearSession)

    useEffect(
        () =>
            onSessionExpired(() => {
                if (!useAuthStore.getState().token) {
                    return
                }
                clearSession()
                message.error('登录已过期，请重新登录')
            }),
        [clearSession, message],
    )

    return null
}

export default function App() {
    const initialized = useAuthStore((s) => s.initialized)
    const init = useAuthStore((s) => s.init)

    useEffect(() => {
        void init()
    }, [init])

    return (
        <ConfigProvider locale={zhCN} theme={{ token: { colorPrimary: '#1677ff' }, cssVar: {} }}>
            <AntdApp>
                <ErrorBridge />
                <SessionExpiredBridge />
                {initialized ? (
                    <RouterProvider router={router} />
                ) : (
                    <Spin style={{ display: 'block', margin: '120px auto' }} />
                )}
            </AntdApp>
        </ConfigProvider>
    )
}
