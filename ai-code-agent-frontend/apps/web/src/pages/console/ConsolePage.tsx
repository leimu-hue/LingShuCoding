import { ReloadOutlined } from '@ant-design/icons'
import { http } from '@ai-code-agent/shared'
import { Alert, Button, Card, Table, Typography } from 'antd'
import { useCallback, useEffect, useState } from 'react'

interface DemoUser {
    id: number
    name: string
    age: number
}

export default function ConsolePage() {
    const [users, setUsers] = useState<DemoUser[]>([])
    const [loading, setLoading] = useState(false)
    const [error, setError] = useState<string | null>(null)

    const loadUsers = useCallback(async () => {
        setLoading(true)
        setError(null)
        try {
            const data = await http<DemoUser[]>({ url: '/demo-users' })
            setUsers(data ?? [])
        } catch (reason) {
            setError(reason instanceof Error ? reason.message : String(reason))
        } finally {
            setLoading(false)
        }
    }, [])

    useEffect(() => {
        // 数据加载属于外部系统同步场景，需在挂载时发起请求
        // eslint-disable-next-line react-hooks/set-state-in-effect
        void loadUsers()
    }, [loadUsers])

    return (
        <div style={{ height: '100%', display: 'flex', flexDirection: 'column', gap: 12 }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Typography.Title level={4} style={{ margin: 0 }}>
                    应用管理
                </Typography.Title>
                <Button icon={<ReloadOutlined />} onClick={() => void loadUsers()}>
                    刷新
                </Button>
            </div>
            {error && (
                <Alert
                    type="error"
                    showIcon
                    message="请求失败"
                    description={`${error}（请确认后端已启动，/api 已代理至 http://localhost:8080）`}
                    action={
                        <Button size="small" danger onClick={() => void loadUsers()}>
                            重试
                        </Button>
                    }
                />
            )}
            <Card style={{ flex: 1, overflow: 'auto' }}>
                <Table<DemoUser>
                    rowKey="id"
                    loading={loading}
                    dataSource={users}
                    pagination={false}
                    columns={[
                        { title: 'ID', dataIndex: 'id', width: 80 },
                        { title: '姓名', dataIndex: 'name' },
                        { title: '年龄', dataIndex: 'age', width: 120 },
                    ]}
                />
            </Card>
        </div>
    )
}
