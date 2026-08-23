import { App, Button, Form, Input, Modal, Select, Space, Switch, Table, Tag } from 'antd'
import type { TableProps } from 'antd'
import { useCallback, useEffect, useState } from 'react'
import * as adminApi from '../../api/admin'
import type { UserAdminDTO, UserRole } from '../../types/user'

/** 展示创建时间（ISO 串 → YYYY-MM-DD HH:mm） */
function formatDate(value: string | null): string {
    if (typeof value !== 'string' || value.length === 0) {
        return '-'
    }
    return value.replace('T', ' ').slice(0, 16)
}

const roleMeta: Record<UserRole, { color: string; label: string }> = {
    ADMIN: { color: 'gold', label: '管理员' },
    USER: { color: 'blue', label: '普通用户' },
}

export default function UserManagePage() {
    const { message } = App.useApp()
    const [users, setUsers] = useState<UserAdminDTO[]>([])
    const [total, setTotal] = useState(0)
    const [page, setPage] = useState(1)
    const [size, setSize] = useState(10)
    const [keyword, setKeyword] = useState('')
    const [status, setStatus] = useState<number | undefined>(undefined)
    const [userRole, setUserRole] = useState<UserRole | undefined>(undefined)
    const [loading, setLoading] = useState(false)

    const [resetUser, setResetUser] = useState<UserAdminDTO | null>(null)

    const loadUsers = useCallback(async () => {
        setLoading(true)
        try {
            const data = await adminApi.listUsers({
                page,
                size,
                keyword: keyword || undefined,
                status,
                userRole,
            })
            setUsers(data.records)
            setTotal(data.total)
        } catch {
            // 错误已由全局 message 提示
        } finally {
            setLoading(false)
        }
    }, [page, size, keyword, status, userRole])

    useEffect(() => {
        // 挂载/筛选条件变化即拉取是预期行为
        // eslint-disable-next-line react-hooks/set-state-in-effect -- 发起请求前设置 loading 是数据请求的固有模式
        void loadUsers()
    }, [loadUsers])

    const handleToggleStatus = async (user: UserAdminDTO, enabled: boolean) => {
        try {
            await adminApi.setUserStatus(user.id, enabled)
            message.success(enabled ? `已启用「${user.username}」` : `已禁用「${user.username}」`)
            void loadUsers()
        } catch {
            // 错误已由全局 message 提示
        }
    }

    const columns: TableProps<UserAdminDTO>['columns'] = [
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
                    onChange={(v) => void handleToggleStatus(record, v)}
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
                    <Button size="small" onClick={() => setResetUser(record)}>
                        重置密码
                    </Button>
                </Space>
            ),
        },
    ]

    return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
            <Space wrap>
                <Input.Search
                    placeholder="用户名 / 昵称"
                    allowClear
                    style={{ width: 220 }}
                    onSearch={(v) => {
                        setPage(1)
                        setKeyword(v)
                    }}
                />
                <Select
                    placeholder="状态"
                    allowClear
                    style={{ width: 120 }}
                    options={[
                        { value: 1, label: '启用' },
                        { value: 0, label: '禁用' },
                    ]}
                    onChange={(v) => {
                        setPage(1)
                        setStatus(v)
                    }}
                />
                <Select
                    placeholder="角色"
                    allowClear
                    style={{ width: 140 }}
                    options={[
                        { value: 'ADMIN', label: '管理员' },
                        { value: 'USER', label: '普通用户' },
                    ]}
                    onChange={(v) => {
                        setPage(1)
                        setUserRole(v)
                    }}
                />
            </Space>
            <Table<UserAdminDTO>
                rowKey="id"
                columns={columns}
                dataSource={users}
                loading={loading}
                pagination={{
                    current: page,
                    pageSize: size,
                    total,
                    showSizeChanger: true,
                    showTotal: (t) => `共 ${t} 条`,
                    onChange: (p, ps) => {
                        setPage(p)
                        setSize(ps)
                    },
                }}
            />
            <ResetPasswordModal
                user={resetUser}
                onClose={() => setResetUser(null)}
                onDone={loadUsers}
            />
        </div>
    )
}

function ResetPasswordModal({
    user,
    onClose,
    onDone,
}: {
    user: UserAdminDTO | null
    onClose: () => void
    onDone: () => void
}) {
    const { message } = App.useApp()
    const [form] = Form.useForm<{ newPassword: string }>()
    const [loading, setLoading] = useState(false)

    const handleOk = async () => {
        const values = await form.validateFields()
        if (!user) return
        setLoading(true)
        try {
            await adminApi.resetPassword(user.id, values.newPassword)
            message.success('密码已重置')
            form.resetFields()
            onClose()
            onDone()
        } catch {
            // 错误已由全局 message 提示
        } finally {
            setLoading(false)
        }
    }

    return (
        <Modal
            open={!!user}
            title={`重置密码 - ${user?.username ?? ''}`}
            onOk={handleOk}
            onCancel={onClose}
            confirmLoading={loading}
            destroyOnHidden
        >
            <Form form={form} layout="vertical" style={{ marginTop: 16 }}>
                <Form.Item
                    name="newPassword"
                    label="新密码"
                    rules={[
                        { required: true, message: '请输入新密码' },
                        { min: 6, max: 64, message: '密码长度 6-64 位' },
                    ]}
                >
                    <Input.Password placeholder="新密码（6-64 位）" />
                </Form.Item>
            </Form>
        </Modal>
    )
}
