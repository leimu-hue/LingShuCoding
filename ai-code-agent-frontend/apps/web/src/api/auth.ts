import { http } from '@ai-code-agent/shared'
import type { LoginResult, UserIdentity } from '../types/user'

export function login(username: string, password: string) {
    return http<LoginResult>({ url: '/auth/login', method: 'POST', data: { username, password } })
}

export function register(username: string, password: string, nickname?: string) {
    return http<UserIdentity>({
        url: '/auth/register',
        method: 'POST',
        data: { username, password, nickname },
    })
}

export function logout() {
    return http<void>({ url: '/auth/logout', method: 'POST' })
}

export function me() {
    return http<UserIdentity>({ url: '/auth/me', method: 'GET' })
}
