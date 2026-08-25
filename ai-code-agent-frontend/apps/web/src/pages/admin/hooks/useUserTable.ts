import { App } from 'antd'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useCallback, useState } from 'react'
import * as adminApi from '../../../api/admin'
import type { PageResult, UserAdminDTO, UserRole } from '../../../types/user'

/**
 * 用户管理页业务逻辑 hook。
 * 封装：分页/筛选状态、数据查询（自动响应查询 key 变化）、启用/禁用变更。
 * 筛选条件变化时统一将页码重置为第一页，避免当前页超出总页数导致空数据。
 */
export function useUserTable() {
    const { message } = App.useApp()
    const queryClient = useQueryClient()

    const [page, setPage] = useState(1)
    const [size, setSize] = useState(10)
    const [keyword, setKeyword] = useState('')
    const [status, setStatus] = useState<number | undefined>(undefined)
    const [userRole, setUserRole] = useState<UserRole | undefined>(undefined)

    // 分页 / 筛选条件只作为「查询 key」存在：key 变化会自动触发重取，无需手动 effect。
    const { data, isFetching, refetch } = useQuery({
        queryKey: ['users', { page, size, keyword, status, userRole }],
        queryFn: () =>
            adminApi.listUsers({ page, size, keyword: keyword || undefined, status, userRole }),
        // 翻页 / 筛选期间保留上一页数据，避免整表闪烁（配合 isFetching 显示 loading）
        placeholderData: (previous: PageResult<UserAdminDTO> | undefined) => previous,
    })

    const { mutate: toggleStatus } = useMutation({
        mutationFn: ({ id, enabled }: { id: number; enabled: boolean; username: string }) =>
            adminApi.setUserStatus(id, enabled),
        onSuccess: (
            _data: void,
            { enabled, username }: { id: number; enabled: boolean; username: string },
        ) => {
            message.success(enabled ? `已启用「${username}」` : `已禁用「${username}」`)
            void queryClient.invalidateQueries({ queryKey: ['users'] })
        },
    })

    const handleToggleStatus = useCallback(
        (user: UserAdminDTO, enabled: boolean) => {
            toggleStatus({ id: user.id, enabled, username: user.username })
        },
        [toggleStatus],
    )

    // 智能重置：筛选条件变化时，先回到第一页再更新条件
    const handleSearch = useCallback((value: string) => {
        setPage(1)
        setKeyword(value)
    }, [])

    const handleStatusChange = useCallback((value: number | undefined) => {
        setPage(1)
        setStatus(value)
    }, [])

    const handleUserRoleChange = useCallback((value: UserRole | undefined) => {
        setPage(1)
        setUserRole(value)
    }, [])

    const handlePageChange = useCallback((nextPage: number, nextSize: number) => {
        setPage(nextPage)
        setSize(nextSize)
    }, [])

    return {
        data,
        isFetching,
        refetch,
        page,
        size,
        handleSearch,
        handleStatusChange,
        handleUserRoleChange,
        handlePageChange,
        handleToggleStatus,
    }
}
