import { Spin } from 'antd'
import type { ReactNode } from 'react'
import { Navigate, useLocation } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'

const loadingFallback = <Spin style={{ display: 'block', margin: '96px auto' }} />

/** 需要登录才能访问；未登录跳转登录页并携带来源路径 */
export function RequireAuth({ children }: { children: ReactNode }) {
    const token = useAuthStore((s) => s.token)
    const initialized = useAuthStore((s) => s.initialized)
    const location = useLocation()

    if (!initialized) {
        return loadingFallback
    }
    if (!token) {
        return <Navigate to="/login" state={{ from: location.pathname }} replace />
    }
    return <>{children}</>
}

/** 需要 ADMIN 角色才能访问；非管理员跳回前台 */
export function RequireAdmin({ children }: { children: ReactNode }) {
    const user = useAuthStore((s) => s.user)
    if (!user || user.userRole !== 'ADMIN') {
        return <Navigate to="/chat" replace />
    }
    return <>{children}</>
}
