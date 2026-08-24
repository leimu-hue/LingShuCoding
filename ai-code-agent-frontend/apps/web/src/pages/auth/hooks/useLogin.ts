import { App } from 'antd'
import { useCallback, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../../store/authStore'

export interface LoginForm {
    username: string
    password: string
}

/**
 * 登录表单提交逻辑 hook。
 * 封装：提交 loading 状态、鉴权调用、成功跳转与失败兜底（错误已由全局 message 提示）。
 */
export function useLogin() {
    const { message } = App.useApp()
    const login = useAuthStore((s) => s.login)
    const navigate = useNavigate()
    const location = useLocation()
    const [loading, setLoading] = useState(false)

    const from = (location.state as { from?: string } | null)?.from ?? '/chat'

    const onFinish = useCallback(
        async (values: LoginForm) => {
            setLoading(true)
            try {
                await login(values.username, values.password)
                message.success('登录成功')
                navigate(from, { replace: true })
            } catch {
                // 错误已由全局 message 提示
            } finally {
                setLoading(false)
            }
        },
        [login, message, navigate, from],
    )

    return { loading, onFinish }
}
