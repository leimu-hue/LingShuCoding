import { App, Button, Form, Input, Modal, Select, Space, Switch, Table, Tag } from 'antd'
import type { TableProps } from 'antd'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
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
    const queryClient = useQueryClient()

    // 分页 / 筛选条件只作为「查询 key」存在：key 变化会自动触发重取，无需 effect。
    const [page, setPage] = useState(1)
    const [size, setSize] = useState(10)
    const [keyword, setKeyword] = useState('')
    const [status, setStatus] = useState<number | undefined>(undefined)
    const [userRole, setUserRole] = useState<UserRole | undefined>(undefined)

    const [resetUser, setResetUser] = useState<UserAdminDTO | null>(null)

    const { data, isFetching } = useQuery({
        queryKey: ['users', { page, size, keyword, status, userRole }],
        queryFn: () =>
            adminApi.listUsers({ page, size, keyword: keyword || undefined, status, userRole }),
        // 翻页 / 筛选期间保留上一页数据，避免整表闪烁（配合 isFetching 显示 loading）
        placeholderData: (previous) => previous,
    })

    const toggleStatus = useMutation({
        mutationFn: ({ id, enabled }: { id: number; enabled: boolean; username: string }) =>
            adminApi.setUserStatus(id, enabled),
        onSuccess: (_data, { enabled, username }) => {
            message.success(enabled ? `已启用「${username}」` : `已禁用「${username}」`)
            void queryClient.invalidateQueries({ queryKey: ['users'] })
        },
    })

    const handleToggleStatus = (user: UserAdminDTO, enabled: boolean) => {
        toggleStatus.mutate({ id: user.id, enabled, username: user.username })
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
                    onChange={(v) => handleToggleStatus(record, v)}
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
                dataSource={data?.records ?? []}
                loading={isFetching}
                pagination={{
                    current: page,
                    pageSize: size,
                    total: data?.total ?? 0,
                    showSizeChanger: true,
                    showTotal: (t) => `共 ${t} 条`,
                    onChange: (p, ps) => {
                        setPage(p)
                        setSize(ps)
                    },
                }}
            />
            <ResetPasswordModal user={resetUser} onClose={() => setResetUser(null)} />
        </div>
    )
}

function ResetPasswordModal({ user, onClose }: { user: UserAdminDTO | null; onClose: () => void }) {
    const { message } = App.useApp()
    const queryClient = useQueryClient()
    const [form] = Form.useForm<{ newPassword: string }>()

    const resetPassword = useMutation({
        mutationFn: ({ id, newPassword }: { id: number; newPassword: string }) =>
            adminApi.resetPassword(id, newPassword),
        onSuccess: () => {
            message.success('密码已重置')
            form.resetFields()
            onClose()
            void queryClient.invalidateQueries({ queryKey: ['users'] })
        },
    })

    const handleOk = async () => {
        if (!user) return
        let values: { newPassword: string }
        try {
            values = await form.validateFields()
        } catch {
            // 校验失败，antd 已内联提示，不发起请求
            return
        }
        resetPassword.mutate({ id: user.id, newPassword: values.newPassword })
    }

    return (
        <Modal
            open={!!user}
            title={`重置密码 - ${user?.username ?? ''}`}
            onOk={handleOk}
            onCancel={onClose}
            confirmLoading={resetPassword.isPending}
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
