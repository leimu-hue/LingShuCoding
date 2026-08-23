import { http } from '@ai-code-agent/shared'
import type { PageResult, UserAdminDTO, UserRole } from '../types/user'

export interface UserPageParams {
    page?: number
    size?: number
    keyword?: string
    status?: number
    userRole?: UserRole
}

export function listUsers(params: UserPageParams) {
    return http<PageResult<UserAdminDTO>>({ url: '/admin/users', method: 'GET', params })
}

export function setUserStatus(id: number, enabled: boolean) {
    return http<void>({ url: `/admin/users/${id}/status`, method: 'PATCH', data: { enabled } })
}

export function resetPassword(id: number, newPassword: string) {
    return http<void>({
        url: `/admin/users/${id}/reset-password`,
        method: 'POST',
        data: { newPassword },
    })
}
