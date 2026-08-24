import { LockOutlined, SmileOutlined, UserOutlined } from '@ant-design/icons'
import { Button, Card, Form, Input, Typography } from 'antd'
import { Link } from 'react-router-dom'
import { useRegister, type RegisterForm } from './hooks/useRegister'

export default function RegisterPage() {
    const { loading, onFinish } = useRegister()

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
                        <Input
                            prefix={<UserOutlined />}
                            placeholder="用户名（3-64 位）"
                            autoComplete="username"
                        />
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
                        <Input.Password
                            prefix={<LockOutlined />}
                            placeholder="密码（6-64 位）"
                            autoComplete="new-password"
                        />
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
                        <Input.Password
                            prefix={<LockOutlined />}
                            placeholder="确认密码"
                            autoComplete="new-password"
                        />
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
