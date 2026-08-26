import { lazy, Suspense, type ReactNode } from 'react'
import { Spin } from 'antd'
import { createBrowserRouter, Navigate } from 'react-router-dom'
import AdminLayout from '@/layouts/AdminLayout'
import FrontLayout from '@/layouts/FrontLayout'
import { RequireAdmin, RequireAuth } from '@/router/guards'

// 路由级懒加载：仅在使用时按需加载对应页面，
// 重型依赖（如编辑器页的 @xyflow/react）因此不会进入首屏包。
const ChatPage = lazy(() => import('@/pages/chat/ChatPage'))
const ConsolePage = lazy(() => import('@/pages/console/ConsolePage'))
const EditorPage = lazy(() => import('@/pages/editor/EditorPage'))
const LoginPage = lazy(() => import('@/pages/auth/LoginPage'))
const RegisterPage = lazy(() => import('@/pages/auth/RegisterPage'))
const UserManagePage = lazy(() => import('@/pages/admin/UserManagePage'))

function lazyPage(node: ReactNode) {
    return (
        <Suspense
            fallback={
                <div style={{ display: 'flex', justifyContent: 'center', padding: 48 }}>
                    <Spin />
                </div>
            }
        >
            {node}
        </Suspense>
    )
}

// 后台管理界面路由：均为管理员功能，仅挂载在 /admin 下
const adminRoutes = [
    { path: 'chat', element: lazyPage(<ChatPage />) },
    { path: 'console', element: lazyPage(<ConsolePage />) },
    { path: 'editor', element: lazyPage(<EditorPage />) },
    { path: 'users', element: lazyPage(<UserManagePage />) },
]

export const router = createBrowserRouter([
    { path: '/login', element: lazyPage(<LoginPage />) },
    { path: '/register', element: lazyPage(<RegisterPage />) },
    {
        // 前台界面：顶部导航 + 路由内容区 + 底部页脚（仅普通用户功能）
        path: '/',
        element: (
            <RequireAuth>
                <FrontLayout />
            </RequireAuth>
        ),
        children: [
            { index: true, element: <Navigate to="/chat" replace /> },
            { path: 'chat', element: lazyPage(<ChatPage />) },
        ],
    },
    {
        // 后台管理界面：侧边栏布局（需管理员角色）
        path: '/admin',
        element: (
            <RequireAuth>
                <RequireAdmin>
                    <AdminLayout />
                </RequireAdmin>
            </RequireAuth>
        ),
        children: [{ index: true, element: <Navigate to="/admin/chat" replace /> }, ...adminRoutes],
    },
])
