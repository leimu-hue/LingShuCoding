import { Button, Space, Switch, Tag } from 'antd'
import type { TableProps } from 'antd'
import type { UserAdminDTO, UserRole } from '../../types/user'
import { formatDate, roleMeta } from './userConstants'

/** 表格列所需的回调动作（由页面组合层注入，保持列定义为纯函数） */
export interface UserTableColumnActions {
    onToggleStatus: (user: UserAdminDTO, enabled: boolean) => void
    onResetPassword: (user: UserAdminDTO) => void
}

/** 构造用户表格列定义；作为纯函数供 useMemo 缓存，避免每次渲染重建 */
export function buildUserColumns(
    actions: UserTableColumnActions,
): TableProps<UserAdminDTO>['columns'] {
    return [
        { title: 'ID', dataIndex: 'id', width: 72 },
        { title: '用户名', dataIndex: 'username', width: 140 },
        { title: '昵称', dataIndex: 'nickname', width: 140, render: (v: string) => v || '-' },
        {
            title: '角色',
            dataIndex: 'userRole',
            width: 120,
            render: (r: UserRole) => {
                const meta = roleMeta[r] ?? { color: 'default', label: r }
                return <Tag color={meta.color}>{meta.label}</Tag>
            },
        },
        {
            title: '状态',
            dataIndex: 'enabled',
            width: 96,
            render: (enabled: boolean, record) => (
                <Switch
                    checked={enabled}
                    checkedChildren="启用"
                    unCheckedChildren="禁用"
                    onChange={(v) => actions.onToggleStatus(record, v)}
                />
            ),
        },
        {
            title: '创建时间',
            dataIndex: 'createdTime',
            width: 160,
            render: (v: string | null) => formatDate(v),
        },
        {
            title: '操作',
            key: 'action',
            width: 120,
            render: (_, record) => (
                <Space>
                    <Button size="small" onClick={() => actions.onResetPassword(record)}>
                        重置密码
                    </Button>
                </Space>
            ),
        },
    ]
}
