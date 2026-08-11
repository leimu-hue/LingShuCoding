import { CommentOutlined } from '@ant-design/icons'
import { Avatar, Layout, Menu, Typography, theme } from 'antd'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import AppFooter from '../components/AppFooter'
import logo from '@root/logo.svg'

const { Header, Content } = Layout

// 前台菜单：目前仅面向普通用户的 AI 对话生成；应用管理与可视化编辑器为管理员功能，入口在 /admin
const menuItems = [{ key: '/chat', icon: <CommentOutlined />, label: 'AI 对话生成' }]

/**
 * 前台主布局：顶部导航栏（logo + 标题 + 菜单 + 用户头像昵称）+ 路由内容区 + 底部页脚。
 */
export default function FrontLayout() {
    const navigate = useNavigate()
    const location = useLocation()
    const { token } = theme.useToken()

    return (
        <Layout style={{ height: '100%' }}>
            <Header
                style={{
                    padding: '0 24px',
                    background: token.colorBgContainer,
                    borderBottom: `1px solid ${token.colorBorderSecondary}`,
                    display: 'flex',
                    alignItems: 'center',
                    gap: 16,
                }}
            >
                <div
                    onClick={() => navigate('/')}
                    style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: 8,
                        cursor: 'pointer',
                        flexShrink: 0,
                    }}
                >
                    <img src={logo} alt="AI 零代码应用生成平台" style={{ height: 32, width: 32 }} />
                    <Typography.Title level={4} style={{ margin: 0, whiteSpace: 'nowrap' }}>
                        AI 零代码应用生成平台
                    </Typography.Title>
                </div>
                <Menu
                    mode="horizontal"
                    selectedKeys={[location.pathname]}
                    items={menuItems}
                    onClick={({ key }) => navigate(key)}
                    style={{
                        flex: 1,
                        minWidth: 0,
                        borderBottom: 'none',
                        background: 'transparent',
                    }}
                />
                <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexShrink: 0 }}>
                    <Avatar src={logo} alt="用户头像" size={32} />
                    <Typography.Text>AI 用户</Typography.Text>
                </div>
            </Header>
            <Content
                style={{
                    flex: 1,
                    overflow: 'auto',
                    padding: 16,
                    background: token.colorBgLayout,
                }}
            >
                <Outlet />
            </Content>
            <AppFooter />
        </Layout>
    )
}
