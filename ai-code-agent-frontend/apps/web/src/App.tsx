import { useEffect } from 'react'
import { App as AntdApp, ConfigProvider, Spin } from 'antd'
import zhCN from 'antd/locale/zh_CN'
import { RouterProvider } from 'react-router-dom'
import { onApiError } from '@ai-code-agent/shared'
import { router } from './router'
import { useAuthStore } from './store/authStore'

/** 订阅全局 API 错误并转为 antd 提示（必须是 <AntdApp> 的子组件才能使用 useApp） */
function ErrorBridge() {
    const { message } = AntdApp.useApp()
    useEffect(() => onApiError((msg) => message.error(msg)), [message])
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
                {initialized ? (
                    <RouterProvider router={router} />
                ) : (
                    <Spin style={{ display: 'block', margin: '120px auto' }} />
                )}
            </AntdApp>
        </ConfigProvider>
    )
}
