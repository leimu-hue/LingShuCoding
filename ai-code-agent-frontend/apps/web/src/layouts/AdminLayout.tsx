import {
  AppstoreOutlined,
  CommentOutlined,
  DeploymentUnitOutlined,
  MenuFoldOutlined,
  MenuUnfoldOutlined,
} from '@ant-design/icons'
import { Layout, Menu, Typography } from 'antd'
import { Outlet, useLocation, useNavigate } from 'react-router-dom'
import { useAppShellStore } from '../store/appShell'

const { Header, Sider, Content } = Layout

const menuItems = [
  { key: '/chat', icon: <CommentOutlined />, label: 'AI 对话生成' },
  { key: '/console', icon: <AppstoreOutlined />, label: '应用管理' },
  { key: '/editor', icon: <DeploymentUnitOutlined />, label: '可视化编辑器' },
]

export default function AdminLayout() {
  const collapsed = useAppShellStore((state) => state.collapsed)
  const toggleCollapsed = useAppShellStore((state) => state.toggleCollapsed)
  const navigate = useNavigate()
  const location = useLocation()

  return (
    <Layout style={{ height: '100%' }}>
      <Sider trigger={null} collapsible collapsed={collapsed} width={220}>
        <div style={{ height: 48, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <Typography.Text strong style={{ color: '#fff', whiteSpace: 'nowrap' }}>
            {collapsed ? 'AI' : 'AI 零代码平台'}
          </Typography.Text>
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={[location.pathname]}
          items={menuItems}
          onClick={({ key }) => navigate(key)}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            padding: '0 16px',
            background: '#fff',
            display: 'flex',
            alignItems: 'center',
            gap: 12,
          }}
        >
          <span onClick={toggleCollapsed} style={{ cursor: 'pointer', fontSize: 16 }}>
            {collapsed ? <MenuUnfoldOutlined /> : <MenuFoldOutlined />}
          </span>
          <Typography.Text type="secondary">AI 零代码应用生成平台</Typography.Text>
        </Header>
        <Content style={{ margin: 16, background: '#fff', padding: 16, borderRadius: 8, overflow: 'auto' }}>
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  )
}