import { create } from 'zustand'
import { getAuthToken, setAuthToken } from '@ai-code-agent/shared'
import * as authApi from '../api/auth'
import type { UserIdentity } from '../types/user'

interface AuthState {
    token: string | null
    user: UserIdentity | null
    /** 是否已完成首屏鉴权初始化（用于路由守卫前的加载门） */
    initialized: boolean
    init: () => Promise<void>
    login: (username: string, password: string) => Promise<void>
    register: (username: string, password: string, nickname?: string) => Promise<void>
    logout: () => Promise<void>
    /** 仅清理本地凭证与状态（不调用后端），用于会话过期时强制下线 */
    clearSession: () => void
}

export const useAuthStore = create<AuthState>()((set, get) => ({
    token: getAuthToken(),
    user: null,
    initialized: false,

    init: async () => {
        if (!getAuthToken()) {
            set({ initialized: true })
            return
        }
        try {
            const user = await authApi.me()
            set({ user, initialized: true })
        } catch {
            // token 失效：清理本地凭证并视为未登录
            setAuthToken(null)
            set({ token: null, user: null, initialized: true })
        }
    },

    login: async (username, password) => {
        const { token, user } = await authApi.login(username, password)
        setAuthToken(token)
        set({ token, user })
    },

    register: async (username, password, nickname) => {
        await authApi.register(username, password, nickname)
        // 注册接口不返回 token，注册成功后自动登录
        await get().login(username, password)
    },

    logout: async () => {
        try {
            await authApi.logout()
        } catch {
            // 忽略注销接口失败，本地状态照常清理
        }
        setAuthToken(null)
        set({ token: null, user: null })
    },

    clearSession: () => {
        setAuthToken(null)
        set({ token: null, user: null })
    },
}))
