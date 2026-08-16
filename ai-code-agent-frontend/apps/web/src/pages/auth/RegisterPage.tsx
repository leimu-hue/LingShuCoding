import { LockOutlined, SmileOutlined, UserOutlined } from '@ant-design/icons'
import { App, Button, Card, Form, Input, Typography } from 'antd'
import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuthStore } from '../../store/authStore'

interface RegisterForm {
    username: string
    nickname?: string
    password: string
    confirm: string
}

export default function RegisterPage() {
    const { message } = App.useApp()
    const register = useAuthStore((s) => s.register)
    const navigate = useNavigate()
    const [loading, setLoading] = useState(false)

    const onFinish = async (values: RegisterForm) => {
        setLoading(true)
        try {
            await register(values.username, values.password, values.nickname)
            message.success('注册成功')
            navigate('/chat', { replace: true })
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
                    注册账号
                </Typography.Title>
                <Form<RegisterForm> onFinish={onFinish} size="large">
                    <Form.Item
                        name="username"
                        rules={[
                            { required: true, message: '请输入用户名' },
                            { min: 3, max: 64, message: '用户名长度 3-64 位' },
                        ]}
                    >
                        <Input prefix={<UserOutlined />} placeholder="用户名（3-64 位）" autoComplete="username" />
                    </Form.Item>
                    <Form.Item name="nickname" rules={[{ max: 64, message: '昵称不超过 64 位' }]}>
                        <Input prefix={<SmileOutlined />} placeholder="昵称（可选）" />
                    </Form.Item>
                    <Form.Item
                        name="password"
                        rules={[
                            { required: true, message: '请输入密码' },
                            { min: 6, max: 64, message: '密码长度 6-64 位' },
                        ]}
                    >
                        <Input.Password prefix={<LockOutlined />} placeholder="密码（6-64 位）" autoComplete="new-password" />
                    </Form.Item>
                    <Form.Item
                        name="confirm"
                        dependencies={['password']}
                        rules={[
                            { required: true, message: '请确认密码' },
                            ({ getFieldValue }) => ({
                                validator(_, value) {
                                    if (!value || getFieldValue('password') === value) {
                                        return Promise.resolve()
                                    }
                                    return Promise.reject(new Error('两次输入的密码不一致'))
                                },
                            }),
                        ]}
                    >
                        <Input.Password prefix={<LockOutlined />} placeholder="确认密码" autoComplete="new-password" />
                    </Form.Item>
                    <Form.Item style={{ marginBottom: 12 }}>
                        <Button type="primary" htmlType="submit" block loading={loading}>
                            注册
                        </Button>
                    </Form.Item>
                </Form>
                <div style={{ textAlign: 'center' }}>
                    <Typography.Text type="secondary">已有账号？</Typography.Text>{' '}
                    <Link to="/login">去登录</Link>
                </div>
            </Card>
        </div>
    )
}
