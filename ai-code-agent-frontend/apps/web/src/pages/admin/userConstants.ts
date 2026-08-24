import type { UserRole } from '../../types/user'

/** 角色 → 展示元数据（不依赖组件状态，模块级常量） */
export const roleMeta: Record<UserRole, { color: string; label: string }> = {
    ADMIN: { color: 'gold', label: '管理员' },
    USER: { color: 'blue', label: '普通用户' },
}

/** 状态筛选选项 */
export const statusOptions = [
    { value: 1, label: '启用' },
    { value: 0, label: '禁用' },
]

/** 角色筛选选项 */
export const userRoleOptions = [
    { value: 'ADMIN', label: '管理员' },
    { value: 'USER', label: '普通用户' },
]

/** 展示创建时间（ISO 串 → YYYY-MM-DD HH:mm） */
export function formatDate(value: string | null): string {
    if (typeof value !== 'string' || value.length === 0) {
        return '-'
    }
    return value.replace('T', ' ').slice(0, 16)
}
