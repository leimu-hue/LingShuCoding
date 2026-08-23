import { LockOutlined, UserOutlined } from '@ant-design/icons'
import { App, Button, Card, Form, Input, Typography } from 'antd'
import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../store/authStore'

interface LoginForm {
    username: string
    password: string
}

export default function LoginPage() {
    const { message } = App.useApp()
    const login = useAuthStore((s) => s.login)
    const navigate = useNavigate()
    const location = useLocation()
    const [loading, setLoading] = useState(false)

    const from = (location.state as { from?: string } | null)?.from ?? '/chat'

    const onFinish = async (values: LoginForm) => {
        setLoading(true)
        try {
            await login(values.username, values.password)
            message.success('登录成功')
            navigate(from, { replace: true })
        } catch {
            // 错误已由全局 message 提示
        } finally {
            setLoading(false)
        }
    }

    return (
        <div
            style={{
                minHeight: '100vh',
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                background: '#f0f2f5',
            }}
        >
            <Card style={{ width: 380 }}>
                <Typography.Title level={4} style={{ textAlign: 'center', marginTop: 0 }}>
                    AI 零代码应用生成平台
                </Typography.Title>
                <Form<LoginForm> onFinish={onFinish} size="large">
                    <Form.Item
                        name="username"
                        rules={[{ required: true, message: '请输入用户名' }]}
                    >
                        <Input
                            prefix={<UserOutlined />}
                            placeholder="用户名"
                            autoComplete="username"
                        />
                    </Form.Item>
                    <Form.Item name="password" rules={[{ required: true, message: '请输入密码' }]}>
                        <Input.Password
                            prefix={<LockOutlined />}
                            placeholder="密码"
                            autoComplete="current-password"
                        />
                    </Form.Item>
                    <Form.Item style={{ marginBottom: 12 }}>
                        <Button type="primary" htmlType="submit" block loading={loading}>
                            登录
                        </Button>
                    </Form.Item>
                </Form>
                <div style={{ textAlign: 'center' }}>
                    <Typography.Text type="secondary">还没有账号？</Typography.Text>{' '}
                    <Link to="/register">立即注册</Link>
                </div>
            </Card>
        </div>
    )
}
