import { Table } from 'antd'
import { useCallback, useMemo, useState } from 'react'
import type { UserAdminDTO } from '../../types/user'
import ResetPasswordModal from './ResetPasswordModal'
import UserFilterBar from './UserFilterBar'
import { useUserTable } from './hooks/useUserTable'
import { buildUserColumns } from './userTableColumns'

/**
 * 用户管理页（组合层）：仅负责组合 hook 与子组件，保持 JSX 层级扁平。
 */
export default function UserManagePage() {
    const {
        data,
        isFetching,
        page,
        size,
        handleSearch,
        handleStatusChange,
        handleUserRoleChange,
        handlePageChange,
        handleToggleStatus,
    } = useUserTable()

    const [resetUser, setResetUser] = useState<UserAdminDTO | null>(null)
    const handleCloseReset = useCallback(() => setResetUser(null), [])

    const columns = useMemo(
        () =>
            buildUserColumns({ onToggleStatus: handleToggleStatus, onResetPassword: setResetUser }),
        [handleToggleStatus],
    )

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <UserFilterBar
                onSearch={handleSearch}
                onStatusChange={handleStatusChange}
                onUserRoleChange={handleUserRoleChange}
            />
            <Table<UserAdminDTO>
                rowKey="id"
                columns={columns}
                dataSource={data?.records ?? []}
                loading={isFetching}
                pagination={{
                    current: page,
                    pageSize: size,
                    total: data?.total ?? 0,
                    showSizeChanger: true,
                    showTotal: (t) => `共 ${t} 条`,
                    onChange: handlePageChange,
                }}
            />
            <ResetPasswordModal user={resetUser} onClose={handleCloseReset} />
        </div>
    )
}
