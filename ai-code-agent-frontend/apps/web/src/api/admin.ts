import { http } from '@ai-code-agent/shared'
import type { PageResult, RoleDTO, UserAdminDTO } from '../types/user'

export interface UserPageParams {
    page?: number
    size?: number
    keyword?: string
    status?: number
    roleId?: number
}

export function listUsers(params: UserPageParams) {
    return http<PageResult<UserAdminDTO>>({ url: '/admin/users', method: 'GET', params })
}

export function setUserStatus(id: number, enabled: boolean) {
    return http<void>({ url: `/admin/users/${id}/status`, method: 'PATCH', data: { enabled } })
}

export function resetPassword(id: number, newPassword: string) {
    return http<void>({ url: `/admin/users/${id}/reset-password`, method: 'POST', data: { newPassword } })
}

export function assignRoles(id: number, roleIds: number[]) {
    return http<void>({ url: `/admin/users/${id}/roles`, method: 'PUT', data: { roleIds } })
}

export function listRoles() {
    return http<RoleDTO[]>({ url: '/admin/roles', method: 'GET' })
}

export function grantPermissions(roleId: number, permissionIds: number[]) {
    return http<void>({ url: `/admin/roles/${roleId}/permissions`, method: 'PUT', data: { permissionIds } })
}
