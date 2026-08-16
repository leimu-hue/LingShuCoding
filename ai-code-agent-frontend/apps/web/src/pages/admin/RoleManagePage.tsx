import { App, Button, Checkbox, Modal, Table, Tag } from 'antd'
import type { TableProps } from 'antd'
import { useCallback, useEffect, useMemo, useState } from 'react'
import * as adminApi from '../../api/admin'
import type { PermissionDTO, RoleDTO } from '../../types/user'

export default function RoleManagePage() {
    const [roles, setRoles] = useState<RoleDTO[]>([])
    const [loading, setLoading] = useState(false)
    const [grantRole, setGrantRole] = useState<RoleDTO | null>(null)

    const load = useCallback(async () => {
        setLoading(true)
        try {
            setRoles(await adminApi.listRoles())
        } catch {
            // 错误已由全局 message 提示
        } finally {
            setLoading(false)
        }
    }, [])

    useEffect(() => {
        void load()
    }, [load])

    // 全部权限目录：取所有角色权限的并集（预置数据中 ADMIN 拥有全部权限）
    const allPermissions = useMemo(() => {
        const map = new Map<number, PermissionDTO>()
        roles.forEach((r) => r.permissions.forEach((p) => map.set(p.id, p)))
        return Array.from(map.values())
    }, [roles])

    const columns: TableProps<RoleDTO>['columns'] = [
        { title: '角色码', dataIndex: 'code', width: 120, render: (v: string) => <Tag color="purple">{v}</Tag> },
        { title: '角色名', dataIndex: 'name', width: 160 },
        {
            title: '权限',
            dataIndex: 'permissions',
            render: (ps: PermissionDTO[]) =>
                ps.length ? (
                    <>
                        {ps.map((p) => (
                            <Tag key={p.id} color="green">
                                {p.name}
                            </Tag>
                        ))}
                    </>
                ) : (
                    '-'
                ),
        },
        {
            title: '操作',
            key: 'action',
            width: 100,
            render: (_, record) => (
                <Button size="small" onClick={() => setGrantRole(record)}>
                    授权
                </Button>
            ),
        },
    ]

    return (
        <div>
            <Table<RoleDTO>
                rowKey="id"
                columns={columns}
                dataSource={roles}
                loading={loading}
                pagination={false}
            />
            <GrantPermissionsModal
                role={grantRole}
                allPermissions={allPermissions}
                onClose={() => setGrantRole(null)}
                onDone={load}
            />
        </div>
    )
}

function GrantPermissionsModal({
    role,
    allPermissions,
    onClose,
    onDone,
}: {
    role: RoleDTO | null
    allPermissions: PermissionDTO[]
    onClose: () => void
    onDone: () => void
}) {
    const { message } = App.useApp()
    const [selected, setSelected] = useState<number[]>([])
    const [loading, setLoading] = useState(false)

    useEffect(() => {
        setSelected(role?.permissions.map((p) => p.id) ?? [])
    }, [role])

    const handleOk = async () => {
        if (!role) return
        setLoading(true)
        try {
            await adminApi.grantPermissions(role.id, selected)
            message.success('权限已更新')
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
            open={!!role}
            title={`授权 - ${role?.name ?? ''}（${role?.code ?? ''}）`}
            onOk={handleOk}
            onCancel={onClose}
            confirmLoading={loading}
            destroyOnHidden
        >
            <Checkbox.Group
                value={selected}
                onChange={(v) => setSelected(v as number[])}
                style={{ display: 'flex', flexDirection: 'column', gap: 8, marginTop: 16 }}
            >
                {allPermissions.map((p) => (
                    <Checkbox key={p.id} value={p.id}>
                        {p.name}（{p.code}）
                    </Checkbox>
                ))}
            </Checkbox.Group>
        </Modal>
    )
}
