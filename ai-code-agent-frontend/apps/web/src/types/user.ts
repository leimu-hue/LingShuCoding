export interface PermissionDTO {
    id: number
    code: string
    name: string
}

export interface RoleDTO {
    id: number
    code: string
    name: string
    permissions: PermissionDTO[]
}

export interface UserIdentity {
    id: number
    username: string
    nickname: string | null
    roles: RoleDTO[]
    permissions: PermissionDTO[]
    enabled: boolean
}

export interface LoginResult {
    token: string
    user: UserIdentity
}

export interface UserAdminDTO {
    id: number
    username: string
    nickname: string | null
    enabled: boolean
    createdAt: string | null
    roles: RoleDTO[]
}

export interface PageResult<T> {
    records: T[]
    total: number
    pageNum: number
    pageSize: number
    pages: number
}
