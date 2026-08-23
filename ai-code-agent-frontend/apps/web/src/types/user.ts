export type UserRole = 'ADMIN' | 'USER'

export interface UserIdentity {
    id: number
    username: string
    nickname: string
    userRole: UserRole
    enabled: boolean
}

export interface LoginResult {
    token: string
    user: UserIdentity
}

export interface UserAdminDTO {
    id: number
    username: string
    nickname: string
    userRole: UserRole
    enabled: boolean
    createdTime: string | null
}

export interface PageResult<T> {
    records: T[]
    total: number
    pageNum: number
    pageSize: number
    pages: number
}
