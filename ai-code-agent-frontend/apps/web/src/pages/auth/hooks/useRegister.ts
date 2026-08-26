import { App } from 'antd'
import { useCallback, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '@/store/authStore'

export interface RegisterForm {
    username: string
    nickname?: string
    password: string
    confirm: string
}

/**
 * 注册表单提交逻辑 hook。
 * 封装：提交 loading 状态、注册调用（成功后自动登录）与失败兜底。
 */
export function useRegister() {
    const { message } = App.useApp()
    const register = useAuthStore((s) => s.register)
    const navigate = useNavigate()
    const [loading, setLoading] = useState(false)

    const onFinish = useCallback(
        async (values: RegisterForm) => {
            setLoading(true)
            try {
                await register(values.username, values.password, values.nickname)
                message.success('注册成功')
                navigate('/chat', { replace: true })
            } catch {
                // 错误已由全局 message 提示
            } finally {
                setLoading(false)
            }
        },
        [register, message, navigate],
    )

    return { loading, onFinish }
}
