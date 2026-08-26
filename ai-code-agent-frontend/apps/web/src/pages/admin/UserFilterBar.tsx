import { Input, Select, Space } from 'antd'
import type { UserRole } from '@/types/user'
import { statusOptions, userRoleOptions } from '@/constants/user'

interface UserFilterBarProps {
    onSearch: (value: string) => void
    onStatusChange: (value: number | undefined) => void
    onUserRoleChange: (value: UserRole | undefined) => void
}

/** 用户管理页筛选栏（独立 UI 区块，仅负责渲染与回调上抛） */
export default function UserFilterBar({
    onSearch,
    onStatusChange,
    onUserRoleChange,
}: UserFilterBarProps) {
    return (
        <Space wrap>
            <Input.Search
                placeholder="用户名 / 昵称"
                allowClear
                style={{ width: 220 }}
                onSearch={onSearch}
            />
            <Select
                placeholder="状态"
                allowClear
                style={{ width: 120 }}
                options={statusOptions}
                onChange={onStatusChange}
            />
            <Select
                placeholder="角色"
                allowClear
                style={{ width: 140 }}
                options={userRoleOptions}
                onChange={onUserRoleChange}
            />
        </Space>
    )
}
