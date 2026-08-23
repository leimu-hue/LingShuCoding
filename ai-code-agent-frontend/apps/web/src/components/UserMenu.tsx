import { LogoutOutlined, UserOutlined } from '@ant-design/icons'
import { App, Avatar, Dropdown, Space, Typography } from 'antd'
import { useNavigate } from 'react-router-dom'
import { useAuthStore } from '../store/authStore'

/**
 * 顶栏用户菜单：展示当前登录用户昵称，下拉提供退出登录。
 */
export default function UserMenu() {
    const { message } = App.useApp()
    const user = useAuthStore((s) => s.user)
    const logout = useAuthStore((s) => s.logout)
    const navigate = useNavigate()

    const handleLogout = async () => {
        await logout()
        message.success('已退出登录')
        navigate('/login', { replace: true })
    }

    const nickname = user?.nickname || user?.username || '用户'

    return (
        <Dropdown
            menu={{
                items: [
                    {
                        key: 'logout',
                        icon: <LogoutOutlined />,
                        label: '退出登录',
                        onClick: handleLogout,
                    },
                ],
            }}
        >
            <Space style={{ cursor: 'pointer' }}>
                <Avatar size={32} icon={<UserOutlined />} />
                <Typography.Text>{nickname}</Typography.Text>
            </Space>
        </Dropdown>
    )
}
