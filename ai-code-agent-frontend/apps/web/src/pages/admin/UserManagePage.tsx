import { App, Button, Form, Input, Modal, Select, Space, Switch, Table, Tag } from 'antd'
import type { TableProps } from 'antd'
import { useCallback, useEffect, useState } from 'react'
import * as adminApi from '../../api/admin'
import type { RoleDTO, UserAdminDTO } from '../../types/user'

/** 展示创建时间（ISO 串 → YYYY-MM-DD HH:mm） */
function formatDate(value: string | null): string {
    if (typeof value !== 'string' || value.length === 0) {
        return '-'
    }
    return value.replace('T', ' ').slice(0, 16)
}

export default function UserManagePage() {
    const { message } = App.useApp()
    const [users, setUsers] = useState<UserAdminDTO[]>([])
    const [roles, setRoles] = useState<RoleDTO[]>([])
    const [total, setTotal] = useState(0)
    const [page, setPage] = useState(1)
    const [size, setSize] = useState(10)
    const [keyword, setKeyword] = useState('')
    const [status, setStatus] = useState<number | undefined>(undefined)
    const [roleId, setRoleId] = useState<number | undefined>(undefined)
    const [loading, setLoading] = useState(false)

    const [resetUser, setResetUser] = useState<UserAdminDTO | null>(null)
    const [assignUser, setAssignUser] = useState<UserAdminDTO | null>(null)

    const loadUsers = useCallback(async () => {
        setLoading(true)
        try {
            const data = await adminApi.listUsers({
                page,
                size,
                keyword: keyword || undefined,
                status,
                roleId,
            })
            setUsers(data.records)
            setTotal(data.total)
        } catch {
            // 错误已由全局 message 提示
        } finally {
            setLoading(false)
        }
    }, [page, size, keyword, status, roleId])

    useEffect(() => {
        void loadUsers()
    }, [loadUsers])

    useEffect(() => {
        adminApi
            .listRoles()
            .then(setRoles)
            .catch(() => {})
    }, [])

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
        { title: '昵称', dataIndex: 'nickname', width: 140, render: (v: string | null) => v || '-' },
        {
            title: '角色',
            dataIndex: 'roles',
            render: (rs: RoleDTO[]) =>
                rs.length ? (
                    <>
                        {rs.map((r) => (
                            <Tag key={r.id} color="blue">
                                {r.name}
                            </Tag>
                        ))}
                    </>
                ) : (
                    '-'
                ),
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
        { title: '创建时间', dataIndex: 'createdAt', width: 160, render: (v: string | null) => formatDate(v) },
        {
            title: '操作',
            key: 'action',
            width: 200,
            render: (_, record) => (
                <Space>
                    <Button size="small" onClick={() => setResetUser(record)}>
                        重置密码
                    </Button>
                    <Button size="small" onClick={() => setAssignUser(record)}>
                        分配角色
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
                    style={{ width: 160 }}
                    options={roles.map((r) => ({ value: r.id, label: r.name }))}
                    onChange={(v) => {
                        setPage(1)
                        setRoleId(v)
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
            <ResetPasswordModal user={resetUser} onClose={() => setResetUser(null)} onDone={loadUsers} />
            <AssignRolesModal
                user={assignUser}
                roles={roles}
                onClose={() => setAssignUser(null)}
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

function AssignRolesModal({
    user,
    roles,
    onClose,
    onDone,
}: {
    user: UserAdminDTO | null
    roles: RoleDTO[]
    onClose: () => void
    onDone: () => void
}) {
    const { message } = App.useApp()
    const [selected, setSelected] = useState<number[]>([])
    const [loading, setLoading] = useState(false)

    useEffect(() => {
        setSelected(user?.roles.map((r) => r.id) ?? [])
    }, [user])

    const handleOk = async () => {
        if (!user) return
        setLoading(true)
        try {
            await adminApi.assignRoles(user.id, selected)
            message.success('角色已更新')
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
            title={`分配角色 - ${user?.username ?? ''}`}
            onOk={handleOk}
            onCancel={onClose}
            confirmLoading={loading}
            destroyOnHidden
        >
            <Select
                mode="multiple"
                style={{ width: '100%', marginTop: 16 }}
                value={selected}
                onChange={setSelected}
                options={roles.map((r) => ({ value: r.id, label: `${r.name}（${r.code}）` }))}
                placeholder="选择角色"
            />
        </Modal>
    )
}
