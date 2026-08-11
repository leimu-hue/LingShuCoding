import { ReloadOutlined } from '@ant-design/icons'
import { Alert, Button, Card, Table, Typography } from 'antd'
import { useFetch } from '@/hooks/useFetch'

interface DemoUser {
    id: number
    name: string
    age: number
}

export default function ConsolePage() {
    const { data: users, loading, error, run } = useFetch<DemoUser[]>({ url: '/demo-users' })

    return (
        <div style={{ height: '100%', display: 'flex', flexDirection: 'column', gap: 12 }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                <Typography.Title level={4} style={{ margin: 0 }}>
                    应用管理
                </Typography.Title>
                <Button icon={<ReloadOutlined />} onClick={() => void run()}>
                    刷新
                </Button>
            </div>
            {error && (
                <Alert
                    type="error"
                    showIcon
                    title="请求失败"
                    description={`${error}（请确认后端已启动，/api 已代理至 http://localhost:8080）`}
                    action={
                        <Button size="small" danger onClick={() => void run()}>
                            重试
                        </Button>
                    }
                />
            )}
            <Card style={{ flex: 1, overflow: 'auto' }}>
                <Table<DemoUser>
                    rowKey="id"
                    loading={loading}
                    dataSource={users ?? []}
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
