const TOKEN_KEY = 'ai_code_agent_token'

export function getAuthToken(): string | null {
    try {
        return localStorage.getItem(TOKEN_KEY)
    } catch {
        return null
    }
}

export function setAuthToken(token: string | null): void {
    try {
        if (token) {
            localStorage.setItem(TOKEN_KEY, token)
        } else {
            localStorage.removeItem(TOKEN_KEY)
        }
    } catch {
        // 忽略存储不可用的环境（如隐私模式）
    }
}

/** 统一获取鉴权请求头，axios 拦截器与 SSE 共用，避免重复逻辑 */
export function authHeaders(): Record<string, string> {
    const token = getAuthToken()
    return token ? { Authorization: `Bearer ${token}` } : {}
}
